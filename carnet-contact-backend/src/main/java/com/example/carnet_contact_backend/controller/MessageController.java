package com.example.carnet_contact_backend.controller;

import com.example.carnet_contact_backend.model.Message;
import com.example.carnet_contact_backend.model.Reaction;
import com.example.carnet_contact_backend.model.Utilisateur;
import com.example.carnet_contact_backend.repository.MessageRepository;
import com.example.carnet_contact_backend.repository.ReactionRepository;
import com.example.carnet_contact_backend.repository.UtilisateurRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Messagerie interne : les comptes de l'application s'écrivent entre eux.
 */
@RestController
@RequestMapping("/api/messages")
public class MessageController {

    /**
     * Les seules réactions acceptées.
     *
     * Pourquoi une liste fermée plutôt que « n'importe quel emoji » ? Deux
     * raisons. D'abord la validation : « est-ce bien un emoji ? » est une
     * question étonnamment difficile (séquences composées, modificateurs de
     * teinte, drapeaux), alors que « est-ce dans cette liste ? » est trivial.
     * Ensuite l'affichage : une barre de cinq boutons se dessine et se compte,
     * là où un champ libre produirait autant de colonnes que d'emojis existants.
     *
     * Le serveur ne fait pas confiance au client sur ce point : la barre
     * Angular propose ces cinq-là, mais rien n'empêche d'appeler l'API à la
     * main avec autre chose.
     */
    public static final List<String> EMOJIS_AUTORISES = List.of("👍", "❤️", "😂", "😮", "😢");

    private static final Set<String> EMOJIS_VALIDES = Set.copyOf(EMOJIS_AUTORISES);

    private final MessageRepository messageRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final ReactionRepository reactionRepository;

    public MessageController(
            MessageRepository messageRepository,
            UtilisateurRepository utilisateurRepository,
            ReactionRepository reactionRepository) {
        this.messageRepository = messageRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.reactionRepository = reactionRepository;
    }

    public record DemandeMessage(Long destinataireId, String contenu) {}

    public record DemandeReaction(String emoji) {}

    /**
     * Les réactions d'un message, REGROUPÉES par emoji.
     *
     * Le client n'a pas besoin de la liste nominative : il affiche « 👍 3 ».
     * En revanche il a besoin de `parMoi`, pour mettre en évidence le bouton
     * sur lequel on a déjà cliqué — une information qui dépend de qui regarde,
     * et que le serveur est donc le mieux placé pour calculer.
     */
    public record ReactionResume(String emoji, long nombre, boolean parMoi) {}

    /**
     * Ce qu'on renvoie pour un message.
     *
     * Un DTO plutôt que l'entité : les réactions ne sont pas un champ de
     * `Message` (elles vivent dans leur propre table), et `parMoi` n'existe
     * nulle part en base — c'est une lecture relative au demandeur.
     */
    public record MessageVu(
            Long id,
            Utilisateur expediteur,
            Utilisateur destinataire,
            String contenu,
            Instant dateEnvoi,
            boolean lu,
            List<ReactionResume> reactions) {}

    private Utilisateur utilisateurConnecte(String email) {
        return utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    /**
     * Assemble les DTO d'un lot de messages.
     *
     * Les réactions sont chargées en UNE requête pour tout le lot, puis
     * réparties en mémoire. Interroger la base une fois par message ferait
     * cinquante allers-retours pour un fil de cinquante messages — le
     * classique problème « N+1 ».
     */
    private List<MessageVu> assembler(List<Message> messages, Long moiId) {
        if (messages.isEmpty()) {
            return List.of();
        }

        List<Reaction> reactions = reactionRepository.findByMessageIdIn(
                messages.stream().map(Message::getId).toList());

        return messages.stream()
                .map(message -> new MessageVu(
                        message.getId(),
                        message.getExpediteur(),
                        message.getDestinataire(),
                        message.getContenu(),
                        message.getDateEnvoi(),
                        message.isLu(),
                        resumer(reactions, message.getId(), moiId)))
                .toList();
    }

    private List<ReactionResume> resumer(List<Reaction> toutes, Long messageId, Long moiId) {
        // LinkedHashMap : conserve l'ordre d'insertion, donc l'ordre d'affichage
        // reste stable d'un rafraîchissement à l'autre. Une HashMap ordinaire
        // ferait sauter les emojis de place à chaque sondage.
        Map<String, long[]> parEmoji = new LinkedHashMap<>();

        toutes.stream()
                .filter(r -> r.getMessage().getId().equals(messageId))
                .sorted(Comparator.comparing(Reaction::getId))
                .forEach(r -> {
                    long[] compte = parEmoji.computeIfAbsent(r.getEmoji(), c -> new long[2]);
                    compte[0]++;
                    if (r.getUtilisateur().getId().equals(moiId)) {
                        compte[1] = 1;
                    }
                });

        return parEmoji.entrySet().stream()
                .map(e -> new ReactionResume(e.getKey(), e.getValue()[0], e.getValue()[1] == 1))
                .toList();
    }

    /**
     * Le fil complet avec un interlocuteur. L'ouverture du fil marque au
     * passage comme lus les messages reçus de cette personne.
     */
    @GetMapping("/{autreId}")
    public List<MessageVu> conversation(
            @PathVariable Long autreId,
            @AuthenticationPrincipal String email) {
        Utilisateur moi = utilisateurConnecte(email);

        List<Message> aMarquer =
                messageRepository.findByDestinataireIdAndExpediteurIdAndLuFalse(moi.getId(), autreId);
        if (!aMarquer.isEmpty()) {
            aMarquer.forEach(m -> m.setLu(true));
            messageRepository.saveAll(aMarquer);
        }

        return assembler(messageRepository.conversation(moi.getId(), autreId), moi.getId());
    }

    @PostMapping
    public MessageVu envoyer(
            @RequestBody DemandeMessage demande,
            @AuthenticationPrincipal String email) {
        if (demande.contenu() == null || demande.contenu().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message vide.");
        }

        Utilisateur expediteur = utilisateurConnecte(email);
        Utilisateur destinataire = utilisateurRepository.findById(demande.destinataireId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Destinataire inconnu."));

        Message message = new Message();
        // L'expéditeur vient du jeton, jamais du corps de la requête : on ne
        // peut pas écrire un message en se faisant passer pour quelqu'un.
        message.setExpediteur(expediteur);
        message.setDestinataire(destinataire);
        message.setContenu(demande.contenu());
        message.setDateEnvoi(Instant.now());
        message.setLu(false);

        Message cree = messageRepository.save(message);

        // Un message neuf n'a évidemment aucune réaction : inutile d'aller le
        // demander à la base.
        return new MessageVu(cree.getId(), expediteur, destinataire, cree.getContenu(),
                cree.getDateEnvoi(), cree.isLu(), List.of());
    }

    /** Tous les messages reçus non lus, pour la pastille du menu. */
    @GetMapping("/non-lus")
    public List<MessageVu> nonLus(@AuthenticationPrincipal String email) {
        Utilisateur moi = utilisateurConnecte(email);
        return assembler(
                messageRepository.findByDestinataireIdAndLuFalse(moi.getId()), moi.getId());
    }

    /**
     * Pose, remplace ou retire sa réaction sur un message.
     *
     * Un seul point d'entrée pour les trois cas, parce que du point de vue de
     * l'utilisateur il n'y a qu'une action : « cliquer sur un emoji ». Cliquer
     * sur celui qu'on avait déjà choisi le retire, cliquer sur un autre le
     * remplace. Trois routes auraient obligé le client à savoir dans quel état
     * il se trouve avant d'agir.
     *
     * PUT et non POST : l'opération est IDEMPOTENTE dans son résultat — quel
     * que soit l'état de départ, on finit avec exactement une réaction (ou
     * aucune) de cette personne sur ce message.
     */
    @PutMapping("/{messageId}/reaction")
    public MessageVu reagir(
            @PathVariable Long messageId,
            @RequestBody DemandeReaction demande,
            @AuthenticationPrincipal String email) {

        if (demande.emoji() == null || !EMOJIS_VALIDES.contains(demande.emoji())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Emoji non autorisé.");
        }

        Utilisateur moi = utilisateurConnecte(email);
        Message message = messageAccessible(messageId, moi);

        reactionRepository.findByMessageIdAndUtilisateurId(messageId, moi.getId())
                .ifPresentOrElse(
                        existante -> {
                            if (existante.getEmoji().equals(demande.emoji())) {
                                // Même emoji : on retire.
                                reactionRepository.delete(existante);
                            } else {
                                // Autre emoji : on remplace.
                                existante.setEmoji(demande.emoji());
                                reactionRepository.save(existante);
                            }
                        },
                        () -> {
                            Reaction nouvelle = new Reaction();
                            nouvelle.setMessage(message);
                            nouvelle.setUtilisateur(moi);
                            nouvelle.setEmoji(demande.emoji());
                            reactionRepository.save(nouvelle);
                        });

        return assembler(List.of(message), moi.getId()).getFirst();
    }

    /**
     * On ne peut réagir qu'à un message de SA propre conversation.
     *
     * Sans cette vérification, connaître un identifiant suffirait à réagir au
     * message privé de deux inconnus — et à en découvrir l'existence.
     */
    private Message messageAccessible(Long messageId, Utilisateur moi) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        boolean leMien = message.getExpediteur().getId().equals(moi.getId())
                || message.getDestinataire().getId().equals(moi.getId());

        if (!leMien) {
            // 404 et non 403 : un 403 confirmerait que ce message existe.
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        return message;
    }

    /** Les emojis que le client peut proposer. */
    @GetMapping("/emojis")
    public ResponseEntity<List<String>> emojis() {
        return ResponseEntity.ok(EMOJIS_AUTORISES);
    }
}

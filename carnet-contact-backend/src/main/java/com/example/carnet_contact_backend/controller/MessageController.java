package com.example.carnet_contact_backend.controller;

import com.example.carnet_contact_backend.controller.Reactions.ReactionResume;
import com.example.carnet_contact_backend.model.Message;
import com.example.carnet_contact_backend.model.Reaction;
import com.example.carnet_contact_backend.model.Utilisateur;
import com.example.carnet_contact_backend.repository.MessageRepository;
import com.example.carnet_contact_backend.repository.ReactionRepository;
import com.example.carnet_contact_backend.repository.UtilisateurRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Messagerie interne : les comptes de l'application s'écrivent entre eux.
 */
@RestController
@RequestMapping("/api/messages")
public class MessageController {

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

    /**
     * Le corps d'un envoi, avec ses règles.
     *
     * @Valid, sur le paramètre de `envoyer`, demande à Spring de vérifier ces
     * annotations AVANT d'appeler la méthode. Un contenu vide, trop long ou un
     * destinataire absent produisent un 400 sans qu'une ligne de ce contrôleur
     * ne s'exécute. Auparavant, seul le contenu vide était testé à la main : un
     * message de 2001 caractères atteignait la base et ressortait en erreur 500.
     */
    public record DemandeMessage(
            @NotNull(message = "Destinataire requis.")
            Long destinataireId,

            @NotBlank(message = "Message vide.")
            @Size(max = 2000, message = "Message trop long (2000 caractères au plus).")
            String contenu) {}

    public record DemandeReaction(String emoji) {}

    /**
     * Ce qu'on renvoie pour un message.
     *
     * Un DTO plutôt que l'entité : les réactions ne sont pas un champ de
     * `Message` (elles vivent dans leur propre table), et `parMoi` n'existe
     * nulle part en base — c'est une lecture relative au demandeur.
     *
     * Expéditeur et destinataire sont des AuteurPublic : le fil n'a besoin que
     * d'un nom et d'une photo, pas de l'email ni du rôle de l'autre personne.
     */
    public record MessageVu(
            Long id,
            AuteurPublic expediteur,
            AuteurPublic destinataire,
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
     * classique problème « N+1 ». Le regroupement lui-même est partagé avec les
     * publications du fil (classe Reactions).
     */
    private List<MessageVu> assembler(List<Message> messages, Long moiId) {
        if (messages.isEmpty()) {
            return List.of();
        }

        Map<Long, List<ReactionResume>> reactions = Reactions.resumerParCible(
                reactionRepository.findByMessageIdIn(messages.stream().map(Message::getId).toList()),
                moiId);

        return messages.stream()
                .map(message -> vue(message, reactions.getOrDefault(message.getId(), List.of())))
                .toList();
    }

    private MessageVu vue(Message message, List<ReactionResume> reactions) {
        return new MessageVu(
                message.getId(),
                AuteurPublic.de(message.getExpediteur()),
                AuteurPublic.de(message.getDestinataire()),
                message.getContenu(),
                message.getDateEnvoi(),
                message.isLu(),
                reactions);
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
            @Valid @RequestBody DemandeMessage demande,
            @AuthenticationPrincipal String email) {
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

        // Un message neuf n'a évidemment aucune réaction : inutile d'aller le
        // demander à la base.
        return vue(messageRepository.save(message), List.of());
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

        if (!Reactions.estAutorise(demande.emoji())) {
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
        return ResponseEntity.ok(Reactions.EMOJIS_AUTORISES);
    }
}

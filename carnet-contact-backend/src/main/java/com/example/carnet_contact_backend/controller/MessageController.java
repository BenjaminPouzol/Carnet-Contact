package com.example.carnet_contact_backend.controller;

import com.example.carnet_contact_backend.model.Message;
import com.example.carnet_contact_backend.model.Utilisateur;
import com.example.carnet_contact_backend.repository.MessageRepository;
import com.example.carnet_contact_backend.repository.UtilisateurRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

/**
 * Messagerie interne : les comptes de l'application s'écrivent entre eux.
 */
@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageRepository messageRepository;
    private final UtilisateurRepository utilisateurRepository;

    public MessageController(
            MessageRepository messageRepository,
            UtilisateurRepository utilisateurRepository) {
        this.messageRepository = messageRepository;
        this.utilisateurRepository = utilisateurRepository;
    }

    public record DemandeMessage(Long destinataireId, String contenu) {}

    private Utilisateur utilisateurConnecte(String email) {
        return utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    /**
     * Le fil complet avec un interlocuteur. L'ouverture du fil marque au
     * passage comme lus les messages reçus de cette personne.
     */
    @GetMapping("/{autreId}")
    public List<Message> conversation(
            @PathVariable Long autreId,
            @AuthenticationPrincipal String email) {
        Utilisateur moi = utilisateurConnecte(email);

        List<Message> aMarquer =
                messageRepository.findByDestinataireIdAndExpediteurIdAndLuFalse(moi.getId(), autreId);
        if (!aMarquer.isEmpty()) {
            aMarquer.forEach(m -> m.setLu(true));
            messageRepository.saveAll(aMarquer);
        }

        return messageRepository.conversation(moi.getId(), autreId);
    }

    @PostMapping
    public Message envoyer(
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

        return messageRepository.save(message);
    }

    /** Tous les messages reçus non lus, pour la pastille du menu. */
    @GetMapping("/non-lus")
    public List<Message> nonLus(@AuthenticationPrincipal String email) {
        return messageRepository.findByDestinataireIdAndLuFalse(
                utilisateurConnecte(email).getId());
    }
}

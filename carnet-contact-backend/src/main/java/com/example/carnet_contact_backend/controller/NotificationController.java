package com.example.carnet_contact_backend.controller;

import com.example.carnet_contact_backend.abonnement.Relations;
import com.example.carnet_contact_backend.abonnement.VueRelations;
import com.example.carnet_contact_backend.model.Notification;
import com.example.carnet_contact_backend.model.TypeNotification;
import com.example.carnet_contact_backend.model.Utilisateur;
import com.example.carnet_contact_backend.repository.NotificationRepository;
import com.example.carnet_contact_backend.repository.UtilisateurRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

/**
 * Les notifications d'activité : nouveaux abonnés, demandes, acceptations.
 *
 * `/non-lues` est la route sondée toutes les quinze secondes par le client ;
 * `GET /` rend l'historique pour la page Abonnements, qui marque ensuite tout
 * comme lu.
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    /** L'historique affiché : au-delà, personne ne remonte si loin. */
    private static final int TAILLE_HISTORIQUE = 30;

    private final UtilisateurRepository utilisateurRepository;
    private final NotificationRepository notificationRepository;
    private final Relations relations;

    public NotificationController(
            UtilisateurRepository utilisateurRepository,
            NotificationRepository notificationRepository,
            Relations relations) {
        this.utilisateurRepository = utilisateurRepository;
        this.notificationRepository = notificationRepository;
        this.relations = relations;
    }

    /** L'acteur en AuteurPublic : un nom et une photo, pas son email. */
    public record NotificationVue(
            Long id,
            TypeNotification type,
            AuteurPublic acteur,
            Instant date,
            boolean lue) {}

    private Utilisateur utilisateurConnecte(String email) {
        return utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    @GetMapping
    public List<NotificationVue> recentes(@AuthenticationPrincipal String email) {
        Utilisateur moi = utilisateurConnecte(email);
        return vues(relations.vuePour(moi),
                notificationRepository.recentesPour(moi.getId(), PageRequest.of(0, TAILLE_HISTORIQUE)));
    }

    @GetMapping("/non-lues")
    public List<NotificationVue> nonLues(@AuthenticationPrincipal String email) {
        Utilisateur moi = utilisateurConnecte(email);
        return vues(relations.vuePour(moi), notificationRepository.nonLuesPour(moi.getId()));
    }

    @PutMapping("/lues")
    @Transactional
    public ResponseEntity<Void> marquerLues(@AuthenticationPrincipal String email) {
        Utilisateur moi = utilisateurConnecte(email);
        notificationRepository.marquerLues(moi.getId());
        return ResponseEntity.noContent().build();
    }

    /**
     * Écarte les notifications dont l'acteur a été désactivé, ou avec qui une
     * relation de blocage existe. Un blocage les supprime déjà en base ; ce
     * filtre couvre en plus la désactivation, qui, elle, n'efface rien.
     *
     * La vue est chargée une fois pour toute la liste : pas de requête par
     * notification.
     */
    private List<NotificationVue> vues(VueRelations vue, List<Notification> notifications) {
        return notifications.stream()
                .filter(n -> n.getActeur().isActif() && !vue.bloque(n.getActeur().getId()))
                .map(n -> new NotificationVue(
                        n.getId(), n.getType(), AuteurPublic.de(n.getActeur()), n.getDate(), n.isLue()))
                .toList();
    }
}

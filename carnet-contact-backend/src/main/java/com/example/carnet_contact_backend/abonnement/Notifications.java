package com.example.carnet_contact_backend.abonnement;

import com.example.carnet_contact_backend.model.Notification;
import com.example.carnet_contact_backend.model.TypeNotification;
import com.example.carnet_contact_backend.model.Utilisateur;
import com.example.carnet_contact_backend.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Créer et retirer des notifications.
 *
 * Un service plutôt que des appels directs au dépôt dans chaque contrôleur :
 * « une notification neuve est non lue et datée de maintenant » s'écrit ainsi
 * une seule fois.
 *
 * @Transactional sur les méthodes : elles sont appelées DEPUIS L'EXTÉRIEUR
 * (les contrôleurs), donc à travers le proxy de Spring — l'annotation est bien
 * appliquée. Appelées dans une transaction déjà ouverte, elles la rejoignent.
 */
@Service
public class Notifications {

    private final NotificationRepository notificationRepository;

    public Notifications(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public void notifier(Utilisateur destinataire, Utilisateur acteur, TypeNotification type) {
        Notification notification = new Notification();
        notification.setDestinataire(destinataire);
        notification.setActeur(acteur);
        notification.setType(type);
        notification.setDate(Instant.now());
        notification.setLue(false);
        notificationRepository.save(notification);
    }

    /** Retire les notifications de ces types, envoyées par `acteur` à `destinataire`. */
    @Transactional
    public void retirer(Long destinataireId, Long acteurId, TypeNotification... types) {
        notificationRepository.supprimer(destinataireId, acteurId, List.of(types));
    }

    /** Toutes les notifications échangées entre deux comptes, dans les deux sens. */
    @Transactional
    public void retirerEntre(Long a, Long b) {
        notificationRepository.supprimerEntre(a, b);
    }
}

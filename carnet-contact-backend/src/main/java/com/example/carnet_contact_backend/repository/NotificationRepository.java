package com.example.carnet_contact_backend.repository;

import com.example.carnet_contact_backend.model.Notification;
import com.example.carnet_contact_backend.model.TypeNotification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // L'acteur arrive avec la notification (JOIN FETCH) : son nom est affiché.
    // n.id DESC départage deux notifications de la même milliseconde.
    @Query("SELECT n FROM Notification n JOIN FETCH n.acteur "
            + "WHERE n.destinataire.id = :id ORDER BY n.date DESC, n.id DESC")
    List<Notification> recentesPour(@Param("id") Long id, Pageable limite);

    @Query("SELECT n FROM Notification n JOIN FETCH n.acteur "
            + "WHERE n.destinataire.id = :id AND n.lue = false ORDER BY n.date DESC, n.id DESC")
    List<Notification> nonLuesPour(@Param("id") Long id);

    // Une seule requête UPDATE pour tout marquer, au lieu de charger chaque
    // notification, changer son drapeau et l'enregistrer.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Notification n SET n.lue = true WHERE n.destinataire.id = :id AND n.lue = false")
    int marquerLues(@Param("id") Long id);

    /**
     * Retire les notifications devenues fausses : « Alice vous suit » n'a plus
     * lieu d'être quand Alice ne suit plus. IN :types accepte une collection —
     * une seule requête pour plusieurs types.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM Notification n WHERE n.destinataire.id = :destinataire "
            + "AND n.acteur.id = :acteur AND n.type IN :types")
    void supprimer(
            @Param("destinataire") Long destinataire,
            @Param("acteur") Long acteur,
            @Param("types") Collection<TypeNotification> types);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM Notification n WHERE (n.destinataire.id = :a AND n.acteur.id = :b) "
            + "OR (n.destinataire.id = :b AND n.acteur.id = :a)")
    void supprimerEntre(@Param("a") Long a, @Param("b") Long b);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM Notification n WHERE n.destinataire.id = :id OR n.acteur.id = :id")
    void supprimerCellesDe(@Param("id") Long id);
}

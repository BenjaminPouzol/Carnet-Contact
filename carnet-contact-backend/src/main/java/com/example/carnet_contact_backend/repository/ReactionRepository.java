package com.example.carnet_contact_backend.repository;

import com.example.carnet_contact_backend.model.Reaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReactionRepository extends JpaRepository<Reaction, Long> {

    /**
     * Toutes les réactions d'un lot de messages, en UNE requête.
     *
     * `In` traduit un `WHERE message_id IN (…)`. C'est la parade au problème
     * dit « N+1 » : interroger la base une fois par message d'un fil de
     * cinquante messages ferait cinquante allers-retours là où un seul suffit.
     */
    List<Reaction> findByMessageIdIn(List<Long> messageIds);

    /** La réaction d'une personne sur un message — au plus une, par contrainte. */
    Optional<Reaction> findByMessageIdAndUtilisateurId(Long messageId, Long utilisateurId);

    @Modifying
    @Query("DELETE FROM Reaction r WHERE r.utilisateur.id = :utilisateurId")
    void supprimerCellesDe(@Param("utilisateurId") Long utilisateurId);

    @Modifying
    @Query("DELETE FROM Reaction r WHERE r.message.id IN "
            + "(SELECT m.id FROM Message m WHERE m.expediteur.id = :utilisateurId "
            + "OR m.destinataire.id = :utilisateurId)")
    void supprimerCellesDesMessagesDe(@Param("utilisateurId") Long utilisateurId);
}

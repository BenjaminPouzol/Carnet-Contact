package com.example.carnet_contact_backend.repository;

import com.example.carnet_contact_backend.model.ReactionPublication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReactionPublicationRepository extends JpaRepository<ReactionPublication, Long> {

    /** Toutes les réactions d'une page du fil, en UNE requête (parade au N+1). */
    List<ReactionPublication> findByPublicationIdIn(List<Long> publicationIds);

    /** La réaction d'une personne sur une publication — au plus une, par contrainte. */
    Optional<ReactionPublication> findByPublicationIdAndUtilisateurId(Long publicationId, Long utilisateurId);

    /**
     * Une suppression EN MASSE : un seul DELETE envoyé à la base, sans charger
     * les réactions une par une.
     *
     * Le revers : Hibernate n'en est pas prévenu. Une réaction déjà chargée en
     * mémoire (dans le « contexte de persistance ») y reste, bien vivante à ses
     * yeux, alors que sa ligne a disparu. Supprimer ensuite la publication
     * qu'elle pointe fait échouer l'écriture suivante (TransientPropertyValueException).
     *
     * flushAutomatically : écrire d'abord ce qui est en attente, pour que le
     * DELETE voie un état à jour. clearAutomatically : vider le contexte après,
     * pour que plus aucun objet périmé ne traîne en mémoire.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM ReactionPublication r WHERE r.publication.id = :publicationId")
    void supprimerCellesDeLaPublication(@Param("publicationId") Long publicationId);

    // À la suppression d'un compte : ses réactions sur les publications des autres…
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM ReactionPublication r WHERE r.utilisateur.id = :utilisateurId")
    void supprimerCellesDe(@Param("utilisateurId") Long utilisateurId);

    // … et les réactions des autres sur SES publications.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM ReactionPublication r WHERE r.publication.id IN "
            + "(SELECT p.id FROM Publication p WHERE p.auteur.id = :utilisateurId)")
    void supprimerCellesDesPublicationsDe(@Param("utilisateurId") Long utilisateurId);
}

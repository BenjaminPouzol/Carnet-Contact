package com.example.carnet_contact_backend.repository;

import com.example.carnet_contact_backend.model.Abonnement;
import com.example.carnet_contact_backend.model.StatutAbonnement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AbonnementRepository extends JpaRepository<Abonnement, Long> {

    Optional<Abonnement> findByAbonneIdAndSuiviId(Long abonneId, Long suiviId);

    // Les compteurs du profil public : seuls les abonnements ACCEPTÉS comptent.
    long countBySuiviIdAndStatut(Long suiviId, StatutAbonnement statut);

    long countByAbonneIdAndStatut(Long abonneId, StatutAbonnement statut);

    /**
     * Mes abonnements, réduits à (identifiant suivi, statut).
     *
     * Une PROJECTION : on ne charge pas les entités entières, seulement les deux
     * colonnes utiles. Chaque ligne du résultat est un tableau
     * [Long, StatutAbonnement] — c'est ce que Relations range dans une Map.
     */
    @Query("SELECT a.suivi.id, a.statut FROM Abonnement a WHERE a.abonne.id = :id")
    List<Object[]> statutsSortants(@Param("id") Long id);

    @Query("SELECT a.abonne.id FROM Abonnement a WHERE a.suivi.id = :id AND a.statut = :statut")
    List<Long> idsAbonnes(@Param("id") Long id, @Param("statut") StatutAbonnement statut);

    // JOIN FETCH : le compte suivi arrive avec l'abonnement, en une requête pour
    // toute la liste (pas une requête par ligne).
    @Query("SELECT a FROM Abonnement a JOIN FETCH a.suivi WHERE a.abonne.id = :id")
    List<Abonnement> sortantsDe(@Param("id") Long id);

    @Query("SELECT a FROM Abonnement a JOIN FETCH a.abonne WHERE a.suivi.id = :id AND a.statut = :statut")
    List<Abonnement> entrantsDe(@Param("id") Long id, @Param("statut") StatutAbonnement statut);

    /**
     * Suggestions : les comptes suivis par ceux que je suis, avec le nombre de
     * « ponts » qui y mènent.
     *
     * Deux fois la même table dans le FROM (a1, a2) : a1 part de moi, a2 part
     * de chaque compte atteint par a1. GROUP BY regroupe les lignes par compte
     * d'arrivée, COUNT compte combien de chemins y mènent — trois de mes
     * abonnements qui suivent Dave font de Dave une meilleure suggestion qu'un
     * compte suivi par un seul d'entre eux.
     */
    @Query("""
            SELECT a2.suivi.id, COUNT(a2)
            FROM Abonnement a1, Abonnement a2
            WHERE a1.abonne.id = :moi AND a1.statut = :accepte
              AND a2.abonne.id = a1.suivi.id AND a2.statut = :accepte
            GROUP BY a2.suivi.id
            ORDER BY COUNT(a2) DESC
            """)
    List<Object[]> suivisParMesAbonnements(
            @Param("moi") Long moi, @Param("accepte") StatutAbonnement accepte);

    @Query("""
            SELECT a.suivi.id, COUNT(a)
            FROM Abonnement a
            WHERE a.statut = :accepte
            GROUP BY a.suivi.id
            ORDER BY COUNT(a) DESC
            """)
    List<Object[]> lesPlusSuivis(@Param("accepte") StatutAbonnement accepte);

    // Au blocage : plus aucun abonnement entre les deux comptes, dans aucun sens.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM Abonnement a WHERE (a.abonne.id = :a AND a.suivi.id = :b) "
            + "OR (a.abonne.id = :b AND a.suivi.id = :a)")
    void supprimerEntre(@Param("a") Long a, @Param("b") Long b);

    // À la suppression d'un compte : ses abonnements et ceux qui le visent.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM Abonnement a WHERE a.abonne.id = :id OR a.suivi.id = :id")
    void supprimerCeuxDe(@Param("id") Long id);
}

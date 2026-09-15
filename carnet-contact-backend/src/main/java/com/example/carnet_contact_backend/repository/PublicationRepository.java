package com.example.carnet_contact_backend.repository;

import com.example.carnet_contact_backend.model.Categorie;
import com.example.carnet_contact_backend.model.Publication;
import com.example.carnet_contact_backend.model.StatutAbonnement;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PublicationRepository extends JpaRepository<Publication, Long> {

    /**
     * Une tranche du fil, par CURSEUR : les publications plus anciennes que
     * `avant` (toutes si `avant` est null), filtrées si demandé.
     *
     * Tri par id décroissant plutôt que par date : l'id est croissant et
     * unique, alors que deux dates peuvent être égales — et un curseur ambigu
     * ferait sauter ou répéter une publication à la frontière de deux pages.
     *
     * `(:categorie IS NULL OR …)` : un paramètre absent désactive le filtre.
     * Une seule requête couvre ainsi « tout le fil », « une catégorie », « mes
     * abonnements » et « un auteur », comme la recherche vide des contacts
     * (section 22).
     *
     * Les deux dernières conditions ne sont pas des filtres mais des RÈGLES,
     * toujours appliquées : pas de publication d'un compte en relation de
     * blocage, et un compte privé n'est lu que par lui-même, un administrateur
     * ou un abonné accepté. Ce sont les règles de VueRelations.voitContenu,
     * réécrites en JPQL — et c'est volontaire : filtrer en Java APRÈS la requête
     * casserait le curseur (une tranche de 10 pourrait revenir avec 3 lignes,
     * alors que d'autres publications visibles existent plus loin).
     *
     * Retour en List et non en Page : Spring Data n'exécute alors pas la
     * requête COUNT qui accompagne une Page. Le curseur n'a pas besoin du total.
     * Le Pageable ne sert qu'à limiter le nombre de lignes.
     */
    @Query("""
            SELECT p FROM Publication p
            JOIN FETCH p.auteur a
            WHERE (:categorie IS NULL OR p.categorie = :categorie)
              AND (:avant IS NULL OR p.id < :avant)
              AND (:auteurId IS NULL OR a.id = :auteurId)
              AND (:seulementAbonnements = false OR EXISTS (
                    SELECT ab.id FROM Abonnement ab
                    WHERE ab.abonne.id = :moi AND ab.suivi.id = a.id AND ab.statut = :accepte))
              AND NOT EXISTS (
                    SELECT b.id FROM Blocage b
                    WHERE (b.bloqueur.id = :moi AND b.bloque.id = a.id)
                       OR (b.bloqueur.id = a.id AND b.bloque.id = :moi))
              AND (a.comptePrive = false OR a.id = :moi OR :admin = true OR EXISTS (
                    SELECT ab2.id FROM Abonnement ab2
                    WHERE ab2.abonne.id = :moi AND ab2.suivi.id = a.id AND ab2.statut = :accepte))
            ORDER BY p.id DESC
            """)
    List<Publication> fil(
            @Param("categorie") Categorie categorie,
            @Param("avant") Long avant,
            @Param("auteurId") Long auteurId,
            @Param("seulementAbonnements") boolean seulementAbonnements,
            @Param("moi") Long moi,
            @Param("admin") boolean admin,
            @Param("accepte") StatutAbonnement accepte,
            Pageable limite);

    // Pour le tableau d'administration : combien de publications par compte.
    long countByAuteurId(Long auteurId);

    // Suppression en masse : même précaution que dans ReactionPublicationRepository,
    // pour ne laisser aucune publication périmée dans le contexte de persistance.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM Publication p WHERE p.auteur.id = :auteurId")
    void supprimerCellesDe(@Param("auteurId") Long auteurId);
}

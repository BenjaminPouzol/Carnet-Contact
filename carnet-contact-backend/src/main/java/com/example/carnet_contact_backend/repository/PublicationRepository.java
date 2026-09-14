package com.example.carnet_contact_backend.repository;

import com.example.carnet_contact_backend.model.Categorie;
import com.example.carnet_contact_backend.model.Publication;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PublicationRepository extends JpaRepository<Publication, Long> {

    /**
     * Une tranche du fil, par CURSEUR : les publications plus anciennes que
     * `avant` (toutes si `avant` est null), filtrées par catégorie si demandé.
     *
     * Tri par id décroissant plutôt que par date : l'id est croissant et
     * unique, alors que deux dates peuvent être égales — et un curseur ambigu
     * ferait sauter ou répéter une publication à la frontière de deux pages.
     *
     * `(:categorie IS NULL OR …)` : un paramètre absent désactive le filtre.
     * Une seule requête couvre ainsi « tout le fil » et « une catégorie », comme
     * la recherche vide des contacts (section 22).
     *
     * Retour en List et non en Page : Spring Data n'exécute alors pas la
     * requête COUNT qui accompagne une Page. Le curseur n'a pas besoin du total.
     * Le Pageable ne sert qu'à limiter le nombre de lignes.
     */
    @Query("""
            SELECT p FROM Publication p
            JOIN FETCH p.auteur
            WHERE (:categorie IS NULL OR p.categorie = :categorie)
              AND (:avant IS NULL OR p.id < :avant)
            ORDER BY p.id DESC
            """)
    List<Publication> fil(
            @Param("categorie") Categorie categorie,
            @Param("avant") Long avant,
            Pageable limite);

    // Pour le tableau d'administration : combien de publications par compte.
    long countByAuteurId(Long auteurId);

    // Suppression en masse : même précaution que dans ReactionPublicationRepository,
    // pour ne laisser aucune publication périmée dans le contexte de persistance.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM Publication p WHERE p.auteur.id = :auteurId")
    void supprimerCellesDe(@Param("auteurId") Long auteurId);
}

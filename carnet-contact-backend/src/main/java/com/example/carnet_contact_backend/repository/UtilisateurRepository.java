package com.example.carnet_contact_backend.repository;

import com.example.carnet_contact_backend.model.Role;
import com.example.carnet_contact_backend.model.Utilisateur;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {

    // REQUÊTES DÉRIVÉES : Spring Data lit le NOM de la méthode et écrit le SQL
    // tout seul. findByEmail devient "SELECT ... WHERE email = ?".
    // Optional<> plutôt que null : le type dit explicitement que le résultat
    // peut être absent, et le compilateur force à traiter ce cas.
    Optional<Utilisateur> findByEmail(String email);

    boolean existsByEmail(String email);

    // "Not" dans le nom se traduit par "WHERE id <> ?" : tous les comptes
    // sauf celui passé en paramètre (pour lister ses interlocuteurs possibles).
    List<Utilisateur> findByIdNotOrderByNomAffichageAsc(Long id);

    // Idem, mais en excluant les comptes désactivés : on n'écrit pas à
    // quelqu'un qui ne peut plus se connecter.
    List<Utilisateur> findByIdNotAndActifTrueOrderByNomAffichageAsc(Long id);

    /**
     * Combien d'administrateurs actifs À PART celui-ci ?
     *
     * Sert au garde-fou du panel d'administration : tant que ce compte est
     * le dernier, on refuse de le désactiver, de le rétrograder ou de le
     * supprimer — sinon plus personne ne pourrait administrer l'application.
     */
    long countByRoleAndActifTrueAndIdNot(Role role, Long id);

    /**
     * Recherche de comptes à suivre, par nom affiché.
     *
     * Les comptes en relation de blocage sont exclus DANS la requête, et non
     * après coup en Java : la limite (20 résultats) s'applique ainsi à des
     * comptes réellement affichables. Filtrer après aurait pu rendre une page
     * de trois résultats alors que d'autres existaient plus loin.
     */
    @Query("""
            SELECT u FROM Utilisateur u
            WHERE u.id <> :moi AND u.actif = true
              AND LOWER(u.nomAffichage) LIKE LOWER(CONCAT('%', :terme, '%'))
              AND NOT EXISTS (
                    SELECT b.id FROM Blocage b
                    WHERE (b.bloqueur.id = :moi AND b.bloque.id = u.id)
                       OR (b.bloqueur.id = u.id AND b.bloque.id = :moi))
            ORDER BY u.nomAffichage
            """)
    List<Utilisateur> rechercher(@Param("moi") Long moi, @Param("terme") String terme, Pageable limite);

    // Dernier recours des suggestions, quand personne ne suit encore personne :
    // les inscrits les plus récents. Top20 : Spring Data ajoute la limite.
    List<Utilisateur> findTop20ByActifTrueAndIdNotOrderByDateInscriptionDesc(Long id);
}

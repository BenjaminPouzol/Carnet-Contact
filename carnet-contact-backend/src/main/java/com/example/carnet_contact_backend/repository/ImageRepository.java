package com.example.carnet_contact_backend.repository;

import com.example.carnet_contact_backend.model.Image;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

// JpaRepository<Image, String> : le second type est celui de l'identifiant,
// ici un UUID stocké en texte.
public interface ImageRepository extends JpaRepository<Image, String> {

    // Suppression en masse avec le compte : même précaution que les autres
    // dépôts, pour ne laisser aucune image périmée dans le contexte de persistance.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM Image i WHERE i.proprietaire.id = :utilisateurId")
    void supprimerCellesDe(@Param("utilisateurId") Long utilisateurId);

    /**
     * Supprime, en une seule requête, les images envoyées avant `limite`
     * qu'aucun des trois champs d'adresse ne référence.
     *
     * Les TROIS tables sont consultées, et pas seulement celle qui a changé :
     * la même adresse peut servir à plusieurs endroits (une photo de profil
     * recopiée dans un contact, par exemple).
     *
     * NOT EXISTS (sous-requête) : « il n'existe aucune ligne qui… ». LIKE
     * '%/api/images/<id>' compare la FIN de l'adresse, ce qui marche quel que
     * soit l'hôte (localhost:8080 en développement, un vrai domaine ailleurs).
     *
     * @Transactional est posé ICI, sur le dépôt, et pas sur le service : la
     * méthode planifiée appelle `nettoyer()` de l'intérieur de sa propre
     * classe, et un appel interne ne traverse pas le proxy qui ouvre les
     * transactions. Sans transaction, une requête @Modifying est refusée.
     */
    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            DELETE FROM Image i
            WHERE i.dateEnvoi < :limite
              AND NOT EXISTS (SELECT c.id FROM Contact c WHERE c.photoUrl LIKE CONCAT('%/api/images/', i.id))
              AND NOT EXISTS (SELECT u.id FROM Utilisateur u WHERE u.photoUrl LIKE CONCAT('%/api/images/', i.id))
              AND NOT EXISTS (SELECT p.id FROM Publication p WHERE p.imageUrl LIKE CONCAT('%/api/images/', i.id))
            """)
    int supprimerOrphelinesAvant(@Param("limite") Instant limite);
}

package com.example.carnet_contact_backend.repository;

import com.example.carnet_contact_backend.model.Image;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// JpaRepository<Image, String> : le second type est celui de l'identifiant,
// ici un UUID stocké en texte.
public interface ImageRepository extends JpaRepository<Image, String> {

    // Suppression en masse avec le compte : même précaution que les autres
    // dépôts, pour ne laisser aucune image périmée dans le contexte de persistance.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM Image i WHERE i.proprietaire.id = :utilisateurId")
    void supprimerCellesDe(@Param("utilisateurId") Long utilisateurId);
}

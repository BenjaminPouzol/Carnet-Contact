package com.example.carnet_contact_backend.repository;

import com.example.carnet_contact_backend.model.JetonRafraichissement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface JetonRafraichissementRepository extends JpaRepository<JetonRafraichissement, Long> {

    /**
     * Retrouver une ligne à partir de la valeur présentée par le client.
     *
     * C'est ce SELECT qui fait toute la différence avec le JWT. Vérifier un
     * JWT ne coûte rien (un calcul de signature) mais ne permet aucune
     * annulation ; vérifier un jeton opaque coûte un aller-retour en base,
     * mais la ligne peut être marquée révoquée à tout moment.
     */
    Optional<JetonRafraichissement> findByValeur(String valeur);

    /**
     * Révoque d'un coup tous les jetons d'un compte — « déconnecter de
     * partout ». Utilisé quand un administrateur désactive un compte : sans
     * cela, la session en cours continuerait de se renouveler indéfiniment.
     *
     * @Modifying est obligatoire dès qu'une @Query écrit : par défaut Spring
     * Data attend un SELECT et refuserait d'exécuter un UPDATE.
     */
    @Modifying
    @Query("UPDATE JetonRafraichissement j SET j.revoque = true WHERE j.utilisateur.id = :utilisateurId")
    void revoquerTousPour(@Param("utilisateurId") Long utilisateurId);

    /**
     * Supprime les lignes, au lieu de les marquer.
     *
     * Révoquer suffit d'ordinaire — une ligne révoquée garde une trace utile.
     * Mais à la suppression d'un compte, ces lignes pointent vers un
     * utilisateur qui n'existera plus : la clé étrangère refuserait de rester
     * orpheline.
     */
    @Modifying
    @Query("DELETE FROM JetonRafraichissement j WHERE j.utilisateur.id = :utilisateurId")
    void supprimerTousPour(@Param("utilisateurId") Long utilisateurId);
}

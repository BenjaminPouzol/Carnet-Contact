package com.example.carnet_contact_backend.repository;

import com.example.carnet_contact_backend.model.JetonRafraichissement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JetonRafraichissementRepository extends JpaRepository<JetonRafraichissement, Long> {

    /**
     * Le seul accès dont l'application ait besoin : retrouver une ligne à
     * partir de la valeur présentée par le client.
     *
     * C'est ce SELECT qui fait toute la différence avec le JWT. Vérifier un
     * JWT ne coûte rien (un calcul de signature) mais ne permet aucune
     * annulation ; vérifier un jeton opaque coûte un aller-retour en base,
     * mais la ligne peut être marquée révoquée à tout moment.
     */
    Optional<JetonRafraichissement> findByValeur(String valeur);
}

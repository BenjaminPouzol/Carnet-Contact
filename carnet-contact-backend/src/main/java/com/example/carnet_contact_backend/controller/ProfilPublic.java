package com.example.carnet_contact_backend.controller;

import com.example.carnet_contact_backend.abonnement.StatutRelation;

/**
 * La page d'une personne, vue par un autre compte.
 *
 * Tout ce qui dépend des droits est CALCULÉ PAR LE SERVEUR, pour celui qui
 * regarde — comme `modifiable` sur une publication :
 *
 * @param contenuVisible ses publications me sont-elles visibles ?
 * @param peutEcrire     puis-je lui envoyer un message ?
 * @param coordonnees    null tant que je ne le suis pas : le client n'a pas à
 *                       décider de les cacher, il ne les reçoit pas
 */
public record ProfilPublic(
        Long id,
        String nomAffichage,
        String photoUrl,
        boolean comptePrive,
        StatutRelation statut,
        boolean ilMeSuit,
        long nombreAbonnes,
        long nombreAbonnements,
        boolean contenuVisible,
        boolean peutEcrire,
        CoordonneesPro coordonnees) {}

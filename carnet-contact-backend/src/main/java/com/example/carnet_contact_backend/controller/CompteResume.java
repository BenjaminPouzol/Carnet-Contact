package com.example.carnet_contact_backend.controller;

import com.example.carnet_contact_backend.abonnement.StatutRelation;

/**
 * Un compte tel qu'il apparaît dans une liste : résultats de recherche,
 * abonnements, abonnés, demandes.
 *
 * Liste blanche, comme AuteurPublic (section 35) : ni email, ni rôle, ni
 * coordonnées professionnelles. Seulement de quoi afficher une ligne et choisir
 * le libellé du bouton « Suivre ».
 *
 * @param statut   MA relation vers ce compte
 * @param ilMeSuit SA relation vers moi (abonnement accepté)
 */
public record CompteResume(
        Long id,
        String nomAffichage,
        String photoUrl,
        boolean comptePrive,
        StatutRelation statut,
        boolean ilMeSuit) {}

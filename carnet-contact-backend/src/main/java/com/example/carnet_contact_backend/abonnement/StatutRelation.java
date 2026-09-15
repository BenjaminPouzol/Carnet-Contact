package com.example.carnet_contact_backend.abonnement;

/**
 * Ma relation vers un autre compte, telle que le client la reçoit.
 *
 * Pourquoi pas StatutAbonnement directement ? Parce que « aucun abonnement »
 * n'est pas un statut qu'on enregistre — c'est l'absence de ligne. Le client,
 * lui, a besoin d'une valeur pour choisir le libellé du bouton (« Suivre »,
 * « Demande envoyée », « Abonné·e ») : AUCUN la lui donne sans null à gérer.
 */
public enum StatutRelation {
    AUCUN,
    EN_ATTENTE,
    ACCEPTE
}

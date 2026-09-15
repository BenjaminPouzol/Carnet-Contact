package com.example.carnet_contact_backend.model;

/**
 * L'état d'un abonnement, tel qu'il est enregistré en base.
 *
 * Une demande acceptée ne change pas de table : c'est la même ligne dont le
 * statut passe de EN_ATTENTE à ACCEPTE. Suivre un compte public crée
 * directement une ligne ACCEPTE.
 *
 * Stocké par son nom (@Enumerated(STRING) côté entité), comme Role.
 */
public enum StatutAbonnement {
    EN_ATTENTE,
    ACCEPTE
}

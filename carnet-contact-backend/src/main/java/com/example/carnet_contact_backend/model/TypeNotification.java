package com.example.carnet_contact_backend.model;

/**
 * Ce qui s'est passé, du point de vue du destinataire de la notification.
 *
 * Le texte affiché (« Alice vous suit ») n'est pas stocké : le client le
 * compose à partir du type et du nom de l'acteur. Un nom changé entre-temps
 * s'affiche ainsi à jour, et la formulation reste l'affaire de l'interface.
 */
public enum TypeNotification {
    /** L'acteur s'est abonné à un compte public. */
    NOUVEL_ABONNE,
    /** L'acteur demande à suivre un compte privé. */
    DEMANDE_RECUE,
    /** L'acteur a accepté la demande du destinataire. */
    DEMANDE_ACCEPTEE
}

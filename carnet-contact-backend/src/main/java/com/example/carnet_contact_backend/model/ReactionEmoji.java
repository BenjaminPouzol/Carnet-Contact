package com.example.carnet_contact_backend.model;

/**
 * Ce que toute réaction emoji sait dire d'elle-même, qu'elle porte sur un
 * message ou sur une publication.
 *
 * Les deux entités vivent dans des tables séparées et n'ont pas d'ancêtre
 * commun. Cette interface est le contrat qui permet à `Reactions` de les
 * regrouper avec UN seul code, sans savoir laquelle il manipule.
 */
public interface ReactionEmoji {

    Long getId();

    String getEmoji();

    Utilisateur getUtilisateur();

    /** L'identifiant de l'élément réagi : un message, ou une publication. */
    Long idCible();
}

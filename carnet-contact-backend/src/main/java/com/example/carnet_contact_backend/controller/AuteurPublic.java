package com.example.carnet_contact_backend.controller;

import com.example.carnet_contact_backend.model.Utilisateur;

/**
 * Ce qu'un compte laisse voir de lui aux AUTRES comptes.
 *
 * Jusqu'ici, messages et liste des interlocuteurs renvoyaient l'entité
 * Utilisateur telle quelle. @JsonIgnore cachait le mot de passe, mais tout le
 * reste sortait : email, rôle, état du compte, date d'inscription. N'importe
 * quel compte connecté pouvait lister les adresses de tous les autres et
 * repérer les administrateurs.
 *
 * @JsonIgnore raisonne en LISTE NOIRE : tout ce qu'on n'a pas pensé à cacher
 * est publié, y compris un champ ajouté plus tard. Un DTO raisonne en LISTE
 * BLANCHE : seul ce qui est écrit ici sort.
 */
public record AuteurPublic(Long id, String nomAffichage, String photoUrl) {

    public static AuteurPublic de(Utilisateur utilisateur) {
        return new AuteurPublic(
                utilisateur.getId(), utilisateur.getNomAffichage(), utilisateur.getPhotoUrl());
    }
}

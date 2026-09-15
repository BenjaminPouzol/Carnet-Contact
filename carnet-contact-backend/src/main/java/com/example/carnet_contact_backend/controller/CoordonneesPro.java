package com.example.carnet_contact_backend.controller;

import com.example.carnet_contact_backend.model.Utilisateur;

/**
 * Ce qu'un compte montre à ceux qui le suivent : email professionnel et
 * réseaux sociaux.
 *
 * Aucun champ `email` ici, et c'est la garantie la plus solide que l'email de
 * connexion ne sortira pas : il n'existe tout simplement pas dans ce type. Un
 * oubli de @JsonIgnore ne peut rien y changer.
 */
public record CoordonneesPro(
        String emailPro,
        String instagram,
        String twitter,
        String facebook,
        String twitch,
        String youtube,
        String linkedin) {

    public static CoordonneesPro de(Utilisateur u) {
        return new CoordonneesPro(
                u.getEmailPro(), u.getInstagram(), u.getTwitter(), u.getFacebook(),
                u.getTwitch(), u.getYoutube(), u.getLinkedin());
    }
}

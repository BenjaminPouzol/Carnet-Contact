package com.example.carnet_contact_backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class Contact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nom;
    private String prenom;
    private String email;
    private String telephone;

    // Champ optionnel : adresse professionnelle, en plus de l'adresse perso.
    private String emailPro;

    // Photo du contact : une URL — saisie à la main, ou obtenue en envoyant
    // une image (/api/images), qui renvoie justement une adresse.
    private String photoUrl;

    // Réseaux sociaux, tous optionnels. Stockés comme du texte libre : selon
    // ce que l'utilisateur colle, ce sera une URL complète ou un pseudo.
    private String instagram;
    private String twitter;
    private String facebook;
    private String twitch;
    private String youtube;
    private String linkedin;

    /**
     * Le compte à qui ce contact appartient.
     *
     * @ManyToOne : PLUSIEURS contacts pointent vers UN utilisateur. C'est le
     * côté « propriétaire » de la relation : la table contact reçoit une
     * colonne proprietaire_id (une clé étrangère).
     *
     * fetch = LAZY : Hibernate ne charge l'utilisateur que si on le demande
     * vraiment, au lieu de faire une jointure à chaque lecture de contact.
     *
     * @JsonIgnore : le client n'a pas besoin de cette information — il ne
     * reçoit de toute façon que ses propres contacts. L'omettre évite en plus
     * d'alourdir chaque contact avec son propriétaire complet.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proprietaire_id")
    @JsonIgnore
    private Utilisateur proprietaire;

    // Getters et setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public String getEmailPro() {
        return emailPro;
    }

    public void setEmailPro(String emailPro) {
        this.emailPro = emailPro;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public String getInstagram() {
        return instagram;
    }

    public void setInstagram(String instagram) {
        this.instagram = instagram;
    }

    public String getTwitter() {
        return twitter;
    }

    public void setTwitter(String twitter) {
        this.twitter = twitter;
    }

    public String getFacebook() {
        return facebook;
    }

    public void setFacebook(String facebook) {
        this.facebook = facebook;
    }

    public String getTwitch() {
        return twitch;
    }

    public void setTwitch(String twitch) {
        this.twitch = twitch;
    }

    public String getYoutube() {
        return youtube;
    }

    public void setYoutube(String youtube) {
        this.youtube = youtube;
    }

    public String getLinkedin() {
        return linkedin;
    }

    public void setLinkedin(String linkedin) {
        this.linkedin = linkedin;
    }

    public Utilisateur getProprietaire() {
        return proprietaire;
    }

    public void setProprietaire(Utilisateur proprietaire) {
        this.proprietaire = proprietaire;
    }
}

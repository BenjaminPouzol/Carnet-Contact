package com.example.carnet_contact_backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Un compte de l'application. C'est le PROPRIÉTAIRE des contacts : chaque
 * utilisateur ne voit que les siens.
 *
 * "utilisateur" est écrit explicitement comme nom de table car "user" est un
 * mot réservé en SQL — la table serait refusée par H2.
 */
@Entity
@Table(name = "utilisateur")
public class Utilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // unique = true : la base elle-même refuse deux comptes de même email,
    // même si un bug côté Java laissait passer la vérification.
    @Column(nullable = false, unique = true)
    private String email;

    // @JsonIgnore : ce champ ne sort JAMAIS du serveur. Sans cette annotation,
    // Jackson sérialiserait le mot de passe haché dans chaque réponse JSON où
    // un Utilisateur apparaît (une conversation, par exemple).
    @JsonIgnore
    @Column(nullable = false)
    private String motDePasse;

    private String nomAffichage;

    // Photo de profil : simple URL vers une image hébergée ailleurs.
    private String photoUrl;

    // Getters et setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMotDePasse() {
        return motDePasse;
    }

    public void setMotDePasse(String motDePasse) {
        this.motDePasse = motDePasse;
    }

    public String getNomAffichage() {
        return nomAffichage;
    }

    public void setNomAffichage(String nomAffichage) {
        this.nomAffichage = nomAffichage;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }
}

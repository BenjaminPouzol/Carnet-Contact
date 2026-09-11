package com.example.carnet_contact_backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

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

    /**
     * Le rôle du compte.
     *
     * @Enumerated(STRING) et non ORDINAL : par défaut, JPA stocke la POSITION
     * de la valeur dans l'enum (0, 1…). Réordonner l'enum, ou insérer une
     * valeur au milieu, changerait alors silencieusement le rôle de tous les
     * comptes déjà en base. Stocker le nom coûte quelques octets et supprime
     * ce piège.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.UTILISATEUR;

    /**
     * Un compte désactivé existe toujours mais ne peut plus se connecter.
     *
     * Pourquoi ne pas simplement supprimer ? Parce que supprimer emporte les
     * contacts et les messages, et que c'est irréversible. Désactiver répond
     * au besoin courant — couper l'accès — sans détruire quoi que ce soit.
     */
    @Column(nullable = false)
    private boolean actif = true;

    /** Pour que le panel d'administration puisse trier par ancienneté. */
    private Instant dateInscription;

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

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isActif() {
        return actif;
    }

    public void setActif(boolean actif) {
        this.actif = actif;
    }

    public Instant getDateInscription() {
        return dateInscription;
    }

    public void setDateInscription(Instant dateInscription) {
        this.dateInscription = dateInscription;
    }
}

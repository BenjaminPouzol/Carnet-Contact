package com.example.carnet_contact_backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import java.time.Instant;

/**
 * Un événement à signaler à un compte : quelqu'un le suit, demande à le suivre,
 * ou a accepté sa demande.
 *
 * Pourquoi une table, plutôt que de déduire ces événements des abonnements ?
 * Parce que « demande acceptée » concerne le DEMANDEUR, pas le compte suivi, et
 * qu'il faut retenir pour chacun ce qui a déjà été lu. Une ligne par événement,
 * avec son drapeau `lue`, répond aux deux besoins sans contorsion.
 */
@Entity
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Celui qui reçoit la notification.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destinataire_id")
    private Utilisateur destinataire;

    // Celui qui a provoqué l'événement.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "acteur_id")
    private Utilisateur acteur;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TypeNotification type;

    @Column(nullable = false)
    private Instant date;

    @Column(nullable = false)
    private boolean lue;

    // Getters et setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Utilisateur getDestinataire() {
        return destinataire;
    }

    public void setDestinataire(Utilisateur destinataire) {
        this.destinataire = destinataire;
    }

    public Utilisateur getActeur() {
        return acteur;
    }

    public void setActeur(Utilisateur acteur) {
        this.acteur = acteur;
    }

    public TypeNotification getType() {
        return type;
    }

    public void setType(TypeNotification type) {
        this.type = type;
    }

    public Instant getDate() {
        return date;
    }

    public void setDate(Instant date) {
        this.date = date;
    }

    public boolean isLue() {
        return lue;
    }

    public void setLue(boolean lue) {
        this.lue = lue;
    }
}

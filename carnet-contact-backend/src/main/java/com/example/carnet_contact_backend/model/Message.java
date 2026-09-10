package com.example.carnet_contact_backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import java.time.Instant;

/**
 * Un message échangé entre deux comptes de l'application.
 *
 * Un message n'a pas de « conversation » comme entité séparée : la
 * conversation entre A et B, c'est simplement l'ensemble des messages où
 * (expediteur = A et destinataire = B) ou l'inverse. Une table de moins à
 * gérer, et aucune information dupliquée.
 */
@Entity
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Deux relations @ManyToOne vers la MÊME entité : il faut donc nommer
    // explicitement les colonnes, sinon Hibernate ne saurait pas les
    // distinguer. Ces deux-ci sont chargées avec le message (pas LAZY) car le
    // client a besoin de savoir qui parle pour afficher le fil.
    @ManyToOne(optional = false)
    @JoinColumn(name = "expediteur_id")
    private Utilisateur expediteur;

    @ManyToOne(optional = false)
    @JoinColumn(name = "destinataire_id")
    private Utilisateur destinataire;

    @Column(nullable = false, length = 2000)
    private String contenu;

    // Instant : un point précis dans le temps, en UTC, sans fuseau horaire.
    // C'est le type à utiliser pour un horodatage technique — la conversion
    // vers l'heure locale de l'utilisateur est l'affaire de l'affichage.
    private Instant dateEnvoi;

    private boolean lu;

    // Getters et setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Utilisateur getExpediteur() {
        return expediteur;
    }

    public void setExpediteur(Utilisateur expediteur) {
        this.expediteur = expediteur;
    }

    public Utilisateur getDestinataire() {
        return destinataire;
    }

    public void setDestinataire(Utilisateur destinataire) {
        this.destinataire = destinataire;
    }

    public String getContenu() {
        return contenu;
    }

    public void setContenu(String contenu) {
        this.contenu = contenu;
    }

    public Instant getDateEnvoi() {
        return dateEnvoi;
    }

    public void setDateEnvoi(Instant dateEnvoi) {
        this.dateEnvoi = dateEnvoi;
    }

    public boolean isLu() {
        return lu;
    }

    public void setLu(boolean lu) {
        this.lu = lu;
    }
}

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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

/**
 * « abonne suit suivi ».
 *
 * Une table de liaison écrite à la main, comme les réactions, plutôt qu'un
 * @ManyToMany entre deux Utilisateur. Un @ManyToMany ne sait stocker que le
 * couple ; ici la relation porte ses propres données (un statut, deux dates),
 * et on l'interroge dans les deux sens (« qui je suis », « qui me suit »).
 *
 * La relation n'est pas symétrique : Alice peut suivre Bob sans que Bob suive
 * Alice. Chaque sens est une ligne distincte.
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(
        name = "uk_abonnement_couple", columnNames = {"abonne_id", "suivi_id"}))
public class Abonnement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Deux @ManyToOne vers la même entité : colonnes nommées explicitement,
    // comme expediteur / destinataire dans Message.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "abonne_id")
    private Utilisateur abonne;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "suivi_id")
    private Utilisateur suivi;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private StatutAbonnement statut;

    @Column(nullable = false)
    private Instant dateDemande;

    // null tant que la demande attend l'accord d'un compte privé.
    private Instant dateAcceptation;

    // Getters et setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Utilisateur getAbonne() {
        return abonne;
    }

    public void setAbonne(Utilisateur abonne) {
        this.abonne = abonne;
    }

    public Utilisateur getSuivi() {
        return suivi;
    }

    public void setSuivi(Utilisateur suivi) {
        this.suivi = suivi;
    }

    public StatutAbonnement getStatut() {
        return statut;
    }

    public void setStatut(StatutAbonnement statut) {
        this.statut = statut;
    }

    public Instant getDateDemande() {
        return dateDemande;
    }

    public void setDateDemande(Instant dateDemande) {
        this.dateDemande = dateDemande;
    }

    public Instant getDateAcceptation() {
        return dateAcceptation;
    }

    public void setDateAcceptation(Instant dateAcceptation) {
        this.dateAcceptation = dateAcceptation;
    }
}

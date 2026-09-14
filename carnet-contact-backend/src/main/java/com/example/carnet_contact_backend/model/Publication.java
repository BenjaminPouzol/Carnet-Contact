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
 * Une publication du fil d'actualité : un compte parle d'un de ses hobbies.
 *
 * Contrairement à un contact (privé à son propriétaire) ou à un message (privé
 * aux deux interlocuteurs), une publication est PUBLIQUE : tous les comptes la
 * voient. Cette différence gouverne les règles d'accès du contrôleur.
 */
@Entity
public class Publication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * LAZY : l'auteur n'est pas chargé d'office avec la publication. La requête
     * du fil le ramène explicitement par JOIN FETCH, en une seule requête pour
     * toute la page — au lieu d'une requête par auteur (le problème N+1 de la
     * section 32).
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "auteur_id")
    private Utilisateur auteur;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Categorie categorie;

    @Column(nullable = false, length = 2000)
    private String contenu;

    // Une adresse d'image, comme la photo de profil : pas d'envoi de fichier.
    @Column(length = 500)
    private String imageUrl;

    @Column(nullable = false)
    private Instant datePublication;

    // null tant que l'auteur n'a pas modifié sa publication.
    private Instant dateModification;

    // Getters et setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Utilisateur getAuteur() {
        return auteur;
    }

    public void setAuteur(Utilisateur auteur) {
        this.auteur = auteur;
    }

    public Categorie getCategorie() {
        return categorie;
    }

    public void setCategorie(Categorie categorie) {
        this.categorie = categorie;
    }

    public String getContenu() {
        return contenu;
    }

    public void setContenu(String contenu) {
        this.contenu = contenu;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Instant getDatePublication() {
        return datePublication;
    }

    public void setDatePublication(Instant datePublication) {
        this.datePublication = datePublication;
    }

    public Instant getDateModification() {
        return dateModification;
    }

    public void setDateModification(Instant dateModification) {
        this.dateModification = dateModification;
    }
}

package com.example.carnet_contact_backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;

import java.time.Instant;

/**
 * Une image envoyée depuis l'appareil d'un utilisateur.
 *
 * Elle n'est rattachée à aucun contact ni publication : ceux-ci ne connaissent
 * que son ADRESSE (/api/images/<id>), exactement comme ils connaîtraient celle
 * d'une image hébergée ailleurs. C'est ce qui a permis de n'ajouter aucune
 * colonne aux entités existantes.
 */
@Entity
public class Image {

    /**
     * Un UUID plutôt qu'un compteur : l'image se lit sans jeton, son identifiant
     * est donc sa seule protection. /api/images/42 inviterait à essayer 43 ; un
     * UUID aléatoire (122 bits de hasard) ne se devine pas.
     *
     * GenerationType.UUID : c'est Hibernate qui le tire au sort à l'insertion.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    /**
     * @Lob (« Large OBject ») : un contenu volumineux, rangé dans une colonne
     * BLOB. Sans lui, JPA choisirait un VARBINARY de taille modeste, trop petit
     * pour une photo.
     */
    @Lob
    @Column(nullable = false)
    private byte[] donnees;

    // Déduit des octets par FormatImage, jamais recopié de la requête : c'est
    // ce type qui sera renvoyé au navigateur à la lecture.
    @Column(nullable = false, length = 20)
    private String typeContenu;

    @Column(nullable = false)
    private long taille;

    @Column(nullable = false)
    private Instant dateEnvoi;

    // Qui l'a envoyée : sert à supprimer ses images avec son compte.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proprietaire_id")
    private Utilisateur proprietaire;

    // Getters et setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public byte[] getDonnees() {
        return donnees;
    }

    public void setDonnees(byte[] donnees) {
        this.donnees = donnees;
    }

    public String getTypeContenu() {
        return typeContenu;
    }

    public void setTypeContenu(String typeContenu) {
        this.typeContenu = typeContenu;
    }

    public long getTaille() {
        return taille;
    }

    public void setTaille(long taille) {
        this.taille = taille;
    }

    public Instant getDateEnvoi() {
        return dateEnvoi;
    }

    public void setDateEnvoi(Instant dateEnvoi) {
        this.dateEnvoi = dateEnvoi;
    }

    public Utilisateur getProprietaire() {
        return proprietaire;
    }

    public void setProprietaire(Utilisateur proprietaire) {
        this.proprietaire = proprietaire;
    }
}

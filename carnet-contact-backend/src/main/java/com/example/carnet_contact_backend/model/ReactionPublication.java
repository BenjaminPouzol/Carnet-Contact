package com.example.carnet_contact_backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * La réaction d'une personne à une publication du fil.
 *
 * Le calque exact de Reaction, dans sa propre table. Pourquoi ne pas réutiliser
 * la table `reaction` avec une seconde clé étrangère facultative (message OU
 * publication) ? Parce que chaque ligne devrait alors garantir « exactement
 * une des deux », et la contrainte d'unicité « une réaction par personne »
 * deviendrait fragile sur des colonnes nullables. Deux tables simples valent
 * mieux qu'une table ambiguë ; le code commun, lui, est partagé par l'interface
 * ReactionEmoji.
 */
@Entity
@Table(
        name = "reaction_publication",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_reaction_publication_utilisateur",
                columnNames = {"publication_id", "utilisateur_id"}))
public class ReactionPublication implements ReactionEmoji {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "publication_id")
    private Publication publication;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utilisateur_id")
    private Utilisateur utilisateur;

    // length = 8 : un emoji peut occuper plusieurs caractères Java (voir Reaction).
    @Column(nullable = false, length = 8)
    private String emoji;

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Publication getPublication() {
        return publication;
    }

    public void setPublication(Publication publication) {
        this.publication = publication;
    }

    @Override
    public Utilisateur getUtilisateur() {
        return utilisateur;
    }

    public void setUtilisateur(Utilisateur utilisateur) {
        this.utilisateur = utilisateur;
    }

    @Override
    public String getEmoji() {
        return emoji;
    }

    public void setEmoji(String emoji) {
        this.emoji = emoji;
    }

    @Override
    public Long idCible() {
        return publication.getId();
    }
}

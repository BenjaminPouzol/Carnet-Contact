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
 * La réaction d'une personne à un message.
 *
 * Une TABLE plutôt qu'une colonne sur le message : un message reçoit plusieurs
 * réactions, de personnes différentes. Ranger cela dans une colonne
 * obligerait à y stocker une liste sérialisée — impossible à interroger, à
 * compter, ou à modifier sans tout relire.
 *
 * La contrainte d'unicité sur (message, utilisateur) est la traduction en base
 * de la règle « une réaction par personne et par message ». On pourrait se
 * contenter de la vérifier en Java ; la déclarer ici la rend impossible à
 * contourner, même par un bug ou deux requêtes simultanées.
 */
@Entity
@Table(
        name = "reaction",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_reaction_message_utilisateur",
                columnNames = {"message_id", "utilisateur_id"}))
public class Reaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "message_id")
    private Message message;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utilisateur_id")
    private Utilisateur utilisateur;

    /**
     * L'emoji lui-même, tel quel.
     *
     * length = 8 : un emoji n'est pas un caractère. « 👍 » en occupe deux au
     * sens de Java, et certains (drapeaux, emojis composés) bien davantage.
     * Une colonne de longueur 1 refuserait la plupart d'entre eux.
     */
    @Column(nullable = false, length = 8)
    private String emoji;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    public Utilisateur getUtilisateur() {
        return utilisateur;
    }

    public void setUtilisateur(Utilisateur utilisateur) {
        this.utilisateur = utilisateur;
    }

    public String getEmoji() {
        return emoji;
    }

    public void setEmoji(String emoji) {
        this.emoji = emoji;
    }
}

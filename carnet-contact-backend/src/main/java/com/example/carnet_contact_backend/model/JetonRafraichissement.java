package com.example.carnet_contact_backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import java.time.Instant;

/**
 * Un jeton de rafraîchissement : le droit de demander un nouveau jeton d'accès
 * sans retaper son mot de passe.
 *
 * Pourquoi une ENTITÉ, alors que le jeton d'accès (JWT) ne se stocke nulle
 * part ? Parce que les deux ne résolvent pas le même problème.
 *
 * Le JWT est « sans état » : le serveur n'a rien à retenir, il vérifie une
 * signature. C'est rapide, mais cela a une conséquence gênante — un JWT émis
 * ne peut PAS être annulé. Tant qu'il n'a pas expiré, il ouvre la porte, même
 * si le compte a été compromis entre-temps. La seule parade est de lui donner
 * une vie courte (ici 15 minutes).
 *
 * Mais un utilisateur ne veut pas se reconnecter tous les quarts d'heure. D'où
 * le second jeton, long (7 jours) — et celui-là est stocké en base, donc
 * RÉVOCABLE : une déconnexion le supprime, et il ne rouvrira plus rien.
 *
 * Le partage des rôles est donc : le jeton court paie le prix de la vitesse
 * (aucun accès base à chaque requête), le jeton long paie le prix du contrôle
 * (un accès base, mais seulement toutes les 15 minutes).
 */
@Entity
public class JetonRafraichissement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // La valeur présentée par le client. unique = true : la base elle-même
    // interdit deux lignes de même valeur.
    @Column(nullable = false, unique = true, length = 64)
    private String valeur;

    // LAZY : on ne charge le propriétaire que si on le demande vraiment. Sur
    // une table qui grossit à chaque connexion, cela évite un JOIN inutile
    // quand on ne fait que vérifier une expiration.
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utilisateur_id")
    private Utilisateur utilisateur;

    @Column(nullable = false)
    private Instant expiration;

    // On marque plutôt qu'on supprime : une ligne révoquée garde une trace de
    // ce qui s'est passé, utile pour détecter un jeton rejoué.
    @Column(nullable = false)
    private boolean revoque;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getValeur() {
        return valeur;
    }

    public void setValeur(String valeur) {
        this.valeur = valeur;
    }

    public Utilisateur getUtilisateur() {
        return utilisateur;
    }

    public void setUtilisateur(Utilisateur utilisateur) {
        this.utilisateur = utilisateur;
    }

    public Instant getExpiration() {
        return expiration;
    }

    public void setExpiration(Instant expiration) {
        this.expiration = expiration;
    }

    public boolean isRevoque() {
        return revoque;
    }

    public void setRevoque(boolean revoque) {
        this.revoque = revoque;
    }

    /** Utilisable = ni révoqué, ni périmé. */
    public boolean estUtilisable() {
        return !revoque && expiration.isAfter(Instant.now());
    }
}

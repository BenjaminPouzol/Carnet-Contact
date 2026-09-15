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

import java.time.Instant;

/**
 * « bloqueur a bloqué bloque ».
 *
 * La ligne ne garde qu'un sens — celui qui a bloqué, le seul qui peut
 * débloquer —, mais ses EFFETS s'appliquent dans les deux : tant qu'elle existe,
 * aucun des deux comptes ne voit l'autre ni n'interagit avec lui (VueRelations).
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(
        name = "uk_blocage_couple", columnNames = {"bloqueur_id", "bloque_id"}))
public class Blocage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bloqueur_id")
    private Utilisateur bloqueur;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bloque_id")
    private Utilisateur bloque;

    @Column(nullable = false)
    private Instant dateBlocage;

    // Getters et setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Utilisateur getBloqueur() {
        return bloqueur;
    }

    public void setBloqueur(Utilisateur bloqueur) {
        this.bloqueur = bloqueur;
    }

    public Utilisateur getBloque() {
        return bloque;
    }

    public void setBloque(Utilisateur bloque) {
        this.bloque = bloque;
    }

    public Instant getDateBlocage() {
        return dateBlocage;
    }

    public void setDateBlocage(Instant dateBlocage) {
        this.dateBlocage = dateBlocage;
    }
}

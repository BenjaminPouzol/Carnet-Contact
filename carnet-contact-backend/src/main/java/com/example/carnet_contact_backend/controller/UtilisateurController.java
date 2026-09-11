package com.example.carnet_contact_backend.controller;

import com.example.carnet_contact_backend.model.Utilisateur;
import com.example.carnet_contact_backend.repository.UtilisateurRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/utilisateurs")
public class UtilisateurController {

    private final UtilisateurRepository utilisateurRepository;

    public UtilisateurController(UtilisateurRepository utilisateurRepository) {
        this.utilisateurRepository = utilisateurRepository;
    }

    public record DemandeProfil(String nomAffichage, String photoUrl) {}

    private Utilisateur utilisateurConnecte(String email) {
        return utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    /** Le compte connecté, pour réafficher son profil au rechargement. */
    @GetMapping("/moi")
    public Utilisateur moi(@AuthenticationPrincipal String email) {
        return utilisateurConnecte(email);
    }

    /**
     * Les autres comptes : les interlocuteurs possibles de la messagerie.
     *
     * Les comptes désactivés en sont exclus — écrire à quelqu'un qui ne peut
     * plus se connecter n'aurait aucun sens. Leurs anciens messages restent
     * en revanche visibles dans les fils déjà ouverts : les désactiver coupe
     * l'accès, cela n'efface pas l'historique.
     */
    @GetMapping
    public List<Utilisateur> autres(@AuthenticationPrincipal String email) {
        return utilisateurRepository.findByIdNotAndActifTrueOrderByNomAffichageAsc(
                utilisateurConnecte(email).getId());
    }

    /** Mise à jour du profil : nom affiché et photo. */
    @PutMapping("/moi")
    public Utilisateur modifierProfil(
            @RequestBody DemandeProfil demande,
            @AuthenticationPrincipal String email) {
        Utilisateur moi = utilisateurConnecte(email);

        if (demande.nomAffichage() != null && !demande.nomAffichage().isBlank()) {
            moi.setNomAffichage(demande.nomAffichage());
        }
        // La photo peut être volontairement effacée : on accepte la chaîne
        // vide, qu'on ramène à null pour ne pas stocker de valeur bidon.
        if (demande.photoUrl() != null) {
            moi.setPhotoUrl(demande.photoUrl().isBlank() ? null : demande.photoUrl());
        }

        return utilisateurRepository.save(moi);
    }
}

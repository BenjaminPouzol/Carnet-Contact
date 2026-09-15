package com.example.carnet_contact_backend.controller;

import com.example.carnet_contact_backend.abonnement.VueRelations;
import com.example.carnet_contact_backend.model.StatutAbonnement;
import com.example.carnet_contact_backend.model.Utilisateur;
import com.example.carnet_contact_backend.repository.AbonnementRepository;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Fabrique les DTO de comptes à partir d'une VueRelations déjà chargée.
 *
 * Plusieurs contrôleurs montrent des comptes (abonnements, recherche,
 * suggestions, profil) : la manière de les assembler s'écrit ici une fois.
 * @Component plutôt que des méthodes statiques, parce que le profil a besoin
 * d'un dépôt pour ses compteurs.
 */
@Component
public class VuesComptes {

    private final AbonnementRepository abonnementRepository;

    public VuesComptes(AbonnementRepository abonnementRepository) {
        this.abonnementRepository = abonnementRepository;
    }

    /** Aucune requête : tout se lit dans la vue. */
    public CompteResume resume(VueRelations vue, Utilisateur autre) {
        return new CompteResume(
                autre.getId(),
                autre.getNomAffichage(),
                autre.getPhotoUrl(),
                autre.isComptePrive(),
                vue.statut(autre.getId()),
                vue.ilMeSuit(autre.getId()));
    }

    public List<CompteResume> resumes(VueRelations vue, List<Utilisateur> autres) {
        return autres.stream().map(u -> resume(vue, u)).toList();
    }

    /** Deux requêtes de comptage, pour un profil seul : acceptable. */
    public ProfilPublic profil(VueRelations vue, Utilisateur autre) {
        Long id = autre.getId();

        return new ProfilPublic(
                id,
                autre.getNomAffichage(),
                autre.getPhotoUrl(),
                autre.isComptePrive(),
                vue.statut(id),
                vue.ilMeSuit(id),
                abonnementRepository.countBySuiviIdAndStatut(id, StatutAbonnement.ACCEPTE),
                abonnementRepository.countByAbonneIdAndStatut(id, StatutAbonnement.ACCEPTE),
                vue.voitContenu(id, autre.isComptePrive()),
                vue.peutEcrire(id),
                vue.voitCoordonnees(id) ? CoordonneesPro.de(autre) : null);
    }
}

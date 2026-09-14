package com.example.carnet_contact_backend.controller;

import com.example.carnet_contact_backend.model.Role;
import com.example.carnet_contact_backend.model.Utilisateur;
import com.example.carnet_contact_backend.repository.ContactRepository;
import com.example.carnet_contact_backend.repository.JetonRafraichissementRepository;
import com.example.carnet_contact_backend.repository.MessageRepository;
import com.example.carnet_contact_backend.repository.PublicationRepository;
import com.example.carnet_contact_backend.repository.ReactionPublicationRepository;
import com.example.carnet_contact_backend.repository.ReactionRepository;
import com.example.carnet_contact_backend.repository.UtilisateurRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

/**
 * Le panel d'administration : gérer les comptes de l'application.
 *
 * L'accès est verrouillé en un seul endroit, dans SecurityConfig
 * (`/api/admin/** → hasRole("ADMIN")`). Aucune méthode de cette classe n'a
 * donc à revérifier le rôle : si le code s'exécute, c'est que l'appelant est
 * administrateur.
 *
 * Ce qui reste à vérifier ici, en revanche, ce sont les règles que Spring
 * Security ne peut pas connaître : un administrateur a le droit d'agir, mais
 * pas de se tirer une balle dans le pied.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UtilisateurRepository utilisateurRepository;
    private final ContactRepository contactRepository;
    private final MessageRepository messageRepository;
    private final ReactionRepository reactionRepository;
    private final PublicationRepository publicationRepository;
    private final ReactionPublicationRepository reactionPublicationRepository;
    private final JetonRafraichissementRepository jetonRepository;

    public AdminController(
            UtilisateurRepository utilisateurRepository,
            ContactRepository contactRepository,
            MessageRepository messageRepository,
            ReactionRepository reactionRepository,
            PublicationRepository publicationRepository,
            ReactionPublicationRepository reactionPublicationRepository,
            JetonRafraichissementRepository jetonRepository) {
        this.utilisateurRepository = utilisateurRepository;
        this.contactRepository = contactRepository;
        this.messageRepository = messageRepository;
        this.reactionRepository = reactionRepository;
        this.publicationRepository = publicationRepository;
        this.reactionPublicationRepository = reactionPublicationRepository;
        this.jetonRepository = jetonRepository;
    }

    /**
     * Une ligne du tableau d'administration.
     *
     * Un DTO plutôt que l'entité, parce qu'il porte des informations qui
     * n'existent pas dans `Utilisateur` : le nombre de contacts, de messages et
     * de publications, qui se comptent dans d'autres tables.
     */
    public record LigneCompte(
            Long id,
            String email,
            String nomAffichage,
            String photoUrl,
            Role role,
            boolean actif,
            Instant dateInscription,
            long nombreContacts,
            long nombreMessages,
            long nombrePublications,
            boolean estMoi) {}

    public record DemandeActif(boolean actif) {}

    public record DemandeRole(Role role) {}

    private Utilisateur utilisateurConnecte(String email) {
        return utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    @GetMapping("/comptes")
    public List<LigneCompte> comptes(@AuthenticationPrincipal String email) {
        Utilisateur moi = utilisateurConnecte(email);

        return utilisateurRepository.findAll(Sort.by("dateInscription").ascending())
                .stream()
                .map(u -> ligne(u, moi))
                .toList();
    }

    /**
     * Active ou désactive un compte.
     *
     * Désactiver révoque au passage les jetons de rafraîchissement : sans
     * cela, la session en cours continuerait de se renouveler indéfiniment, et
     * la désactivation ne prendrait effet qu'à la déconnexion volontaire de
     * l'intéressé — c'est-à-dire peut-être jamais.
     */
    @PutMapping("/comptes/{id}/actif")
    @Transactional
    public LigneCompte changerActif(
            @PathVariable Long id,
            @RequestBody DemandeActif demande,
            @AuthenticationPrincipal String email) {

        Utilisateur moi = utilisateurConnecte(email);
        Utilisateur cible = compteExistant(id);

        // Se désactiver soi-même reviendrait à se déconnecter définitivement,
        // sans moyen de revenir en arrière.
        if (cible.getId().equals(moi.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Vous ne pouvez pas désactiver votre propre compte.");
        }

        if (!demande.actif() && cible.getRole() == Role.ADMIN && dernierAdminActif(cible)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "C'est le dernier administrateur actif.");
        }

        cible.setActif(demande.actif());
        utilisateurRepository.save(cible);

        if (!demande.actif()) {
            jetonRepository.revoquerTousPour(cible.getId());
        }

        return ligne(cible, moi);
    }

    /** Promeut ou rétrograde un compte. */
    @PutMapping("/comptes/{id}/role")
    @Transactional
    public LigneCompte changerRole(
            @PathVariable Long id,
            @RequestBody DemandeRole demande,
            @AuthenticationPrincipal String email) {

        if (demande.role() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rôle manquant.");
        }

        Utilisateur moi = utilisateurConnecte(email);
        Utilisateur cible = compteExistant(id);

        // Se rétrograder soi-même ferait perdre l'accès au panel dans la
        // seconde — et si c'était le dernier administrateur, plus personne ne
        // pourrait le rendre.
        if (cible.getId().equals(moi.getId()) && demande.role() != Role.ADMIN) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Vous ne pouvez pas retirer votre propre rôle.");
        }

        if (demande.role() != Role.ADMIN && cible.getRole() == Role.ADMIN && dernierAdminActif(cible)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "C'est le dernier administrateur actif.");
        }

        cible.setRole(demande.role());
        utilisateurRepository.save(cible);

        return ligne(cible, moi);
    }

    /**
     * Supprime un compte et tout ce qui lui appartient.
     *
     * L'ordre des suppressions n'est pas décoratif : une ligne pointée par une
     * clé étrangère ne peut pas partir avant celles qui la pointent. On part
     * donc des feuilles (réactions) vers la racine (le compte).
     *
     * @Transactional couvre l'ensemble : si l'une des étapes échoue, aucune
     * n'est appliquée. Sans cela, un compte pourrait se retrouver à moitié
     * supprimé — ses messages effacés mais lui toujours là.
     */
    @DeleteMapping("/comptes/{id}")
    @Transactional
    public ResponseEntity<Void> supprimer(
            @PathVariable Long id,
            @AuthenticationPrincipal String email) {

        Utilisateur moi = utilisateurConnecte(email);
        Utilisateur cible = compteExistant(id);

        if (cible.getId().equals(moi.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Vous ne pouvez pas supprimer votre propre compte.");
        }

        if (cible.getRole() == Role.ADMIN && dernierAdminActif(cible)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "C'est le dernier administrateur actif.");
        }

        // Le fil d'abord : les réactions de ce compte, puis celles des autres
        // sur ses publications, puis ses publications elles-mêmes. Toujours la
        // même règle — des feuilles vers la racine.
        reactionPublicationRepository.supprimerCellesDe(cible.getId());
        reactionPublicationRepository.supprimerCellesDesPublicationsDe(cible.getId());
        publicationRepository.supprimerCellesDe(cible.getId());

        reactionRepository.supprimerCellesDe(cible.getId());
        reactionRepository.supprimerCellesDesMessagesDe(cible.getId());
        messageRepository.supprimerCeuxDe(cible.getId());
        contactRepository.supprimerCeuxDe(cible.getId());
        jetonRepository.supprimerTousPour(cible.getId());
        utilisateurRepository.delete(cible);

        return ResponseEntity.noContent().build();
    }

    private Utilisateur compteExistant(Long id) {
        return utilisateurRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    /**
     * Y a-t-il un autre administrateur actif que celui-ci ?
     *
     * C'est le garde-fou central de cette classe. Sans lui, désactiver,
     * rétrograder ou supprimer le dernier administrateur laisserait
     * l'application sans personne pour en administrer une autre — état dont on
     * ne pourrait plus sortir par l'interface.
     */
    private boolean dernierAdminActif(Utilisateur cible) {
        return utilisateurRepository.countByRoleAndActifTrueAndIdNot(Role.ADMIN, cible.getId()) == 0;
    }

    private LigneCompte ligne(Utilisateur u, Utilisateur moi) {
        return new LigneCompte(
                u.getId(), u.getEmail(), u.getNomAffichage(), u.getPhotoUrl(),
                u.getRole(), u.isActif(), u.getDateInscription(),
                contactRepository.countByProprietaireId(u.getId()),
                messageRepository.countByExpediteurIdOrDestinataireId(u.getId(), u.getId()),
                publicationRepository.countByAuteurId(u.getId()),
                u.getId().equals(moi.getId()));
    }
}

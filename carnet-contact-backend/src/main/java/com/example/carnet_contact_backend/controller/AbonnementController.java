package com.example.carnet_contact_backend.controller;

import com.example.carnet_contact_backend.abonnement.Notifications;
import com.example.carnet_contact_backend.abonnement.Relations;
import com.example.carnet_contact_backend.abonnement.StatutRelation;
import com.example.carnet_contact_backend.abonnement.VueRelations;
import com.example.carnet_contact_backend.model.Abonnement;
import com.example.carnet_contact_backend.model.StatutAbonnement;
import com.example.carnet_contact_backend.model.TypeNotification;
import com.example.carnet_contact_backend.model.Utilisateur;
import com.example.carnet_contact_backend.repository.AbonnementRepository;
import com.example.carnet_contact_backend.repository.UtilisateurRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

/**
 * Suivre des comptes, gérer ses demandes et ses abonnés.
 *
 * Les verbes HTTP suivent la ressource « mon abonnement à {id} » : PUT la crée
 * (ou la laisse telle quelle si elle existe déjà), DELETE la supprime. Les deux
 * sont IDEMPOTENTS — les rejouer donne le même résultat —, ce qui rend un
 * double clic ou une requête renvoyée après une coupure sans conséquence.
 */
@RestController
@RequestMapping("/api/abonnements")
public class AbonnementController {

    private final UtilisateurRepository utilisateurRepository;
    private final AbonnementRepository abonnementRepository;
    private final Relations relations;
    private final Notifications notifications;
    private final VuesComptes vuesComptes;

    public AbonnementController(
            UtilisateurRepository utilisateurRepository,
            AbonnementRepository abonnementRepository,
            Relations relations,
            Notifications notifications,
            VuesComptes vuesComptes) {
        this.utilisateurRepository = utilisateurRepository;
        this.abonnementRepository = abonnementRepository;
        this.relations = relations;
        this.notifications = notifications;
        this.vuesComptes = vuesComptes;
    }

    private Utilisateur utilisateurConnecte(String email) {
        return utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    /**
     * Suivre un compte.
     *
     * Compte public : l'abonnement est accepté d'emblée. Compte privé : il reste
     * en attente de l'accord du titulaire. Dans les deux cas, le compte suivi est
     * prévenu — une seule fois, même si la requête est rejouée.
     *
     * 404 pour un compte en relation de blocage, et non 403 : un compte qui m'a
     * bloqué doit disparaître de mon point de vue, pas se signaler.
     */
    @PutMapping("/{id}")
    @Transactional
    public CompteResume suivre(@PathVariable Long id, @AuthenticationPrincipal String email) {
        Utilisateur moi = utilisateurConnecte(email);

        if (moi.getId().equals(id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vous ne pouvez pas vous suivre vous-même.");
        }

        Utilisateur cible = utilisateurRepository.findById(id)
                .filter(Utilisateur::isActif)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        VueRelations vue = relations.vuePour(moi);
        if (vue.bloque(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        if (vue.statut(id) == StatutRelation.AUCUN) {
            StatutAbonnement statut = cible.isComptePrive() ? StatutAbonnement.EN_ATTENTE : StatutAbonnement.ACCEPTE;

            Abonnement abonnement = new Abonnement();
            abonnement.setAbonne(moi);
            abonnement.setSuivi(cible);
            abonnement.setStatut(statut);
            abonnement.setDateDemande(Instant.now());
            if (statut == StatutAbonnement.ACCEPTE) {
                abonnement.setDateAcceptation(Instant.now());
            }
            abonnementRepository.save(abonnement);

            notifications.notifier(cible, moi, statut == StatutAbonnement.ACCEPTE
                    ? TypeNotification.NOUVEL_ABONNE
                    : TypeNotification.DEMANDE_RECUE);

            // La vue a été chargée AVANT l'abonnement : on la relit pour renvoyer
            // le nouveau statut.
            vue = relations.vuePour(moi);
        }

        return vuesComptes.resume(vue, cible);
    }

    /** Ne plus suivre, ou annuler sa demande. */
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> nePlusSuivre(@PathVariable Long id, @AuthenticationPrincipal String email) {
        Utilisateur moi = utilisateurConnecte(email);

        abonnementRepository.findByAbonneIdAndSuiviId(moi.getId(), id)
                .ifPresent(abonnementRepository::delete);

        // « Alice vous suit » / « Alice demande à vous suivre » ne sont plus vrais.
        notifications.retirer(id, moi.getId(), TypeNotification.NOUVEL_ABONNE, TypeNotification.DEMANDE_RECUE);

        return ResponseEntity.noContent().build();
    }

    /** Mes abonnements, acceptés comme en attente : le client distingue par `statut`. */
    @GetMapping
    public List<CompteResume> mesAbonnements(@AuthenticationPrincipal String email) {
        Utilisateur moi = utilisateurConnecte(email);
        VueRelations vue = relations.vuePour(moi);

        return affichables(vue, abonnementRepository.sortantsDe(moi.getId()).stream()
                .map(Abonnement::getSuivi)
                .toList());
    }

    @GetMapping("/abonnes")
    public List<CompteResume> mesAbonnes(@AuthenticationPrincipal String email) {
        Utilisateur moi = utilisateurConnecte(email);
        VueRelations vue = relations.vuePour(moi);

        return affichables(vue, abonnementRepository.entrantsDe(moi.getId(), StatutAbonnement.ACCEPTE).stream()
                .map(Abonnement::getAbonne)
                .toList());
    }

    @GetMapping("/demandes")
    public List<CompteResume> demandesRecues(@AuthenticationPrincipal String email) {
        Utilisateur moi = utilisateurConnecte(email);
        VueRelations vue = relations.vuePour(moi);

        return affichables(vue, abonnementRepository.entrantsDe(moi.getId(), StatutAbonnement.EN_ATTENTE).stream()
                .map(Abonnement::getAbonne)
                .toList());
    }

    /**
     * Accepter la demande du compte {id}.
     *
     * L'ORDRE des trois opérations compte. Retirer une notification passe par une
     * requête @Modifying(clearAutomatically = true), qui VIDE le contexte de
     * persistance : tout objet chargé avant devient détaché, et une modification
     * faite ensuite sur lui ne serait jamais enregistrée — sans la moindre erreur.
     * On enregistre donc le nouveau statut, puis on crée la notification pour le
     * demandeur, et on ne retire l'ancienne qu'à la toute fin.
     */
    @PutMapping("/demandes/{id}")
    @Transactional
    public ResponseEntity<Void> accepter(@PathVariable Long id, @AuthenticationPrincipal String email) {
        Utilisateur moi = utilisateurConnecte(email);

        Abonnement demande = abonnementRepository.findByAbonneIdAndSuiviId(id, moi.getId())
                .filter(a -> a.getStatut() == StatutAbonnement.EN_ATTENTE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        demande.setStatut(StatutAbonnement.ACCEPTE);
        demande.setDateAcceptation(Instant.now());
        abonnementRepository.save(demande);

        notifications.notifier(demande.getAbonne(), moi, TypeNotification.DEMANDE_ACCEPTEE);
        notifications.retirer(moi.getId(), id, TypeNotification.DEMANDE_RECUE);

        return ResponseEntity.noContent().build();
    }

    /** Refuser la demande du compte {id}. Il pourra redemander plus tard. */
    @DeleteMapping("/demandes/{id}")
    @Transactional
    public ResponseEntity<Void> refuser(@PathVariable Long id, @AuthenticationPrincipal String email) {
        Utilisateur moi = utilisateurConnecte(email);

        abonnementRepository.findByAbonneIdAndSuiviId(id, moi.getId())
                .filter(a -> a.getStatut() == StatutAbonnement.EN_ATTENTE)
                .ifPresent(abonnementRepository::delete);

        notifications.retirer(moi.getId(), id, TypeNotification.DEMANDE_RECUE);

        return ResponseEntity.noContent().build();
    }

    /**
     * Retirer un abonné : il ne me suit plus, sans être bloqué — il peut
     * redemander. C'est la mesure douce ; le blocage est la mesure forte.
     */
    @DeleteMapping("/abonnes/{id}")
    @Transactional
    public ResponseEntity<Void> retirerAbonne(@PathVariable Long id, @AuthenticationPrincipal String email) {
        Utilisateur moi = utilisateurConnecte(email);

        abonnementRepository.findByAbonneIdAndSuiviId(id, moi.getId())
                .ifPresent(abonnementRepository::delete);

        notifications.retirer(moi.getId(), id, TypeNotification.NOUVEL_ABONNE, TypeNotification.DEMANDE_RECUE);

        return ResponseEntity.noContent().build();
    }

    /** Écarte les comptes désactivés ou bloqués, trie par nom, construit les DTO. */
    private List<CompteResume> affichables(VueRelations vue, List<Utilisateur> comptes) {
        return vuesComptes.resumes(vue, comptes.stream()
                .filter(u -> u.isActif() && !vue.bloque(u.getId()))
                .sorted(Comparator.comparing(Utilisateur::getNomAffichage, String.CASE_INSENSITIVE_ORDER))
                .toList());
    }
}

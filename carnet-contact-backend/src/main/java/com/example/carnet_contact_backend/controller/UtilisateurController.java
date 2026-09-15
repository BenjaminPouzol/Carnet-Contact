package com.example.carnet_contact_backend.controller;

import com.example.carnet_contact_backend.abonnement.Notifications;
import com.example.carnet_contact_backend.abonnement.Relations;
import com.example.carnet_contact_backend.abonnement.VueRelations;
import com.example.carnet_contact_backend.model.Abonnement;
import com.example.carnet_contact_backend.model.StatutAbonnement;
import com.example.carnet_contact_backend.model.TypeNotification;
import com.example.carnet_contact_backend.model.Utilisateur;
import com.example.carnet_contact_backend.repository.AbonnementRepository;
import com.example.carnet_contact_backend.repository.UtilisateurRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/utilisateurs")
public class UtilisateurController {

    private static final int TAILLE_RECHERCHE = 20;
    private static final int NOMBRE_SUGGESTIONS = 5;

    private final UtilisateurRepository utilisateurRepository;
    private final AbonnementRepository abonnementRepository;
    private final Relations relations;
    private final Notifications notifications;
    private final VuesComptes vuesComptes;

    public UtilisateurController(
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

    /**
     * Le corps d'une modification de profil.
     *
     * Un champ ABSENT (null) laisse la valeur actuelle en place ; une chaîne VIDE
     * l'efface. Le client peut ainsi n'envoyer que ce qui change.
     *
     * @Email n'accepte que ce qui ressemble à une adresse — et laisse passer null
     * et la chaîne vide, ce qui convient à un champ facultatif.
     */
    public record DemandeProfil(
            String nomAffichage,
            String photoUrl,

            @Email(message = "L'email professionnel n'est pas une adresse valide.")
            @Size(max = 255, message = "L'email professionnel dépasse 255 caractères.")
            String emailPro,

            @Size(max = 255) String instagram,
            @Size(max = 255) String twitter,
            @Size(max = 255) String facebook,
            @Size(max = 255) String twitch,
            @Size(max = 255) String youtube,
            @Size(max = 255) String linkedin,

            // Boolean (objet) et non boolean : il faut pouvoir distinguer « absent »
            // de « false ».
            Boolean comptePrive) {}

    /** `enCommun` : combien de mes abonnements suivent déjà ce compte. */
    public record Suggestion(CompteResume compte, long enCommun) {}

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
     * Rechercher des comptes par nom affiché.
     *
     * Sans terme, la même route rend les premiers comptes par ordre alphabétique.
     * Elle remplace l'ancienne liste de TOUS les comptes, qui servait à la
     * messagerie : celle-ci a désormais sa propre liste d'interlocuteurs.
     */
    @GetMapping
    public List<CompteResume> rechercher(
            @RequestParam(defaultValue = "") String recherche,
            @AuthenticationPrincipal String email) {
        Utilisateur moi = utilisateurConnecte(email);
        VueRelations vue = relations.vuePour(moi);

        return vuesComptes.resumes(vue, utilisateurRepository.rechercher(
                moi.getId(), recherche.trim(), PageRequest.of(0, TAILLE_RECHERCHE)));
    }

    /**
     * Des comptes à suivre, en trois niveaux :
     * 1. ceux que suivent déjà les comptes que je suis — un « ami d'ami » est la
     *    meilleure piste, et plus il y a de chemins qui y mènent, mieux c'est ;
     * 2. à défaut, les comptes les plus suivis ;
     * 3. à défaut encore, les derniers inscrits — sans quoi, tant que personne ne
     *    suit personne, la liste resterait vide.
     *
     * LinkedHashMap : une Map qui garde l'ORDRE d'insertion. putIfAbsent n'écrase
     * jamais : un compte trouvé au niveau 1 garde son rang et son compteur.
     */
    @GetMapping("/suggestions")
    public List<Suggestion> suggestions(@AuthenticationPrincipal String email) {
        Utilisateur moi = utilisateurConnecte(email);
        VueRelations vue = relations.vuePour(moi);

        Map<Long, Long> candidats = new LinkedHashMap<>();
        for (Object[] ligne : abonnementRepository.suivisParMesAbonnements(moi.getId(), StatutAbonnement.ACCEPTE)) {
            candidats.putIfAbsent((Long) ligne[0], (Long) ligne[1]);
        }
        for (Object[] ligne : abonnementRepository.lesPlusSuivis(StatutAbonnement.ACCEPTE)) {
            candidats.putIfAbsent((Long) ligne[0], 0L);
        }
        for (Utilisateur recent : utilisateurRepository.findTop20ByActifTrueAndIdNotOrderByDateInscriptionDesc(moi.getId())) {
            candidats.putIfAbsent(recent.getId(), 0L);
        }

        // Exclusions qui se lisent dans la vue, sans requête : moi, les comptes
        // déjà suivis ou demandés, les blocages.
        List<Long> ids = candidats.keySet().stream()
                .filter(id -> !vue.estMoi(id) && !vue.sortantsTous().contains(id) && !vue.bloque(id))
                .toList();

        // UNE requête pour charger tous les comptes retenus, puis on les replace
        // dans l'ordre des candidats (findAllById ne le garantit pas).
        Map<Long, Utilisateur> comptes = utilisateurRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Utilisateur::getId, Function.identity()));

        return ids.stream()
                .map(comptes::get)
                .filter(u -> u != null && u.isActif())
                .limit(NOMBRE_SUGGESTIONS)
                .map(u -> new Suggestion(vuesComptes.resume(vue, u), candidats.get(u.getId())))
                .toList();
    }

    /**
     * La page d'une personne.
     *
     * 404 pour un compte inconnu, désactivé, ou en relation de blocage : dans
     * les trois cas, il n'y a rien à montrer — et un blocage ne doit pas se
     * signaler par un code différent.
     */
    @GetMapping("/{id}")
    public ProfilPublic profil(@PathVariable Long id, @AuthenticationPrincipal String email) {
        Utilisateur moi = utilisateurConnecte(email);

        Utilisateur autre = utilisateurRepository.findById(id)
                .filter(u -> u.isActif() || u.getId().equals(moi.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        VueRelations vue = relations.vuePour(moi);
        if (vue.bloque(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        return vuesComptes.profil(vue, autre);
    }

    /**
     * Mise à jour de son profil.
     *
     * @Valid déclenche les annotations du record avant d'entrer dans la méthode :
     * un email professionnel mal formé donne un 400 sans qu'une ligne ne
     * s'exécute (section 33).
     */
    @PutMapping("/moi")
    @Transactional
    public Utilisateur modifierProfil(
            @Valid @RequestBody DemandeProfil demande,
            @AuthenticationPrincipal String email) {
        Utilisateur moi = utilisateurConnecte(email);
        boolean etaitPrive = moi.isComptePrive();

        if (demande.nomAffichage() != null && !demande.nomAffichage().isBlank()) {
            moi.setNomAffichage(demande.nomAffichage());
        }
        // La photo peut être volontairement effacée : on accepte la chaîne
        // vide, qu'on ramène à null pour ne pas stocker de valeur bidon.
        if (demande.photoUrl() != null) {
            moi.setPhotoUrl(videEnNull(demande.photoUrl()));
        }
        if (demande.emailPro() != null) {
            moi.setEmailPro(videEnNull(demande.emailPro()));
        }
        if (demande.instagram() != null) {
            moi.setInstagram(videEnNull(demande.instagram()));
        }
        if (demande.twitter() != null) {
            moi.setTwitter(videEnNull(demande.twitter()));
        }
        if (demande.facebook() != null) {
            moi.setFacebook(videEnNull(demande.facebook()));
        }
        if (demande.twitch() != null) {
            moi.setTwitch(videEnNull(demande.twitch()));
        }
        if (demande.youtube() != null) {
            moi.setYoutube(videEnNull(demande.youtube()));
        }
        if (demande.linkedin() != null) {
            moi.setLinkedin(videEnNull(demande.linkedin()));
        }
        if (demande.comptePrive() != null) {
            moi.setComptePrive(demande.comptePrive());
        }

        Utilisateur enregistre = utilisateurRepository.save(moi);

        if (etaitPrive && !enregistre.isComptePrive()) {
            accepterLesDemandesEnAttente(enregistre);
        }

        return enregistre;
    }

    /**
     * Repasser en public, c'est dire « tout le monde peut me suivre » : les
     * demandes qui attendaient n'ont plus de raison d'attendre.
     *
     * Même ordre que dans AbonnementController.accepter, pour la même raison :
     * les statuts sont enregistrés et les identifiants mis de côté AVANT que la
     * suppression des notifications ne vide le contexte de persistance.
     */
    private void accepterLesDemandesEnAttente(Utilisateur moi) {
        List<Abonnement> demandes = abonnementRepository.entrantsDe(moi.getId(), StatutAbonnement.EN_ATTENTE);
        if (demandes.isEmpty()) {
            return;
        }

        Instant maintenant = Instant.now();
        for (Abonnement demande : demandes) {
            demande.setStatut(StatutAbonnement.ACCEPTE);
            demande.setDateAcceptation(maintenant);
        }
        abonnementRepository.saveAll(demandes);

        List<Long> demandeurs = demandes.stream().map(d -> d.getAbonne().getId()).toList();

        for (Abonnement demande : demandes) {
            notifications.notifier(demande.getAbonne(), moi, TypeNotification.DEMANDE_ACCEPTEE);
        }
        for (Long demandeur : demandeurs) {
            notifications.retirer(moi.getId(), demandeur, TypeNotification.DEMANDE_RECUE);
        }
    }

    private static String videEnNull(String valeur) {
        return valeur.isBlank() ? null : valeur.trim();
    }
}

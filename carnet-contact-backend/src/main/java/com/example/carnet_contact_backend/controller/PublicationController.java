package com.example.carnet_contact_backend.controller;

import com.example.carnet_contact_backend.abonnement.Relations;
import com.example.carnet_contact_backend.controller.Reactions.ReactionResume;
import com.example.carnet_contact_backend.model.Categorie;
import com.example.carnet_contact_backend.model.Publication;
import com.example.carnet_contact_backend.model.ReactionPublication;
import com.example.carnet_contact_backend.model.Role;
import com.example.carnet_contact_backend.model.StatutAbonnement;
import com.example.carnet_contact_backend.model.Utilisateur;
import com.example.carnet_contact_backend.repository.PublicationRepository;
import com.example.carnet_contact_backend.repository.ReactionPublicationRepository;
import com.example.carnet_contact_backend.repository.UtilisateurRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Le fil d'actualité : un espace commun à tous les comptes, où chacun parle de
 * ses hobbies.
 *
 * « Commun », avec deux exceptions arrivées avec les abonnements : un compte
 * privé n'est lu que par ses abonnés acceptés, et un blocage retire les
 * publications de part et d'autre.
 */
@RestController
@RequestMapping("/api/publications")
public class PublicationController {

    /** Au-delà, une seule requête chargerait trop de lignes d'un coup. */
    private static final int TAILLE_MAX = 30;

    private final PublicationRepository publicationRepository;
    private final ReactionPublicationRepository reactionRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final Relations relations;

    public PublicationController(
            PublicationRepository publicationRepository,
            ReactionPublicationRepository reactionRepository,
            UtilisateurRepository utilisateurRepository,
            Relations relations) {
        this.publicationRepository = publicationRepository;
        this.reactionRepository = reactionRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.relations = relations;
    }

    /**
     * Le corps d'une création ou d'une modification, avec ses règles.
     *
     * Les annotations ne font rien seules : c'est le @Valid posé sur le
     * paramètre des méthodes qui déclenche leur vérification.
     *
     * `^$|` dans le motif de l'image : la chaîne vide est acceptée (le champ est
     * facultatif) ; sinon, une adresse http(s) sans espace. Cela écarte
     * notamment une adresse `javascript:…`.
     */
    public record DemandePublication(
            @NotNull(message = "La catégorie est obligatoire.")
            Categorie categorie,

            @NotBlank(message = "Le contenu est obligatoire.")
            @Size(max = 2000, message = "Le contenu dépasse 2000 caractères.")
            String contenu,

            @Size(max = 500, message = "L'adresse de l'image dépasse 500 caractères.")
            @Pattern(regexp = "^$|^https?://\\S+$", message = "L'image doit être une adresse http ou https.")
            String imageUrl) {}

    public record DemandeReaction(String emoji) {}

    /**
     * Ce qu'on renvoie pour une publication.
     *
     * `modifiable` et `supprimable` sont calculés pour le compte qui regarde,
     * comme `estMoi` dans le tableau d'administration : le client n'a ni à
     * comparer des identifiants, ni à connaître les règles de droits.
     */
    public record PublicationVue(
            Long id,
            AuteurPublic auteur,
            Categorie categorie,
            String contenu,
            String imageUrl,
            Instant datePublication,
            Instant dateModification,
            List<ReactionResume> reactions,
            boolean modifiable,
            boolean supprimable) {}

    /** `curseurSuivant` : l'id à passer en `avant` pour la suite, null s'il n'y en a plus. */
    public record PageFil(List<PublicationVue> publications, Long curseurSuivant) {}

    private Utilisateur utilisateurConnecte(String email) {
        return utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    /**
     * Une tranche du fil.
     *
     * Une catégorie inconnue dans l'URL (`?categorie=PEINTURE`) ne peut pas être
     * convertie en Categorie : Spring répond 400 avant même d'entrer ici.
     *
     * Deux filtres se combinent avec la catégorie : `abonnements=true` (seulement
     * les comptes que je suis) et `auteur=<id>` (la page d'une personne). Les
     * règles de visibilité — comptes privés, blocages — s'appliquent toujours,
     * filtre ou pas : elles sont écrites DANS la requête (voir
     * PublicationRepository.fil).
     */
    @GetMapping
    public PageFil fil(
            @RequestParam(required = false) Categorie categorie,
            @RequestParam(required = false) Long avant,
            @RequestParam(defaultValue = "10") int taille,
            @RequestParam(defaultValue = "false") boolean abonnements,
            @RequestParam(required = false) Long auteur,
            @AuthenticationPrincipal String email) {

        Utilisateur moi = utilisateurConnecte(email);
        int tailleBornee = Math.clamp(taille, 1, TAILLE_MAX);

        // Une ligne de PLUS que demandé : le moyen le plus économe de savoir
        // s'il reste des publications plus anciennes, sans requête COUNT.
        List<Publication> lues = publicationRepository.fil(
                categorie, avant, auteur, abonnements,
                moi.getId(), moi.getRole() == Role.ADMIN, StatutAbonnement.ACCEPTE,
                PageRequest.of(0, tailleBornee + 1));

        boolean resteDesPlusAnciennes = lues.size() > tailleBornee;
        List<Publication> tranche = resteDesPlusAnciennes ? lues.subList(0, tailleBornee) : lues;
        Long curseurSuivant = resteDesPlusAnciennes ? tranche.getLast().getId() : null;

        return new PageFil(assembler(tranche, moi), curseurSuivant);
    }

    @PostMapping
    public PublicationVue publier(
            @Valid @RequestBody DemandePublication demande,
            @AuthenticationPrincipal String email) {
        Utilisateur moi = utilisateurConnecte(email);

        Publication publication = new Publication();
        // L'auteur vient du jeton, jamais du corps de la requête : on ne publie
        // pas au nom de quelqu'un d'autre.
        publication.setAuteur(moi);
        appliquer(demande, publication);
        publication.setDatePublication(Instant.now());

        // Une publication neuve n'a aucune réaction : inutile d'interroger la base.
        return vue(publicationRepository.save(publication), List.of(), moi);
    }

    @PutMapping("/{id}")
    public PublicationVue modifier(
            @PathVariable Long id,
            @Valid @RequestBody DemandePublication demande,
            @AuthenticationPrincipal String email) {
        Utilisateur moi = utilisateurConnecte(email);
        Publication publication = publicationExistante(id);

        // Même un administrateur ne réécrit pas les mots d'un autre : il peut
        // retirer une publication, pas parler à la place de son auteur.
        //
        // 403 et non 404 : la publication est publique, tout le monde la voit
        // déjà dans le fil. « Introuvable » serait un mensonge qui ne protège
        // aucun secret — à l'inverse d'un message privé (MessageController).
        if (!estAuteur(publication, moi)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Seul l'auteur peut modifier cette publication.");
        }

        appliquer(demande, publication);
        publication.setDateModification(Instant.now());

        return assembler(List.of(publicationRepository.save(publication)), moi).getFirst();
    }

    /**
     * @Transactional : les réactions partent AVANT la publication (la clé
     * étrangère l'exige), et les deux suppressions réussissent ou échouent
     * ensemble. Il est de toute façon obligatoire pour une requête @Modifying.
     */
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> supprimer(
            @PathVariable Long id,
            @AuthenticationPrincipal String email) {
        Utilisateur moi = utilisateurConnecte(email);
        Publication publication = publicationExistante(id);

        if (!peutSupprimer(publication, moi)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Vous ne pouvez pas supprimer cette publication.");
        }

        reactionRepository.supprimerCellesDeLaPublication(publication.getId());
        publicationRepository.delete(publication);
        return ResponseEntity.noContent().build();
    }

    /** Poser, remplacer ou retirer sa réaction : la même bascule que les messages. */
    @PutMapping("/{id}/reaction")
    public PublicationVue reagir(
            @PathVariable Long id,
            @RequestBody DemandeReaction demande,
            @AuthenticationPrincipal String email) {

        if (!Reactions.estAutorise(demande.emoji())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Emoji non autorisé.");
        }

        Utilisateur moi = utilisateurConnecte(email);
        Publication publication = publicationExistante(id);

        // Une publication que le fil ne me montrerait pas (compte privé non
        // suivi, blocage) ne doit pas devenir atteignable parce que j'en connais
        // l'identifiant. 404 et non 403 : de mon point de vue, elle n'existe pas.
        Utilisateur auteur = publication.getAuteur();
        if (!relations.vuePour(moi).voitContenu(auteur.getId(), auteur.isComptePrive())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        reactionRepository.findByPublicationIdAndUtilisateurId(id, moi.getId())
                .ifPresentOrElse(
                        existante -> {
                            if (existante.getEmoji().equals(demande.emoji())) {
                                // Même emoji : on retire.
                                reactionRepository.delete(existante);
                            } else {
                                // Autre emoji : on remplace.
                                existante.setEmoji(demande.emoji());
                                reactionRepository.save(existante);
                            }
                        },
                        () -> {
                            ReactionPublication nouvelle = new ReactionPublication();
                            nouvelle.setPublication(publication);
                            nouvelle.setUtilisateur(moi);
                            nouvelle.setEmoji(demande.emoji());
                            reactionRepository.save(nouvelle);
                        });

        return assembler(List.of(publication), moi).getFirst();
    }

    private Publication publicationExistante(Long id) {
        return publicationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private void appliquer(DemandePublication demande, Publication publication) {
        publication.setCategorie(demande.categorie());
        publication.setContenu(demande.contenu().strip());
        // Chaîne vide ramenée à null : « pas d'image » ne s'écrit que d'une façon.
        publication.setImageUrl(
                demande.imageUrl() == null || demande.imageUrl().isBlank() ? null : demande.imageUrl());
    }

    private boolean estAuteur(Publication publication, Utilisateur moi) {
        return publication.getAuteur().getId().equals(moi.getId());
    }

    /**
     * Le rôle est lu sur `moi`, rechargé depuis la base à chaque requête, et
     * non dans le jeton. Pour une action destructrice, on ne se fie pas à un
     * rôle qui a pu être retiré depuis l'émission du jeton (section 29).
     */
    private boolean peutSupprimer(Publication publication, Utilisateur moi) {
        return estAuteur(publication, moi) || moi.getRole() == Role.ADMIN;
    }

    /** Les DTO d'un lot de publications, avec leurs réactions chargées en une requête. */
    private List<PublicationVue> assembler(List<Publication> publications, Utilisateur moi) {
        if (publications.isEmpty()) {
            return List.of();
        }

        Map<Long, List<ReactionResume>> reactions = Reactions.resumerParCible(
                reactionRepository.findByPublicationIdIn(
                        publications.stream().map(Publication::getId).toList()),
                moi.getId());

        return publications.stream()
                .map(p -> vue(p, reactions.getOrDefault(p.getId(), List.of()), moi))
                .toList();
    }

    private PublicationVue vue(Publication p, List<ReactionResume> reactions, Utilisateur moi) {
        return new PublicationVue(
                p.getId(),
                AuteurPublic.de(p.getAuteur()),
                p.getCategorie(),
                p.getContenu(),
                p.getImageUrl(),
                p.getDatePublication(),
                p.getDateModification(),
                reactions,
                estAuteur(p, moi),
                peutSupprimer(p, moi));
    }
}

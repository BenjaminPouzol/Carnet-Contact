package com.example.carnet_contact_backend.controller;

import com.example.carnet_contact_backend.model.Contact;
import com.example.carnet_contact_backend.model.Utilisateur;
import com.example.carnet_contact_backend.repository.ContactRepository;
import com.example.carnet_contact_backend.repository.UtilisateurRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;

/**
 * Les contacts sont désormais PRIVÉS : chaque méthode travaille sur ceux de
 * l'utilisateur authentifié, jamais sur toute la table.
 *
 * @CrossOrigin a disparu : le CORS est traité une fois pour toutes dans
 * SecurityConfig.
 */
@RestController
@RequestMapping("/api/contacts")
public class ContactController {

    private final ContactRepository contactRepository;
    private final UtilisateurRepository utilisateurRepository;

    public ContactController(
            ContactRepository contactRepository,
            UtilisateurRepository utilisateurRepository) {
        this.contactRepository = contactRepository;
        this.utilisateurRepository = utilisateurRepository;
    }

    /**
     * Retrouve le compte connecté.
     *
     * @AuthenticationPrincipal injecte le « principal » que le filtre JWT a
     * posé dans le SecurityContext — ici l'email. On ne fait donc JAMAIS
     * confiance à un identifiant venu du corps de la requête ou de l'URL pour
     * savoir qui parle : seul le jeton fait foi.
     */
    private Utilisateur utilisateurConnecte(String email) {
        return utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    /**
     * La réponse d'une liste paginée n'est plus un simple tableau : le client a
     * besoin de savoir combien de résultats existent au total, sinon il ne peut
     * pas afficher « page 2 sur 7 » ni griser le bouton « suivant ».
     *
     * On déclare notre propre record plutôt que de renvoyer le `Page<Contact>`
     * de Spring Data : ce dernier sérialise une douzaine de champs internes
     * (`pageable`, `sort`, `first`, `numberOfElements`…) dont la forme n'est pas
     * garantie d'une version à l'autre. Un DTO maison fige le contrat d'API.
     */
    public record PageContacts(
            List<Contact> contenu,
            int page,
            int taille,
            long total,
            int totalPages) {}

    /**
     * @RequestParam lit un paramètre de la QUERY STRING
     * (/api/contacts?page=2&recherche=dupont), là où @PathVariable lit un
     * morceau du chemin. La règle habituelle : le chemin identifie la
     * ressource, la query string la filtre ou la découpe.
     *
     * defaultValue évite d'avoir à gérer le cas absent : un appel sans
     * paramètre reste valide et donne la première page.
     */
    @GetMapping
    public PageContacts getMesContacts(
            @AuthenticationPrincipal String email,
            @RequestParam(defaultValue = "") String recherche,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int taille) {

        // Le client fixe la taille de page, donc on la borne : sans ce garde-fou,
        // ?taille=1000000 ferait charger toute la table en mémoire d'un coup.
        int tailleBornee = Math.clamp(taille, 1, 50);
        int pageBornee = Math.max(0, page);

        Page<Contact> resultat = contactRepository.rechercher(
                utilisateurConnecte(email).getId(),
                recherche.trim(),
                // Le tri appartient à la pagination : trier APRÈS avoir découpé
                // n'aurait aucun sens (la page 2 doit contenir les résultats
                // 7 à 12 d'un ordre stable, pas six lignes au hasard triées).
                PageRequest.of(pageBornee, tailleBornee, Sort.by("nom").ascending()));

        return new PageContacts(
                resultat.getContent(),
                resultat.getNumber(),
                resultat.getSize(),
                resultat.getTotalElements(),
                resultat.getTotalPages());
    }

    /**
     * Un contact seul.
     *
     * Ce point d'entrée n'existait pas : la page de détail se contentait de
     * chercher dans la liste déjà chargée. La pagination a rendu cette
     * astuce fausse — la liste ne contient plus qu'une page, et le contact
     * demandé peut être sur une autre. Découper une collection oblige donc à
     * offrir un accès unitaire.
     */
    @GetMapping("/{id}")
    public Contact getContact(
            @PathVariable Long id,
            @AuthenticationPrincipal String email) {
        return contactRepository
                .findByIdAndProprietaireId(id, utilisateurConnecte(email).getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    public Contact createContact(
            @RequestBody Contact contact,
            @AuthenticationPrincipal String email) {
        // Le propriétaire est imposé par le serveur, pas lu dans la requête :
        // un client ne peut pas créer un contact dans le carnet d'un autre.
        contact.setProprietaire(utilisateurConnecte(email));
        contact.setId(null);
        return contactRepository.save(contact);
    }

    @PutMapping("/{id}")
    public Contact updateContact(
            @PathVariable Long id,
            @RequestBody Contact contact,
            @AuthenticationPrincipal String email) {
        Utilisateur proprietaire = utilisateurConnecte(email);

        // On vérifie d'abord que ce contact appartient bien au demandeur.
        // Sans cette lecture, un PUT sur /api/contacts/42 écraserait le
        // contact 42 de n'importe qui.
        Contact existant = contactRepository.findByIdAndProprietaireId(id, proprietaire.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        existant.setNom(contact.getNom());
        existant.setPrenom(contact.getPrenom());
        existant.setEmail(contact.getEmail());
        existant.setTelephone(contact.getTelephone());
        existant.setEmailPro(contact.getEmailPro());
        existant.setPhotoUrl(contact.getPhotoUrl());
        existant.setInstagram(contact.getInstagram());
        existant.setTwitter(contact.getTwitter());
        existant.setFacebook(contact.getFacebook());
        existant.setTwitch(contact.getTwitch());
        existant.setYoutube(contact.getYoutube());
        existant.setLinkedin(contact.getLinkedin());

        return contactRepository.save(existant);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteContact(
            @PathVariable Long id,
            @AuthenticationPrincipal String email) {
        Utilisateur proprietaire = utilisateurConnecte(email);

        Contact existant = contactRepository.findByIdAndProprietaireId(id, proprietaire.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        contactRepository.delete(existant);
        return ResponseEntity.noContent().build();
    }
}

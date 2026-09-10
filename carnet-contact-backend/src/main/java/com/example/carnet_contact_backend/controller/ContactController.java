package com.example.carnet_contact_backend.controller;

import com.example.carnet_contact_backend.model.Contact;
import com.example.carnet_contact_backend.model.Utilisateur;
import com.example.carnet_contact_backend.repository.ContactRepository;
import com.example.carnet_contact_backend.repository.UtilisateurRepository;
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

    @GetMapping
    public List<Contact> getMesContacts(@AuthenticationPrincipal String email) {
        return contactRepository.findByProprietaireIdOrderByNomAsc(
                utilisateurConnecte(email).getId());
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

package com.example.carnet_contact_backend.controller;

import com.example.carnet_contact_backend.model.Utilisateur;
import com.example.carnet_contact_backend.repository.UtilisateurRepository;
import com.example.carnet_contact_backend.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Les deux seuls points d'entrée publics de l'API : créer un compte, et
 * échanger un couple email / mot de passe contre un jeton.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(
            UtilisateurRepository utilisateurRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.utilisateurRepository = utilisateurRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /**
     * Un « record » Java : une classe de données immuable écrite en une ligne.
     * Le compilateur en génère le constructeur, les accesseurs, equals() et
     * toString(). Idéal pour transporter les données d'une requête ou d'une
     * réponse (un DTO), sans les cérémonies d'une entité JPA.
     *
     * Pourquoi ne pas recevoir directement un Utilisateur ? Parce que la
     * requête d'inscription n'est pas un utilisateur : elle contient un mot de
     * passe EN CLAIR, qui n'existe nulle part dans l'entité. Des types
     * distincts pour des choses distinctes.
     */
    public record DemandeInscription(String email, String motDePasse, String nomAffichage) {}

    public record DemandeConnexion(String email, String motDePasse) {}

    public record ReponseAuth(String jeton, Utilisateur utilisateur) {}

    @PostMapping("/inscription")
    public ResponseEntity<?> inscription(@RequestBody DemandeInscription demande) {
        if (demande.email() == null || demande.email().isBlank()
                || demande.motDePasse() == null || demande.motDePasse().length() < 6) {
            return ResponseEntity.badRequest()
                    .body("Email requis et mot de passe d'au moins 6 caractères.");
        }

        if (utilisateurRepository.existsByEmail(demande.email())) {
            // 409 Conflict : la requête est correcte, mais elle se heurte à
            // l'état actuel du serveur — ce compte existe déjà.
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Un compte existe déjà avec cet email.");
        }

        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setEmail(demande.email());
        // Le mot de passe est haché AVANT d'atteindre la base. Le mot de passe
        // en clair n'est jamais écrit nulle part, ni en base, ni dans un log.
        utilisateur.setMotDePasse(passwordEncoder.encode(demande.motDePasse()));
        utilisateur.setNomAffichage(
                demande.nomAffichage() == null || demande.nomAffichage().isBlank()
                        ? demande.email()
                        : demande.nomAffichage());

        Utilisateur cree = utilisateurRepository.save(utilisateur);

        // On connecte directement après l'inscription : pas de second
        // formulaire à remplir.
        return ResponseEntity.ok(new ReponseAuth(jwtService.genererJeton(cree.getEmail()), cree));
    }

    @PostMapping("/connexion")
    public ResponseEntity<?> connexion(@RequestBody DemandeConnexion demande) {
        var trouve = utilisateurRepository.findByEmail(demande.email());

        // Un seul message pour « email inconnu » et « mauvais mot de passe ».
        // Distinguer les deux renseignerait un attaquant sur les comptes qui
        // existent réellement.
        if (trouve.isEmpty()
                || !passwordEncoder.matches(demande.motDePasse(), trouve.get().getMotDePasse())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Email ou mot de passe incorrect.");
        }

        Utilisateur utilisateur = trouve.get();
        return ResponseEntity.ok(
                new ReponseAuth(jwtService.genererJeton(utilisateur.getEmail()), utilisateur));
    }
}

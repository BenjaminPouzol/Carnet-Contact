package com.example.carnet_contact_backend.controller;

import com.example.carnet_contact_backend.model.JetonRafraichissement;
import com.example.carnet_contact_backend.model.Utilisateur;
import com.example.carnet_contact_backend.repository.UtilisateurRepository;
import com.example.carnet_contact_backend.security.JwtService;
import com.example.carnet_contact_backend.security.PolitiqueMotDePasse;
import com.example.carnet_contact_backend.security.RafraichissementService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Les points d'entrée publics de l'API : créer un compte, échanger un couple
 * email / mot de passe contre des jetons, renouveler le jeton d'accès, et
 * rendre le jeton de rafraîchissement à la déconnexion.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RafraichissementService rafraichissementService;

    public AuthController(
            UtilisateurRepository utilisateurRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RafraichissementService rafraichissementService) {
        this.utilisateurRepository = utilisateurRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.rafraichissementService = rafraichissementService;
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

    /** Le corps commun à /rafraichir et /deconnexion. */
    public record DemandeRafraichissement(String jetonRafraichissement) {}

    /**
     * La réponse porte désormais DEUX jetons : celui qui sert à chaque requête
     * (court), et celui qui sert à renouveler le premier (long).
     */
    public record ReponseAuth(String jeton, String jetonRafraichissement, Utilisateur utilisateur) {}

    /** Fabrique la paire de jetons d'un compte. Utilisé par les trois entrées. */
    private ReponseAuth ouvrirSession(Utilisateur utilisateur) {
        JetonRafraichissement rafraichissement = rafraichissementService.emettre(utilisateur);
        return new ReponseAuth(
                jwtService.genererJeton(utilisateur.getEmail()),
                rafraichissement.getValeur(),
                utilisateur);
    }

    @PostMapping("/inscription")
    public ResponseEntity<?> inscription(@RequestBody DemandeInscription demande) {
        if (demande.email() == null || demande.email().isBlank()) {
            return ResponseEntity.badRequest().body("Email requis.");
        }

        // La même politique est appliquée côté Angular, pour un retour immédiat
        // pendant la saisie. Elle est REVÉRIFIÉE ici, et ce n'est pas une
        // redondance inutile : la validation du navigateur est un confort
        // d'interface, pas une sécurité — n'importe qui peut envoyer une requête
        // directement à l'API sans passer par le formulaire.
        PolitiqueMotDePasse.Resultat verification =
                PolitiqueMotDePasse.verifier(demande.motDePasse());

        if (!verification.valide()) {
            return ResponseEntity.badRequest().body(verification.message());
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
        return ResponseEntity.ok(ouvrirSession(cree));
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

        return ResponseEntity.ok(ouvrirSession(trouve.get()));
    }

    /**
     * Échange un jeton de rafraîchissement contre une paire neuve.
     *
     * Ce point d'entrée est PUBLIC, et c'est normal : il est appelé justement
     * quand le jeton d'accès n'est plus valable. Exiger une authentification
     * pour venir se réauthentifier n'aurait aucun sens — c'est le même
     * raisonnement que pour /connexion. La preuve d'identité, ici, c'est la
     * possession du jeton de rafraîchissement.
     */
    @PostMapping("/rafraichir")
    public ResponseEntity<?> rafraichir(@RequestBody DemandeRafraichissement demande) {
        if (demande.jetonRafraichissement() == null || demande.jetonRafraichissement().isBlank()) {
            return ResponseEntity.badRequest().body("Jeton de rafraîchissement requis.");
        }

        return rafraichissementService.faireTourner(demande.jetonRafraichissement())
                .<ResponseEntity<?>>map(nouveau -> ResponseEntity.ok(new ReponseAuth(
                        jwtService.genererJeton(nouveau.getUtilisateur().getEmail()),
                        nouveau.getValeur(),
                        nouveau.getUtilisateur())))
                // 401 et non 403 : le client doit comprendre « reconnecte-toi »,
                // pas « tu n'as pas le droit ».
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Jeton de rafraîchissement invalide ou expiré."));
    }

    /**
     * Déconnexion côté serveur : on révoque le jeton de rafraîchissement.
     *
     * Jusqu'ici, se déconnecter revenait à jeter le jeton côté navigateur — le
     * serveur n'en savait rien. Avec un jeton long stocké en base, la
     * déconnexion devient réelle : même recopié ailleurs, ce jeton n'ouvrira
     * plus rien. Le jeton d'accès déjà émis, lui, reste valable jusqu'à son
     * expiration : c'est la contrepartie assumée du « sans état ».
     *
     * 204 No Content : l'opération a réussi et il n'y a rien à renvoyer.
     */
    @PostMapping("/deconnexion")
    public ResponseEntity<Void> deconnexion(@RequestBody DemandeRafraichissement demande) {
        if (demande.jetonRafraichissement() != null) {
            rafraichissementService.revoquer(demande.jetonRafraichissement());
        }
        return ResponseEntity.noContent().build();
    }
}

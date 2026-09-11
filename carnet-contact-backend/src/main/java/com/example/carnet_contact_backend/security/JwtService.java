package com.example.carnet_contact_backend.security;

import com.example.carnet_contact_backend.model.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * Fabrique et vérifie les jetons JWT.
 *
 * Un JWT (JSON Web Token) est une chaîne en trois parties séparées par des
 * points : en-tête.charge_utile.signature. Les deux premières sont du JSON
 * encodé en base64 — donc LISIBLES par n'importe qui : on n'y met jamais de
 * secret. La troisième est une signature calculée avec une clé que seul le
 * serveur connaît.
 *
 * L'intérêt : le serveur n'a rien à stocker. Il n'a pas besoin de se souvenir
 * des sessions ouvertes — il lui suffit de vérifier que la signature du jeton
 * présenté correspond bien à sa clé. C'est ce qu'on appelle une
 * authentification « sans état » (stateless).
 */
@Service
public class JwtService {

    /** Le nom de la revendication qui porte le rôle, dans la charge utile. */
    private static final String CLE_ROLE = "role";

    private final SecretKey cle;
    private final long dureeMs;

    /**
     * @Value injecte une valeur venue de application.properties, au lieu d'un
     * autre bean. La syntaxe ${cle:defaut} fournit une valeur de repli.
     */
    public JwtService(
            @Value("${carnet.jwt.secret}") String secret,
            @Value("${carnet.jwt.duree-ms}") long dureeMs) {
        // HS256 exige une clé d'au moins 256 bits, soit 32 caractères.
        this.cle = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.dureeMs = dureeMs;
    }

    /**
     * Fabrique un jeton pour un email et un rôle. Le « subject » est le champ
     * standard du JWT désignant à qui il appartient ; le rôle est une
     * revendication (« claim ») que l'on ajoute nous-mêmes.
     *
     * Pourquoi transporter le rôle dans le jeton plutôt que de le relire en
     * base à chaque requête ? Pour la même raison qui a fait choisir le JWT :
     * ne rien avoir à demander à la base pour autoriser une requête.
     *
     * La contrepartie est réelle et il faut la connaître : un compte rétrogradé
     * conserve son jeton d'administrateur jusqu'à l'expiration de celui-ci —
     * quinze minutes au pire. C'est le même compromis que pour la révocation
     * (section 24), et c'est précisément ce qui a fait raccourcir la durée du
     * jeton d'accès.
     *
     * Rappel : la charge utile d'un JWT est LISIBLE par tous. Écrire le rôle
     * dedans n'est pas un secret dévoilé — l'utilisateur connaît déjà le sien.
     * Ce qu'il ne peut pas faire, c'est le MODIFIER : la signature ne
     * correspondrait plus.
     */
    public String genererJeton(String email, Role role) {
        Instant maintenant = Instant.now();

        return Jwts.builder()
                .subject(email)
                .claim(CLE_ROLE, role.name())
                .issuedAt(Date.from(maintenant))
                .expiration(Date.from(maintenant.plusMillis(dureeMs)))
                .signWith(cle)
                .compact();
    }

    /**
     * Le rôle contenu dans un jeton valide, ou UTILISATEUR par défaut.
     *
     * Le repli sur le rôle le moins puissant est volontaire : en cas de doute
     * (jeton ancien émis avant l'ajout des rôles, revendication absente ou
     * illisible), on accorde le MOINS de droits possible, jamais le plus.
     */
    public Role roleDuJeton(String jeton) {
        Claims charge = charge(jeton);

        if (charge == null) {
            return Role.UTILISATEUR;
        }

        try {
            return Role.valueOf(charge.get(CLE_ROLE, String.class));
        } catch (IllegalArgumentException | NullPointerException e) {
            return Role.UTILISATEUR;
        }
    }

    /**
     * Vérifie la signature et la date d'expiration, puis rend l'email
     * contenu dans le jeton — ou null si le jeton est invalide.
     *
     * On retourne null plutôt que de laisser filer l'exception : un jeton
     * expiré ou trafiqué n'est pas une panne du serveur, c'est un cas
     * d'usage normal auquel le filtre doit réagir en refusant l'accès.
     */
    public String emailDuJeton(String jeton) {
        Claims charge = charge(jeton);
        return charge == null ? null : charge.getSubject();
    }

    /**
     * Vérifie la signature et l'expiration, puis rend la charge utile — ou
     * null si le jeton n'est pas valable.
     *
     * Extrait en méthode privée parce que `emailDuJeton` et `roleDuJeton` en
     * ont besoin toutes les deux : la vérification cryptographique ne doit
     * exister qu'à un seul endroit. Deux copies finiraient par diverger, et
     * celle qu'on oublierait de corriger serait une faille.
     */
    private Claims charge(String jeton) {
        try {
            return Jwts.parser()
                    .verifyWith(cle)
                    .build()
                    .parseSignedClaims(jeton)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }
}

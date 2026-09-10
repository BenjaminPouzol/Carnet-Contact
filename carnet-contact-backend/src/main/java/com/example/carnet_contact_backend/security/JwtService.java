package com.example.carnet_contact_backend.security;

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
     * Fabrique un jeton pour un email donné. Le « subject » est le champ
     * standard du JWT désignant à qui il appartient.
     */
    public String genererJeton(String email) {
        Instant maintenant = Instant.now();

        return Jwts.builder()
                .subject(email)
                .issuedAt(Date.from(maintenant))
                .expiration(Date.from(maintenant.plusMillis(dureeMs)))
                .signWith(cle)
                .compact();
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
        try {
            Claims charge = Jwts.parser()
                    .verifyWith(cle)
                    .build()
                    .parseSignedClaims(jeton)
                    .getPayload();

            return charge.getSubject();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }
}

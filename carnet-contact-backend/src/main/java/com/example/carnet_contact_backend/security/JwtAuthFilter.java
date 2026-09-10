package com.example.carnet_contact_backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Lit l'en-tête Authorization de chaque requête entrante et, si le jeton est
 * valide, déclare l'utilisateur comme authentifié pour la durée de la requête.
 *
 * C'est l'exact symétrique de l'intercepteur Angular (section 17) : côté
 * client un intercepteur AJOUTE le jeton à chaque requête sortante, côté
 * serveur un filtre le RELIT à chaque requête entrante. Même idée — traiter
 * une préoccupation transverse à un seul endroit — appliquée aux deux bouts.
 *
 * OncePerRequestFilter garantit une seule exécution par requête, même quand
 * le conteneur fait des redirections internes.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String PREFIXE = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest requete,
            HttpServletResponse reponse,
            FilterChain chaine) throws ServletException, IOException {

        String entete = requete.getHeader("Authorization");

        if (entete != null && entete.startsWith(PREFIXE)) {
            String jeton = entete.substring(PREFIXE.length());
            String email = jwtService.emailDuJeton(jeton);

            if (email != null) {
                // On pose l'identité dans le SecurityContext : un porte-clés
                // propre à la requête en cours, que Spring Security consulte
                // ensuite pour décider si l'accès est autorisé, et que les
                // contrôleurs peuvent lire pour savoir QUI parle.
                var authentification = new UsernamePasswordAuthenticationToken(
                        email,      // le "principal" : ici, l'email
                        null,       // pas de mot de passe : le jeton fait foi
                        List.of()   // aucun rôle : l'application n'en a pas
                );
                SecurityContextHolder.getContext().setAuthentication(authentification);
            }
        }

        // Toujours passer la main à la suite de la chaîne, même sans jeton
        // valide : ce filtre AUTHENTIFIE, il n'autorise pas. C'est la
        // configuration de SecurityConfig qui décidera de refuser ou non —
        // sans quoi les routes publiques (connexion, inscription) seraient
        // bloquées elles aussi.
        chaine.doFilter(requete, reponse);
    }
}

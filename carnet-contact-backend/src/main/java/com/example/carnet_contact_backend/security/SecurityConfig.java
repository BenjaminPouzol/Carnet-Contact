package com.example.carnet_contact_backend.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configuration de Spring Security.
 *
 * Dès que la dépendance spring-boot-starter-security est présente, TOUT est
 * fermé par défaut (et un mot de passe aléatoire s'affiche au démarrage).
 * Cette classe redéfinit ce comportement : ce qui est public, ce qui exige un
 * jeton, et où brancher notre filtre.
 */
@Configuration
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    /**
     * BCrypt : l'algorithme de hachage des mots de passe.
     *
     * Hacher n'est pas chiffrer : l'opération est à SENS UNIQUE, il n'existe
     * aucun moyen de retrouver le mot de passe à partir du haché. Pour
     * vérifier une connexion, on rehache ce que l'utilisateur a tapé et on
     * compare les deux hachés.
     *
     * BCrypt ajoute deux propriétés essentielles : un « sel » aléatoire
     * différent pour chaque mot de passe (deux comptes avec le même mot de
     * passe ont donc des hachés différents), et une lenteur volontaire, qui
     * rend le test de milliards de combinaisons impraticable.
     */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CORS repris ici, de façon centralisée. Les annotations
                // @CrossOrigin des contrôleurs ont disparu : quand Spring
                // Security est en place, c'est lui qui doit gérer le CORS,
                // sinon les requêtes preflight OPTIONS sont rejetées avant
                // même d'atteindre le contrôleur.
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // CSRF désactivé : cette protection sert aux applications à
                // session par cookie, où le navigateur envoie l'identité
                // automatiquement. Ici l'identité voyage dans un en-tête que
                // seul notre code JavaScript ajoute — un site tiers ne peut
                // pas le forger, l'attaque visée n'existe pas.
                .csrf(csrf -> csrf.disable())

                // STATELESS : aucune session serveur, aucun cookie de session.
                // Chaque requête se justifie seule, par son jeton.
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        // Le preflight du navigateur ne porte jamais de jeton :
                        // il doit passer sans authentification.
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Inscription et connexion : forcément publiques, on ne
                        // peut pas exiger un jeton pour venir en chercher un.
                        .requestMatchers("/api/auth/**").permitAll()

                        // Console H2, pratique en développement.
                        .requestMatchers("/h2-console/**").permitAll()

                        // Les images sont affichées par des balises <img>, qui
                        // n'envoient jamais le jeton : leur lecture doit être
                        // publique. Seul le GET est ouvert — ENVOYER une image
                        // reste réservé aux comptes connectés. L'identifiant,
                        // un UUID aléatoire, ne se devine pas.
                        .requestMatchers(HttpMethod.GET, "/api/images/*").permitAll()

                        // Le panel d'administration exige le rôle ADMIN, en un
                        // seul endroit pour toutes ses routes. On pourrait
                        // aussi annoter chaque méthode (@PreAuthorize), mais
                        // une règle centralisée se relit d'un coup d'œil et ne
                        // peut pas être oubliée sur une route ajoutée plus tard.
                        //
                        // hasRole("ADMIN") cherche l'autorité « ROLE_ADMIN » :
                        // Spring Security ajoute le préfixe lui-même, d'où
                        // l'obligation de NE PAS l'écrire ici.
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // /error doit rester ouvert. Quand un contrôleur lève
                        // une ResponseStatusException, Spring réachemine la
                        // requête en interne vers /error — et sur ce second
                        // passage, OncePerRequestFilter ne rejoue PAS notre
                        // filtre JWT (comportement par défaut). L'identité
                        // étant perdue, un 404 ressortait transformé en 403.
                        .requestMatchers("/error").permitAll()

                        // Tout le reste exige un jeton valide.
                        .anyRequest().authenticated())

                // Sans point d'entrée explicite, Spring Security répond 403 à
                // une requête non authentifiée. Or 401 et 403 ne disent pas la
                // même chose : 401 = « je ne sais pas qui tu es », 403 = « je
                // sais qui tu es, mais c'est interdit ». Le client a besoin de
                // distinguer les deux pour renvoyer vers la connexion.
                .exceptionHandling(ex -> ex.authenticationEntryPoint(
                        (requete, reponse, erreur) ->
                                reponse.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                                        "Authentification requise")))

                // Notre filtre s'exécute AVANT celui qui traite les
                // formulaires de connexion classiques : quand ce dernier
                // s'exécute, l'identité est déjà posée et il n'a rien à faire.
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)

                // La console H2 s'affiche dans une <frame> ; Spring Security
                // l'interdit par défaut (protection contre le clickjacking).
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:4200"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        // Indispensable ici : sans cela, l'en-tête Authorization envoyé par
        // l'intercepteur Angular serait retiré par le navigateur.
        config.setAllowedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}

package com.example.carnet_contact_backend.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Le cycle de vie complet d'une session : inscription, connexion,
 * renouvellement, déconnexion.
 *
 * Ici les tests passent par les vraies routes HTTP, avec de vrais mots de
 * passe hachés par BCrypt. C'est plus lent qu'un test unitaire, mais c'est le
 * seul moyen de vérifier des règles qui n'existent qu'à l'assemblage — par
 * exemple qu'un jeton de rafraîchissement déjà utilisé cesse de fonctionner.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * Inscrit un compte et rend le corps JSON brut de la réponse.
     *
     * Beaucoup de ces tests enchaînent deux requêtes : la seconde a besoin d'un
     * jeton produit par la première. On ne peut donc pas se contenter des
     * matchers `jsonPath(...)`, qui vérifient sans extraire.
     */
    private String inscrire(String email, String motDePasse) throws Exception {
        return mockMvc.perform(post("/api/auth/inscription")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","motDePasse":"%s","nomAffichage":"Test"}
                                """.formatted(email, motDePasse)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    /**
     * JsonPath.read : la même syntaxe de chemin que les matchers
     * `jsonPath("$.jeton")`, mais utilisée pour RÉCUPÉRER une valeur au lieu de
     * l'affirmer. « $ » désigne la racine du document, puis on descend par
     * points.
     */
    private String lire(String json, String chemin) {
        return JsonPath.read(json, chemin);
    }

    // --- Inscription ---------------------------------------------------------

    @Test
    @DisplayName("L'inscription renvoie les deux jetons et le compte créé")
    void inscription_renvoieLesDeuxJetons() throws Exception {
        String reponse = inscrire("alice@exemple.fr", "MotDeP4sse!");

        assertThat(lire(reponse, "$.jeton")).isNotBlank();
        assertThat(lire(reponse, "$.jetonRafraichissement")).isNotBlank();
        assertThat(lire(reponse, "$.utilisateur.email")).isEqualTo("alice@exemple.fr");
    }

    /**
     * Une régression facile à introduire et impossible à voir à l'œil nu : il
     * suffit de retirer le @JsonIgnore de l'entité pour que chaque réponse
     * expose le mot de passe haché. Un test l'attrape immédiatement.
     */
    @Test
    @DisplayName("Le mot de passe ne sort jamais du serveur")
    void inscription_neRenvoieJamaisLeMotDePasse() throws Exception {
        String reponse = inscrire("alice@exemple.fr", "MotDeP4sse!");

        assertThat(reponse).doesNotContain("motDePasse");
        assertThat(reponse).doesNotContain("MotDeP4sse!");
    }

    @Test
    @DisplayName("Un mot de passe trop court est refusé (400)")
    void inscription_motDePasseTropCourt_renvoie400() throws Exception {
        mockMvc.perform(post("/api/auth/inscription")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"a@exemple.fr\",\"motDePasse\":\"court\"}"))
                .andExpect(status().isBadRequest());
    }

    /**
     * La validation du formulaire Angular est un confort d'interface, pas une
     * sécurité : on peut toujours appeler l'API directement. Ce test vérifie
     * que le serveur refuse aussi de son côté.
     */
    @Test
    @DisplayName("Un mot de passe assez long mais sans majuscule ni chiffre est refusé")
    void inscription_motDePasseFaible_renvoie400() throws Exception {
        mockMvc.perform(post("/api/auth/inscription")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"a@exemple.fr\",\"motDePasse\":\"tellementlong\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Un email déjà pris est refusé (409)")
    void inscription_emailDejaPris_renvoie409() throws Exception {
        inscrire("alice@exemple.fr", "MotDeP4sse!");

        // Le mot de passe doit être valide, sinon le contrôleur s'arrêterait au
        // 400 sans jamais atteindre la vérification de l'email : on rejette une
        // requête MAL FORMÉE (400) avant de consulter l'état du serveur (409).
        mockMvc.perform(post("/api/auth/inscription")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"alice@exemple.fr","motDePasse":"AutreCh0se!"}
                                """))
                .andExpect(status().isConflict());
    }

    // --- Connexion -----------------------------------------------------------

    @Test
    @DisplayName("Un mauvais mot de passe est refusé (401)")
    void connexion_mauvaisMotDePasse_renvoie401() throws Exception {
        inscrire("alice@exemple.fr", "MotDeP4sse!");

        mockMvc.perform(post("/api/auth/connexion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"alice@exemple.fr","motDePasse":"pasbon"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Un email inconnu donne le même 401 qu'un mauvais mot de passe")
    void connexion_emailInconnu_renvoie401() throws Exception {
        // Deux causes, un seul message : distinguer les deux dirait à un
        // attaquant quels comptes existent.
        mockMvc.perform(post("/api/auth/connexion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"personne@exemple.fr","motDePasse":"MotDeP4sse!"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Le jeton reçu à la connexion ouvre bien l'API")
    void connexion_leJetonDonneAcces() throws Exception {
        inscrire("alice@exemple.fr", "MotDeP4sse!");

        String corps = mockMvc.perform(post("/api/auth/connexion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"alice@exemple.fr","motDePasse":"MotDeP4sse!"}
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        mockMvc.perform(get("/api/contacts")
                        .header("Authorization", "Bearer " + lire(corps, "$.jeton")))
                .andExpect(status().isOk());
    }

    // --- Rafraîchissement ----------------------------------------------------

    @Test
    @DisplayName("Le rafraîchissement rend une paire de jetons neuve")
    void rafraichir_rendUneNouvellePaire() throws Exception {
        String inscription = inscrire("alice@exemple.fr", "MotDeP4sse!");
        String ancienRafraichissement = lire(inscription, "$.jetonRafraichissement");

        String reponse = mockMvc.perform(post("/api/auth/rafraichir")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jetonRafraichissement":"%s"}
                                """.formatted(ancienRafraichissement)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // La ROTATION : le jeton long change à chaque usage.
        assertThat(lire(reponse, "$.jetonRafraichissement"))
                .isNotEqualTo(ancienRafraichissement);

        // Et le jeton d'accès neuf fonctionne.
        mockMvc.perform(get("/api/contacts")
                        .header("Authorization", "Bearer " + lire(reponse, "$.jeton")))
                .andExpect(status().isOk());
    }

    /**
     * Le test qui donne tout son sens à la rotation : un jeton déjà consommé
     * est mort. C'est ce qui rend un vol détectable — le voleur et le
     * propriétaire ne peuvent pas se servir du même jeton l'un après l'autre.
     */
    @Test
    @DisplayName("Un jeton de rafraîchissement déjà utilisé est refusé (401)")
    void rafraichir_deuxFoisAvecLeMemeJeton_renvoie401() throws Exception {
        String jetonLong = lire(inscrire("alice@exemple.fr", "MotDeP4sse!"),
                "$.jetonRafraichissement");

        mockMvc.perform(post("/api/auth/rafraichir")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jetonRafraichissement\":\"%s\"}".formatted(jetonLong)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/rafraichir")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jetonRafraichissement\":\"%s\"}".formatted(jetonLong)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Un jeton de rafraîchissement inventé est refusé (401)")
    void rafraichir_jetonInconnu_renvoie401() throws Exception {
        mockMvc.perform(post("/api/auth/rafraichir")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jetonRafraichissement\":\"valeur-inventee\"}"))
                .andExpect(status().isUnauthorized());
    }

    // --- Déconnexion ---------------------------------------------------------

    @Test
    @DisplayName("La déconnexion révoque le jeton de rafraîchissement")
    void deconnexion_revoqueLeJeton() throws Exception {
        String jetonLong = lire(inscrire("alice@exemple.fr", "MotDeP4sse!"),
                "$.jetonRafraichissement");

        mockMvc.perform(post("/api/auth/deconnexion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jetonRafraichissement\":\"%s\"}".formatted(jetonLong)))
                .andExpect(status().isNoContent());

        // C'est ici que se voit la différence avec l'ancienne déconnexion, qui
        // se contentait d'oublier le jeton côté navigateur : même recopié
        // ailleurs, celui-ci n'ouvre plus rien.
        mockMvc.perform(post("/api/auth/rafraichir")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jetonRafraichissement\":\"%s\"}".formatted(jetonLong)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Les routes d'authentification restent publiques")
    void routesAuth_sontPubliques() throws Exception {
        // Un 401 ici ne voudrait pas dire « identifiants refusés » mais
        // « il faut être connecté pour se connecter » — l'impasse.
        mockMvc.perform(post("/api/auth/rafraichir")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jetonRafraichissement\":\"\"}"))
                .andExpect(status().isBadRequest());
    }
}

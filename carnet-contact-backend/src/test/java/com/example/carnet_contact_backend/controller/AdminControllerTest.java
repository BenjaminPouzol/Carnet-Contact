package com.example.carnet_contact_backend.controller;

import com.example.carnet_contact_backend.model.Role;
import com.example.carnet_contact_backend.model.Utilisateur;
import com.example.carnet_contact_backend.repository.PublicationRepository;
import com.example.carnet_contact_backend.repository.ReactionPublicationRepository;
import com.example.carnet_contact_backend.repository.UtilisateurRepository;
import com.example.carnet_contact_backend.security.JwtService;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Le panel d'administration.
 *
 * Deux familles de règles y cohabitent, et les deux méritent des tests :
 *
 * - Celles que Spring Security applique (« seul un ADMIN entre ici »). On les
 *   teste en fabriquant un jeton de simple utilisateur et en vérifiant qu'il
 *   est refusé — c'est la chaîne complète filtre + configuration qui répond.
 * - Celles que Spring Security ne peut pas connaître (« on ne supprime pas le
 *   dernier administrateur »). Elles vivent dans le contrôleur, et ce sont
 *   elles qui empêchent l'application de devenir inadministrable.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private PublicationRepository publicationRepository;

    @Autowired
    private ReactionPublicationRepository reactionPublicationRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private EntityManager entityManager;

    private Utilisateur patron;
    private Utilisateur simple;
    private String jetonPatron;
    private String jetonSimple;

    @BeforeEach
    void preparer() {
        patron = creer("patron@exemple.fr", Role.ADMIN);
        simple = creer("simple@exemple.fr", Role.UTILISATEUR);

        jetonPatron = jwtService.genererJeton(patron.getEmail(), Role.ADMIN);
        jetonSimple = jwtService.genererJeton(simple.getEmail(), Role.UTILISATEUR);
    }

    private Utilisateur creer(String email, Role role) {
        Utilisateur u = new Utilisateur();
        u.setEmail(email);
        u.setMotDePasse("peu-importe");
        u.setNomAffichage(email);
        u.setRole(role);
        u.setActif(true);
        u.setDateInscription(java.time.Instant.now());
        return utilisateurRepository.save(u);
    }

    private Utilisateur relire(Long id) {
        return utilisateurRepository.findById(id).orElseThrow();
    }

    private Long publier(String jeton, String contenu) throws Exception {
        String corps = mockMvc.perform(post("/api/publications")
                        .header("Authorization", "Bearer " + jeton)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categorie\":\"AUTRE\",\"contenu\":\"%s\"}".formatted(contenu)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(corps, "$.id")).longValue();
    }

    private void reagir(String jeton, Long publicationId) throws Exception {
        mockMvc.perform(put("/api/publications/" + publicationId + "/reaction")
                        .header("Authorization", "Bearer " + jeton)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"emoji\":\"👍\"}"))
                .andExpect(status().isOk());
    }

    // --- Accès --------------------------------------------------------------

    @Test
    @DisplayName("Sans jeton, le panel est fermé (401)")
    void sansJeton_renvoie401() throws Exception {
        mockMvc.perform(get("/api/admin/comptes"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * 403 et non 401, et la nuance est exactement celle de la section 18 :
     * « je sais qui tu es, mais c'est interdit », par opposition à « je ne sais
     * pas qui tu es ». Le client en a besoin pour distinguer « reconnecte-toi »
     * de « tu n'as pas les droits ».
     */
    @Test
    @DisplayName("Un simple utilisateur est refusé (403)")
    void simpleUtilisateur_renvoie403() throws Exception {
        mockMvc.perform(get("/api/admin/comptes")
                        .header("Authorization", "Bearer " + jetonSimple))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Un administrateur voit tous les comptes, avec leurs compteurs")
    void admin_voitTousLesComptes() throws Exception {
        mockMvc.perform(get("/api/admin/comptes")
                        .header("Authorization", "Bearer " + jetonPatron))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].email").value("patron@exemple.fr"))
                .andExpect(jsonPath("$[0].role").value("ADMIN"))
                // estMoi évite au client de comparer des identifiants pour
                // savoir quelles actions griser sur sa propre ligne.
                .andExpect(jsonPath("$[0].estMoi").value(true))
                .andExpect(jsonPath("$[1].estMoi").value(false))
                .andExpect(jsonPath("$[1].nombreContacts").value(0));
    }

    /**
     * Le rôle voyage dans le jeton, pas en base : un jeton forgé avec le rôle
     * ADMIN mais signé avec une autre clé ne doit rien ouvrir. C'est la
     * signature, et elle seule, qui rend la revendication crédible.
     */
    @Test
    @DisplayName("Un jeton ADMIN signé avec une autre clé est refusé")
    void jetonAdminMalSigne_renvoie401() throws Exception {
        JwtService faussaire = new JwtService("une-tout-autre-cle-de-32-caracteres-au-moins", 60_000);
        String jetonForge = faussaire.genererJeton(simple.getEmail(), Role.ADMIN);

        mockMvc.perform(get("/api/admin/comptes")
                        .header("Authorization", "Bearer " + jetonForge))
                .andExpect(status().isUnauthorized());
    }

    // --- Activation ---------------------------------------------------------

    @Test
    @DisplayName("Un administrateur peut désactiver puis réactiver un compte")
    void admin_desactiveEtReactive() throws Exception {
        mockMvc.perform(put("/api/admin/comptes/" + simple.getId() + "/actif")
                        .header("Authorization", "Bearer " + jetonPatron)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actif\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actif").value(false));

        assertThat(relire(simple.getId()).isActif()).isFalse();

        mockMvc.perform(put("/api/admin/comptes/" + simple.getId() + "/actif")
                        .header("Authorization", "Bearer " + jetonPatron)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actif\":true}"))
                .andExpect(jsonPath("$.actif").value(true));
    }

    @Test
    @DisplayName("Un compte désactivé ne peut plus se connecter (403)")
    void compteDesactive_neSeConnectePlus() throws Exception {
        // On passe par l'inscription pour avoir un vrai mot de passe haché.
        mockMvc.perform(post("/api/auth/inscription")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"victime@exemple.fr","motDePasse":"MotDeP4sse!"}
                                """))
                .andExpect(status().isOk());

        Long id = utilisateurRepository.findByEmail("victime@exemple.fr").orElseThrow().getId();

        mockMvc.perform(put("/api/admin/comptes/" + id + "/actif")
                        .header("Authorization", "Bearer " + jetonPatron)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actif\":false}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/connexion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"victime@exemple.fr","motDePasse":"MotDeP4sse!"}
                                """))
                // Les identifiants sont bons — c'est le compte qui est fermé.
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("On ne peut pas désactiver son propre compte")
    void admin_neSeDesactivePas() throws Exception {
        mockMvc.perform(put("/api/admin/comptes/" + patron.getId() + "/actif")
                        .header("Authorization", "Bearer " + jetonPatron)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actif\":false}"))
                .andExpect(status().isBadRequest());

        assertThat(relire(patron.getId()).isActif()).isTrue();
    }

    // --- Rôles --------------------------------------------------------------

    @Test
    @DisplayName("Un administrateur peut promouvoir puis rétrograder un autre compte")
    void admin_promeutEtRetrograde() throws Exception {
        mockMvc.perform(put("/api/admin/comptes/" + simple.getId() + "/role")
                        .header("Authorization", "Bearer " + jetonPatron)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));

        assertThat(relire(simple.getId()).getRole()).isEqualTo(Role.ADMIN);

        mockMvc.perform(put("/api/admin/comptes/" + simple.getId() + "/role")
                        .header("Authorization", "Bearer " + jetonPatron)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"UTILISATEUR\"}"))
                .andExpect(jsonPath("$.role").value("UTILISATEUR"));
    }

    @Test
    @DisplayName("On ne peut pas retirer son propre rôle d'administrateur")
    void admin_neSeRetrogradePas() throws Exception {
        mockMvc.perform(put("/api/admin/comptes/" + patron.getId() + "/role")
                        .header("Authorization", "Bearer " + jetonPatron)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"UTILISATEUR\"}"))
                .andExpect(status().isBadRequest());

        assertThat(relire(patron.getId()).getRole()).isEqualTo(Role.ADMIN);
    }

    // --- Le dernier administrateur ------------------------------------------

    /**
     * Le garde-fou central. Sans lui, l'application peut se retrouver sans
     * aucun administrateur — un état dont on ne sort plus par l'interface,
     * puisqu'il faudrait justement être administrateur pour en promouvoir un.
     */
    @Test
    @DisplayName("Le dernier administrateur actif ne peut pas être rétrogradé par un autre")
    void dernierAdmin_nePeutPasEtreRetrograde() throws Exception {
        // Un second admin, qui va tenter de rétrograder le premier alors qu'il
        // est lui-même désactivé : il ne compte donc pas comme « admin actif ».
        Utilisateur adjoint = creer("adjoint@exemple.fr", Role.ADMIN);
        String jetonAdjoint = jwtService.genererJeton(adjoint.getEmail(), Role.ADMIN);

        adjoint.setActif(false);
        utilisateurRepository.save(adjoint);

        mockMvc.perform(put("/api/admin/comptes/" + patron.getId() + "/role")
                        .header("Authorization", "Bearer " + jetonAdjoint)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"UTILISATEUR\"}"))
                .andExpect(status().isBadRequest());

        assertThat(relire(patron.getId()).getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    @DisplayName("Un administrateur peut être rétrogradé s'il en reste un autre")
    void admin_retrogradableSiUnAutreExiste() throws Exception {
        Utilisateur adjoint = creer("adjoint@exemple.fr", Role.ADMIN);

        mockMvc.perform(put("/api/admin/comptes/" + adjoint.getId() + "/role")
                        .header("Authorization", "Bearer " + jetonPatron)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"UTILISATEUR\"}"))
                .andExpect(status().isOk());
    }

    // --- Suppression --------------------------------------------------------

    @Test
    @DisplayName("Un administrateur peut supprimer un compte")
    void admin_supprimeUnCompte() throws Exception {
        mockMvc.perform(delete("/api/admin/comptes/" + simple.getId())
                        .header("Authorization", "Bearer " + jetonPatron))
                .andExpect(status().isNoContent());

        assertThat(utilisateurRepository.findById(simple.getId())).isEmpty();
    }

    /**
     * Trois sortes de lignes pointent vers un compte depuis le fil : ses
     * publications, les réactions des AUTRES sur ses publications, et SES
     * réactions sur les publications des autres. Oublier l'une des trois, et la
     * clé étrangère bloque la suppression.
     */
    @Test
    @DisplayName("Supprimer un compte emporte ses publications et les réactions qui s'y rattachent")
    void supprimerUnCompte_emporteSesPublications() throws Exception {
        Long publicationSimple = publier(jetonSimple, "Ma sortie");
        Long publicationPatron = publier(jetonPatron, "Mon voyage");
        reagir(jetonPatron, publicationSimple);
        reagir(jetonSimple, publicationPatron);

        mockMvc.perform(get("/api/admin/comptes")
                        .header("Authorization", "Bearer " + jetonPatron))
                .andExpect(jsonPath("$[?(@.email == 'simple@exemple.fr')].nombrePublications").value(1));

        mockMvc.perform(delete("/api/admin/comptes/" + simple.getId())
                        .header("Authorization", "Bearer " + jetonPatron))
                .andExpect(status().isNoContent());

        // Écriture forcée : c'est au flush que les clés étrangères sont vérifiées,
        // et dans un test @Transactional le commit n'arrive jamais.
        entityManager.flush();
        entityManager.clear();

        assertThat(publicationRepository.findById(publicationSimple)).isEmpty();
        assertThat(publicationRepository.findById(publicationPatron)).isPresent();
        assertThat(reactionPublicationRepository.count()).isZero();
    }

    @Test
    @DisplayName("On ne peut pas supprimer son propre compte")
    void admin_neSeSupprimePas() throws Exception {
        mockMvc.perform(delete("/api/admin/comptes/" + patron.getId())
                        .header("Authorization", "Bearer " + jetonPatron))
                .andExpect(status().isBadRequest());

        assertThat(utilisateurRepository.findById(patron.getId())).isPresent();
    }

    @Test
    @DisplayName("Un simple utilisateur ne peut supprimer personne")
    void simpleUtilisateur_neSupprimePas() throws Exception {
        mockMvc.perform(delete("/api/admin/comptes/" + patron.getId())
                        .header("Authorization", "Bearer " + jetonSimple))
                .andExpect(status().isForbidden());

        assertThat(utilisateurRepository.findById(patron.getId())).isPresent();
    }

    @Test
    @DisplayName("Un compte inconnu renvoie 404")
    void compteInconnu_renvoie404() throws Exception {
        mockMvc.perform(delete("/api/admin/comptes/999999")
                        .header("Authorization", "Bearer " + jetonPatron))
                .andExpect(status().isNotFound());
    }
}

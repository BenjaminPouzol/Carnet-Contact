package com.example.carnet_contact_backend.controller;

import com.example.carnet_contact_backend.model.Categorie;
import com.example.carnet_contact_backend.model.Publication;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Le fil d'actualité : lecture par curseur, validation, droits et réactions.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PublicationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private PublicationRepository publicationRepository;

    @Autowired
    private ReactionPublicationRepository reactionRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private EntityManager entityManager;

    private Utilisateur alice;
    private Utilisateur bob;
    private String jetonAlice;
    private String jetonBob;
    private String jetonAdmin;

    @BeforeEach
    void preparer() {
        alice = creer("alice@exemple.fr", "Alice", Role.UTILISATEUR);
        bob = creer("bob@exemple.fr", "Bob", Role.UTILISATEUR);
        Utilisateur admin = creer("admin@exemple.fr", "Admin", Role.ADMIN);

        jetonAlice = jwtService.genererJeton(alice.getEmail(), Role.UTILISATEUR);
        jetonBob = jwtService.genererJeton(bob.getEmail(), Role.UTILISATEUR);
        jetonAdmin = jwtService.genererJeton(admin.getEmail(), Role.ADMIN);
    }

    private Utilisateur creer(String email, String nom, Role role) {
        Utilisateur u = new Utilisateur();
        u.setEmail(email);
        u.setMotDePasse("peu-importe");
        u.setNomAffichage(nom);
        u.setRole(role);
        u.setActif(true);
        u.setDateInscription(Instant.now());
        return utilisateurRepository.save(u);
    }

    /** Publie par l'API et rend l'identifiant créé. */
    private Long publier(String jeton, String categorie, String contenu) throws Exception {
        String corps = mockMvc.perform(post("/api/publications")
                        .header("Authorization", "Bearer " + jeton)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categorie\":\"%s\",\"contenu\":\"%s\"}".formatted(categorie, contenu)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(corps, "$.id")).longValue();
    }

    /** Enregistre directement en base : plus rapide quand il en faut beaucoup. */
    private Publication enregistrer(Utilisateur auteur, String contenu) {
        Publication p = new Publication();
        p.setAuteur(auteur);
        p.setCategorie(Categorie.AUTRE);
        p.setContenu(contenu);
        p.setDatePublication(Instant.now());
        return publicationRepository.save(p);
    }

    private ResultActions lireFil(String jeton, String query) throws Exception {
        return mockMvc.perform(get("/api/publications" + query)
                .header("Authorization", "Bearer " + jeton));
    }

    private String corpsDuFil(String jeton, String query) throws Exception {
        return lireFil(jeton, query)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private void reagir(String jeton, Long id, String emoji) throws Exception {
        mockMvc.perform(put("/api/publications/" + id + "/reaction")
                        .header("Authorization", "Bearer " + jeton)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"emoji\":\"%s\"}".formatted(emoji)))
                .andExpect(status().isOk());
    }

    // --- Lecture ------------------------------------------------------------

    @Test
    @DisplayName("Sans jeton, le fil est fermé (401)")
    void sansJeton_renvoie401() throws Exception {
        mockMvc.perform(get("/api/publications"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Une publication apparaît dans le fil de tous, sans l'email de son auteur")
    void publication_visibleParTous_sansEmail() throws Exception {
        publier(jetonAlice, "SPORT", "Sortie vélo dimanche");

        lireFil(jetonBob, "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publications.length()").value(1))
                .andExpect(jsonPath("$.publications[0].contenu").value("Sortie vélo dimanche"))
                .andExpect(jsonPath("$.publications[0].categorie").value("SPORT"))
                .andExpect(jsonPath("$.publications[0].auteur.nomAffichage").value("Alice"))
                .andExpect(jsonPath("$.publications[0].auteur.email").doesNotExist())
                .andExpect(jsonPath("$.publications[0].datePublication").isNotEmpty())
                .andExpect(jsonPath("$.curseurSuivant").value(nullValue()));
    }

    @Test
    @DisplayName("Le fil va du plus récent au plus ancien")
    void fil_duPlusRecentAuPlusAncien() throws Exception {
        publier(jetonAlice, "SPORT", "premiere");
        publier(jetonBob, "CUISINE", "deuxieme");
        publier(jetonAlice, "VOYAGE", "troisieme");

        lireFil(jetonAlice, "")
                .andExpect(jsonPath("$.publications[0].contenu").value("troisieme"))
                .andExpect(jsonPath("$.publications[2].contenu").value("premiere"));
    }

    @Test
    @DisplayName("Le filtre ne garde que la catégorie demandée")
    void filtre_parCategorie() throws Exception {
        publier(jetonAlice, "SPORT", "velo");
        publier(jetonBob, "CUISINE", "tarte");
        publier(jetonBob, "SPORT", "piscine");

        lireFil(jetonAlice, "?categorie=SPORT")
                .andExpect(jsonPath("$.publications.length()").value(2))
                .andExpect(jsonPath("$.publications[0].contenu").value("piscine"))
                .andExpect(jsonPath("$.publications[1].contenu").value("velo"));
    }

    @Test
    @DisplayName("Une catégorie inconnue dans l'URL est refusée (400)")
    void categorieInconnue_renvoie400() throws Exception {
        lireFil(jetonAlice, "?categorie=PEINTURE")
                .andExpect(status().isBadRequest());
    }

    // --- Curseur ------------------------------------------------------------

    @Test
    @DisplayName("Le curseur parcourt tout le fil, sans doublon ni oubli")
    void curseur_parcourtToutSansDoublon() throws Exception {
        for (int i = 1; i <= 5; i++) {
            enregistrer(alice, "p" + i);
        }

        String page1 = corpsDuFil(jetonAlice, "?taille=2");
        assertThat(JsonPath.<List<String>>read(page1, "$.publications[*].contenu"))
                .containsExactly("p5", "p4");
        long curseur1 = ((Number) JsonPath.read(page1, "$.curseurSuivant")).longValue();

        String page2 = corpsDuFil(jetonAlice, "?taille=2&avant=" + curseur1);
        assertThat(JsonPath.<List<String>>read(page2, "$.publications[*].contenu"))
                .containsExactly("p3", "p2");
        long curseur2 = ((Number) JsonPath.read(page2, "$.curseurSuivant")).longValue();

        String page3 = corpsDuFil(jetonAlice, "?taille=2&avant=" + curseur2);
        assertThat(JsonPath.<List<String>>read(page3, "$.publications[*].contenu"))
                .containsExactly("p1");
        assertThat((Object) JsonPath.read(page3, "$.curseurSuivant")).isNull();
    }

    /**
     * LE test qui justifie le curseur. Avec une pagination par numéro de page,
     * la publication ajoutée entre deux lectures décalerait tout d'un cran : la
     * page 2 réafficherait « p3 », déjà vue en page 1.
     */
    @Test
    @DisplayName("Une publication ajoutée entre deux pages ne décale pas la suivante")
    void insertionEntreDeuxPages_neDecalePas() throws Exception {
        for (int i = 1; i <= 4; i++) {
            enregistrer(alice, "p" + i);
        }

        String page1 = corpsDuFil(jetonAlice, "?taille=2");
        long curseur = ((Number) JsonPath.read(page1, "$.curseurSuivant")).longValue();

        enregistrer(bob, "nouvelle");

        String page2 = corpsDuFil(jetonAlice, "?taille=2&avant=" + curseur);
        assertThat(JsonPath.<List<String>>read(page2, "$.publications[*].contenu"))
                .containsExactly("p2", "p1");
    }

    @Test
    @DisplayName("La taille demandée est bornée à 30")
    void taille_bornee() throws Exception {
        for (int i = 1; i <= 31; i++) {
            enregistrer(alice, "p" + i);
        }

        lireFil(jetonAlice, "?taille=100000")
                .andExpect(jsonPath("$.publications.length()").value(30))
                .andExpect(jsonPath("$.curseurSuivant").isNumber());
    }

    // --- Validation ---------------------------------------------------------

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"contenu\":\"Sans catégorie\"}",
            "{\"categorie\":\"SPORT\",\"contenu\":\"   \"}",
            "{\"categorie\":\"PEINTURE\",\"contenu\":\"Catégorie inconnue\"}",
            "{\"categorie\":\"SPORT\",\"contenu\":\"Image piégée\",\"imageUrl\":\"javascript:alert(1)\"}"
    })
    @DisplayName("Une publication invalide est refusée (400)")
    void publicationInvalide_renvoie400(String corps) throws Exception {
        mockMvc.perform(post("/api/publications")
                        .header("Authorization", "Bearer " + jetonAlice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corps))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Un contenu de plus de 2000 caractères est refusé (400)")
    void contenuTropLong_renvoie400() throws Exception {
        mockMvc.perform(post("/api/publications")
                        .header("Authorization", "Bearer " + jetonAlice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categorie\":\"SPORT\",\"contenu\":\"%s\"}".formatted("a".repeat(2001))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Une image vide est enregistrée comme absente")
    void imageVide_devientNull() throws Exception {
        mockMvc.perform(post("/api/publications")
                        .header("Authorization", "Bearer " + jetonAlice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categorie\":\"SPORT\",\"contenu\":\"Vélo\",\"imageUrl\":\"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imageUrl").value(nullValue()));
    }

    // --- Droits -------------------------------------------------------------

    @Test
    @DisplayName("Les drapeaux modifiable / supprimable dépendent de qui regarde")
    void drapeaux_selonCeluiQuiRegarde() throws Exception {
        publier(jetonAlice, "SPORT", "velo");

        lireFil(jetonAlice, "")
                .andExpect(jsonPath("$.publications[0].modifiable").value(true))
                .andExpect(jsonPath("$.publications[0].supprimable").value(true));
        lireFil(jetonBob, "")
                .andExpect(jsonPath("$.publications[0].modifiable").value(false))
                .andExpect(jsonPath("$.publications[0].supprimable").value(false));
        lireFil(jetonAdmin, "")
                .andExpect(jsonPath("$.publications[0].modifiable").value(false))
                .andExpect(jsonPath("$.publications[0].supprimable").value(true));
    }

    @Test
    @DisplayName("L'auteur modifie sa publication, et la date de modification apparaît")
    void modifier_parLAuteur() throws Exception {
        Long id = publier(jetonAlice, "SPORT", "velo");

        mockMvc.perform(put("/api/publications/" + id)
                        .header("Authorization", "Bearer " + jetonAlice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categorie\":\"NATURE\",\"contenu\":\"randonnee\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categorie").value("NATURE"))
                .andExpect(jsonPath("$.contenu").value("randonnee"))
                .andExpect(jsonPath("$.dateModification").isNotEmpty());
    }

    /**
     * 403 et non 404 : la publication est PUBLIQUE, tout le monde la voit dans
     * le fil. Répondre « introuvable » serait un mensonge qui n'abrite aucun
     * secret — contrairement aux messages privés, où le 404 cache une existence.
     */
    @Test
    @DisplayName("Ni un autre compte ni un administrateur ne modifient la publication d'autrui (403)")
    void modifier_parUnAutre_renvoie403() throws Exception {
        Long id = publier(jetonAlice, "SPORT", "velo");

        for (String jeton : List.of(jetonBob, jetonAdmin)) {
            mockMvc.perform(put("/api/publications/" + id)
                            .header("Authorization", "Bearer " + jeton)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"categorie\":\"SPORT\",\"contenu\":\"piratage\"}"))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    @DisplayName("Une publication inconnue renvoie 404")
    void publicationInconnue_renvoie404() throws Exception {
        mockMvc.perform(delete("/api/publications/999999")
                        .header("Authorization", "Bearer " + jetonAlice))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Un autre compte ne supprime pas la publication d'autrui (403)")
    void supprimer_parUnAutre_renvoie403() throws Exception {
        Long id = publier(jetonAlice, "SPORT", "velo");

        mockMvc.perform(delete("/api/publications/" + id)
                        .header("Authorization", "Bearer " + jetonBob))
                .andExpect(status().isForbidden());
    }

    /**
     * Le rôle est relu en BASE pour une suppression : un jeton qui prétend
     * ADMIN (compte rétrogradé depuis moins de quinze minutes) ne suffit pas.
     */
    @Test
    @DisplayName("Un jeton ADMIN d'un compte qui ne l'est pas en base ne supprime rien (403)")
    void supprimer_roleReluEnBase() throws Exception {
        Long id = publier(jetonAlice, "SPORT", "velo");
        String jetonBobPerime = jwtService.genererJeton(bob.getEmail(), Role.ADMIN);

        mockMvc.perform(delete("/api/publications/" + id)
                        .header("Authorization", "Bearer " + jetonBobPerime))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Un administrateur supprime la publication d'un autre")
    void supprimer_parUnAdmin() throws Exception {
        Long id = publier(jetonAlice, "SPORT", "velo");

        mockMvc.perform(delete("/api/publications/" + id)
                        .header("Authorization", "Bearer " + jetonAdmin))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Supprimer sa publication emporte ses réactions")
    void supprimer_emporteLesReactions() throws Exception {
        Long id = publier(jetonAlice, "SPORT", "velo");
        reagir(jetonBob, id, "👍");

        mockMvc.perform(delete("/api/publications/" + id)
                        .header("Authorization", "Bearer " + jetonAlice))
                .andExpect(status().isNoContent());

        // flush : force l'écriture en base maintenant, donc la vérification des
        // clés étrangères. Dans un test @Transactional, le commit n'arrive jamais
        // — sans flush, une contrainte violée passerait inaperçue.
        entityManager.flush();
        entityManager.clear();

        assertThat(publicationRepository.findById(id)).isEmpty();
        assertThat(reactionRepository.count()).isZero();
    }

    // --- Réactions ----------------------------------------------------------

    @Test
    @DisplayName("Une réaction est comptée, attribuée, puis retirée au second clic")
    void reaction_cycleComplet() throws Exception {
        Long id = publier(jetonAlice, "SPORT", "velo");

        reagir(jetonBob, id, "👍");
        lireFil(jetonBob, "")
                .andExpect(jsonPath("$.publications[0].reactions[0].emoji").value("👍"))
                .andExpect(jsonPath("$.publications[0].reactions[0].nombre").value(1))
                .andExpect(jsonPath("$.publications[0].reactions[0].parMoi").value(true));
        lireFil(jetonAlice, "")
                .andExpect(jsonPath("$.publications[0].reactions[0].parMoi").value(false));

        reagir(jetonBob, id, "👍");
        lireFil(jetonBob, "")
                .andExpect(jsonPath("$.publications[0].reactions.length()").value(0));
    }

    @Test
    @DisplayName("Un emoji hors liste est refusé (400)")
    void emojiNonAutorise_renvoie400() throws Exception {
        Long id = publier(jetonAlice, "SPORT", "velo");

        mockMvc.perform(put("/api/publications/" + id + "/reaction")
                        .header("Authorization", "Bearer " + jetonBob)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"emoji\":\"🦆\"}"))
                .andExpect(status().isBadRequest());
    }
}

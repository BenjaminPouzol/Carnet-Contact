package com.example.carnet_contact_backend.controller;

import com.example.carnet_contact_backend.model.Role;
import com.example.carnet_contact_backend.model.Utilisateur;
import com.example.carnet_contact_backend.repository.UtilisateurRepository;
import com.example.carnet_contact_backend.security.JwtService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Messagerie : accusé de lecture et réactions.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MessageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private JwtService jwtService;

    private Utilisateur alice;
    private Utilisateur bob;
    private Utilisateur carol;
    private String jetonAlice;
    private String jetonBob;
    private String jetonCarol;

    @BeforeEach
    void preparer() {
        alice = creer("alice@exemple.fr");
        bob = creer("bob@exemple.fr");
        carol = creer("carol@exemple.fr");

        jetonAlice = jwtService.genererJeton(alice.getEmail(), Role.UTILISATEUR);
        jetonBob = jwtService.genererJeton(bob.getEmail(), Role.UTILISATEUR);
        jetonCarol = jwtService.genererJeton(carol.getEmail(), Role.UTILISATEUR);
    }

    private Utilisateur creer(String email) {
        Utilisateur u = new Utilisateur();
        u.setEmail(email);
        u.setMotDePasse("peu-importe");
        u.setNomAffichage(email);
        u.setDateInscription(Instant.now());
        return utilisateurRepository.save(u);
    }

    /** Envoie un message et rend son identifiant. */
    private Long envoyer(String jetonExpediteur, Long destinataireId, String contenu) throws Exception {
        String corps = mockMvc.perform(post("/api/messages")
                        .header("Authorization", "Bearer " + jetonExpediteur)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"destinataireId":%d,"contenu":"%s"}
                                """.formatted(destinataireId, contenu)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return ((Number) JsonPath.read(corps, "$.id")).longValue();
    }

    private void reagir(String jeton, Long messageId, String emoji) throws Exception {
        mockMvc.perform(put("/api/messages/" + messageId + "/reaction")
                        .header("Authorization", "Bearer " + jeton)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"emoji\":\"%s\"}".formatted(emoji)))
                .andExpect(status().isOk());
    }

    // --- Accusé de lecture --------------------------------------------------

    @Test
    @DisplayName("Un message part non lu, et le reste tant que personne ne l'ouvre")
    void messageEnvoye_estNonLu() throws Exception {
        envoyer(jetonAlice, bob.getId(), "Bonjour");

        // Alice relit son propre fil : Bob n'a rien ouvert, le message est
        // toujours non lu. C'est cette valeur qui affiche « Envoyé » plutôt
        // que « Lu » sous sa bulle.
        mockMvc.perform(get("/api/messages/" + bob.getId())
                        .header("Authorization", "Bearer " + jetonAlice))
                .andExpect(jsonPath("$[0].lu").value(false));
    }

    /**
     * L'accusé de lecture n'est pas une action explicite : ouvrir le fil
     * suffit. C'est le GET lui-même qui marque les messages reçus comme lus.
     */
    @Test
    @DisplayName("Ouvrir le fil marque comme lus les messages reçus")
    void ouvrirLeFil_marqueCommeLu() throws Exception {
        envoyer(jetonAlice, bob.getId(), "Bonjour");

        mockMvc.perform(get("/api/messages/" + alice.getId())
                        .header("Authorization", "Bearer " + jetonBob))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/messages/" + bob.getId())
                        .header("Authorization", "Bearer " + jetonAlice))
                .andExpect(jsonPath("$[0].lu").value(true));
    }

    @Test
    @DisplayName("Lire son propre message ne le marque pas comme lu")
    void relireSonPropreMessage_neMarquePas() throws Exception {
        envoyer(jetonAlice, bob.getId(), "Bonjour");

        // Sinon on s'enverrait à soi-même un accusé de lecture, et l'expéditeur
        // croirait son message lu alors que personne ne l'a ouvert.
        mockMvc.perform(get("/api/messages/" + bob.getId())
                        .header("Authorization", "Bearer " + jetonAlice))
                .andExpect(jsonPath("$[0].lu").value(false));
    }

    @Test
    @DisplayName("La date d'envoi est renvoyée avec le message")
    void message_porteSaDateDEnvoi() throws Exception {
        envoyer(jetonAlice, bob.getId(), "Bonjour");

        mockMvc.perform(get("/api/messages/" + bob.getId())
                        .header("Authorization", "Bearer " + jetonAlice))
                .andExpect(jsonPath("$[0].dateEnvoi").isNotEmpty());
    }

    // --- Réactions ----------------------------------------------------------

    @Test
    @DisplayName("Une réaction est comptée et attribuée à celui qui l'a posée")
    void reaction_estCompteeEtAttribuee() throws Exception {
        Long id = envoyer(jetonAlice, bob.getId(), "Bonjour");

        reagir(jetonBob, id, "👍");

        // Pour Bob, c'est SA réaction : le bouton doit apparaître sélectionné.
        mockMvc.perform(get("/api/messages/" + alice.getId())
                        .header("Authorization", "Bearer " + jetonBob))
                .andExpect(jsonPath("$[0].reactions[0].emoji").value("👍"))
                .andExpect(jsonPath("$[0].reactions[0].nombre").value(1))
                .andExpect(jsonPath("$[0].reactions[0].parMoi").value(true));

        // Pour Alice, la même réaction est comptée mais n'est pas la sienne.
        mockMvc.perform(get("/api/messages/" + bob.getId())
                        .header("Authorization", "Bearer " + jetonAlice))
                .andExpect(jsonPath("$[0].reactions[0].nombre").value(1))
                .andExpect(jsonPath("$[0].reactions[0].parMoi").value(false));
    }

    @Test
    @DisplayName("Recliquer sur le même emoji retire la réaction")
    void memeEmojiDeuxFois_retireLaReaction() throws Exception {
        Long id = envoyer(jetonAlice, bob.getId(), "Bonjour");

        reagir(jetonBob, id, "👍");
        reagir(jetonBob, id, "👍");

        mockMvc.perform(get("/api/messages/" + alice.getId())
                        .header("Authorization", "Bearer " + jetonBob))
                .andExpect(jsonPath("$[0].reactions.length()").value(0));
    }

    @Test
    @DisplayName("Choisir un autre emoji remplace le précédent")
    void autreEmoji_remplace() throws Exception {
        Long id = envoyer(jetonAlice, bob.getId(), "Bonjour");

        reagir(jetonBob, id, "👍");
        reagir(jetonBob, id, "❤️");

        // Une réaction par personne : on ne cumule pas, on remplace.
        mockMvc.perform(get("/api/messages/" + alice.getId())
                        .header("Authorization", "Bearer " + jetonBob))
                .andExpect(jsonPath("$[0].reactions.length()").value(1))
                .andExpect(jsonPath("$[0].reactions[0].emoji").value("❤️"));
    }

    @Test
    @DisplayName("Deux personnes peuvent poser le même emoji, qui se cumule")
    void deuxPersonnes_memeEmoji_seCumulent() throws Exception {
        Long id = envoyer(jetonAlice, bob.getId(), "Bonjour");

        reagir(jetonAlice, id, "👍");
        reagir(jetonBob, id, "👍");

        mockMvc.perform(get("/api/messages/" + alice.getId())
                        .header("Authorization", "Bearer " + jetonBob))
                .andExpect(jsonPath("$[0].reactions.length()").value(1))
                .andExpect(jsonPath("$[0].reactions[0].nombre").value(2));
    }

    /**
     * Sans cette vérification, connaître un identifiant suffirait à réagir au
     * message privé de deux inconnus — et à en découvrir l'existence.
     */
    @Test
    @DisplayName("On ne peut pas réagir au message d'une conversation qui n'est pas la sienne")
    void reagirAuMessageDesAutres_renvoie404() throws Exception {
        Long id = envoyer(jetonAlice, bob.getId(), "Secret");

        mockMvc.perform(put("/api/messages/" + id + "/reaction")
                        .header("Authorization", "Bearer " + jetonCarol)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"emoji\":\"👍\"}"))
                // 404 et non 403 : un 403 confirmerait que ce message existe.
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Un emoji hors de la liste autorisée est refusé (400)")
    void emojiNonAutorise_renvoie400() throws Exception {
        Long id = envoyer(jetonAlice, bob.getId(), "Bonjour");

        // La barre Angular ne propose que cinq emojis, mais rien n'empêche
        // d'appeler l'API à la main : le serveur ne fait pas confiance au client.
        mockMvc.perform(put("/api/messages/" + id + "/reaction")
                        .header("Authorization", "Bearer " + jetonBob)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"emoji\":\"🦆\"}"))
                .andExpect(status().isBadRequest());
    }
}

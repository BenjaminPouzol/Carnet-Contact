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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * L'envoi et la lecture des images.
 *
 * `multipart()` est l'équivalent MockMvc d'un formulaire qui envoie un fichier :
 * il construit le corps découpé en parties, avec la frontière qui les sépare.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ImageControllerTest {

    /** Juste la signature PNG et quelques octets : FormatImage ne lit que le début. */
    static final byte[] PNG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D};

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private JwtService jwtService;

    private String jeton;

    @BeforeEach
    void preparer() {
        Utilisateur alice = new Utilisateur();
        alice.setEmail("alice@exemple.fr");
        alice.setMotDePasse("peu-importe");
        alice.setNomAffichage("Alice");
        alice.setRole(Role.UTILISATEUR);
        alice.setActif(true);
        alice.setDateInscription(Instant.now());
        utilisateurRepository.save(alice);
        jeton = jwtService.genererJeton(alice.getEmail(), Role.UTILISATEUR);
    }

    /** Le nom et le type annoncés sont volontairement flatteurs : le serveur ne doit pas s'y fier. */
    private static MockMultipartFile fichier(String nomPartie, byte[] contenu) {
        return new MockMultipartFile(nomPartie, "photo.png", "image/png", contenu);
    }

    private String envoyer(byte[] contenu) throws Exception {
        String corps = mockMvc.perform(multipart("/api/images")
                        .file(fichier("fichier", contenu))
                        .header("Authorization", "Bearer " + jeton))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(corps, "$.url");
    }

    @Test
    @DisplayName("Un envoi valide renvoie 201 et une adresse absolue")
    void envoi_renvoieUneUrlAbsolue() throws Exception {
        String url = envoyer(PNG);

        // MockMvc simule un serveur « localhost » sans port : l'adresse est bien
        // construite à partir de la requête reçue, pas écrite en dur.
        assertThat(url).startsWith("http://localhost/api/images/");
        assertThat(url.substring(url.lastIndexOf('/') + 1)).hasSize(36);
    }

    @Test
    @DisplayName("Un faux PNG est refusé en 415, quel que soit son nom")
    void fauxPng_renvoie415() throws Exception {
        mockMvc.perform(multipart("/api/images")
                        .file(fichier("fichier", "pas une image".getBytes()))
                        .header("Authorization", "Bearer " + jeton))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    @DisplayName("Un fichier vide est refusé en 400")
    void fichierVide_renvoie400() throws Exception {
        mockMvc.perform(multipart("/api/images")
                        .file(fichier("fichier", new byte[0]))
                        .header("Authorization", "Bearer " + jeton))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Sans partie « fichier », la requête est refusée en 400")
    void partieAbsente_renvoie400() throws Exception {
        mockMvc.perform(multipart("/api/images")
                        .file(fichier("autre", PNG))
                        .header("Authorization", "Bearer " + jeton))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("L'envoi exige un jeton (401)")
    void envoiSansJeton_renvoie401() throws Exception {
        mockMvc.perform(multipart("/api/images").file(fichier("fichier", PNG)))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Le test qui justifie la règle permitAll : une balise <img> ne sait pas
     * envoyer d'en-tête Authorization. Sans lecture publique, aucune image
     * envoyée ne s'afficherait.
     */
    @Test
    @DisplayName("La lecture est publique et rend les mêmes octets, avec le bon type")
    void lecture_estPublique() throws Exception {
        String url = envoyer(PNG);
        String id = url.substring(url.lastIndexOf('/') + 1);

        mockMvc.perform(get("/api/images/" + id))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(content().bytes(PNG))
                .andExpect(header().string("Cache-Control", containsString("immutable")));
    }

    @Test
    @DisplayName("Une image inconnue renvoie 404")
    void imageInconnue_renvoie404() throws Exception {
        mockMvc.perform(get("/api/images/00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isNotFound());
    }
}

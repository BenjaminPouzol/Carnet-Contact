package com.example.carnet_contact_backend.controller;

import com.example.carnet_contact_backend.abonnement.Notifications;
import com.example.carnet_contact_backend.model.Role;
import com.example.carnet_contact_backend.model.TypeNotification;
import com.example.carnet_contact_backend.model.Utilisateur;
import com.example.carnet_contact_backend.repository.UtilisateurRepository;
import com.example.carnet_contact_backend.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private Notifications notifications;

    @Autowired
    private JwtService jwtService;

    private Utilisateur alice;
    private Utilisateur bob;
    private String jetonAlice;
    private String jetonBob;

    @BeforeEach
    void preparer() {
        alice = creer("alice@exemple.fr", "Alice");
        bob = creer("bob@exemple.fr", "Bob");
        jetonAlice = jwtService.genererJeton(alice.getEmail(), Role.UTILISATEUR);
        jetonBob = jwtService.genererJeton(bob.getEmail(), Role.UTILISATEUR);
    }

    private Utilisateur creer(String email, String nom) {
        Utilisateur u = new Utilisateur();
        u.setEmail(email);
        u.setMotDePasse("peu-importe");
        u.setNomAffichage(nom);
        u.setRole(Role.UTILISATEUR);
        u.setActif(true);
        u.setDateInscription(Instant.now());
        return utilisateurRepository.save(u);
    }

    @Test
    @DisplayName("Suivre quelqu'un lui crée une notification non lue, avec l'acteur sans email")
    void suivre_creeUneNonLue() throws Exception {
        mockMvc.perform(put("/api/abonnements/" + bob.getId()).header("Authorization", "Bearer " + jetonAlice))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/notifications/non-lues").header("Authorization", "Bearer " + jetonBob))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].type").value("NOUVEL_ABONNE"))
                .andExpect(jsonPath("$[0].lue").value(false))
                .andExpect(jsonPath("$[0].acteur.nomAffichage").value("Alice"))
                .andExpect(jsonPath("$[0].acteur.email").doesNotExist());

        // Alice, elle, n'a rien reçu.
        mockMvc.perform(get("/api/notifications/non-lues").header("Authorization", "Bearer " + jetonAlice))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("Tout marquer comme lu vide les non-lues, sans rien effacer")
    void marquerLues() throws Exception {
        notifications.notifier(bob, alice, TypeNotification.NOUVEL_ABONNE);

        mockMvc.perform(put("/api/notifications/lues").header("Authorization", "Bearer " + jetonBob))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/notifications/non-lues").header("Authorization", "Bearer " + jetonBob))
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/api/notifications").header("Authorization", "Bearer " + jetonBob))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].lue").value(true));
    }

    @Test
    @DisplayName("Les notifications d'un compte désactivé ne sont plus montrées")
    void acteurDesactive_exclu() throws Exception {
        notifications.notifier(bob, alice, TypeNotification.NOUVEL_ABONNE);
        alice.setActif(false);
        utilisateurRepository.save(alice);

        mockMvc.perform(get("/api/notifications/non-lues").header("Authorization", "Bearer " + jetonBob))
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/api/notifications").header("Authorization", "Bearer " + jetonBob))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("L'historique est limité aux 30 plus récentes")
    void historique_limiteA30() throws Exception {
        for (int i = 0; i < 35; i++) {
            notifications.notifier(bob, alice, TypeNotification.NOUVEL_ABONNE);
        }

        mockMvc.perform(get("/api/notifications").header("Authorization", "Bearer " + jetonBob))
                .andExpect(jsonPath("$.length()").value(30));
    }

    @Test
    @DisplayName("Les notifications exigent un jeton (401)")
    void sansJeton_renvoie401() throws Exception {
        mockMvc.perform(get("/api/notifications/non-lues"))
                .andExpect(status().isUnauthorized());
    }
}

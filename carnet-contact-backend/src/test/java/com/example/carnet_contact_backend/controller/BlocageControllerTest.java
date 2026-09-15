package com.example.carnet_contact_backend.controller;

import com.example.carnet_contact_backend.model.Role;
import com.example.carnet_contact_backend.model.Utilisateur;
import com.example.carnet_contact_backend.repository.AbonnementRepository;
import com.example.carnet_contact_backend.repository.BlocageRepository;
import com.example.carnet_contact_backend.repository.NotificationRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Bloquer : la mesure forte. Tout ce qui reliait deux comptes disparaît, et
 * plus rien ne peut se recréer tant que le blocage existe.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BlocageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private AbonnementRepository abonnementRepository;

    @Autowired
    private BlocageRepository blocageRepository;

    @Autowired
    private NotificationRepository notificationRepository;

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

    private void suivre(String jeton, Long id) throws Exception {
        mockMvc.perform(put("/api/abonnements/" + id).header("Authorization", "Bearer " + jeton))
                .andExpect(status().isOk());
    }

    private void bloquer(String jeton, Long id) throws Exception {
        mockMvc.perform(put("/api/blocages/" + id).header("Authorization", "Bearer " + jeton))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Bloquer supprime les abonnements et les notifications, dans les deux sens")
    void bloquer_supprimeTouteRelation() throws Exception {
        suivre(jetonAlice, bob.getId());
        suivre(jetonBob, alice.getId());
        assertThat(abonnementRepository.count()).isEqualTo(2);
        assertThat(notificationRepository.count()).isEqualTo(2);

        bloquer(jetonAlice, bob.getId());

        assertThat(abonnementRepository.count()).isZero();
        assertThat(notificationRepository.count()).isZero();
        assertThat(blocageRepository.existsByBloqueurIdAndBloqueId(alice.getId(), bob.getId())).isTrue();
    }

    @Test
    @DisplayName("Le compte bloqué ne peut plus suivre ni voir le profil de celui qui l'a bloqué")
    void bloque_nePeutPlusRien() throws Exception {
        bloquer(jetonAlice, bob.getId());

        mockMvc.perform(put("/api/abonnements/" + alice.getId()).header("Authorization", "Bearer " + jetonBob))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/utilisateurs/" + alice.getId()).header("Authorization", "Bearer " + jetonBob))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Bloquer deux fois ne crée pas de doublon")
    void bloquerDeuxFois_estIdempotent() throws Exception {
        bloquer(jetonAlice, bob.getId());
        bloquer(jetonAlice, bob.getId());

        assertThat(blocageRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("On ne se bloque pas soi-même (400), ni un compte inconnu (404)")
    void bloquerSoiMemeOuInconnu() throws Exception {
        mockMvc.perform(put("/api/blocages/" + alice.getId()).header("Authorization", "Bearer " + jetonAlice))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/blocages/999999").header("Authorization", "Bearer " + jetonAlice))
                .andExpect(status().isNotFound());
    }

    /**
     * Débloquer lève l'interdiction, sans remettre en place ce que le blocage a
     * supprimé : ce serait décider à la place des deux personnes.
     */
    @Test
    @DisplayName("Débloquer rend le profil visible, sans restaurer l'abonnement supprimé")
    void debloquer_neRestaureRien() throws Exception {
        suivre(jetonAlice, bob.getId());
        bloquer(jetonAlice, bob.getId());

        mockMvc.perform(delete("/api/blocages/" + bob.getId()).header("Authorization", "Bearer " + jetonAlice))
                .andExpect(status().isNoContent());

        assertThat(blocageRepository.count()).isZero();
        assertThat(abonnementRepository.count()).isZero();
        mockMvc.perform(get("/api/utilisateurs/" + alice.getId()).header("Authorization", "Bearer " + jetonBob))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Seul le bloqueur voit le compte dans sa liste, sans email")
    void liste_visibleDuSeulBloqueur() throws Exception {
        bloquer(jetonAlice, bob.getId());

        mockMvc.perform(get("/api/blocages").header("Authorization", "Bearer " + jetonAlice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(bob.getId()))
                .andExpect(jsonPath("$[0].nomAffichage").value("Bob"))
                .andExpect(jsonPath("$[0].email").doesNotExist());

        mockMvc.perform(get("/api/blocages").header("Authorization", "Bearer " + jetonBob))
                .andExpect(jsonPath("$.length()").value(0));
    }
}

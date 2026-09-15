package com.example.carnet_contact_backend.controller;

import com.example.carnet_contact_backend.model.Blocage;
import com.example.carnet_contact_backend.model.Notification;
import com.example.carnet_contact_backend.model.Role;
import com.example.carnet_contact_backend.model.StatutAbonnement;
import com.example.carnet_contact_backend.model.TypeNotification;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Suivre, demander, accepter, refuser, retirer.
 *
 * Alice et Bob ont des comptes publics, Carol un compte privé.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AbonnementControllerTest {

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
    private Utilisateur carol;
    private String jetonAlice;
    private String jetonBob;
    private String jetonCarol;

    @BeforeEach
    void preparer() {
        alice = creer("alice@exemple.fr", "Alice", false);
        bob = creer("bob@exemple.fr", "Bob", false);
        carol = creer("carol@exemple.fr", "Carol", true);

        jetonAlice = jwtService.genererJeton(alice.getEmail(), Role.UTILISATEUR);
        jetonBob = jwtService.genererJeton(bob.getEmail(), Role.UTILISATEUR);
        jetonCarol = jwtService.genererJeton(carol.getEmail(), Role.UTILISATEUR);
    }

    private Utilisateur creer(String email, String nom, boolean prive) {
        Utilisateur u = new Utilisateur();
        u.setEmail(email);
        u.setMotDePasse("peu-importe");
        u.setNomAffichage(nom);
        u.setRole(Role.UTILISATEUR);
        u.setActif(true);
        u.setComptePrive(prive);
        u.setDateInscription(Instant.now());
        return utilisateurRepository.save(u);
    }

    private ResultActions suivre(String jeton, Long id) throws Exception {
        return mockMvc.perform(put("/api/abonnements/" + id)
                .header("Authorization", "Bearer " + jeton));
    }

    private List<Notification> nonLuesDe(Utilisateur u) {
        return notificationRepository.nonLuesPour(u.getId());
    }

    private StatutAbonnement statutEntre(Utilisateur abonne, Utilisateur suivi) {
        return abonnementRepository.findByAbonneIdAndSuiviId(abonne.getId(), suivi.getId())
                .map(a -> a.getStatut())
                .orElse(null);
    }

    // --- Suivre -------------------------------------------------------------

    @Test
    @DisplayName("Suivre un compte public : accepté tout de suite, et le compte est prévenu")
    void suivreUnComptePublic_accepteEtNotifie() throws Exception {
        suivre(jetonAlice, bob.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(bob.getId()))
                .andExpect(jsonPath("$.statut").value("ACCEPTE"))
                // La réponse décrit un AUTRE compte : pas d'email de connexion.
                .andExpect(jsonPath("$.email").doesNotExist());

        assertThat(statutEntre(alice, bob)).isEqualTo(StatutAbonnement.ACCEPTE);
        assertThat(nonLuesDe(bob))
                .extracting(Notification::getType)
                .containsExactly(TypeNotification.NOUVEL_ABONNE);
        assertThat(nonLuesDe(bob).getFirst().getActeur().getId()).isEqualTo(alice.getId());
    }

    @Test
    @DisplayName("Suivre un compte privé : la demande attend son accord")
    void suivreUnComptePrive_enAttente() throws Exception {
        suivre(jetonAlice, carol.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("EN_ATTENTE"))
                .andExpect(jsonPath("$.comptePrive").value(true));

        assertThat(nonLuesDe(carol))
                .extracting(Notification::getType)
                .containsExactly(TypeNotification.DEMANDE_RECUE);
    }

    /**
     * Un double clic, ou une requête rejouée après une coupure réseau, ne doit
     * ni créer une seconde ligne — la contrainte d'unicité l'interdirait de
     * toute façon, en erreur 500 — ni envoyer une seconde notification.
     */
    @Test
    @DisplayName("Suivre deux fois ne crée ni doublon ni seconde notification")
    void suivreDeuxFois_estIdempotent() throws Exception {
        suivre(jetonAlice, bob.getId()).andExpect(status().isOk());
        suivre(jetonAlice, bob.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("ACCEPTE"));

        assertThat(abonnementRepository.count()).isEqualTo(1);
        assertThat(nonLuesDe(bob)).hasSize(1);
    }

    @Test
    @DisplayName("On ne se suit pas soi-même (400)")
    void suivreSoiMeme_renvoie400() throws Exception {
        suivre(jetonAlice, alice.getId()).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Suivre un compte inconnu renvoie 404")
    void suivreUnCompteInconnu_renvoie404() throws Exception {
        suivre(jetonAlice, 999_999L).andExpect(status().isNotFound());
    }

    /** 404 et non 403 : un compte qui m'a bloqué doit disparaître, pas se signaler. */
    @Test
    @DisplayName("Suivre un compte qui m'a bloqué renvoie 404")
    void suivreUnCompteQuiMaBloque_renvoie404() throws Exception {
        Blocage blocage = new Blocage();
        blocage.setBloqueur(bob);
        blocage.setBloque(alice);
        blocage.setDateBlocage(Instant.now());
        blocageRepository.save(blocage);

        suivre(jetonAlice, bob.getId()).andExpect(status().isNotFound());
        assertThat(abonnementRepository.count()).isZero();
    }

    // --- Ne plus suivre -----------------------------------------------------

    @Test
    @DisplayName("Ne plus suivre supprime l'abonnement et la notification devenue fausse")
    void nePlusSuivre_supprimeTout() throws Exception {
        suivre(jetonAlice, bob.getId()).andExpect(status().isOk());

        mockMvc.perform(delete("/api/abonnements/" + bob.getId())
                        .header("Authorization", "Bearer " + jetonAlice))
                .andExpect(status().isNoContent());

        assertThat(abonnementRepository.count()).isZero();
        // « Alice vous suit » ne serait plus vrai.
        assertThat(nonLuesDe(bob)).isEmpty();
    }

    // --- Demandes -----------------------------------------------------------

    /**
     * Le test qui garde l'ordre des opérations : retirer une notification vide
     * le contexte de persistance (clearAutomatically). Si le passage à ACCEPTE
     * n'était pas enregistré AVANT, il serait perdu sans le moindre message.
     */
    @Test
    @DisplayName("Accepter une demande : l'abonnement devient actif et le demandeur est prévenu")
    void accepterUneDemande() throws Exception {
        suivre(jetonAlice, carol.getId()).andExpect(status().isOk());

        mockMvc.perform(put("/api/abonnements/demandes/" + alice.getId())
                        .header("Authorization", "Bearer " + jetonCarol))
                .andExpect(status().isNoContent());

        assertThat(statutEntre(alice, carol)).isEqualTo(StatutAbonnement.ACCEPTE);
        assertThat(nonLuesDe(carol)).isEmpty();
        assertThat(nonLuesDe(alice))
                .extracting(Notification::getType)
                .containsExactly(TypeNotification.DEMANDE_ACCEPTEE);
    }

    @Test
    @DisplayName("Accepter une demande qui n'existe pas renvoie 404")
    void accepterSansDemande_renvoie404() throws Exception {
        mockMvc.perform(put("/api/abonnements/demandes/" + alice.getId())
                        .header("Authorization", "Bearer " + jetonCarol))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Refuser une demande la supprime, avec sa notification")
    void refuserUneDemande() throws Exception {
        suivre(jetonAlice, carol.getId()).andExpect(status().isOk());

        mockMvc.perform(delete("/api/abonnements/demandes/" + alice.getId())
                        .header("Authorization", "Bearer " + jetonCarol))
                .andExpect(status().isNoContent());

        assertThat(abonnementRepository.count()).isZero();
        assertThat(nonLuesDe(carol)).isEmpty();
    }

    @Test
    @DisplayName("Retirer un abonné supprime son abonnement")
    void retirerUnAbonne() throws Exception {
        suivre(jetonAlice, bob.getId()).andExpect(status().isOk());

        mockMvc.perform(delete("/api/abonnements/abonnes/" + alice.getId())
                        .header("Authorization", "Bearer " + jetonBob))
                .andExpect(status().isNoContent());

        assertThat(abonnementRepository.count()).isZero();
    }

    // --- Listes -------------------------------------------------------------

    @Test
    @DisplayName("Les trois listes : mes abonnements, mes abonnés, les demandes reçues — sans email")
    void listes() throws Exception {
        suivre(jetonAlice, bob.getId()).andExpect(status().isOk());
        suivre(jetonAlice, carol.getId()).andExpect(status().isOk());

        mockMvc.perform(get("/api/abonnements").header("Authorization", "Bearer " + jetonAlice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.id == %d)].statut".formatted(bob.getId())).value("ACCEPTE"))
                .andExpect(jsonPath("$[?(@.id == %d)].statut".formatted(carol.getId())).value("EN_ATTENTE"))
                .andExpect(jsonPath("$[*].email").isEmpty());

        mockMvc.perform(get("/api/abonnements/abonnes").header("Authorization", "Bearer " + jetonBob))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nomAffichage").value("Alice"))
                // Bob ne suit pas Alice en retour : le bouton doit proposer « Suivre ».
                .andExpect(jsonPath("$[0].statut").value("AUCUN"))
                .andExpect(jsonPath("$[0].ilMeSuit").value(true));

        mockMvc.perform(get("/api/abonnements/demandes").header("Authorization", "Bearer " + jetonCarol))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(alice.getId()));
    }

    @Test
    @DisplayName("Les routes d'abonnement exigent un jeton (401)")
    void sansJeton_renvoie401() throws Exception {
        mockMvc.perform(put("/api/abonnements/" + bob.getId()))
                .andExpect(status().isUnauthorized());
    }
}

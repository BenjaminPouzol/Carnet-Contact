package com.example.carnet_contact_backend.controller;

import com.example.carnet_contact_backend.model.Abonnement;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Ce qu'un compte voit des autres : recherche, profil public, suggestions — et
 * ce qu'il renseigne sur lui-même.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UtilisateurControllerTest {

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
    private Utilisateur dave;
    private Utilisateur frank;
    private Utilisateur gina;
    private Utilisateur bobette;
    private String jetonAlice;
    private String jetonCarol;

    @BeforeEach
    void preparer() {
        alice = creer("alice@exemple.fr", "Alice", false, true);
        bob = creer("bob@exemple.fr", "Bob", false, true);
        bob.setEmailPro("bob@entreprise.fr");
        bob.setInstagram("bob.insta");
        bob = utilisateurRepository.save(bob);
        carol = creer("carol@exemple.fr", "Carol", true, true);
        dave = creer("dave@exemple.fr", "Dave", false, true);
        frank = creer("frank@exemple.fr", "Frank", false, true);
        gina = creer("gina@exemple.fr", "Gina", false, true);
        bobette = creer("bobette@exemple.fr", "Bobette", false, false);

        jetonAlice = jwtService.genererJeton(alice.getEmail(), Role.UTILISATEUR);
        jetonCarol = jwtService.genererJeton(carol.getEmail(), Role.UTILISATEUR);
    }

    private Utilisateur creer(String email, String nom, boolean prive, boolean actif) {
        Utilisateur u = new Utilisateur();
        u.setEmail(email);
        u.setMotDePasse("peu-importe");
        u.setNomAffichage(nom);
        u.setRole(Role.UTILISATEUR);
        u.setActif(actif);
        u.setComptePrive(prive);
        u.setDateInscription(Instant.now());
        return utilisateurRepository.save(u);
    }

    private void suit(Utilisateur abonne, Utilisateur suivi, StatutAbonnement statut) {
        Abonnement a = new Abonnement();
        a.setAbonne(abonne);
        a.setSuivi(suivi);
        a.setStatut(statut);
        a.setDateDemande(Instant.now());
        abonnementRepository.save(a);
    }

    private void bloque(Utilisateur bloqueur, Utilisateur bloque) {
        Blocage b = new Blocage();
        b.setBloqueur(bloqueur);
        b.setBloque(bloque);
        b.setDateBlocage(Instant.now());
        blocageRepository.save(b);
    }

    // --- Recherche ----------------------------------------------------------

    @Test
    @DisplayName("La recherche ignore la casse et écarte soi-même, les comptes désactivés et bloqués")
    void recherche_filtreEtSansEmail() throws Exception {
        Utilisateur bobby = creer("bobby@exemple.fr", "Bobby", false, true);
        bloque(alice, bobby);

        mockMvc.perform(get("/api/utilisateurs").param("recherche", "BO")
                        .header("Authorization", "Bearer " + jetonAlice))
                .andExpect(status().isOk())
                // Bob seul : Bobette est désactivée, Bobby bloqué.
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nomAffichage").value("Bob"))
                .andExpect(jsonPath("$[0].statut").value("AUCUN"))
                .andExpect(jsonPath("$[*].email").isEmpty());
    }

    // --- Profil public -------------------------------------------------------

    @Test
    @DisplayName("Sans abonnement : pas de coordonnées pro, et jamais d'email")
    void profilNonSuivi_sansCoordonnees() throws Exception {
        mockMvc.perform(get("/api/utilisateurs/" + bob.getId())
                        .header("Authorization", "Bearer " + jetonAlice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nomAffichage").value("Bob"))
                .andExpect(jsonPath("$.statut").value("AUCUN"))
                .andExpect(jsonPath("$.contenuVisible").value(true))
                .andExpect(jsonPath("$.peutEcrire").value(false))
                .andExpect(jsonPath("$.coordonnees").value(nullValue()))
                .andExpect(jsonPath("$.email").doesNotExist());
    }

    @Test
    @DisplayName("Un compte privé non suivi ne montre pas ses publications")
    void profilPrive_contenuInvisible() throws Exception {
        mockMvc.perform(get("/api/utilisateurs/" + carol.getId())
                        .header("Authorization", "Bearer " + jetonAlice))
                .andExpect(jsonPath("$.comptePrive").value(true))
                .andExpect(jsonPath("$.contenuVisible").value(false));
    }

    /**
     * LE test de la demande initiale : « avoir accès à son mail pro et ses
     * réseaux sociaux, mais pas à son mail perso ».
     */
    @Test
    @DisplayName("Avec abonnement : email pro et réseaux visibles, email de connexion toujours absent")
    void profilSuivi_avecCoordonneesSansEmail() throws Exception {
        suit(alice, bob, StatutAbonnement.ACCEPTE);

        mockMvc.perform(get("/api/utilisateurs/" + bob.getId())
                        .header("Authorization", "Bearer " + jetonAlice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("ACCEPTE"))
                .andExpect(jsonPath("$.peutEcrire").value(true))
                .andExpect(jsonPath("$.nombreAbonnes").value(1))
                .andExpect(jsonPath("$.coordonnees.emailPro").value("bob@entreprise.fr"))
                .andExpect(jsonPath("$.coordonnees.instagram").value("bob.insta"))
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.coordonnees.email").doesNotExist());
    }

    @Test
    @DisplayName("Une demande en attente ne donne pas accès aux coordonnées")
    void demandeEnAttente_sansCoordonnees() throws Exception {
        suit(alice, carol, StatutAbonnement.EN_ATTENTE);

        mockMvc.perform(get("/api/utilisateurs/" + carol.getId())
                        .header("Authorization", "Bearer " + jetonAlice))
                .andExpect(jsonPath("$.statut").value("EN_ATTENTE"))
                .andExpect(jsonPath("$.coordonnees").value(nullValue()))
                .andExpect(jsonPath("$.peutEcrire").value(false));
    }

    @Test
    @DisplayName("Le profil d'un compte qui m'a bloqué renvoie 404")
    void profilDUnCompteQuiMaBloque_renvoie404() throws Exception {
        bloque(bob, alice);

        mockMvc.perform(get("/api/utilisateurs/" + bob.getId())
                        .header("Authorization", "Bearer " + jetonAlice))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Un compte inconnu ou désactivé renvoie 404")
    void profilInconnuOuDesactive_renvoie404() throws Exception {
        mockMvc.perform(get("/api/utilisateurs/999999")
                        .header("Authorization", "Bearer " + jetonAlice))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/utilisateurs/" + bobette.getId())
                        .header("Authorization", "Bearer " + jetonAlice))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Son propre profil public montre ses coordonnées, sans bouton Écrire")
    void sonProprePofil() throws Exception {
        mockMvc.perform(get("/api/utilisateurs/" + alice.getId())
                        .header("Authorization", "Bearer " + jetonAlice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coordonnees").exists())
                .andExpect(jsonPath("$.peutEcrire").value(false));
    }

    // --- Son propre profil ---------------------------------------------------

    @Test
    @DisplayName("Un email professionnel mal formé est refusé (400)")
    void emailProInvalide_renvoie400() throws Exception {
        mockMvc.perform(put("/api/utilisateurs/moi")
                        .header("Authorization", "Bearer " + jetonAlice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"emailPro\":\"pas-un-email\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("L'email pro, les réseaux et la confidentialité s'enregistrent")
    void modifierProfil_enregistreLesCoordonnees() throws Exception {
        mockMvc.perform(put("/api/utilisateurs/moi")
                        .header("Authorization", "Bearer " + jetonAlice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"emailPro":"alice@entreprise.fr","instagram":"alice.insta","comptePrive":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailPro").value("alice@entreprise.fr"))
                .andExpect(jsonPath("$.instagram").value("alice.insta"))
                .andExpect(jsonPath("$.comptePrive").value(true))
                // Un champ absent de la requête reste tel quel.
                .andExpect(jsonPath("$.nomAffichage").value("Alice"));
    }

    /**
     * Passer en public, c'est dire « tout le monde peut me suivre » : les
     * demandes qui attendaient n'ont plus de raison d'attendre.
     */
    @Test
    @DisplayName("Repasser en public accepte les demandes en attente et prévient les demandeurs")
    void repasserPublic_accepteLesDemandes() throws Exception {
        mockMvc.perform(put("/api/abonnements/" + carol.getId())
                        .header("Authorization", "Bearer " + jetonAlice))
                .andExpect(jsonPath("$.statut").value("EN_ATTENTE"));

        mockMvc.perform(put("/api/utilisateurs/moi")
                        .header("Authorization", "Bearer " + jetonCarol)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comptePrive\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comptePrive").value(false));

        assertThat(abonnementRepository.findByAbonneIdAndSuiviId(alice.getId(), carol.getId()))
                .get().extracting(Abonnement::getStatut).isEqualTo(StatutAbonnement.ACCEPTE);
        assertThat(notificationRepository.nonLuesPour(carol.getId())).isEmpty();
        assertThat(notificationRepository.nonLuesPour(alice.getId()))
                .extracting(Notification::getType)
                .containsExactly(TypeNotification.DEMANDE_ACCEPTEE);
    }

    // --- Suggestions --------------------------------------------------------

    /**
     * Alice suit Bob et Frank. Bob et Frank suivent tous deux Dave ; Frank suit
     * aussi Gina. Dave est donc la meilleure suggestion (deux « ponts »), Gina la
     * suivante — et Bob comme Frank, déjà suivis, ne sont pas proposés.
     */
    @Test
    @DisplayName("Les suggestions passent d'abord par les comptes suivis par mes abonnements")
    void suggestions_parComptesEnCommun() throws Exception {
        suit(alice, bob, StatutAbonnement.ACCEPTE);
        suit(alice, frank, StatutAbonnement.ACCEPTE);
        suit(bob, dave, StatutAbonnement.ACCEPTE);
        suit(frank, dave, StatutAbonnement.ACCEPTE);
        suit(frank, gina, StatutAbonnement.ACCEPTE);

        mockMvc.perform(get("/api/utilisateurs/suggestions")
                        .header("Authorization", "Bearer " + jetonAlice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].compte.id").value(dave.getId()))
                .andExpect(jsonPath("$[0].enCommun").value(2))
                .andExpect(jsonPath("$[1].compte.id").value(gina.getId()))
                .andExpect(jsonPath("$[1].enCommun").value(1))
                .andExpect(jsonPath("$[*].compte.id", not(hasItem(bob.getId().intValue()))))
                .andExpect(jsonPath("$[*].compte.id", not(hasItem(frank.getId().intValue()))))
                .andExpect(jsonPath("$[*].compte.id", not(hasItem(alice.getId().intValue()))))
                .andExpect(jsonPath("$[*].compte.id", not(hasItem(bobette.getId().intValue()))))
                .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.lessThanOrEqualTo(5)));
    }

    /** Sans aucun abonnement nulle part, la liste ne doit pas rester vide. */
    @Test
    @DisplayName("Sans abonnement nulle part, les suggestions proposent des inscrits récents")
    void suggestions_sansAbonnement_inscritsRecents() throws Exception {
        mockMvc.perform(get("/api/utilisateurs/suggestions")
                        .header("Authorization", "Bearer " + jetonAlice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[0].enCommun").value(0));
    }
}

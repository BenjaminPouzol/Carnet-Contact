package com.example.carnet_contact_backend.controller;

import com.example.carnet_contact_backend.model.Abonnement;
import com.example.carnet_contact_backend.model.Blocage;
import com.example.carnet_contact_backend.model.Role;
import com.example.carnet_contact_backend.model.StatutAbonnement;
import com.example.carnet_contact_backend.model.Utilisateur;
import com.example.carnet_contact_backend.repository.AbonnementRepository;
import com.example.carnet_contact_backend.repository.BlocageRepository;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Messagerie : accusé de lecture, réactions, et qui peut écrire à qui.
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
    private AbonnementRepository abonnementRepository;

    @Autowired
    private BlocageRepository blocageRepository;

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

        // Depuis les abonnements, on n'écrit qu'aux comptes qu'on suit. Alice et
        // Bob se suivent : les tests d'accusé de lecture et de réactions, qui les
        // font dialoguer, gardent ainsi exactement leur sens. Carol ne suit
        // personne — c'est elle qui sert aux nouvelles règles.
        suit(alice, bob, StatutAbonnement.ACCEPTE);
        suit(bob, alice, StatutAbonnement.ACCEPTE);
    }

    private Utilisateur creer(String email) {
        Utilisateur u = new Utilisateur();
        u.setEmail(email);
        u.setMotDePasse("peu-importe");
        u.setNomAffichage(email);
        u.setDateInscription(Instant.now());
        return utilisateurRepository.save(u);
    }

    private void suit(Utilisateur abonne, Utilisateur suivi, StatutAbonnement statut) {
        Abonnement abonnement = new Abonnement();
        abonnement.setAbonne(abonne);
        abonnement.setSuivi(suivi);
        abonnement.setStatut(statut);
        abonnement.setDateDemande(Instant.now());
        abonnementRepository.save(abonnement);
    }

    private void bloque(Utilisateur bloqueur, Utilisateur bloque) {
        Blocage blocage = new Blocage();
        blocage.setBloqueur(bloqueur);
        blocage.setBloque(bloque);
        blocage.setDateBlocage(Instant.now());
        blocageRepository.save(blocage);
    }

    /** Tente un envoi, sans présumer du résultat : les nouveaux tests vérifient le refus. */
    private ResultActions tenterEnvoi(String jetonExpediteur, Long destinataireId, String contenu) throws Exception {
        return mockMvc.perform(post("/api/messages")
                .header("Authorization", "Bearer " + jetonExpediteur)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"destinataireId":%d,"contenu":"%s"}
                        """.formatted(destinataireId, contenu)));
    }

    /** Envoie un message et rend son identifiant. */
    private Long envoyer(String jetonExpediteur, Long destinataireId, String contenu) throws Exception {
        String corps = tenterEnvoi(jetonExpediteur, destinataireId, contenu)
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

    // --- Confidentialité et validation --------------------------------------

    /**
     * Un message est lu par son destinataire : il n'a pas à recevoir au passage
     * l'email et le rôle de l'expéditeur. Seul le nom et la photo sont utiles
     * pour afficher le fil.
     */
    @Test
    @DisplayName("L'expéditeur d'un message n'expose ni son email ni son rôle")
    void expediteur_sansEmailNiRole() throws Exception {
        envoyer(jetonAlice, bob.getId(), "Bonjour");

        mockMvc.perform(get("/api/messages/" + alice.getId())
                        .header("Authorization", "Bearer " + jetonBob))
                .andExpect(jsonPath("$[0].expediteur.nomAffichage").value("alice@exemple.fr"))
                .andExpect(jsonPath("$[0].expediteur.email").doesNotExist())
                .andExpect(jsonPath("$[0].expediteur.role").doesNotExist())
                .andExpect(jsonPath("$[0].destinataire.email").doesNotExist());
    }

    @Test
    @DisplayName("La liste des comptes n'expose pas les emails")
    void listeDesComptes_sansEmail() throws Exception {
        mockMvc.perform(get("/api/utilisateurs")
                        .header("Authorization", "Bearer " + jetonAlice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].nomAffichage").exists())
                .andExpect(jsonPath("$[*].email").isEmpty());
    }

    /**
     * Avant Bean Validation, ces deux requêtes n'étaient arrêtées par rien : le
     * message trop long atteignait la base, qui le refusait en erreur 500, et
     * l'identifiant absent faisait échouer la recherche du destinataire.
     */
    @Test
    @DisplayName("Un message trop long ou sans destinataire est refusé (400)")
    void messageInvalide_renvoie400() throws Exception {
        mockMvc.perform(post("/api/messages")
                        .header("Authorization", "Bearer " + jetonAlice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"destinataireId\":%d,\"contenu\":\"%s\"}"
                                .formatted(bob.getId(), "a".repeat(2001))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/messages")
                        .header("Authorization", "Bearer " + jetonAlice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contenu\":\"Bonjour\"}"))
                .andExpect(status().isBadRequest());
    }

    // --- Qui peut écrire à qui ----------------------------------------------

    /**
     * La règle de la demande initiale : suivre quelqu'un donne le droit de lui
     * écrire. Sans abonnement, pas de message. 403 et non 404 : le compte d'Alice
     * existe et se trouve par la recherche, prétendre le contraire serait faux.
     */
    @Test
    @DisplayName("Écrire à quelqu'un qu'on ne suit pas est refusé (403)")
    void ecrireSansSuivre_renvoie403() throws Exception {
        tenterEnvoi(jetonCarol, alice.getId(), "Bonjour")
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Une fois qu'on suit la personne, on peut lui écrire")
    void ecrireApresAvoirSuivi_autorise() throws Exception {
        suit(carol, alice, StatutAbonnement.ACCEPTE);

        tenterEnvoi(jetonCarol, alice.getId(), "Bonjour")
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Une demande encore en attente ne donne pas le droit d'écrire")
    void demandeEnAttente_neSuffitPas() throws Exception {
        suit(carol, alice, StatutAbonnement.EN_ATTENTE);

        tenterEnvoi(jetonCarol, alice.getId(), "Bonjour")
                .andExpect(status().isForbidden());
    }

    /**
     * Sans cette exception, Alice pourrait écrire à Carol, qui n'aurait aucun
     * moyen de lui répondre tant qu'elle ne la suit pas.
     */
    @Test
    @DisplayName("On peut toujours répondre à quelqu'un qui nous a écrit")
    void repondreAQuiNousAEcrit_autorise() throws Exception {
        suit(alice, carol, StatutAbonnement.ACCEPTE);
        envoyer(jetonAlice, carol.getId(), "Bonjour Carol");

        tenterEnvoi(jetonCarol, alice.getId(), "Bonjour Alice")
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Les interlocuteurs : les comptes suivis et les conversations existantes, sans email")
    void interlocuteurs() throws Exception {
        Utilisateur dave = creer("dave@exemple.fr");
        // Carol a écrit à Alice (elle la suit) ; Alice ne suit pas Carol.
        suit(carol, alice, StatutAbonnement.ACCEPTE);
        envoyer(jetonCarol, alice.getId(), "Bonjour");
        // Une demande en attente vers Dave ne fait pas de lui un interlocuteur.
        suit(alice, dave, StatutAbonnement.EN_ATTENTE);

        mockMvc.perform(get("/api/messages/interlocuteurs")
                        .header("Authorization", "Bearer " + jetonAlice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                // Bob : Alice le suit.
                .andExpect(jsonPath("$[?(@.compte.id == %d)].peutEcrire".formatted(bob.getId())).value(true))
                // Carol : Alice ne la suit pas, mais Carol lui a écrit.
                .andExpect(jsonPath("$[?(@.compte.id == %d)].peutEcrire".formatted(carol.getId())).value(true))
                .andExpect(jsonPath("$[*].compte.email").isEmpty());
    }

    @Test
    @DisplayName("Un blocage coupe la messagerie et retire le compte des interlocuteurs")
    void blocage_couteLaMessagerie() throws Exception {
        bloque(bob, alice);

        // Alice suit toujours Bob en base... mais le blocage l'emporte.
        tenterEnvoi(jetonAlice, bob.getId(), "Bonjour")
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/messages/interlocuteurs")
                        .header("Authorization", "Bearer " + jetonAlice))
                .andExpect(jsonPath("$.length()").value(0));
    }

    /** Sinon la pastille compterait des messages d'une conversation qu'on ne peut plus ouvrir. */
    @Test
    @DisplayName("Les non-lus d'un compte bloqué ne sont plus comptés")
    void nonLus_excluentLesComptesBloques() throws Exception {
        envoyer(jetonBob, alice.getId(), "Bonjour");
        bloque(alice, bob);

        mockMvc.perform(get("/api/messages/non-lus")
                        .header("Authorization", "Bearer " + jetonAlice))
                .andExpect(jsonPath("$.length()").value(0));
    }
}

package com.example.carnet_contact_backend.controller;

import com.example.carnet_contact_backend.model.Contact;
import com.example.carnet_contact_backend.model.Utilisateur;
import com.example.carnet_contact_backend.repository.ContactRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test d'INTÉGRATION : contrairement à JwtServiceTest, celui-ci démarre
 * l'application entière — contexte Spring, base H2, chaîne de filtres de
 * sécurité comprise.
 *
 * C'est indispensable ici, car ce qu'on vérifie n'appartient à aucune classe en
 * particulier : « un utilisateur ne peut pas atteindre les contacts d'un
 * autre » est une propriété du SYSTÈME, produite par la coopération du filtre
 * JWT, de la configuration de sécurité, du contrôleur et du repository. Tester
 * le contrôleur seul, avec un repository simulé, prouverait seulement que le
 * code fait ce qu'on a écrit — pas qu'il protège quoi que ce soit.
 *
 * Les annotations :
 *
 * | Annotation | Rôle |
 * |---|---|
 * | `@SpringBootTest` | Démarre le contexte Spring complet, comme en vrai |
 * | `@AutoConfigureMockMvc` | Fournit MockMvc : envoie des requêtes HTTP sans ouvrir de port réseau |
 * | `@Transactional` | Chaque test s'exécute dans une transaction annulée à la fin |
 *
 * `@Transactional` sur un test ne veut PAS dire la même chose que sur un
 * service : ici, Spring annule systématiquement la transaction à la fin du
 * test. Chaque méthode repart donc d'une base propre, et l'ordre d'exécution
 * des tests cesse d'avoir la moindre importance — un test qui ne passe que
 * s'il tourne en premier n'a aucune valeur.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ContactControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private ContactRepository contactRepository;

    @Autowired
    private JwtService jwtService;

    private Utilisateur alice;
    private Utilisateur bob;
    private String jetonAlice;
    private String jetonBob;

    /**
     * @BeforeEach s'exécute avant CHAQUE méthode de test, pas une fois pour
     * toutes. C'est ce qui garantit que deux tests ne se partagent jamais un
     * objet que l'un aurait modifié.
     */
    @BeforeEach
    void preparerDeuxComptes() {
        alice = creerUtilisateur("alice@exemple.fr");
        bob = creerUtilisateur("bob@exemple.fr");

        jetonAlice = jwtService.genererJeton(alice.getEmail());
        jetonBob = jwtService.genererJeton(bob.getEmail());
    }

    private Utilisateur creerUtilisateur(String email) {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setEmail(email);
        // Le mot de passe n'est pas vérifié dans ces tests-ci (on fabrique le
        // jeton directement) : inutile de payer le coût d'un hachage BCrypt.
        utilisateur.setMotDePasse("peu-importe");
        utilisateur.setNomAffichage(email);
        return utilisateurRepository.save(utilisateur);
    }

    private Contact creerContact(Utilisateur proprietaire, String nom, String prenom, String email) {
        Contact contact = new Contact();
        contact.setNom(nom);
        contact.setPrenom(prenom);
        contact.setEmail(email);
        contact.setProprietaire(proprietaire);
        return contactRepository.save(contact);
    }

    // --- Authentification ----------------------------------------------------

    @Test
    @DisplayName("Sans jeton, l'API refuse avec 401")
    void sansJeton_renvoie401() throws Exception {
        mockMvc.perform(get("/api/contacts"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Avec un jeton bidon, l'API refuse avec 401")
    void jetonInvalide_renvoie401() throws Exception {
        mockMvc.perform(get("/api/contacts")
                        .header("Authorization", "Bearer ceci-nest-pas-un-jeton"))
                .andExpect(status().isUnauthorized());
    }

    // --- Isolation entre comptes --------------------------------------------

    @Test
    @DisplayName("Chacun ne voit que ses propres contacts")
    void chaqueCompteNeVoitQueSesContacts() throws Exception {
        creerContact(alice, "Dupont", "Marie", "marie@exemple.fr");
        creerContact(bob, "Martin", "Paul", "paul@exemple.fr");

        mockMvc.perform(get("/api/contacts").header("Authorization", "Bearer " + jetonAlice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.contenu[0].nom").value("Dupont"));

        mockMvc.perform(get("/api/contacts").header("Authorization", "Bearer " + jetonBob))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.contenu[0].nom").value("Martin"));
    }

    /**
     * Le test le plus important du fichier.
     *
     * 404 et non 403 : le serveur ne dit pas « ce contact existe mais il n'est
     * pas à toi », il dit « je n'ai rien qui corresponde ». La nuance compte —
     * un 403 confirmerait l'existence de la ressource, ce qui est déjà une
     * information qu'on ne doit pas donner.
     */
    @Test
    @DisplayName("Lire le contact d'un autre renvoie 404")
    void lireLeContactDUnAutre_renvoie404() throws Exception {
        Contact contactDAlice = creerContact(alice, "Dupont", "Marie", "marie@exemple.fr");

        mockMvc.perform(get("/api/contacts/" + contactDAlice.getId())
                        .header("Authorization", "Bearer " + jetonBob))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Modifier le contact d'un autre renvoie 404")
    void modifierLeContactDUnAutre_renvoie404() throws Exception {
        Contact contactDAlice = creerContact(alice, "Dupont", "Marie", "marie@exemple.fr");

        mockMvc.perform(put("/api/contacts/" + contactDAlice.getId())
                        .header("Authorization", "Bearer " + jetonBob)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\":\"Pirate\",\"prenom\":\"X\",\"email\":\"x@exemple.fr\"}"))
                .andExpect(status().isNotFound());

        // Et surtout : la donnée n'a pas bougé. Vérifier le code de statut ne
        // suffit pas — un 404 renvoyé APRÈS avoir écrit serait catastrophique.
        Contact inchange = contactRepository.findById(contactDAlice.getId()).orElseThrow();
        assertThat(inchange.getNom()).isEqualTo("Dupont");
    }

    @Test
    @DisplayName("Supprimer le contact d'un autre renvoie 404 et ne supprime rien")
    void supprimerLeContactDUnAutre_renvoie404() throws Exception {
        Contact contactDAlice = creerContact(alice, "Dupont", "Marie", "marie@exemple.fr");

        mockMvc.perform(delete("/api/contacts/" + contactDAlice.getId())
                        .header("Authorization", "Bearer " + jetonBob))
                .andExpect(status().isNotFound());

        assertThat(contactRepository.findById(contactDAlice.getId())).isPresent();
    }

    @Test
    @DisplayName("Le propriétaire est imposé par le serveur, pas par la requête")
    void creation_ignoreLeProprietaireEnvoyeParLeClient() throws Exception {
        // Bob essaie de créer un contact directement dans le carnet d'Alice en
        // glissant un propriétaire dans le corps de la requête.
        mockMvc.perform(post("/api/contacts")
                        .header("Authorization", "Bearer " + jetonBob)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nom": "Intrus",
                                  "prenom": "Paul",
                                  "email": "intrus@exemple.fr",
                                  "proprietaire": { "id": %d }
                                }
                                """.formatted(alice.getId())))
                .andExpect(status().isOk());

        // Le contact existe bien… mais chez Bob.
        mockMvc.perform(get("/api/contacts").header("Authorization", "Bearer " + jetonAlice))
                .andExpect(jsonPath("$.total").value(0));

        mockMvc.perform(get("/api/contacts").header("Authorization", "Bearer " + jetonBob))
                .andExpect(jsonPath("$.total").value(1));
    }

    // --- Recherche et pagination ---------------------------------------------

    @Test
    @DisplayName("La recherche filtre sur le nom, le prénom et l'email")
    void recherche_filtreSurLesTroisChamps() throws Exception {
        creerContact(alice, "Dupont", "Marie", "marie@exemple.fr");
        creerContact(alice, "Martin", "Dupond", "paul@exemple.fr");
        creerContact(alice, "Durand", "Sophie", "sophie.dupont@travail.fr");

        // « dupont » touche le nom du premier et l'email du troisième, mais pas
        // le prénom « Dupond » du deuxième (un D final, pas un T).
        mockMvc.perform(get("/api/contacts")
                        .param("recherche", "dupont")
                        .header("Authorization", "Bearer " + jetonAlice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2));

        // Insensible à la casse.
        mockMvc.perform(get("/api/contacts")
                        .param("recherche", "DURAND")
                        .header("Authorization", "Bearer " + jetonAlice))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.contenu[0].prenom").value("Sophie"));
    }

    @Test
    @DisplayName("La recherche ne franchit pas la frontière entre comptes")
    void recherche_resteDansSonPropreCarnet() throws Exception {
        creerContact(alice, "Dupont", "Marie", "marie@exemple.fr");

        mockMvc.perform(get("/api/contacts")
                        .param("recherche", "dupont")
                        .header("Authorization", "Bearer " + jetonBob))
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    @DisplayName("La pagination découpe les résultats et les trie par nom")
    void pagination_decoupeEtTrie() throws Exception {
        creerContact(alice, "Charlie", "C", "c@exemple.fr");
        creerContact(alice, "Alpha", "A", "a@exemple.fr");
        creerContact(alice, "Bravo", "B", "b@exemple.fr");

        mockMvc.perform(get("/api/contacts")
                        .param("page", "0").param("taille", "2")
                        .header("Authorization", "Bearer " + jetonAlice))
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.contenu.length()").value(2))
                // Le tri s'applique à l'ENSEMBLE avant le découpage : la
                // première page contient les deux premiers de l'ordre complet.
                .andExpect(jsonPath("$.contenu[0].nom").value("Alpha"))
                .andExpect(jsonPath("$.contenu[1].nom").value("Bravo"));

        mockMvc.perform(get("/api/contacts")
                        .param("page", "1").param("taille", "2")
                        .header("Authorization", "Bearer " + jetonAlice))
                .andExpect(jsonPath("$.contenu.length()").value(1))
                .andExpect(jsonPath("$.contenu[0].nom").value("Charlie"));
    }

    @Test
    @DisplayName("Une taille de page démesurée est ramenée à la borne")
    void pagination_borneLaTailleDemandee() throws Exception {
        mockMvc.perform(get("/api/contacts")
                        .param("taille", "100000")
                        .header("Authorization", "Bearer " + jetonAlice))
                .andExpect(status().isOk())
                // Sans le Math.clamp du contrôleur, un client pourrait forcer
                // le chargement de toute la table en une requête.
                .andExpect(jsonPath("$.taille").value(50));
    }
}

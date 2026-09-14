# Fil d'actualité — plan d'implémentation

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Un fil d'actualité commun où chaque compte publie ses hobbies par catégorie, avec réactions emoji, pagination par curseur et droits auteur / administrateur.

**Architecture:** Backend Spring Boot : entités `Publication` et `ReactionPublication`, `PublicationController` validé par Bean Validation, DTO `AuteurPublic` et regroupement des réactions partagé avec la messagerie. Frontend Angular : `PublicationService` (signaux + `Subject`/`switchMap`), composants `publication-form` et `publication-carte`, page `fil`.

**Tech Stack:** Java 21, Spring Boot 4.1.1 (Jackson 3, Hibernate 7), H2 ; Angular 21.2 sans zone.js, signaux, PrimeNG 21, vitest 4.

**Spec:** `docs/superpowers/specs/2026-09-14-fil-actualite-design.md`

## Global Constraints

- Commentaires en français, dans le style du projet (le « pourquoi » avant le « comment ») ; commentaires CSS sans accents, comme les fichiers existants.
- Pas de commit tant que l'utilisateur ne le demande pas (consigne de session) : les étapes « Commit » sont remplacées par un point de contrôle `git status`.
- `spring-boot-starter-validation` absent du dépôt Maven local : la première résolution demande le réseau.
- Budgets `angular.json` : paquet initial alerte à 1 MB (926 kB avant travaux), style de composant alerte à 8 kB.
- Pas de `LOCALE_ID` français : formats de date numériques uniquement (`dd/MM/yyyy, HH:mm`).
- Commandes : backend `.\mvnw.cmd test` dans `carnet-contact-backend` ; frontend `npx ng test --watch=false` et `npx ng build` dans `carnet-contact_frontend`.

## Carte des fichiers

Backend (`carnet-contact-backend/src/main/java/com/example/carnet_contact_backend/`)

| Fichier | Action | Responsabilité |
|---|---|---|
| `pom.xml` (racine backend) | Modifier | Dépendance Bean Validation |
| `model/ReactionEmoji.java` | Créer | Contrat commun aux deux entités de réaction |
| `model/Reaction.java` | Modifier | Implémente `ReactionEmoji` |
| `model/Categorie.java` | Créer | Enum des 11 catégories |
| `model/Publication.java` | Créer | Entité publication |
| `model/ReactionPublication.java` | Créer | Réaction à une publication |
| `repository/PublicationRepository.java` | Créer | Fil par curseur, comptage, suppression par auteur |
| `repository/ReactionPublicationRepository.java` | Créer | Réactions par lot, suppressions en masse |
| `controller/AuteurPublic.java` | Créer | DTO public d'un compte |
| `controller/Reactions.java` | Créer | Emojis autorisés + regroupement |
| `controller/PublicationController.java` | Créer | API `/api/publications` |
| `controller/MessageController.java` | Modifier | `AuteurPublic`, `Reactions`, `@Valid` |
| `controller/UtilisateurController.java` | Modifier | `autres()` renvoie `AuteurPublic` |
| `controller/AdminController.java` | Modifier | Cascade publications, `nombrePublications` |
| Tests `controller/PublicationControllerTest.java` | Créer | |
| Tests `controller/MessageControllerTest.java`, `AdminControllerTest.java` | Modifier | |

Frontend (`carnet-contact_frontend/src/app/`)

| Fichier | Action | Responsabilité |
|---|---|---|
| `reaction.model.ts` | Créer | `ReactionResume`, `EMOJIS_REACTION` |
| `utilisateur.model.ts` | Modifier | `AuteurPublic`, `LigneCompte.nombrePublications` |
| `message.model.ts` | Modifier | Types `AuteurPublic`, réactions importées |
| `publication.model.ts` | Créer | `CATEGORIES`, `Categorie`, `Publication`, `PageFil`, `DemandePublication`, `categorieDe` |
| `donnees-test.ts` | Modifier | `unAuteur`, `unePublication` |
| `services/auth.ts`, `pages/messages/messages.ts` | Modifier | Types `AuteurPublic` |
| `services/admin.spec.ts`, `pages/admin/admin.{ts,html}` | Modifier | `nombrePublications` |
| `services/publication.ts` (+ `.spec.ts`) | Créer | État du fil |
| `components/publication-form/*` (+ `.spec.ts`) | Créer | Création / modification |
| `components/publication-carte/*` | Créer | Affichage, réactions, actions |
| `pages/fil/*` | Créer | Filtres, formulaire, liste, « Voir plus » |
| `app.routes.ts`, `app.html` | Modifier | Route `fil`, lien « Fil » |

Documentation : `docs/support-apprentissage-angular-spring.md`, `docs/progression-pedagogique.md`, `README.md`.

---

### Task 1 : Bean Validation, DTO public et réactions partagées (messagerie)

**Files:**
- Modify: `carnet-contact-backend/pom.xml`
- Create: `model/ReactionEmoji.java`, `controller/AuteurPublic.java`, `controller/Reactions.java`
- Modify: `model/Reaction.java`, `controller/MessageController.java`, `controller/UtilisateurController.java`
- Test: `src/test/.../controller/MessageControllerTest.java`

**Interfaces:**
- Produces: `record AuteurPublic(Long id, String nomAffichage, String photoUrl)` + `static AuteurPublic de(Utilisateur)` ;
  `interface ReactionEmoji { Long getId(); String getEmoji(); Utilisateur getUtilisateur(); Long idCible(); }` ;
  `Reactions.EMOJIS_AUTORISES`, `Reactions.estAutorise(String)`, `record Reactions.ReactionResume(String emoji, long nombre, boolean parMoi)`,
  `Reactions.resumerParCible(List<? extends ReactionEmoji>, Long moiId) : Map<Long, List<ReactionResume>>`.

- [ ] **Step 1 : tests qui échouent** — ajouter à `MessageControllerTest` :

```java
    // --- Confidentialité et validation --------------------------------------

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
    @DisplayName("La liste des interlocuteurs n'expose pas les emails")
    void listeDesComptes_sansEmail() throws Exception {
        mockMvc.perform(get("/api/utilisateurs")
                        .header("Authorization", "Bearer " + jetonAlice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].nomAffichage").exists())
                .andExpect(jsonPath("$[*].email").isEmpty());
    }

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
```

- [ ] **Step 2 : constater l'échec** — `.\mvnw.cmd test -Dtest=MessageControllerTest`. Attendu : les deux premiers échouent (`email` présent), le troisième lève une exception (500 non géré).

- [ ] **Step 3 : dépendance** — dans `pom.xml`, après `spring-boot-starter-webmvc` :

```xml
		<!-- Bean Validation : des règles déclarées par annotations (@NotBlank,
		     @Size, @Pattern) sur les objets de requête, vérifiées par Spring MVC
		     dès qu'un paramètre porte @Valid. Un échec donne un 400. -->
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-validation</artifactId>
		</dependency>
```

- [ ] **Step 4 : `model/ReactionEmoji.java`**

```java
package com.example.carnet_contact_backend.model;

/**
 * Ce que toute réaction emoji sait dire d'elle-même, qu'elle porte sur un
 * message ou sur une publication.
 *
 * Les deux entités vivent dans des tables séparées et n'ont pas d'ancêtre
 * commun. Cette interface est le contrat qui permet à `Reactions` de les
 * regrouper avec UN seul code, sans savoir laquelle il manipule.
 */
public interface ReactionEmoji {

    Long getId();

    String getEmoji();

    Utilisateur getUtilisateur();

    /** L'identifiant de l'élément réagi : un message, ou une publication. */
    Long idCible();
}
```

Dans `model/Reaction.java` : `public class Reaction implements ReactionEmoji {` et, après `setEmoji` :

```java
    @Override
    public Long idCible() {
        return message.getId();
    }
```

- [ ] **Step 5 : `controller/AuteurPublic.java`**

```java
package com.example.carnet_contact_backend.controller;

import com.example.carnet_contact_backend.model.Utilisateur;

/**
 * Ce qu'un compte laisse voir de lui aux AUTRES comptes.
 *
 * Jusqu'ici, messages et liste des interlocuteurs renvoyaient l'entité
 * Utilisateur telle quelle. @JsonIgnore cachait le mot de passe, mais tout le
 * reste sortait : email, rôle, état du compte, date d'inscription. N'importe
 * quel compte connecté pouvait lister les adresses de tous les autres et
 * repérer les administrateurs.
 *
 * @JsonIgnore raisonne en LISTE NOIRE : tout ce qu'on n'a pas pensé à cacher
 * est publié, y compris un champ ajouté plus tard. Un DTO raisonne en LISTE
 * BLANCHE : seul ce qui est écrit ici sort.
 */
public record AuteurPublic(Long id, String nomAffichage, String photoUrl) {

    public static AuteurPublic de(Utilisateur utilisateur) {
        return new AuteurPublic(
                utilisateur.getId(), utilisateur.getNomAffichage(), utilisateur.getPhotoUrl());
    }
}
```

- [ ] **Step 6 : `controller/Reactions.java`**

```java
package com.example.carnet_contact_backend.controller;

import com.example.carnet_contact_backend.model.ReactionEmoji;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Ce que les réactions des messages et celles des publications ont en commun :
 * la liste des emojis acceptés, et le regroupement « 👍 3, dont la mienne ».
 *
 * Classe finale, constructeur privé, méthodes statiques — comme
 * PolitiqueMotDePasse : aucun état, rien à injecter.
 */
public final class Reactions {

    /**
     * Les seules réactions acceptées. Une liste fermée plutôt que « n'importe
     * quel emoji » : « est-ce dans cette liste ? » se vérifie trivialement, là
     * où « est-ce un emoji ? » est une question étonnamment difficile.
     */
    public static final List<String> EMOJIS_AUTORISES = List.of("👍", "❤️", "😂", "😮", "😢");

    private static final Set<String> EMOJIS_VALIDES = Set.copyOf(EMOJIS_AUTORISES);

    /**
     * Les réactions d'un élément, regroupées par emoji. `parMoi` dépend de qui
     * regarde : c'est une lecture, pas une colonne.
     */
    public record ReactionResume(String emoji, long nombre, boolean parMoi) {}

    private Reactions() {
    }

    public static boolean estAutorise(String emoji) {
        return emoji != null && EMOJIS_VALIDES.contains(emoji);
    }

    /**
     * Regroupe les réactions d'un LOT d'éléments, élément par élément.
     *
     * La répartition se fait en une passe (groupingBy). La version précédente
     * refiltrait toute la liste pour chaque message : un coût qui croissait
     * comme le produit du nombre de messages par le nombre de réactions.
     *
     * @return pour chaque id d'élément, ses réactions résumées ; un élément sans
     *         réaction est absent de la map.
     */
    public static Map<Long, List<ReactionResume>> resumerParCible(
            List<? extends ReactionEmoji> reactions, Long moiId) {

        Map<Long, List<ReactionEmoji>> parCible = reactions.stream()
                .collect(Collectors.groupingBy(ReactionEmoji::idCible));

        Map<Long, List<ReactionResume>> resultat = new HashMap<>();
        parCible.forEach((id, liste) -> resultat.put(id, resumer(liste, moiId)));
        return resultat;
    }

    private static List<ReactionResume> resumer(List<ReactionEmoji> reactions, Long moiId) {
        // LinkedHashMap + tri par id : les emojis gardent l'ordre de leur
        // première apparition, stable d'un rafraîchissement à l'autre.
        Map<String, long[]> parEmoji = new LinkedHashMap<>();

        reactions.stream()
                .sorted(Comparator.comparing(ReactionEmoji::getId))
                .forEach(r -> {
                    long[] compte = parEmoji.computeIfAbsent(r.getEmoji(), e -> new long[2]);
                    compte[0]++;
                    if (r.getUtilisateur().getId().equals(moiId)) {
                        compte[1] = 1;
                    }
                });

        return parEmoji.entrySet().stream()
                .map(e -> new ReactionResume(e.getKey(), e.getValue()[0], e.getValue()[1] == 1))
                .toList();
    }
}
```

- [ ] **Step 7 : `MessageController`** — supprimer `EMOJIS_AUTORISES`, `EMOJIS_VALIDES`, le record `ReactionResume`, la méthode `resumer` et le test manuel de contenu vide dans `envoyer`. Remplacer :

```java
    /**
     * @Valid demande à Spring de vérifier les annotations du record AVANT
     * d'appeler la méthode. Un contenu vide, trop long ou un destinataire absent
     * produisent un 400 sans qu'une ligne de ce contrôleur ne s'exécute —
     * auparavant, un message de 2001 caractères atteignait la base et
     * ressortait en erreur 500.
     */
    public record DemandeMessage(
            @NotNull(message = "Destinataire requis.") Long destinataireId,
            @NotBlank(message = "Message vide.")
            @Size(max = 2000, message = "Message trop long (2000 caractères au plus).")
            String contenu) {}

    public record MessageVu(
            Long id,
            AuteurPublic expediteur,
            AuteurPublic destinataire,
            String contenu,
            Instant dateEnvoi,
            boolean lu,
            List<ReactionResume> reactions) {}

    private List<MessageVu> assembler(List<Message> messages, Long moiId) {
        if (messages.isEmpty()) {
            return List.of();
        }

        // Une seule requête pour tout le lot (problème N+1, section 32), puis
        // un regroupement partagé avec les publications.
        Map<Long, List<ReactionResume>> reactions = Reactions.resumerParCible(
                reactionRepository.findByMessageIdIn(messages.stream().map(Message::getId).toList()),
                moiId);

        return messages.stream()
                .map(m -> vue(m, reactions.getOrDefault(m.getId(), List.of())))
                .toList();
    }

    private MessageVu vue(Message m, List<ReactionResume> reactions) {
        return new MessageVu(m.getId(), AuteurPublic.de(m.getExpediteur()),
                AuteurPublic.de(m.getDestinataire()), m.getContenu(), m.getDateEnvoi(),
                m.isLu(), reactions);
    }
```

`envoyer` prend `@Valid @RequestBody DemandeMessage demande` et se termine par `return vue(cree, List.of());`. `reagir` teste `!Reactions.estAutorise(demande.emoji())`. `emojis()` renvoie `Reactions.EMOJIS_AUTORISES`. Imports : `com.example.carnet_contact_backend.controller.Reactions.ReactionResume`, `jakarta.validation.Valid`, `jakarta.validation.constraints.NotBlank`, `NotNull`, `Size` ; retirer `Comparator`, `LinkedHashMap`, `Set` devenus inutiles.

- [ ] **Step 8 : `UtilisateurController.autres`**

```java
    @GetMapping
    public List<AuteurPublic> autres(@AuthenticationPrincipal String email) {
        // AuteurPublic et non l'entité : la liste des interlocuteurs n'a besoin
        // que d'un nom et d'une photo — ni de l'email, ni du rôle des autres.
        return utilisateurRepository
                .findByIdNotAndActifTrueOrderByNomAffichageAsc(utilisateurConnecte(email).getId())
                .stream()
                .map(AuteurPublic::de)
                .toList();
    }
```

- [ ] **Step 9 : vérifier** — `.\mvnw.cmd test` (réseau autorisé pour la première résolution). Attendu : 66 tests, 0 échec.

- [ ] **Step 10 : point de contrôle** — `git status` : seuls les fichiers de la tâche sont modifiés.

---

### Task 2 : Publications côté serveur

**Files:**
- Create: `model/Categorie.java`, `model/Publication.java`, `model/ReactionPublication.java`
- Create: `repository/PublicationRepository.java`, `repository/ReactionPublicationRepository.java`
- Create: `controller/PublicationController.java`
- Test: `src/test/.../controller/PublicationControllerTest.java`

**Interfaces:**
- Consumes (Task 1) : `AuteurPublic.de`, `Reactions.estAutorise`, `Reactions.resumerParCible`, `ReactionEmoji`.
- Produces : JSON `PageFil { publications: PublicationVue[], curseurSuivant: number|null }` ;
  `PublicationVue { id, auteur{id,nomAffichage,photoUrl}, categorie, contenu, imageUrl, datePublication, dateModification, reactions[], modifiable, supprimable }` ;
  `PublicationRepository.countByAuteurId(Long)`, `.supprimerCellesDe(Long)` ;
  `ReactionPublicationRepository.supprimerCellesDe(Long)`, `.supprimerCellesDesPublicationsDe(Long)`.

- [ ] **Step 1 : test qui échoue** — créer `PublicationControllerTest.java` :

```java
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

    @Autowired private MockMvc mockMvc;
    @Autowired private UtilisateurRepository utilisateurRepository;
    @Autowired private PublicationRepository publicationRepository;
    @Autowired private ReactionPublicationRepository reactionRepository;
    @Autowired private JwtService jwtService;
    @Autowired private EntityManager entityManager;

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
        return lireFil(jeton, query).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    // --- Lecture ------------------------------------------------------------

    @Test
    @DisplayName("Sans jeton, le fil est fermé (401)")
    void sansJeton_renvoie401() throws Exception {
        mockMvc.perform(get("/api/publications")).andExpect(status().isUnauthorized());
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
        lireFil(jetonAlice, "?categorie=PEINTURE").andExpect(status().isBadRequest());
    }

    // --- Curseur ------------------------------------------------------------

    @Test
    @DisplayName("Le curseur parcourt tout le fil, sans doublon ni oubli")
    void curseur_parcourtToutSansDoublon() throws Exception {
        for (int i = 1; i <= 5; i++) {
            enregistrer(alice, "p" + i);
        }

        String page1 = corpsDuFil(jetonAlice, "?taille=2");
        assertThat(JsonPath.<List<String>>read(page1, "$.publications[*].contenu")).containsExactly("p5", "p4");
        long curseur1 = ((Number) JsonPath.read(page1, "$.curseurSuivant")).longValue();

        String page2 = corpsDuFil(jetonAlice, "?taille=2&avant=" + curseur1);
        assertThat(JsonPath.<List<String>>read(page2, "$.publications[*].contenu")).containsExactly("p3", "p2");
        long curseur2 = ((Number) JsonPath.read(page2, "$.curseurSuivant")).longValue();

        String page3 = corpsDuFil(jetonAlice, "?taille=2&avant=" + curseur2);
        assertThat(JsonPath.<List<String>>read(page3, "$.publications[*].contenu")).containsExactly("p1");
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
        assertThat(JsonPath.<List<String>>read(page2, "$.publications[*].contenu")).containsExactly("p2", "p1");
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
    @DisplayName("Un jeton ADMIN d'un compte qui ne l'est plus en base ne supprime rien (403)")
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

    private void reagir(String jeton, Long id, String emoji) throws Exception {
        mockMvc.perform(put("/api/publications/" + id + "/reaction")
                        .header("Authorization", "Bearer " + jeton)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"emoji\":\"%s\"}".formatted(emoji)))
                .andExpect(status().isOk());
    }

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
        lireFil(jetonBob, "").andExpect(jsonPath("$.publications[0].reactions.length()").value(0));
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
```

- [ ] **Step 2 : constater l'échec** — `.\mvnw.cmd test -Dtest=PublicationControllerTest`. Attendu : échec de compilation (`Categorie`, `Publication`… inexistants).

- [ ] **Step 3 : `model/Categorie.java`**

```java
package com.example.carnet_contact_backend.model;

/**
 * Les catégories d'une publication.
 *
 * Une énumération plutôt qu'une table : la liste est fixée par l'application,
 * pas par ses utilisateurs. Une table demanderait un écran de gestion, une clé
 * étrangère et une jointure, pour une liste qui ne change qu'avec le code.
 *
 * Stockée par son NOM (@Enumerated(STRING)), comme Role : insérer une valeur au
 * milieu ne change pas la catégorie des publications déjà enregistrées.
 *
 * Les libellés affichés vivent côté Angular (publication.model.ts) : le serveur
 * décide de ce qu'il ACCEPTE, l'interface de ce qu'elle AFFICHE.
 */
public enum Categorie {
    SPORT,
    CULTURE,
    JEU_VIDEO,
    INFORMATIQUE,
    ACTUALITE,
    MUSIQUE,
    CUISINE,
    VOYAGE,
    NATURE,
    CREATIONS,
    AUTRE
}
```

- [ ] **Step 4 : `model/Publication.java`** — champs, annotations, getters/setters :

```java
package com.example.carnet_contact_backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import java.time.Instant;

/** Une publication du fil d'actualité. */
@Entity
public class Publication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * LAZY : l'auteur n'est pas chargé d'office. La requête du fil le ramène
     * explicitement par JOIN FETCH — en une seule requête pour toute la page,
     * au lieu d'une par publication (le problème N+1 de la section 32).
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "auteur_id")
    private Utilisateur auteur;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Categorie categorie;

    @Column(nullable = false, length = 2000)
    private String contenu;

    // Une URL, comme la photo de profil : pas d'envoi de fichier.
    @Column(length = 500)
    private String imageUrl;

    @Column(nullable = false)
    private Instant datePublication;

    // null tant que personne n'a modifié la publication.
    private Instant dateModification;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Utilisateur getAuteur() { return auteur; }
    public void setAuteur(Utilisateur auteur) { this.auteur = auteur; }
    public Categorie getCategorie() { return categorie; }
    public void setCategorie(Categorie categorie) { this.categorie = categorie; }
    public String getContenu() { return contenu; }
    public void setContenu(String contenu) { this.contenu = contenu; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public Instant getDatePublication() { return datePublication; }
    public void setDatePublication(Instant datePublication) { this.datePublication = datePublication; }
    public Instant getDateModification() { return dateModification; }
    public void setDateModification(Instant dateModification) { this.dateModification = dateModification; }
}
```

(Dans le fichier réel, getters et setters sur plusieurs lignes, comme `Message.java`.)

- [ ] **Step 5 : `model/ReactionPublication.java`** — calque de `Reaction.java` : `@Table(name = "reaction_publication", uniqueConstraints = @UniqueConstraint(name = "uk_reaction_publication_utilisateur", columnNames = {"publication_id", "utilisateur_id"}))`, champs `id`, `publication` (`@ManyToOne(fetch = LAZY, optional = false) @JoinColumn(name = "publication_id")`), `utilisateur` (idem, `utilisateur_id`), `emoji` (`@Column(nullable = false, length = 8)`), getters/setters, `implements ReactionEmoji` avec :

```java
    @Override
    public Long idCible() {
        return publication.getId();
    }
```

- [ ] **Step 6 : dépôts**

```java
package com.example.carnet_contact_backend.repository;

import com.example.carnet_contact_backend.model.Categorie;
import com.example.carnet_contact_backend.model.Publication;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PublicationRepository extends JpaRepository<Publication, Long> {

    /**
     * Une tranche du fil, par CURSEUR : les publications plus anciennes que
     * `avant` (toutes si `avant` est null), filtrées par catégorie si demandé.
     *
     * Tri par id décroissant plutôt que par date : l'id est croissant et
     * unique, deux dates peuvent être égales — et un curseur ambigu ferait
     * sauter ou répéter une publication à la frontière de deux pages.
     *
     * Retour en List et non en Page : Spring Data n'exécute alors pas de
     * requête COUNT. Le curseur n'a pas besoin du total.
     */
    @Query("""
            SELECT p FROM Publication p
            JOIN FETCH p.auteur
            WHERE (:categorie IS NULL OR p.categorie = :categorie)
              AND (:avant IS NULL OR p.id < :avant)
            ORDER BY p.id DESC
            """)
    List<Publication> fil(
            @Param("categorie") Categorie categorie,
            @Param("avant") Long avant,
            Pageable limite);

    long countByAuteurId(Long auteurId);

    @Modifying
    @Query("DELETE FROM Publication p WHERE p.auteur.id = :auteurId")
    void supprimerCellesDe(@Param("auteurId") Long auteurId);
}
```

```java
package com.example.carnet_contact_backend.repository;

import com.example.carnet_contact_backend.model.ReactionPublication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReactionPublicationRepository extends JpaRepository<ReactionPublication, Long> {

    List<ReactionPublication> findByPublicationIdIn(List<Long> publicationIds);

    Optional<ReactionPublication> findByPublicationIdAndUtilisateurId(Long publicationId, Long utilisateurId);

    @Modifying
    @Query("DELETE FROM ReactionPublication r WHERE r.publication.id = :publicationId")
    void supprimerCellesDeLaPublication(@Param("publicationId") Long publicationId);

    @Modifying
    @Query("DELETE FROM ReactionPublication r WHERE r.utilisateur.id = :utilisateurId")
    void supprimerCellesDe(@Param("utilisateurId") Long utilisateurId);

    @Modifying
    @Query("DELETE FROM ReactionPublication r WHERE r.publication.id IN "
            + "(SELECT p.id FROM Publication p WHERE p.auteur.id = :utilisateurId)")
    void supprimerCellesDesPublicationsDe(@Param("utilisateurId") Long utilisateurId);
}
```

- [ ] **Step 7 : `controller/PublicationController.java`**

```java
package com.example.carnet_contact_backend.controller;

import com.example.carnet_contact_backend.controller.Reactions.ReactionResume;
import com.example.carnet_contact_backend.model.Categorie;
import com.example.carnet_contact_backend.model.Publication;
import com.example.carnet_contact_backend.model.ReactionPublication;
import com.example.carnet_contact_backend.model.Role;
import com.example.carnet_contact_backend.model.Utilisateur;
import com.example.carnet_contact_backend.repository.PublicationRepository;
import com.example.carnet_contact_backend.repository.ReactionPublicationRepository;
import com.example.carnet_contact_backend.repository.UtilisateurRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Le fil d'actualité : un espace PUBLIC, commun à tous les comptes. */
@RestController
@RequestMapping("/api/publications")
public class PublicationController {

    /** Au-delà, une seule requête chargerait trop de lignes d'un coup. */
    private static final int TAILLE_MAX = 30;

    private final PublicationRepository publicationRepository;
    private final ReactionPublicationRepository reactionRepository;
    private final UtilisateurRepository utilisateurRepository;

    public PublicationController(
            PublicationRepository publicationRepository,
            ReactionPublicationRepository reactionRepository,
            UtilisateurRepository utilisateurRepository) {
        this.publicationRepository = publicationRepository;
        this.reactionRepository = reactionRepository;
        this.utilisateurRepository = utilisateurRepository;
    }

    /**
     * Le corps d'une création ou d'une modification, avec ses règles.
     *
     * `^$|` dans le motif de l'image : la chaîne vide est acceptée (le champ
     * est facultatif) ; sinon, une adresse http(s) sans espace. Cela écarte
     * notamment `javascript:…`.
     */
    public record DemandePublication(
            @NotNull(message = "La catégorie est obligatoire.")
            Categorie categorie,

            @NotBlank(message = "Le contenu est obligatoire.")
            @Size(max = 2000, message = "Le contenu dépasse 2000 caractères.")
            String contenu,

            @Size(max = 500, message = "L'adresse de l'image dépasse 500 caractères.")
            @Pattern(regexp = "^$|^https?://\\S+$", message = "L'image doit être une adresse http ou https.")
            String imageUrl) {}

    public record DemandeReaction(String emoji) {}

    /** `modifiable` et `supprimable` sont calculés pour le compte qui regarde. */
    public record PublicationVue(
            Long id,
            AuteurPublic auteur,
            Categorie categorie,
            String contenu,
            String imageUrl,
            Instant datePublication,
            Instant dateModification,
            List<ReactionResume> reactions,
            boolean modifiable,
            boolean supprimable) {}

    /** `curseurSuivant` : id à passer en `avant` pour la suite, null s'il n'y en a plus. */
    public record PageFil(List<PublicationVue> publications, Long curseurSuivant) {}

    private Utilisateur utilisateurConnecte(String email) {
        return utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    @GetMapping
    public PageFil fil(
            @RequestParam(required = false) Categorie categorie,
            @RequestParam(required = false) Long avant,
            @RequestParam(defaultValue = "10") int taille,
            @AuthenticationPrincipal String email) {

        Utilisateur moi = utilisateurConnecte(email);
        int tailleBornee = Math.clamp(taille, 1, TAILLE_MAX);

        // Une ligne de PLUS que demandé : le moyen le plus économe de savoir
        // s'il reste des publications plus anciennes, sans requête COUNT.
        List<Publication> lues = publicationRepository.fil(
                categorie, avant, PageRequest.of(0, tailleBornee + 1));

        boolean resteDesPlusAnciennes = lues.size() > tailleBornee;
        List<Publication> page = resteDesPlusAnciennes ? lues.subList(0, tailleBornee) : lues;
        Long curseurSuivant = resteDesPlusAnciennes ? page.getLast().getId() : null;

        return new PageFil(assembler(page, moi), curseurSuivant);
    }

    @PostMapping
    public PublicationVue publier(
            @Valid @RequestBody DemandePublication demande,
            @AuthenticationPrincipal String email) {
        Utilisateur moi = utilisateurConnecte(email);

        Publication publication = new Publication();
        // L'auteur vient du jeton, jamais du corps de la requête.
        publication.setAuteur(moi);
        appliquer(demande, publication);
        publication.setDatePublication(Instant.now());

        // Une publication neuve n'a aucune réaction : inutile d'interroger la base.
        return vue(publicationRepository.save(publication), List.of(), moi);
    }

    @PutMapping("/{id}")
    public PublicationVue modifier(
            @PathVariable Long id,
            @Valid @RequestBody DemandePublication demande,
            @AuthenticationPrincipal String email) {
        Utilisateur moi = utilisateurConnecte(email);
        Publication publication = publicationExistante(id);

        // Même un administrateur ne réécrit pas les mots d'un autre : il peut
        // retirer une publication, pas parler à la place de son auteur.
        if (!estAuteur(publication, moi)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Seul l'auteur peut modifier cette publication.");
        }

        appliquer(demande, publication);
        publication.setDateModification(Instant.now());

        return assembler(List.of(publicationRepository.save(publication)), moi).getFirst();
    }

    /**
     * @Transactional : les réactions partent AVANT la publication (la clé
     * étrangère l'exige), et les deux suppressions réussissent ou échouent
     * ensemble. Il est aussi obligatoire pour les requêtes @Modifying.
     */
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> supprimer(
            @PathVariable Long id,
            @AuthenticationPrincipal String email) {
        Utilisateur moi = utilisateurConnecte(email);
        Publication publication = publicationExistante(id);

        if (!peutSupprimer(publication, moi)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Vous ne pouvez pas supprimer cette publication.");
        }

        reactionRepository.supprimerCellesDeLaPublication(publication.getId());
        publicationRepository.delete(publication);
        return ResponseEntity.noContent().build();
    }

    /** Poser, remplacer ou retirer sa réaction : même bascule que les messages. */
    @PutMapping("/{id}/reaction")
    public PublicationVue reagir(
            @PathVariable Long id,
            @RequestBody DemandeReaction demande,
            @AuthenticationPrincipal String email) {

        if (!Reactions.estAutorise(demande.emoji())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Emoji non autorisé.");
        }

        Utilisateur moi = utilisateurConnecte(email);
        Publication publication = publicationExistante(id);

        reactionRepository.findByPublicationIdAndUtilisateurId(id, moi.getId())
                .ifPresentOrElse(
                        existante -> {
                            if (existante.getEmoji().equals(demande.emoji())) {
                                reactionRepository.delete(existante);
                            } else {
                                existante.setEmoji(demande.emoji());
                                reactionRepository.save(existante);
                            }
                        },
                        () -> {
                            ReactionPublication nouvelle = new ReactionPublication();
                            nouvelle.setPublication(publication);
                            nouvelle.setUtilisateur(moi);
                            nouvelle.setEmoji(demande.emoji());
                            reactionRepository.save(nouvelle);
                        });

        return assembler(List.of(publication), moi).getFirst();
    }

    private Publication publicationExistante(Long id) {
        return publicationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private void appliquer(DemandePublication demande, Publication publication) {
        publication.setCategorie(demande.categorie());
        publication.setContenu(demande.contenu().strip());
        // Chaîne vide ramenée à null : « pas d'image » s'écrit d'une seule façon.
        publication.setImageUrl(
                demande.imageUrl() == null || demande.imageUrl().isBlank() ? null : demande.imageUrl());
    }

    private boolean estAuteur(Publication publication, Utilisateur moi) {
        return publication.getAuteur().getId().equals(moi.getId());
    }

    /**
     * Le rôle est lu sur `moi`, rechargé depuis la base à chaque requête — pas
     * dans le jeton. Pour une action destructrice, on ne se fie pas à un rôle
     * qui peut avoir été retiré depuis l'émission du jeton (section 29).
     */
    private boolean peutSupprimer(Publication publication, Utilisateur moi) {
        return estAuteur(publication, moi) || moi.getRole() == Role.ADMIN;
    }

    private List<PublicationVue> assembler(List<Publication> publications, Utilisateur moi) {
        if (publications.isEmpty()) {
            return List.of();
        }

        Map<Long, List<ReactionResume>> reactions = Reactions.resumerParCible(
                reactionRepository.findByPublicationIdIn(
                        publications.stream().map(Publication::getId).toList()),
                moi.getId());

        return publications.stream()
                .map(p -> vue(p, reactions.getOrDefault(p.getId(), List.of()), moi))
                .toList();
    }

    private PublicationVue vue(Publication p, List<ReactionResume> reactions, Utilisateur moi) {
        return new PublicationVue(
                p.getId(), AuteurPublic.de(p.getAuteur()), p.getCategorie(), p.getContenu(),
                p.getImageUrl(), p.getDatePublication(), p.getDateModification(), reactions,
                estAuteur(p, moi), peutSupprimer(p, moi));
    }
}
```

- [ ] **Step 8 : vérifier** — `.\mvnw.cmd test`. Attendu : 66 + 24 = 90 tests (le test paramétré compte pour 4), 0 échec. Si Hibernate refuse `:categorie IS NULL` (type de paramètre indéterminé), remplacer par deux méthodes `fil` / `filParCategorie` appelées selon `categorie == null`, et le noter au pense-bête.

- [ ] **Step 9 : point de contrôle** — `git status`.

---

### Task 3 : Suppression d'un compte et compteur de publications (administration, serveur)

**Files:**
- Modify: `controller/AdminController.java`
- Test: `src/test/.../controller/AdminControllerTest.java`

**Interfaces:**
- Consumes (Task 2) : `PublicationRepository.countByAuteurId`, `.supprimerCellesDe` ; `ReactionPublicationRepository.supprimerCellesDe`, `.supprimerCellesDesPublicationsDe`.
- Produces : `LigneCompte.nombrePublications` (JSON `nombrePublications`, placé après `nombreMessages`).

- [ ] **Step 1 : test qui échoue** — dans `AdminControllerTest`, ajouter les imports (`PublicationRepository`, `ReactionPublicationRepository`, `jakarta.persistence.EntityManager`, `com.jayway.jsonpath.JsonPath`), les champs et le test :

```java
    @Autowired
    private PublicationRepository publicationRepository;

    @Autowired
    private ReactionPublicationRepository reactionPublicationRepository;

    @Autowired
    private EntityManager entityManager;

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

        // Écriture forcée : c'est au flush que les clés étrangères sont vérifiées.
        entityManager.flush();
        entityManager.clear();

        assertThat(publicationRepository.findById(publicationSimple)).isEmpty();
        assertThat(publicationRepository.findById(publicationPatron)).isPresent();
        assertThat(reactionPublicationRepository.count()).isZero();
    }
```

- [ ] **Step 2 : constater l'échec** — `.\mvnw.cmd test -Dtest=AdminControllerTest`. Attendu : `nombrePublications` absent, puis violation de clé étrangère au flush.

- [ ] **Step 3 : `AdminController`** — injecter les deux dépôts (champs `publicationRepository`, `reactionPublicationRepository`, ajoutés au constructeur) ; `LigneCompte` gagne `long nombrePublications` après `nombreMessages` ; dans `supprimer`, AVANT `reactionRepository.supprimerCellesDe(...)` :

```java
        // Le fil d'abord : les réactions de ce compte, puis celles des autres
        // sur ses publications, puis ses publications elles-mêmes. Toujours la
        // même règle — des feuilles vers la racine.
        reactionPublicationRepository.supprimerCellesDe(cible.getId());
        reactionPublicationRepository.supprimerCellesDesPublicationsDe(cible.getId());
        publicationRepository.supprimerCellesDe(cible.getId());
```

et dans `ligne(...)`, après le compteur de messages :

```java
                publicationRepository.countByAuteurId(u.getId()),
```

- [ ] **Step 4 : vérifier** — `.\mvnw.cmd test`. Attendu : 91 tests, 0 échec.

- [ ] **Step 5 : point de contrôle** — `git status`.

---

### Task 4 : Modèles Angular (réactions partagées, auteur public, publication)

**Files:**
- Create: `reaction.model.ts`, `publication.model.ts`
- Modify: `utilisateur.model.ts`, `message.model.ts`, `donnees-test.ts`, `services/auth.ts`, `pages/messages/messages.ts`, `services/admin.spec.ts`, `pages/admin/admin.ts`, `pages/admin/admin.html`, `pages/admin/admin.css`

**Interfaces:**
- Produces : `AuteurPublic { id: number; nomAffichage: string; photoUrl?: string | null }` ;
  `ReactionResume`, `EMOJIS_REACTION` depuis `reaction.model.ts` ;
  `CATEGORIES`, `type Categorie`, `interface Publication`, `interface PageFil`, `interface DemandePublication`, `categorieDe(cle)` depuis `publication.model.ts` ;
  `unAuteur(partiel)`, `unePublication(partiel)` depuis `donnees-test.ts`.

Tâche de refactorisation : le filet de sécurité est la compilation et les 70 tests existants.

- [ ] **Step 1 : `reaction.model.ts`** — déplacer depuis `message.model.ts`, commentaires compris, `ReactionResume` et `EMOJIS_REACTION`, avec cet en-tête :

```typescript
/**
 * Les réactions emoji, communes aux messages et aux publications du fil.
 *
 * Elles vivaient dans message.model.ts tant que seuls les messages en avaient.
 * Dès qu'un second modèle en a besoin, les laisser là obligerait le fil à
 * importer « un morceau de la messagerie » — une dépendance qui ne veut rien
 * dire.
 */
```

- [ ] **Step 2 : `utilisateur.model.ts`** — ajouter après `Utilisateur` :

```typescript
/**
 * Ce qu'un compte laisse voir de lui aux AUTRES : ni email, ni rôle, ni état.
 *
 * C'est la forme que renvoient désormais la liste des interlocuteurs, les
 * messages et le fil. Le compte complet (`Utilisateur`) ne sort plus que pour
 * soi-même (`/api/utilisateurs/moi`) et pour l'administration.
 */
export interface AuteurPublic {
  id: number;
  nomAffichage: string;
  photoUrl?: string | null;
}
```

et `nombrePublications: number;` dans `LigneCompte`, après `nombreMessages`.

- [ ] **Step 3 : `message.model.ts`** — ne garde que `Message`, avec :

```typescript
import { AuteurPublic } from './utilisateur.model';
import { ReactionResume } from './reaction.model';
```

et `expediteur: AuteurPublic;` / `destinataire: AuteurPublic;`.

- [ ] **Step 4 : `publication.model.ts`**

```typescript
import { AuteurPublic } from './utilisateur.model';
import { ReactionResume } from './reaction.model';

/**
 * Les catégories du fil, décrites une seule fois — même patron que RESEAUX.
 *
 * `cle` est écrite exactement comme l'enum Java `Categorie` la sérialise. Le
 * serveur décide des valeurs ACCEPTÉES ; ce tableau décide de leur AFFICHAGE :
 * libellé, emoji et couleur. Il sert à la fois aux pastilles de filtre, au
 * menu du formulaire et à l'étiquette de chaque carte.
 */
export const CATEGORIES = [
  { cle: 'SPORT', libelle: 'Sport', emoji: '⚽', couleur: '#16a34a' },
  { cle: 'CULTURE', libelle: 'Culture', emoji: '🎭', couleur: '#9333ea' },
  { cle: 'JEU_VIDEO', libelle: 'Jeu vidéo', emoji: '🎮', couleur: '#4f46e5' },
  { cle: 'INFORMATIQUE', libelle: 'Informatique', emoji: '💻', couleur: '#0891b2' },
  { cle: 'ACTUALITE', libelle: 'Actualité', emoji: '📰', couleur: '#b45309' },
  { cle: 'MUSIQUE', libelle: 'Musique', emoji: '🎵', couleur: '#db2777' },
  { cle: 'CUISINE', libelle: 'Cuisine', emoji: '🍳', couleur: '#ea580c' },
  { cle: 'VOYAGE', libelle: 'Voyage', emoji: '✈️', couleur: '#0284c7' },
  { cle: 'NATURE', libelle: 'Nature & plein air', emoji: '🌿', couleur: '#65a30d' },
  { cle: 'CREATIONS', libelle: 'Créations', emoji: '🎨', couleur: '#c026d3' },
  { cle: 'AUTRE', libelle: 'Autre', emoji: '💬', couleur: '#64748b' }
] as const;

/** « Une des onze clés », déduit du tableau : impossible de les désynchroniser. */
export type Categorie = typeof CATEGORIES[number]['cle'];

export interface Publication {
  id: number;
  auteur: AuteurPublic;
  categorie: Categorie;
  contenu: string;
  imageUrl: string | null;
  // Instant Java sérialisé en chaîne ISO 8601, comme dateEnvoi des messages.
  datePublication: string;
  dateModification: string | null;
  reactions: ReactionResume[];
  // Calculés par le serveur pour le compte qui regarde : le gabarit n'a pas à
  // comparer des identifiants ni à connaître les règles de droits.
  modifiable: boolean;
  supprimable: boolean;
}

/** Une tranche du fil. `curseurSuivant` : null quand il n'y a plus rien après. */
export interface PageFil {
  publications: Publication[];
  curseurSuivant: number | null;
}

export interface DemandePublication {
  categorie: Categorie;
  contenu: string;
  imageUrl: string;
}

/** La description complète d'une catégorie à partir de sa clé. */
export function categorieDe(cle: Categorie) {
  // Le `!` est sûr : le type Categorie garantit que la clé figure dans le tableau.
  return CATEGORIES.find(categorie => categorie.cle === cle)!;
}
```

- [ ] **Step 5 : `donnees-test.ts`** — imports `ReactionResume` depuis `./reaction.model`, `AuteurPublic` depuis `./utilisateur.model`, `Publication` depuis `./publication.model` ; `unMessage` utilise `expediteur: unAuteur({ id: 2, nomAffichage: 'Bob' })` et `destinataire: unAuteur({ id: 1, nomAffichage: 'Alice' })` ; ajouter :

```typescript
export function unAuteur(modifications: Partial<AuteurPublic> = {}): AuteurPublic {
  return { id: 2, nomAffichage: 'Bob', photoUrl: null, ...modifications };
}

export function unePublication(modifications: Partial<Publication> = {}): Publication {
  return {
    id: 1,
    auteur: unAuteur(),
    categorie: 'SPORT',
    contenu: 'Sortie vélo dimanche',
    imageUrl: null,
    datePublication: '2026-09-14T10:00:00Z',
    dateModification: null,
    reactions: [],
    modifiable: false,
    supprimable: false,
    ...modifications
  };
}
```

- [ ] **Step 6 : types `AuteurPublic` côté messagerie** — `services/auth.ts` : `autresUtilisateurs(): Observable<AuteurPublic[]>` et `http.get<AuteurPublic[]>` ; `pages/messages/messages.ts` : `interlocuteurs = signal<AuteurPublic[]>([])`, `selection = signal<AuteurPublic | null>(null)`, `ouvrir(utilisateur: AuteurPublic)`, `EMOJIS_REACTION` importé de `../../reaction.model`, `Utilisateur` remplacé par `AuteurPublic` dans l'import.

- [ ] **Step 7 : administration** — `services/admin.spec.ts` : `nombrePublications: 2,` après `nombreMessages: 7,` ; `pages/admin/admin.ts`, message de confirmation :

```typescript
      message: `Le compte « ${compte.nomAffichage} » sera supprimé, ainsi que ses `
        + `${compte.nombreContacts} contact(s), ${compte.nombreMessages} message(s) `
        + `et ${compte.nombrePublications} publication(s). Cette action est irréversible.`,
```

`pages/admin/admin.html` : après la colonne Messages, `<th pSortableColumn="nombrePublications" class="colonne-nombre">Publications <p-sortIcon field="nombrePublications" /></th>` et `<td class="colonne-nombre">{{ compte.nombrePublications }}</td>` ; `colspan="8"` ; `'min-width': '52rem'`.

- [ ] **Step 8 : vérifier** — `npx ng test --watch=false` puis `npx ng build`. Attendu : 70 tests au vert, build sans erreur.

- [ ] **Step 9 : point de contrôle** — `git status`.

---

### Task 5 : `PublicationService`

**Files:**
- Create: `services/publication.ts`
- Test: `services/publication.spec.ts`

**Interfaces:**
- Consumes (Task 4) : `Categorie`, `Publication`, `PageFil`, `DemandePublication` ; `unePublication`, `uneReaction`.
- Produces : `PublicationService` avec `publications: Signal<Publication[]>`, `categorie: Signal<Categorie | null>`, `aDesPlusAnciennes: Signal<boolean>`, `charger(categorie: Categorie | null): void`, `chargerPlus(): void`, `publier(d: DemandePublication): Observable<Publication>`, `modifier(id: number, d: DemandePublication): Observable<Publication>`, `supprimer(id: number): void`, `reagir(id: number, emoji: string): void`.

- [ ] **Step 1 : tests qui échouent** — `services/publication.spec.ts` :

```typescript
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { PublicationService } from './publication';
import { PageFil, Publication } from '../publication.model';
import { unePublication, uneReaction } from '../donnees-test';

describe('PublicationService', () => {
  let service: PublicationService;
  let backend: HttpTestingController;

  const page = (publications: Publication[], curseurSuivant: number | null = null): PageFil =>
    ({ publications, curseurSuivant });

  const requeteFil = () =>
    backend.expectOne(r => r.method === 'GET' && r.url === '/api/publications');

  const ids = () => service.publications().map(p => p.id);

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });

    service = TestBed.inject(PublicationService);
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => backend.verify());

  it('charge la première page, sans catégorie ni curseur', () => {
    service.charger(null);

    const requete = requeteFil();
    expect(requete.request.params.get('taille')).toBe('10');
    expect(requete.request.params.has('categorie')).toBe(false);
    expect(requete.request.params.has('avant')).toBe(false);

    requete.flush(page([unePublication({ id: 3 })], 3));

    expect(ids()).toEqual([3]);
    expect(service.aDesPlusAnciennes()).toBe(true);
  });

  it('envoie la catégorie choisie', () => {
    service.charger('SPORT');

    const requete = requeteFil();
    expect(requete.request.params.get('categorie')).toBe('SPORT');
    requete.flush(page([]));

    expect(service.categorie()).toBe('SPORT');
  });

  it('« Voir plus » repart du curseur et ajoute à la fin', () => {
    service.charger(null);
    requeteFil().flush(page([unePublication({ id: 12 }), unePublication({ id: 11 })], 11));

    service.chargerPlus();

    const requete = requeteFil();
    expect(requete.request.params.get('avant')).toBe('11');
    requete.flush(page([unePublication({ id: 10 })], null));

    expect(ids()).toEqual([12, 11, 10]);
    expect(service.aDesPlusAnciennes()).toBe(false);
  });

  it('« Voir plus » ne demande rien quand tout est affiché', () => {
    service.charger(null);
    requeteFil().flush(page([unePublication()], null));

    service.chargerPlus();

    // verify() dans afterEach échouerait si une requête était partie.
  });

  /**
   * Le test qui justifie de faire passer « Voir plus » par le même switchMap
   * que le changement de filtre. Sans annulation, la suite du fil « Tout »
   * arriverait après le changement et s'ajouterait sous les publications
   * « Cuisine ».
   */
  it('changer de catégorie annule un « Voir plus » encore en route', () => {
    service.charger(null);
    requeteFil().flush(page([unePublication({ id: 12 })], 12));

    service.chargerPlus();
    service.charger('CUISINE');

    const requetes = backend.match(r => r.url === '/api/publications');
    expect(requetes.length).toBe(2);
    expect(requetes[0].cancelled).toBe(true);

    requetes[1].flush(page([unePublication({ id: 5, categorie: 'CUISINE' })]));
    expect(ids()).toEqual([5]);
  });

  it('reste utilisable après une erreur serveur', () => {
    service.charger(null);
    requeteFil().flush('', { status: 500, statusText: 'Server Error' });

    service.charger(null);
    requeteFil().flush(page([unePublication({ id: 1 })]));

    expect(ids()).toEqual([1]);
  });

  it('une publication de la catégorie filtrée apparaît en tête', () => {
    service.charger('SPORT');
    requeteFil().flush(page([unePublication({ id: 1, categorie: 'SPORT' })]));

    service.publier({ categorie: 'SPORT', contenu: 'Piscine', imageUrl: '' }).subscribe();
    backend.expectOne(r => r.method === 'POST')
      .flush(unePublication({ id: 2, categorie: 'SPORT' }));

    expect(ids()).toEqual([2, 1]);
  });

  it('une publication d\'une autre catégorie n\'entre pas dans le filtre', () => {
    service.charger('SPORT');
    requeteFil().flush(page([unePublication({ id: 1, categorie: 'SPORT' })]));

    service.publier({ categorie: 'CUISINE', contenu: 'Tarte', imageUrl: '' }).subscribe();
    backend.expectOne(r => r.method === 'POST')
      .flush(unePublication({ id: 2, categorie: 'CUISINE' }));

    expect(ids()).toEqual([1]);
  });

  it('une publication modifiée qui quitte la catégorie filtrée disparaît', () => {
    service.charger('SPORT');
    requeteFil().flush(page([unePublication({ id: 1, categorie: 'SPORT' })]));

    service.modifier(1, { categorie: 'NATURE', contenu: 'Randonnée', imageUrl: '' }).subscribe();
    backend.expectOne(r => r.method === 'PUT' && r.url === '/api/publications/1')
      .flush(unePublication({ id: 1, categorie: 'NATURE' }));

    expect(ids()).toEqual([]);
  });

  /** Le curseur rend la mise à jour locale sûre : rien ne se décale derrière. */
  it('supprime localement, sans recharger le fil', () => {
    service.charger(null);
    requeteFil().flush(page([unePublication({ id: 2 }), unePublication({ id: 1 })]));

    service.supprimer(2);
    backend.expectOne(r => r.method === 'DELETE' && r.url === '/api/publications/2').flush(null);

    expect(ids()).toEqual([1]);
  });

  it('remplace la publication réagie par la réponse du serveur', () => {
    service.charger(null);
    requeteFil().flush(page([unePublication({ id: 1 }), unePublication({ id: 2 })]));

    service.reagir(1, '👍');

    const requete = backend.expectOne('/api/publications/1/reaction');
    expect(requete.request.method).toBe('PUT');
    expect(requete.request.body).toEqual({ emoji: '👍' });
    requete.flush(unePublication({ id: 1, reactions: [uneReaction({ parMoi: true })] }));

    expect(service.publications()[0].reactions[0].parMoi).toBe(true);
    expect(service.publications()[1].reactions).toEqual([]);
  });
});
```

- [ ] **Step 2 : constater l'échec** — `npx ng test --watch=false`. Attendu : échec de compilation, `./publication` introuvable.

- [ ] **Step 3 : `services/publication.ts`**

```typescript
import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { EMPTY, Observable, Subject, catchError, map, switchMap, tap } from 'rxjs';
import { Categorie, DemandePublication, PageFil, Publication } from '../publication.model';
import { contexte } from '../interceptors/http-contexte';

/** Nombre de publications demandées à chaque chargement. */
const TAILLE_FIL = 10;

/**
 * Une demande de tranche du fil.
 *
 * La catégorie et le curseur voyagent DANS la demande, figés au moment du clic,
 * au lieu d'être relus dans les signaux quand la requête part : une demande
 * décrit exactement ce qu'on a voulu, même si l'état a bougé depuis.
 */
interface DemandeTranche {
  suite: boolean;
  categorie: Categorie | null;
  avant: number | null;
}

@Injectable({
  providedIn: 'root'
})
export class PublicationService {
  private http = inject(HttpClient);
  private apiUrl = '/api/publications';

  private publicationsSignal = signal<Publication[]>([]);
  readonly publications = this.publicationsSignal.asReadonly();

  // Le filtre actif. Il vit dans le service, comme la recherche des contacts :
  // après une publication, c'est le service qui doit savoir si elle entre dans
  // la liste affichée.
  private categorieSignal = signal<Categorie | null>(null);
  readonly categorie = this.categorieSignal.asReadonly();

  /**
   * Le CURSEUR : l'id de la plus ancienne publication affichée, à renvoyer en
   * `avant` pour obtenir la suite. null quand le serveur a tout donné.
   *
   * Pas de numéro de page ici. Un fil grandit par le haut pendant qu'on le lit :
   * « page 2 » ne désignerait plus les mêmes publications d'une minute à
   * l'autre, alors que « plus anciennes que 42 » désigne toujours les mêmes.
   */
  private curseurSignal = signal<number | null>(null);
  readonly aDesPlusAnciennes = computed(() => this.curseurSignal() !== null);

  /**
   * Un seul tuyau pour les deux sortes de demandes (nouveau filtre, suite du
   * fil), et un switchMap au bout : la dernière demande annule celle qui était
   * en route. C'est ce qui empêche un « Voir plus » lent de venir ajouter des
   * publications d'une catégorie qu'on vient de quitter.
   */
  private demandes = new Subject<DemandeTranche>();

  constructor() {
    this.demandes.pipe(
      switchMap(demande => this.http.get<PageFil>(this.apiUrl, {
        params: this.parametres(demande),
        context: contexte({ libelle: 'Impossible de charger le fil' })
      }).pipe(
        // On garde `suite` à côté de la réponse : à l'arrivée, il faut savoir
        // s'il s'agit de REMPLACER la liste ou de la PROLONGER.
        map(tranche => ({ tranche, suite: demande.suite })),
        // À l'intérieur du switchMap, comme dans ContactService : une panne ne
        // doit pas fermer le tuyau pour le reste de la session.
        catchError(() => EMPTY)
      ))
    ).subscribe(({ tranche, suite }) => {
      this.publicationsSignal.update(liste =>
        suite ? [...liste, ...tranche.publications] : tranche.publications);
      this.curseurSignal.set(tranche.curseurSuivant);
    });
  }

  /** (Re)part du début du fil, pour une catégorie ou pour tout. */
  charger(categorie: Categorie | null): void {
    this.categorieSignal.set(categorie);
    // On vide tout de suite : laisser les publications « Sport » affichées
    // sous le filtre « Cuisine » le temps de la réponse serait trompeur.
    this.publicationsSignal.set([]);
    this.curseurSignal.set(null);
    this.demandes.next({ suite: false, categorie, avant: null });
  }

  /** La tranche suivante, plus ancienne. Sans effet quand tout est affiché. */
  chargerPlus(): void {
    const avant = this.curseurSignal();
    if (avant === null) {
      return;
    }
    this.demandes.next({ suite: true, categorie: this.categorieSignal(), avant });
  }

  /**
   * Publier RENVOIE l'Observable, comme AuthService.connexion : l'appelant a
   * besoin de savoir que ça a réussi pour vider son formulaire — et surtout de
   * savoir que ça a échoué pour NE PAS le vider.
   *
   * Pas de rechargement après coup, contrairement aux contacts : avec un
   * curseur, insérer en tête ne décale rien de ce qui est déjà chargé.
   */
  publier(demande: DemandePublication): Observable<Publication> {
    return this.http.post<Publication>(this.apiUrl, demande, {
      context: contexte({ libelle: 'Impossible de publier' })
    }).pipe(
      tap(publication => {
        if (this.correspondAuFiltre(publication)) {
          this.publicationsSignal.update(liste => [publication, ...liste]);
        }
      })
    );
  }

  modifier(id: number, demande: DemandePublication): Observable<Publication> {
    return this.http.put<Publication>(`${this.apiUrl}/${id}`, demande, {
      context: contexte({ libelle: 'Impossible d\'enregistrer la modification' })
    }).pipe(
      tap(publication => {
        // Changer de catégorie peut faire sortir la publication du filtre
        // affiché : on la retire plutôt que de laisser une carte « Nature »
        // dans la liste « Sport ».
        this.publicationsSignal.update(liste => this.correspondAuFiltre(publication)
          ? liste.map(p => (p.id === id ? publication : p))
          : liste.filter(p => p.id !== id));
      })
    );
  }

  supprimer(id: number): void {
    this.http.delete<void>(`${this.apiUrl}/${id}`, {
      context: contexte({ libelle: 'Impossible de supprimer la publication' })
    }).pipe(
      catchError(() => EMPTY)
    ).subscribe(() => {
      this.publicationsSignal.update(liste => liste.filter(p => p.id !== id));
    });
  }

  reagir(id: number, emoji: string): void {
    this.http.put<Publication>(`${this.apiUrl}/${id}/reaction`, { emoji }, {
      context: contexte({ discret: true, libelle: 'Impossible d\'enregistrer la réaction' })
    }).pipe(
      catchError(() => EMPTY)
    ).subscribe(publication => {
      this.publicationsSignal.update(liste => liste.map(p => (p.id === id ? publication : p)));
    });
  }

  private correspondAuFiltre(publication: Publication): boolean {
    const filtre = this.categorieSignal();
    return filtre === null || filtre === publication.categorie;
  }

  /** N'envoie que les paramètres utiles : pas de `categorie=null` dans l'URL. */
  private parametres(demande: DemandeTranche): Record<string, string | number> {
    const params: Record<string, string | number> = { taille: TAILLE_FIL };

    if (demande.categorie !== null) {
      params['categorie'] = demande.categorie;
    }
    if (demande.avant !== null) {
      params['avant'] = demande.avant;
    }

    return params;
  }
}
```

- [ ] **Step 4 : vérifier** — `npx ng test --watch=false`. Attendu : 70 + 11 = 81 tests au vert.

- [ ] **Step 5 : point de contrôle** — `git status`.

---

### Task 6 : composant `publication-form`

**Files:**
- Create: `components/publication-form/publication-form.ts`, `.html`, `.css`
- Test: `components/publication-form/publication-form.spec.ts`

**Interfaces:**
- Consumes (Tasks 4, 5) : `CATEGORIES`, `Categorie`, `DemandePublication`, `Publication` ; `PublicationService.publier`, `.modifier` ; `EtatHttpService.chargement`.
- Produces : `<app-publication-form [publication]="p" (termine)="…" (annuler)="…" />` — `publication = input<Publication | null>(null)`, `termine = output<Publication>()`, `annuler = output<void>()`, `formulaire` (contrôles `categorie`, `contenu`, `imageUrl`), `onSubmit()`.

- [ ] **Step 1 : tests qui échouent** — `publication-form.spec.ts` :

```typescript
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { PublicationForm } from './publication-form';
import { Publication } from '../../publication.model';
import { unePublication } from '../../donnees-test';

describe('PublicationForm', () => {
  let fixture: ComponentFixture<PublicationForm>;
  let composant: PublicationForm;
  let backend: HttpTestingController;

  /**
   * Le composant est créé DANS chaque test, et non dans le beforeEach : en mode
   * modification, la publication doit être passée avant le premier affichage,
   * puisque c'est ngOnInit qui pré-remplit le formulaire.
   */
  async function creer(publication: Publication | null = null): Promise<void> {
    fixture = TestBed.createComponent(PublicationForm);
    composant = fixture.componentInstance;
    fixture.componentRef.setInput('publication', publication);
    await fixture.whenStable();
  }

  function remplir(valeurs: Partial<{ categorie: 'SPORT' | 'CUISINE'; contenu: string; imageUrl: string }> = {}) {
    composant.formulaire.setValue({ categorie: 'SPORT', contenu: 'Vélo', imageUrl: '', ...valeurs });
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [PublicationForm],
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => backend.verify());

  it('exige une catégorie et un contenu', async () => {
    await creer();
    expect(composant.formulaire.valid).toBe(false);

    remplir();
    expect(composant.formulaire.valid).toBe(true);
  });

  /** Même règle que @NotBlank côté serveur : des espaces ne sont pas un contenu. */
  it('refuse un contenu fait seulement d\'espaces', async () => {
    await creer();
    remplir({ contenu: '    ' });
    expect(composant.formulaire.valid).toBe(false);
  });

  it('refuse un contenu de plus de 2000 caractères', async () => {
    await creer();
    remplir({ contenu: 'a'.repeat(2001) });
    expect(composant.formulaire.valid).toBe(false);
  });

  it('accepte une image vide ou en http(s), refuse le reste', async () => {
    await creer();

    remplir({ imageUrl: 'javascript:alert(1)' });
    expect(composant.formulaire.valid).toBe(false);

    remplir({ imageUrl: 'https://exemple.fr/photo.jpg' });
    expect(composant.formulaire.valid).toBe(true);

    remplir({ imageUrl: '' });
    expect(composant.formulaire.valid).toBe(true);
  });

  /**
   * LE test qui compte, et le défaut relevé par la revue sur le formulaire de
   * contact : vider la saisie AVANT la réponse du serveur, c'est la perdre au
   * premier échec.
   */
  it('garde la saisie quand la publication échoue', async () => {
    await creer();
    remplir({ contenu: 'Ma sortie vélo' });

    composant.onSubmit();
    backend.expectOne(r => r.method === 'POST')
      .flush('', { status: 500, statusText: 'Server Error' });

    expect(composant.formulaire.controls.contenu.value).toBe('Ma sortie vélo');
  });

  it('vide le texte, garde la catégorie et prévient le parent après un succès', async () => {
    await creer();
    const recues: Publication[] = [];
    composant.termine.subscribe(publication => recues.push(publication));
    remplir({ contenu: 'Ma sortie vélo' });

    composant.onSubmit();
    backend.expectOne(r => r.method === 'POST').flush(unePublication({ id: 7 }));

    expect(composant.formulaire.controls.contenu.value).toBe('');
    expect(composant.formulaire.controls.categorie.value).toBe('SPORT');
    expect(recues.map(p => p.id)).toEqual([7]);
  });

  it('pré-remplit le formulaire et envoie un PUT en mode modification', async () => {
    await creer(unePublication({
      id: 4, categorie: 'CUISINE', contenu: 'Tarte aux pommes', imageUrl: 'https://exemple.fr/tarte.jpg'
    }));

    expect(composant.formulaire.getRawValue()).toEqual({
      categorie: 'CUISINE', contenu: 'Tarte aux pommes', imageUrl: 'https://exemple.fr/tarte.jpg'
    });

    composant.onSubmit();
    backend.expectOne(r => r.method === 'PUT' && r.url === '/api/publications/4')
      .flush(unePublication({ id: 4, categorie: 'CUISINE' }));
  });
});
```

- [ ] **Step 2 : constater l'échec** — `npx ng test --watch=false`. Attendu : `./publication-form` introuvable.

- [ ] **Step 3 : `publication-form.ts`**

```typescript
import { Component, OnInit, inject, input, output } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { PublicationService } from '../../services/publication';
import { EtatHttpService } from '../../services/etat-http';
import { CATEGORIES, Categorie, DemandePublication, Publication } from '../../publication.model';

/** La même limite que la colonne et le @Size du serveur. */
const LONGUEUR_MAX_CONTENU = 2000;

/**
 * Le miroir du @Pattern serveur. Vide est accepté d'office : Validators.pattern
 * ne s'applique pas à un champ vide (c'est le rôle de required).
 */
const MOTIF_IMAGE = /^https?:\/\/\S+$/;

/**
 * Pour que chaque formulaire de la page ait ses propres `id`. Le formulaire de
 * publication et ceux ouverts en modification dans les cartes coexistent :
 * deux `id="contenu"` casseraient les `<label for>` — un clic sur le libellé
 * enverrait le curseur dans le mauvais formulaire.
 */
let prochainNumero = 1;

@Component({
  selector: 'app-publication-form',
  imports: [ReactiveFormsModule, ButtonModule],
  templateUrl: './publication-form.html',
  styleUrl: './publication-form.css'
})
export class PublicationForm implements OnInit {
  private fb = inject(FormBuilder);
  private publicationService = inject(PublicationService);
  private etatHttp = inject(EtatHttpService);

  /** La publication à modifier ; absente, le formulaire en crée une. */
  publication = input<Publication | null>(null);

  /** Émis après un enregistrement RÉUSSI, avec la réponse du serveur. */
  termine = output<Publication>();

  annuler = output<void>();

  readonly categories = CATEGORIES;
  readonly longueurMax = LONGUEUR_MAX_CONTENU;
  readonly numero = prochainNumero++;
  chargement = this.etatHttp.chargement;

  formulaire = this.fb.group({
    // fb.control<Categorie | null> : sans le type explicite, TypeScript
    // déduirait `null` tout court, et refuserait ensuite 'SPORT'.
    categorie: this.fb.control<Categorie | null>(null, Validators.required),
    contenu: ['', [
      Validators.required,
      // Au moins un caractère qui ne soit pas un espace : required seul
      // laisserait passer « ␣␣␣ », que le serveur refuserait (@NotBlank).
      Validators.pattern(/\S/),
      Validators.maxLength(LONGUEUR_MAX_CONTENU)
    ]],
    imageUrl: ['', [Validators.maxLength(500), Validators.pattern(MOTIF_IMAGE)]]
  });

  /**
   * ngOnInit et non le constructeur : les input() ne sont pas encore reçus au
   * moment de la construction. Pas besoin d'effect() non plus (section 14) —
   * la publication est déjà là, elle ne « arrive » pas plus tard du serveur.
   */
  ngOnInit(): void {
    const existante = this.publication();
    if (existante) {
      this.formulaire.setValue({
        categorie: existante.categorie,
        contenu: existante.contenu,
        imageUrl: existante.imageUrl ?? ''
      });
    }
  }

  longueurContenu(): number {
    return this.formulaire.controls.contenu.value?.length ?? 0;
  }

  /** L'aperçu n'est tenté que sur une adresse valide, pas à chaque lettre. */
  apercuImage(): string | null {
    const champ = this.formulaire.controls.imageUrl;
    return champ.valid && champ.value ? champ.value : null;
  }

  onSubmit(): void {
    if (this.formulaire.invalid) {
      return;
    }

    const valeurs = this.formulaire.getRawValue();
    const demande: DemandePublication = {
      categorie: valeurs.categorie!,
      contenu: valeurs.contenu!,
      imageUrl: valeurs.imageUrl ?? ''
    };

    const existante = this.publication();
    const appel = existante
      ? this.publicationService.modifier(existante.id, demande)
      : this.publicationService.publier(demande);

    appel.subscribe({
      next: publication => {
        if (!existante) {
          // On garde la catégorie : publier deux fois de suite dans « Sport »
          // ne doit pas obliger à la rechoisir.
          this.formulaire.reset({ categorie: demande.categorie, contenu: '', imageUrl: '' });
        }
        this.termine.emit(publication);
      },
      // La bannière est déjà affichée par erreurInterceptor. Ce callback vide
      // n'est pas un oubli : sans lui l'erreur remonterait « non gérée » dans la
      // console. Et on ne touche pas au formulaire — la saisie reste prête à
      // être renvoyée.
      error: () => {}
    });
  }
}
```

- [ ] **Step 4 : `publication-form.html`**

```html
<form class="formulaire-publication" [formGroup]="formulaire" (ngSubmit)="onSubmit()">
  <div class="champ-categorie">
    <label [for]="'categorie-' + numero">Catégorie</label>
    <!-- [ngValue] plutôt que value : il transmet la valeur telle quelle, null
         compris. value la convertirait en chaîne « null ». -->
    <select [id]="'categorie-' + numero" formControlName="categorie">
      <option [ngValue]="null" disabled>Choisir une catégorie…</option>
      @for (categorie of categories; track categorie.cle) {
        <option [ngValue]="categorie.cle">{{ categorie.emoji }} {{ categorie.libelle }}</option>
      }
    </select>
  </div>

  <div>
    <label [for]="'contenu-' + numero">Votre publication</label>
    <textarea
      [id]="'contenu-' + numero"
      formControlName="contenu"
      rows="4"
      placeholder="Une sortie, une découverte, un projet en cours…"></textarea>
    <!-- Le compteur prévient AVANT le refus : on voit la limite approcher. -->
    <p class="compteur" [class.depasse]="longueurContenu() > longueurMax">
      {{ longueurContenu() }} / {{ longueurMax }}
    </p>
  </div>

  <div>
    <label [for]="'image-' + numero">Image (adresse, facultative)</label>
    <input
      [id]="'image-' + numero"
      formControlName="imageUrl"
      placeholder="https://exemple.fr/photo.jpg" />
    @if (formulaire.controls.imageUrl.invalid && formulaire.controls.imageUrl.touched) {
      <p class="erreur-champ">L'adresse doit commencer par http:// ou https://.</p>
    }
    @if (apercuImage(); as url) {
      <img class="apercu" [src]="url" alt="Aperçu de l'image" />
    }
  </div>

  <div class="actions">
    @if (publication()) {
      <p-button label="Annuler" severity="secondary" [text]="true" (onClick)="annuler.emit()" />
    }
    <p-button
      type="submit"
      [icon]="publication() ? 'pi pi-check' : 'pi pi-send'"
      [label]="publication() ? 'Enregistrer' : 'Publier'"
      [disabled]="formulaire.invalid || chargement()" />
  </div>
</form>
```

- [ ] **Step 5 : `publication-form.css`**

```css
.formulaire-publication {
  display: grid;
  gap: 0.85rem;
}

.champ-categorie {
  max-width: 20rem;
}

textarea {
  min-height: 6rem;
  resize: vertical;
}

.compteur {
  margin: 0.25rem 0 0;
  font-size: 0.78rem;
  text-align: right;
  color: var(--texte-doux);
  font-variant-numeric: tabular-nums;
}

.compteur.depasse {
  font-weight: 600;
  color: var(--rouge-fonce);
}

.apercu {
  display: block;
  max-width: 100%;
  max-height: 12rem;
  margin-top: 0.5rem;
  border-radius: var(--rayon-petit);
  object-fit: cover;
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
}
```

- [ ] **Step 6 : vérifier** — `npx ng test --watch=false`. Attendu : 81 + 7 = 88 tests au vert.

- [ ] **Step 7 : point de contrôle** — `git status`.

---

### Task 7 : carte, page du fil, route et navigation

**Files:**
- Create: `components/publication-carte/publication-carte.ts`, `.html`, `.css`
- Create: `pages/fil/fil.ts`, `.html`, `.css`
- Modify: `app.routes.ts`, `app.html`

**Interfaces:**
- Consumes : `PublicationService` (Task 5), `PublicationForm` (Task 6), `categorieDe`, `CATEGORIES`, `EMOJIS_REACTION` (Task 4).
- Produces : `<app-publication-carte [publication]="p" />` ; route `/fil` → `Fil`.

Pas de test automatisé pour ces deux composants de présentation (la logique testable est dans le service et le formulaire) ; vérification par le build, puis à l'écran par l'utilisateur.

- [ ] **Step 1 : `publication-carte.ts`**

```typescript
import { Component, computed, inject, input, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ButtonModule } from 'primeng/button';
import { PublicationService } from '../../services/publication';
import { EtatHttpService } from '../../services/etat-http';
import { Publication, categorieDe } from '../../publication.model';
import { EMOJIS_REACTION } from '../../reaction.model';
import { PublicationForm } from '../publication-form/publication-form';

@Component({
  selector: 'app-publication-carte',
  imports: [DatePipe, ButtonModule, PublicationForm],
  templateUrl: './publication-carte.html',
  styleUrl: './publication-carte.css'
})
export class PublicationCarte {
  private publicationService = inject(PublicationService);
  private etatHttp = inject(EtatHttpService);

  publication = input.required<Publication>();

  readonly emojis = EMOJIS_REACTION;
  chargement = this.etatHttp.chargement;

  /** Libellé, emoji et couleur, déduits de la clé : rien n'est stocké en double. */
  categorie = computed(() => categorieDe(this.publication().categorie));

  initiale = computed(() =>
    (this.publication().auteur.nomAffichage || '?').charAt(0).toUpperCase());

  // Trois états locaux à LA carte : ils n'intéressent personne d'autre, un
  // signal de composant suffit.
  enModification = signal(false);
  paletteOuverte = signal(false);

  /**
   * La suppression demande une confirmation, mais DANS la carte plutôt que
   * dans une boîte de dialogue p-confirmDialog. Deux raisons : le paquet
   * initial frôle déjà son budget d'alerte (la page d'administration, elle,
   * est chargée à part), et la question apparaît à l'endroit exact du clic,
   * sans déplacer le regard ni le focus.
   */
  confirmationOuverte = signal(false);

  basculerPalette(): void {
    this.paletteOuverte.update(ouverte => !ouverte);
  }

  reagir(emoji: string): void {
    this.publicationService.reagir(this.publication().id, emoji);
    this.paletteOuverte.set(false);
  }

  modifier(): void {
    this.confirmationOuverte.set(false);
    this.enModification.set(true);
  }

  finModification(): void {
    this.enModification.set(false);
  }

  demanderSuppression(): void {
    this.confirmationOuverte.set(true);
  }

  annulerSuppression(): void {
    this.confirmationOuverte.set(false);
  }

  confirmerSuppression(): void {
    this.confirmationOuverte.set(false);
    this.publicationService.supprimer(this.publication().id);
  }
}
```

- [ ] **Step 2 : `publication-carte.html`**

```html
<!-- --couleur-categorie : une variable CSS posée depuis le composant, comme
     --couleur-reseau (section 20). Le CSS s'en sert pour le liseré et la
     pastille, sans connaître aucune des onze couleurs. -->
<article class="carte publication" [style.--couleur-categorie]="categorie().couleur">
  <header class="entete-publication">
    @if (publication().auteur.photoUrl) {
      <img class="avatar" [src]="publication().auteur.photoUrl" alt="" />
    } @else {
      <span class="avatar avatar-lettre" aria-hidden="true">{{ initiale() }}</span>
    }

    <div class="auteur">
      <strong>{{ publication().auteur.nomAffichage }}</strong>
      <span class="muet date">
        <time [attr.datetime]="publication().datePublication">
          {{ publication().datePublication | date: 'dd/MM/yyyy, HH:mm' }}
        </time>
        @if (publication().dateModification) {
          · modifiée
        }
      </span>
    </div>

    <span class="pastille-categorie">
      <span aria-hidden="true">{{ categorie().emoji }}</span>
      {{ categorie().libelle }}
    </span>
  </header>

  @if (enModification()) {
    <app-publication-form
      [publication]="publication()"
      (termine)="finModification()"
      (annuler)="finModification()" />
  } @else {
    <p class="contenu">{{ publication().contenu }}</p>

    @if (publication().imageUrl; as url) {
      <!-- loading="lazy" : une image hors de l'écran n'est téléchargée qu'à
           l'approche. Sur un fil de photos, c'est la différence entre charger
           dix images et en charger deux. -->
      <img class="image" [src]="url" alt="" loading="lazy" />
    }

    <footer class="pied">
      <div class="reactions">
        @for (reaction of publication().reactions; track reaction.emoji) {
          <button
            type="button"
            class="compteur-reaction"
            [class.par-moi]="reaction.parMoi"
            [attr.aria-pressed]="reaction.parMoi"
            [title]="reaction.parMoi ? 'Retirer ma réaction' : 'Réagir ainsi'"
            (click)="reagir(reaction.emoji)">
            <span class="emoji">{{ reaction.emoji }}</span>
            <span class="nombre">{{ reaction.nombre }}</span>
          </button>
        }

        <button
          type="button"
          class="ouvrir-palette"
          [attr.aria-expanded]="paletteOuverte()"
          aria-label="Réagir à cette publication"
          (click)="basculerPalette()">
          <i class="pi pi-face-smile" aria-hidden="true"></i>
        </button>

        @if (paletteOuverte()) {
          <div class="palette" role="group" aria-label="Choisir une réaction">
            @for (emoji of emojis; track emoji) {
              <button type="button" class="choix-emoji" (click)="reagir(emoji)">{{ emoji }}</button>
            }
          </div>
        }
      </div>

      @if (publication().modifiable || publication().supprimable) {
        <div class="actions">
          @if (confirmationOuverte()) {
            <span class="question" role="alert">Supprimer cette publication ?</span>
            <p-button label="Supprimer" severity="danger" size="small"
              [disabled]="chargement()" (onClick)="confirmerSuppression()" />
            <p-button label="Annuler" severity="secondary" size="small" [text]="true"
              (onClick)="annulerSuppression()" />
          } @else {
            @if (publication().modifiable) {
              <p-button icon="pi pi-pencil" label="Modifier" severity="secondary" size="small"
                [text]="true" (onClick)="modifier()" />
            }
            @if (publication().supprimable) {
              <p-button icon="pi pi-trash" label="Supprimer" severity="danger" size="small"
                [text]="true" (onClick)="demanderSuppression()" />
            }
          }
        </div>
      }
    </footer>
  }
</article>
```

- [ ] **Step 3 : `publication-carte.css`**

```css
:host {
  display: block;
}

/* Le liseré gauche porte la couleur de la categorie : le fil se lit d'un coup
   d'oeil, sans lire les etiquettes. */
.publication {
  display: grid;
  gap: 0.75rem;
  border-left: 4px solid var(--couleur-categorie);
}

.entete-publication {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.65rem;
}

.avatar {
  flex-shrink: 0;
  width: 2.4rem;
  height: 2.4rem;
  border-radius: 50%;
  object-fit: cover;
}

.avatar-lettre {
  display: grid;
  place-items: center;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, var(--bleu), var(--rouge));
}

.auteur {
  display: grid;
  min-width: 0;
  line-height: 1.3;
}

.date {
  font-size: 0.8rem;
}

/* color-mix : la teinte de la categorie melangee au fond de carte. Le melange
   se fait avec var(--carte), donc il s'adapte seul au mode sombre — une couleur
   pale ecrite en dur resterait claire sur fond noir. */
.pastille-categorie {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  margin-left: auto;
  padding: 0.15rem 0.6rem;
  font-size: 0.78rem;
  font-weight: 600;
  color: var(--texte);
  background: color-mix(in srgb, var(--couleur-categorie) 16%, var(--carte));
  border: 1px solid color-mix(in srgb, var(--couleur-categorie) 45%, var(--carte));
  border-radius: 999px;
}

.contenu {
  margin: 0;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.image {
  display: block;
  width: 100%;
  max-height: 24rem;
  border-radius: var(--rayon-petit);
  object-fit: cover;
}

.pied {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.5rem;
}

.reactions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.3rem;
}

.compteur-reaction {
  display: inline-flex;
  align-items: center;
  gap: 0.2rem;
  padding: 0.05rem 0.5rem;
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--texte-doux);
  background: var(--carte);
  border: 1px solid var(--bordure);
  border-radius: 999px;
  box-shadow: none;
}

.compteur-reaction:hover:not(:disabled) {
  background: var(--bulle-recue);
  box-shadow: none;
}

.compteur-reaction.par-moi {
  color: var(--bleu-fonce);
  background: var(--bleu-pale);
  border-color: var(--bleu);
}

.compteur-reaction .nombre {
  font-variant-numeric: tabular-nums;
}

.ouvrir-palette {
  display: grid;
  place-items: center;
  width: 1.9rem;
  height: 1.9rem;
  padding: 0;
  color: var(--texte-doux);
  background: transparent;
  border-radius: 50%;
  box-shadow: none;
}

.ouvrir-palette:hover:not(:disabled) {
  background: var(--bulle-recue);
  box-shadow: none;
}

.palette {
  display: flex;
  gap: 0.1rem;
  padding: 0.2rem;
  background: var(--carte);
  border: 1px solid var(--bordure);
  border-radius: 999px;
  box-shadow: var(--ombre);
}

.choix-emoji {
  padding: 0.15rem 0.3rem;
  font-size: 1.05rem;
  line-height: 1.4;
  background: transparent;
  border-radius: 999px;
  box-shadow: none;
}

.choix-emoji:hover:not(:disabled) {
  background: var(--bulle-recue);
  box-shadow: none;
  transform: scale(1.2);
}

.actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.35rem;
  margin-left: auto;
}

.question {
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--rouge-fonce);
}
```

- [ ] **Step 4 : `pages/fil/fil.ts`**

```typescript
import { Component, OnInit, computed, inject } from '@angular/core';
import { ButtonModule } from 'primeng/button';
import { PublicationService } from '../../services/publication';
import { EtatHttpService } from '../../services/etat-http';
import { CATEGORIES, Categorie, categorieDe } from '../../publication.model';
import { PublicationForm } from '../../components/publication-form/publication-form';
import { PublicationCarte } from '../../components/publication-carte/publication-carte';

@Component({
  selector: 'app-fil',
  imports: [ButtonModule, PublicationForm, PublicationCarte],
  templateUrl: './fil.html',
  styleUrl: './fil.css'
})
export class Fil implements OnInit {
  private publicationService = inject(PublicationService);
  private etatHttp = inject(EtatHttpService);

  readonly categories = CATEGORIES;

  publications = this.publicationService.publications;
  categorie = this.publicationService.categorie;
  aDesPlusAnciennes = this.publicationService.aDesPlusAnciennes;
  chargement = this.etatHttp.chargement;

  /** Le libellé du filtre actif, pour un message « vide » qui dit de quoi. */
  libelleFiltre = computed(() => {
    const cle = this.categorie();
    return cle ? categorieDe(cle).libelle : null;
  });

  /**
   * On recharge à chaque arrivée sur la page, en gardant le filtre mémorisé
   * par le service : revenir sur le fil doit montrer ce qui a été publié
   * entre-temps, pas la liste figée de la dernière visite.
   */
  ngOnInit(): void {
    this.publicationService.charger(this.categorie());
  }

  filtrer(categorie: Categorie | null): void {
    // Recliquer sur le filtre actif ne relance rien.
    if (categorie !== this.categorie()) {
      this.publicationService.charger(categorie);
    }
  }

  voirPlus(): void {
    this.publicationService.chargerPlus();
  }
}
```

- [ ] **Step 5 : `pages/fil/fil.html`**

```html
<div class="entete-fil">
  <h2>Fil d'actualité</h2>
  <p class="muet">Les hobbies de tout le monde, du plus récent au plus ancien.</p>
</div>

<section class="carte bloc-publier" aria-labelledby="titre-publier">
  <h3 id="titre-publier">Partager un hobby</h3>
  <app-publication-form />
</section>

<!-- Des boutons à bascule (aria-pressed), et non des liens : filtrer ne change
     pas de page, cela change ce que la page montre. -->
<div class="filtres" role="group" aria-label="Filtrer par catégorie">
  <button type="button" class="filtre" [attr.aria-pressed]="categorie() === null" (click)="filtrer(null)">
    Tout
  </button>
  @for (c of categories; track c.cle) {
    <button
      type="button"
      class="filtre"
      [style.--couleur-categorie]="c.couleur"
      [attr.aria-pressed]="categorie() === c.cle"
      (click)="filtrer(c.cle)">
      <span aria-hidden="true">{{ c.emoji }}</span> {{ c.libelle }}
    </button>
  }
</div>

@if (publications().length > 0) {
  <ul class="liste-publications">
    @for (publication of publications(); track publication.id) {
      <li><app-publication-carte [publication]="publication" /></li>
    }
  </ul>

  @if (aDesPlusAnciennes()) {
    <div class="suite">
      <p-button
        label="Voir plus"
        icon="pi pi-angle-down"
        severity="secondary"
        [outlined]="true"
        [disabled]="chargement()"
        (onClick)="voirPlus()" />
    </div>
  }
} @else if (!chargement()) {
  <!-- Le « vide » n'est affiché qu'une fois la réponse arrivée : pendant le
       chargement, « aucune publication » serait faux. -->
  <div class="carte vide">
    @if (libelleFiltre(); as libelle) {
      <p class="muet">Aucune publication dans « {{ libelle }} » pour l'instant.</p>
    } @else {
      <p class="muet">Le fil est vide. Soyez le premier à partager un hobby !</p>
    }
  </div>
}
```

- [ ] **Step 6 : `pages/fil/fil.css`**

```css
.entete-fil p {
  margin: -0.4rem 0 1rem;
}

.bloc-publier {
  margin-bottom: 1.25rem;
}

.bloc-publier h3 {
  margin-top: 0;
}

.filtres {
  display: flex;
  flex-wrap: wrap;
  gap: 0.4rem;
  margin-bottom: 1rem;
}

.filtre {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  padding: 0.3rem 0.75rem;
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--texte);
  background: var(--carte);
  border: 1.5px solid var(--bordure);
  border-radius: 999px;
  box-shadow: none;
}

.filtre:hover:not(:disabled) {
  background: var(--bleu-pale);
  box-shadow: none;
}

/* Filtre actif : plein, dans la couleur de sa categorie. « Tout » n'a pas de
   variable : var() retombe alors sur le bleu. */
.filtre[aria-pressed="true"],
.filtre[aria-pressed="true"]:hover:not(:disabled) {
  color: #fff;
  background: var(--couleur-categorie, var(--bleu));
  border-color: var(--couleur-categorie, var(--bleu));
}

.liste-publications {
  display: grid;
  gap: 1rem;
  margin: 0;
  padding: 0;
  list-style: none;
}

.suite {
  display: flex;
  justify-content: center;
  margin-top: 1.25rem;
}

.vide {
  text-align: center;
}
```

- [ ] **Step 7 : route et navigation** — `app.routes.ts` : `import { Fil } from './pages/fil/fil';` et, après la route `''` :

```typescript
  { path: 'fil', component: Fil, canActivate: [authGuard] },
```

`app.html`, dans `<nav>`, après le lien « Contacts » :

```html
        <a routerLink="/fil" routerLinkActive="actif">Fil</a>
```

- [ ] **Step 8 : vérifier** — `npx ng test --watch=false` (88 tests au vert) puis `npx ng build`. Attendu : build sans erreur ; relever la taille du paquet initial (alerte possible au-delà de 1 MB : si elle apparaît, la signaler dans le compte rendu sans relever le budget).

- [ ] **Step 9 : point de contrôle** — `git status`.

---

### Task 8 : vérification de bout en bout contre le vrai serveur

**Files:**
- Create (hors dépôt) : `<scratchpad>/verif-fil.sh`

**Interfaces:**
- Consumes : l'API complète des Tasks 1 à 3.

- [ ] **Step 1 : port libre** — `netstat -ano | findstr :8124`. Attendu : aucune ligne (sinon choisir un autre port : ne jamais mesurer une instance oubliée, incident des Parties 11 et 12).

- [ ] **Step 2 : démarrer le backend** (en arrière-plan) — dans `carnet-contact-backend` : `.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--server.port=8124"`, attendre « Started CarnetContactBackendApplication ».

- [ ] **Step 3 : script** — `verif-fil.sh` (Git Bash ; corps contenant un emoji écrit dans un fichier et envoyé par `--data-binary`, les emojis en ligne de commande étant altérés sous Windows) :

```bash
#!/usr/bin/env bash
set -u
API=http://localhost:8124/api
J='Content-Type: application/json'
DIR="$(dirname "$0")"

jeton() { sed -E 's/.*"jeton":"([^"]+)".*/\1/'; }
code() { curl -s -o /dev/null -w '%{http_code}' "$@"; }

A=$(curl -s -H "$J" -d '{"email":"alice@fil.fr","motDePasse":"Hobbies-2026!","nomAffichage":"Alice"}' $API/auth/inscription | jeton)
B=$(curl -s -H "$J" -d '{"email":"bob@fil.fr","motDePasse":"Hobbies-2026!","nomAffichage":"Bob"}' $API/auth/inscription | jeton)

echo "--- publier (Alice, admin car premiere inscrite ; Bob)"
curl -s -H "$J" -H "Authorization: Bearer $A" -d '{"categorie":"SPORT","contenu":"Velo dimanche"}' $API/publications; echo
P=$(curl -s -H "$J" -H "Authorization: Bearer $B" -d '{"categorie":"CUISINE","contenu":"Tarte aux pommes"}' $API/publications | sed -E 's/^\{"id":([0-9]+).*/\1/')
curl -s -H "$J" -H "Authorization: Bearer $B" -d '{"categorie":"VOYAGE","contenu":"Lisbonne"}' $API/publications; echo

echo "--- fil taille=2 (attendu : 2 publications + curseurSuivant)"
curl -s -H "Authorization: Bearer $A" "$API/publications?taille=2"; echo
echo "--- filtre CUISINE (attendu : 1)"
curl -s -H "Authorization: Bearer $A" "$API/publications?categorie=CUISINE"; echo

echo "--- validations (attendu : 400 400 400)"
code -H "$J" -H "Authorization: Bearer $A" -d '{"contenu":"sans categorie"}' $API/publications; echo
code -H "$J" -H "Authorization: Bearer $A" -d '{"categorie":"SPORT","contenu":"x","imageUrl":"javascript:alert(1)"}' $API/publications; echo
code -H "Authorization: Bearer $A" "$API/publications?categorie=PEINTURE"; echo

echo "--- droits (attendu : 403 modif par Alice, 204 suppression par Alice admin)"
code -X PUT -H "$J" -H "Authorization: Bearer $A" -d '{"categorie":"SPORT","contenu":"piratage"}' $API/publications/$P; echo

printf '{"emoji":"👍"}' > "$DIR/emoji.json"
echo "--- reaction (attendu : nombre 1, parMoi true)"
curl -s -X PUT -H "$J" -H "Authorization: Bearer $A" --data-binary @"$DIR/emoji.json" $API/publications/$P/reaction; echo

code -X DELETE -H "Authorization: Bearer $A" $API/publications/$P; echo

echo "--- confidentialite (attendu : aucun champ email)"
curl -s -H "Authorization: Bearer $A" $API/utilisateurs; echo
echo "--- message trop long (attendu : 400)"
LONG=$(printf 'a%.0s' $(seq 1 2001))
code -H "$J" -H "Authorization: Bearer $A" -d "{\"destinataireId\":2,\"contenu\":\"$LONG\"}" $API/messages; echo
```

- [ ] **Step 4 : exécuter** — `bash <scratchpad>/verif-fil.sh`. Comparer chaque sortie à l'« attendu » affiché.

- [ ] **Step 5 : arrêter le serveur** — arrêter la tâche en arrière-plan, puis `netstat -ano | findstr :8124` : plus rien n'écoute.

---

### Task 9 : support d'apprentissage

**Files:**
- Modify: `docs/support-apprentissage-angular-spring.md`, `docs/cours-angular.md` (un lien)

Règles de `CLAUDE.md` : prose explicative (le « pourquoi ») avant ou autour d'un bloc de syntaxe **générique** commenté, puis un encadré **« Dans le projet »** avec le chemin en lien relatif depuis `docs/` et l'extrait réel ; tableaux à deux colonnes pour les listes d'annotations ou de méthodes ; ne rien réécrire d'existant ; entrées au pense-bête sous la forme de tableau existante.

- [ ] **Step 1 : renuméroter** — sommaire et titres : `33. Backend Spring Boot` → 36, `34. Git et GitHub` → 37, `35. Pense-bête de dépannage` → 38, ancres comprises. Puis `rg -n "section 3[3-5]|#3[3-5]-" docs` et corriger chaque renvoi qui visait l'ancienne numérotation (lignes du pense-bête visant « section 33 », phrase de la section 26, lien de `cours-angular.md` vers `#33-backend-spring-boot`).

- [ ] **Step 2 : section 33 « Validation côté serveur (Bean Validation) »**, insérée avant la section 36. Contenu :
  - Le problème : un `if` par champ dans chaque contrôleur, facile à oublier ; un message de 2001 caractères atteignait la base et ressortait en 500.
  - Déclarer plutôt que vérifier : les annotations sur les composants d'un `record`, `@Valid` sur le paramètre, 400 automatique avant d'entrer dans la méthode.
  - Tableau des annotations : `@NotNull`, `@NotBlank`, `@Size`, `@Pattern`, `@Valid`.
  - Nuances : `@NotNull` vs `@NotBlank` ; une contrainte (sauf `@NotNull`) ignore une valeur `null` ; le motif `^$|…` pour un champ facultatif ; une valeur d'enum inconnue est rejetée par Jackson (400) avant même la validation.
  - Le miroir côté Angular (`Validators.pattern(/\S/)` pour `@NotBlank`), rappel de la règle de la section 27 : confort d'un côté, sécurité de l'autre.
  - « Dans le projet » : `DemandePublication` de `PublicationController.java`, `DemandeMessage` de `MessageController.java`, et les validateurs de `publication-form.ts`.

- [ ] **Step 3 : section 34 « Pagination par curseur »**. Contenu :
  - Le problème : un fil grandit par le haut ; en pagination par numéro de page, une publication ajoutée décale tout, la page 2 réaffiche une publication de la page 1. Schéma ASCII avant / après.
  - Le curseur : « plus anciennes que l'id X », tri par id décroissant (unique et croissant, contrairement à une date).
  - Lire `taille + 1` lignes pour savoir s'il en reste, sans `COUNT` ; `List` au lieu de `Page` pour éviter la requête de comptage.
  - Tableau comparatif page / curseur (sauter à la page 7, total affiché, stabilité sous insertion, coût SQL `OFFSET`).
  - Côté Angular : la demande porte ses paramètres, un seul `Subject` + `switchMap` pour « nouveau filtre » et « Voir plus », conséquence : ajout et suppression locaux sans rechargement (à comparer avec `ContactService`).
  - « Dans le projet » : `PublicationRepository.fil`, `PublicationController.fil`, `services/publication.ts` (constructeur et `chargerPlus`).

- [ ] **Step 4 : section 35 « Fil d'actualité : catégories, droits et DTO public »**. Contenu :
  - Enum Java + tableau `as const` TypeScript, type déduit (`typeof CATEGORIES[number]['cle']`), rappel de `RESEAUX`.
  - DTO `AuteurPublic` : liste blanche contre liste noire (`@JsonIgnore`), la fuite corrigée dans les messages et la liste des comptes.
  - Droits : `modifiable` / `supprimable` calculés par le serveur ; l'administrateur supprime mais ne modifie pas ; rôle relu en base pour une action destructrice ; 403 pour une ressource publique vs 404 pour une ressource privée (tableau).
  - `JOIN FETCH` + `FetchType.LAZY` sur l'auteur (lien avec le N+1 de la section 32).
  - Code partagé Java : interface `ReactionEmoji`, `List<? extends …>`, `groupingBy`.
  - Angular : formulaire unique création / modification (`input()` optionnel lu dans `ngOnInit`), `publier()` qui renvoie l'Observable pour ne vider la saisie qu'au succès, `error: () => {}` justifié, `id` uniques par compteur de module, `[ngValue]` sur `<option>`.
  - CSS : variable `--couleur-categorie` et `color-mix()` qui suit le mode sombre ; confirmation dans la carte plutôt que `p-confirmDialog` (budget du paquet initial).
  - « Dans le projet » : un extrait par point, depuis les fichiers des Tasks 2 à 7.

- [ ] **Step 5 : pense-bête** — une ligne par problème réellement rencontré pendant les Tasks 1 à 8 (au minimum : entité modifiée par une requête `@Modifying` encore présente dans le contexte de persistance d'un test → `flush()` + `clear()` ; contrainte de clé étrangère jamais vérifiée dans un test `@Transactional` sans `flush()` ; toute autre erreur effectivement résolue).

---

### Task 10 : progression et README

**Files:**
- Modify: `docs/progression-pedagogique.md`, `README.md`

- [ ] **Step 1 : Partie 14** dans `progression-pedagogique.md`, avant « Ce qui était prévu ensuite », même forme que les Parties 12 et 13 : la demande (revue, fil, cours), les quatre réponses de l'utilisateur, les décisions et leur justification (curseur, table dédiée, Bean Validation, DTO public, 403/404, confirmation en carte), numérotation continue des points (à partir de 110), vérifications réellement faites avec les chiffres mesurés, ce qui n'a pas été vérifié, sections du support ajoutées et renumérotées. Mettre à jour « Pistes suivantes » avec les défauts de revue restants (accusés de lecture en arrière-plan, saisie perdue des contacts, validation des contacts, emails non normalisés, N+1 de l'administration, `utilisateurConnecte()` dupliquée).

- [ ] **Step 2 : `README.md`** réécrit en Markdown valide : titre, présentation, tableau de la pile technique (Angular 21, PrimeNG 21, Spring Boot 4.1, Spring Security + JWT, JPA/H2, vitest / JUnit), fonctionnalités à jour (carnet paginé, messagerie avec réactions et accusés de lecture, fil d'actualité et ses 11 catégories, administration, mode sombre, notifications), architecture en quelques puces, démarrage en blocs de code, tests (`.\mvnw.cmd test`, `npm test`), et liens vers `docs/`.

- [ ] **Step 3 : point de contrôle final** — `git status` et `git diff --stat` : relire la liste des fichiers touchés avant le compte rendu.

---

## Après ce plan

La refonte de `docs/cours-angular.md` (schémas Mermaid, étapes 11 à 27) fait l'objet d'un plan séparé, écrit une fois le fil livré, pour que l'étape 27 cite le code réel.

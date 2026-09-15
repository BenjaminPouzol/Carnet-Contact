# Abonnements, comptes privés, blocages et notifications — plan d'implémentation

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Suivre des comptes (liste distincte des contacts), avec comptes privés, blocage, suggestions et notifications ; suivre donne le droit d'écrire et l'accès à l'email pro et aux réseaux.

**Architecture:** Deux tables de liaison (`Abonnement` à statut, `Blocage`) et une table `Notification`. Toutes les règles de visibilité vivent dans `VueRelations`, un objet chargé une fois par requête (cinq requêtes, quelle que soit la taille des listes) et testable sans Spring. Le fil applique la même règle en JPQL, pour rester paginable. Côté Angular : un `AbonnementService` qui garde le statut de chaque compte suivi dans un signal, un `ActiviteService` qui sonde les notifications, un `bouton-suivre` réutilisé partout.

**Tech Stack:** Spring Boot 4.1.1 (JPA, H2, Spring Security, Bean Validation), Angular 21 (zoneless, signals, RxJS, PrimeNG 21), Vitest.

**Spec:** `docs/superpowers/specs/2026-09-15-abonnements-design.md`

## Global Constraints

- L'email de connexion n'apparaît dans **aucun** DTO montré à un autre compte ; le téléphone n'existe pas sur le compte.
- Blocage : aucune visibilité ni interaction dans les deux sens ; 404 sur le profil.
- Envoi de message refusé : 403 « Suivez cette personne pour lui écrire. »
- Suivre un compte public : `ACCEPTE` immédiat ; privé : `EN_ATTENTE`.
- Recherche : 20 résultats max ; suggestions : 5 max ; notifications récentes : 30 max ; sondage des notifications : 15 s.
- Pas de nouvelle requête par élément de liste (le N+1 relevé par la revue ne doit pas réapparaître).
- Chaque nouvelle propriété de configuration va aussi dans `src/test/resources/application.properties`.
- Commentaires pédagogiques en français, sur le « pourquoi ». Commits terminés par `Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>`.
- Suite complète au vert à la fin de chaque tâche backend (`.\mvnw.cmd test`) et frontend (`npx ng test --watch=false`).

## Carte des fichiers

Backend (`carnet-contact-backend/src/main/java/com/example/carnet_contact_backend/`)

| Fichier | Responsabilité |
|---|---|
| `model/StatutAbonnement.java`, `model/TypeNotification.java` | Enums stockés par nom |
| `model/Abonnement.java`, `model/Blocage.java`, `model/Notification.java` | Tables de liaison et notifications |
| `model/Utilisateur.java` | + `emailPro`, 6 réseaux, `comptePrive` |
| `repository/AbonnementRepository.java`, `BlocageRepository.java`, `NotificationRepository.java` | Requêtes |
| `abonnement/StatutRelation.java` | `AUCUN`, `EN_ATTENTE`, `ACCEPTE` (vu du client) |
| `abonnement/VueRelations.java` | Les règles, sur des ensembles déjà chargés — aucune dépendance Spring |
| `abonnement/Relations.java` | Service : charge une `VueRelations` pour un compte |
| `abonnement/Notifications.java` | Service : créer / retirer des notifications |
| `controller/VuesComptes.java` | Composant : fabrique `CompteResume` et `ProfilPublic` |
| `controller/CompteResume.java`, `CoordonneesPro.java`, `ProfilPublic.java` | DTO liste blanche |
| `controller/AbonnementController.java`, `BlocageController.java`, `NotificationController.java` | Nouvelles routes |
| `controller/UtilisateurController.java`, `MessageController.java`, `PublicationController.java`, `AdminController.java` | Modifiés |

Frontend (`carnet-contact_frontend/src/app/`)

| Fichier | Responsabilité |
|---|---|
| `abonnement.model.ts` | Types du domaine + `texteNotification()` |
| `services/abonnement.ts`, `services/blocage.ts`, `services/activite.ts` | Appels et état partagé |
| `components/bouton-suivre/`, `components/carte-compte/` | Briques réutilisables |
| `pages/abonnements/`, `pages/personne/` | Nouvelles pages |
| `services/publication.ts`, `pages/fil/`, `components/publication-carte/` | Filtre « Abonnements », lien vers l'auteur |
| `pages/profil/`, `services/auth.ts` | Email pro, réseaux, compte privé |
| `pages/messages/`, `services/message.ts` | Interlocuteurs, `peutEcrire`, saisie conservée en cas d'échec |
| `services/notification.ts`, `app.ts`, `app.html`, `app.routes.ts` | Lien des bandeaux, navigation, routes |

---

## Backend

### Task B1: Les règles (`VueRelations`), testées sans Spring

**Files:**
- Create: `model/StatutAbonnement.java`, `abonnement/StatutRelation.java`, `abonnement/VueRelations.java`
- Test: `src/test/java/.../abonnement/VueRelationsTest.java`

**Interfaces — Produces:**

```java
public enum StatutAbonnement { EN_ATTENTE, ACCEPTE }
public enum StatutRelation { AUCUN, EN_ATTENTE, ACCEPTE }

public final class VueRelations {
    public VueRelations(Long moiId, boolean admin,
                        Map<Long, StatutAbonnement> sortants,  // suiviId -> statut
                        Set<Long> abonnesAcceptes,             // qui me suit
                        Set<Long> mOntEcrit,                   // qui m'a déjà écrit
                        Set<Long> bloques)                     // blocage dans un sens ou l'autre
    public Long moiId()
    public boolean estMoi(Long autreId)
    public boolean bloque(Long autreId)
    public StatutRelation statut(Long autreId)
    public boolean suit(Long autreId)                          // statut ACCEPTE
    public boolean ilMeSuit(Long autreId)
    public boolean voitContenu(Long auteurId, boolean auteurPrive)
    public boolean voitCoordonnees(Long autreId)
    public boolean peutEcrire(Long autreId)
    public Set<Long> suivisAcceptes()
    public Set<Long> sortantsTous()
}
```

Règles (copiées de la spec) :

| Méthode | Règle |
|---|---|
| `voitContenu` | `!bloque` ET (`estMoi` OU `!auteurPrive` OU `suit` OU `admin`) |
| `voitCoordonnees` | `!bloque` ET (`estMoi` OU `suit`) |
| `peutEcrire` | `!estMoi` ET `!bloque` ET (`suit` OU `mOntEcrit.contains`) |

- [ ] **Step 1: Write the failing test** — `VueRelationsTest`, une méthode par ligne de règle, plus `statut` (AUCUN par défaut, EN_ATTENTE, ACCEPTE) et `bloque` prioritaire sur tout (un compte suivi puis bloqué ne voit plus rien, ne peut plus écrire). Fabrique locale : `vue(Map sortants, Set abonnes, Set ecrit, Set bloques, boolean admin)` avec `moiId = 1L`.
- [ ] **Step 2:** `.\mvnw.cmd test -Dtest=VueRelationsTest` → échec de compilation.
- [ ] **Step 3:** écrire les deux enums et `VueRelations` (ensembles copiés en immuables par `Set.copyOf` / `Map.copyOf`).
- [ ] **Step 4:** le test passe.
- [ ] **Step 5:** commit « Abonnements : les règles de visibilité, testées sans Spring ».

### Task B2: Modèle, dépôts et services de chargement

**Files:**
- Create: `model/Abonnement.java`, `model/Blocage.java`, `model/TypeNotification.java`, `model/Notification.java`
- Create: `repository/AbonnementRepository.java`, `repository/BlocageRepository.java`, `repository/NotificationRepository.java`
- Create: `abonnement/Relations.java`, `abonnement/Notifications.java`
- Modify: `model/Utilisateur.java` (+ `emailPro`, `instagram`, `twitter`, `facebook`, `twitch`, `youtube`, `linkedin`, `@Column(nullable = false) boolean comptePrive = false`)
- Modify: `repository/MessageRepository.java`, `repository/UtilisateurRepository.java`
- Test: `src/test/java/.../abonnement/RelationsTest.java`

**Interfaces — Produces:**

```java
// Abonnement : abonne (abonne_id), suivi (suivi_id), statut, dateDemande, dateAcceptation
// @Table(uniqueConstraints = @UniqueConstraint(columnNames = {"abonne_id", "suivi_id"}))
// Blocage : bloqueur (bloqueur_id), bloque (bloque_id), dateBlocage ; unicité du couple
// Notification : destinataire, acteur, type (STRING, 20), date, lue
public enum TypeNotification { NOUVEL_ABONNE, DEMANDE_RECUE, DEMANDE_ACCEPTEE }

interface AbonnementRepository extends JpaRepository<Abonnement, Long> {
    Optional<Abonnement> findByAbonneIdAndSuiviId(Long abonneId, Long suiviId);
    long countBySuiviIdAndStatut(Long suiviId, StatutAbonnement statut);
    long countByAbonneIdAndStatut(Long abonneId, StatutAbonnement statut);
    @Query("SELECT a.suivi.id, a.statut FROM Abonnement a WHERE a.abonne.id = :id")
    List<Object[]> statutsSortants(@Param("id") Long id);
    @Query("SELECT a.abonne.id FROM Abonnement a WHERE a.suivi.id = :id AND a.statut = :statut")
    List<Long> idsAbonnes(@Param("id") Long id, @Param("statut") StatutAbonnement statut);
    @Query("SELECT a FROM Abonnement a JOIN FETCH a.suivi WHERE a.abonne.id = :id")
    List<Abonnement> sortantsDe(@Param("id") Long id);
    @Query("SELECT a FROM Abonnement a JOIN FETCH a.abonne WHERE a.suivi.id = :id AND a.statut = :statut")
    List<Abonnement> entrantsDe(@Param("id") Long id, @Param("statut") StatutAbonnement statut);
    // Suggestions : comptes suivis par ceux que je suis, avec le nombre de « ponts »
    @Query("""
        SELECT a2.suivi.id, COUNT(a2) FROM Abonnement a1, Abonnement a2
        WHERE a1.abonne.id = :moi AND a1.statut = :accepte
          AND a2.abonne.id = a1.suivi.id AND a2.statut = :accepte
        GROUP BY a2.suivi.id ORDER BY COUNT(a2) DESC""")
    List<Object[]> suivisParMesAbonnements(@Param("moi") Long moi, @Param("accepte") StatutAbonnement accepte);
    @Query("SELECT a.suivi.id, COUNT(a) FROM Abonnement a WHERE a.statut = :accepte GROUP BY a.suivi.id ORDER BY COUNT(a) DESC")
    List<Object[]> lesPlusSuivis(@Param("accepte") StatutAbonnement accepte);
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM Abonnement a WHERE (a.abonne.id = :a AND a.suivi.id = :b) OR (a.abonne.id = :b AND a.suivi.id = :a)")
    void supprimerEntre(@Param("a") Long a, @Param("b") Long b);
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM Abonnement a WHERE a.abonne.id = :id OR a.suivi.id = :id")
    void supprimerCeuxDe(@Param("id") Long id);
}

interface BlocageRepository extends JpaRepository<Blocage, Long> {
    boolean existsByBloqueurIdAndBloqueId(Long bloqueurId, Long bloqueId);
    @Query("SELECT b.bloque.id FROM Blocage b WHERE b.bloqueur.id = :id") List<Long> idsBloquesPar(@Param("id") Long id);
    @Query("SELECT b.bloqueur.id FROM Blocage b WHERE b.bloque.id = :id") List<Long> idsQuiOntBloque(@Param("id") Long id);
    @Query("SELECT b FROM Blocage b JOIN FETCH b.bloque WHERE b.bloqueur.id = :id") List<Blocage> parBloqueur(@Param("id") Long id);
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM Blocage b WHERE b.bloqueur.id = :bloqueur AND b.bloque.id = :bloque")
    void supprimerPar(@Param("bloqueur") Long bloqueur, @Param("bloque") Long bloque);
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM Blocage b WHERE b.bloqueur.id = :id OR b.bloque.id = :id")
    void supprimerCeuxDe(@Param("id") Long id);
}

interface NotificationRepository extends JpaRepository<Notification, Long> {
    @Query("SELECT n FROM Notification n JOIN FETCH n.acteur WHERE n.destinataire.id = :id ORDER BY n.date DESC, n.id DESC")
    List<Notification> recentesPour(@Param("id") Long id, Pageable limite);
    @Query("SELECT n FROM Notification n JOIN FETCH n.acteur WHERE n.destinataire.id = :id AND n.lue = false ORDER BY n.date DESC, n.id DESC")
    List<Notification> nonLuesPour(@Param("id") Long id);
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Notification n SET n.lue = true WHERE n.destinataire.id = :id AND n.lue = false")
    int marquerLues(@Param("id") Long id);
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM Notification n WHERE n.destinataire.id = :destinataire AND n.acteur.id = :acteur AND n.type IN :types")
    void supprimer(@Param("destinataire") Long destinataire, @Param("acteur") Long acteur, @Param("types") Collection<TypeNotification> types);
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM Notification n WHERE (n.destinataire.id = :a AND n.acteur.id = :b) OR (n.destinataire.id = :b AND n.acteur.id = :a)")
    void supprimerEntre(@Param("a") Long a, @Param("b") Long b);
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM Notification n WHERE n.destinataire.id = :id OR n.acteur.id = :id")
    void supprimerCellesDe(@Param("id") Long id);
}

// MessageRepository : + 
@Query("SELECT DISTINCT m.expediteur.id FROM Message m WHERE m.destinataire.id = :id") List<Long> idsQuiMOntEcrit(@Param("id") Long id);
@Query("SELECT DISTINCT m.destinataire.id FROM Message m WHERE m.expediteur.id = :id") List<Long> idsAQuiJAiEcrit(@Param("id") Long id);

// UtilisateurRepository : +
@Query("""
    SELECT u FROM Utilisateur u
    WHERE u.id <> :moi AND u.actif = true
      AND LOWER(u.nomAffichage) LIKE LOWER(CONCAT('%', :terme, '%'))
      AND NOT EXISTS (SELECT b.id FROM Blocage b
                      WHERE (b.bloqueur.id = :moi AND b.bloque.id = u.id)
                         OR (b.bloqueur.id = u.id AND b.bloque.id = :moi))
    ORDER BY u.nomAffichage""")
List<Utilisateur> rechercher(@Param("moi") Long moi, @Param("terme") String terme, Pageable limite);
List<Utilisateur> findTop20ByActifTrueAndIdNotOrderByDateInscriptionDesc(Long id);

@Service public class Relations {
    public VueRelations vuePour(Utilisateur moi)   // 5 requêtes : statutsSortants, idsAbonnes(ACCEPTE), idsQuiMOntEcrit, idsBloquesPar, idsQuiOntBloque
}
@Service public class Notifications {
    public void notifier(Utilisateur destinataire, Utilisateur acteur, TypeNotification type)
    public void retirer(Long destinataireId, Long acteurId, TypeNotification... types)
}
```

- [ ] **Step 1: Write the failing test** — `RelationsTest` (`@SpringBootTest @Transactional`) : trois comptes enregistrés en base, un abonnement accepté, une demande en attente, un message reçu, un blocage ; `vuePour(moi)` rend les bons `statut`, `ilMeSuit`, `peutEcrire`, `bloque`.
- [ ] **Step 2:** échec de compilation.
- [ ] **Step 3:** entités, dépôts, `Relations`, `Notifications`, champs de `Utilisateur`.
- [ ] **Step 4:** `RelationsTest` passe, puis suite complète au vert (le schéma change : tous les tests existants doivent encore passer).
- [ ] **Step 5:** commit « Abonnements : modèle, dépôts et chargement des relations ».

### Task B3: DTO et routes d'abonnement

**Files:**
- Create: `controller/CompteResume.java`, `controller/CoordonneesPro.java`, `controller/ProfilPublic.java`, `controller/VuesComptes.java`, `controller/AbonnementController.java`
- Test: `src/test/java/.../controller/AbonnementControllerTest.java`

**Interfaces — Produces:**

```java
public record CompteResume(Long id, String nomAffichage, String photoUrl, boolean comptePrive,
                           StatutRelation statut, boolean ilMeSuit) {}
public record CoordonneesPro(String emailPro, String instagram, String twitter, String facebook,
                             String twitch, String youtube, String linkedin) {
    public static CoordonneesPro de(Utilisateur u)
}
public record ProfilPublic(Long id, String nomAffichage, String photoUrl, boolean comptePrive,
                           StatutRelation statut, boolean ilMeSuit, long nombreAbonnes, long nombreAbonnements,
                           boolean contenuVisible, boolean peutEcrire, CoordonneesPro coordonnees) {}
@Component public class VuesComptes {
    public CompteResume resume(VueRelations vue, Utilisateur autre)
    public List<CompteResume> resumes(VueRelations vue, List<Utilisateur> autres)
    public ProfilPublic profil(VueRelations vue, Utilisateur autre)   // 2 COUNT
}
```

Routes : `PUT /api/abonnements/{id}` (200 `CompteResume`), `DELETE /{id}` (204), `GET` (sortants, suivis actifs et non bloqués), `GET /abonnes`, `GET /demandes`, `PUT /demandes/{id}` (204 / 404), `DELETE /demandes/{id}` (204), `DELETE /abonnes/{id}` (204). Écritures `@Transactional`.

- [ ] **Step 1: Write the failing test** — `AbonnementControllerTest` :
  - suivre un compte public → `statut` `ACCEPTE`, notification `NOUVEL_ABONNE` créée pour lui ;
  - suivre un compte privé → `EN_ATTENTE`, notification `DEMANDE_RECUE` ;
  - suivre deux fois → une seule ligne, une seule notification ;
  - suivre soi-même → 400 ; compte inconnu → 404 ; compte qui m'a bloqué → 404 ;
  - ne plus suivre → 204, ligne et notification supprimées ;
  - accepter une demande → `ACCEPTE`, `DEMANDE_RECUE` retirée, `DEMANDE_ACCEPTEE` créée pour le demandeur ; accepter sans demande → 404 ;
  - refuser → ligne supprimée ; retirer un abonné → ligne supprimée ;
  - `GET`, `/abonnes`, `/demandes` rendent les bonnes listes, **sans champ `email`**.
- [ ] **Step 2:** échec (404 sur les routes).
- [ ] **Step 3:** DTO, `VuesComptes`, `AbonnementController`.
- [ ] **Step 4:** test au vert, suite complète au vert.
- [ ] **Step 5:** commit « Abonnements : suivre, demandes, retrait d'un abonné ».

### Task B4: Profil public, recherche, suggestions, profil enrichi

**Files:**
- Modify: `controller/UtilisateurController.java`
- Test: `src/test/java/.../controller/UtilisateurControllerTest.java`

**Interfaces — Produces:** `GET /api/utilisateurs?recherche=` → `List<CompteResume>` ; `GET /suggestions` → `List<Suggestion>` avec `record Suggestion(CompteResume compte, long enCommun)` ; `GET /{id}` → `ProfilPublic` ; `PUT /moi` avec `DemandeProfil(String nomAffichage, String photoUrl, @Email @Size(max=255) String emailPro, @Size(max=255) × 6 réseaux, Boolean comptePrive)` et `@Valid`. Champ `null` = inchangé, chaîne vide = effacé. Passage privé → public : toutes les demandes en attente deviennent `ACCEPTE`, `DEMANDE_RECUE` retirées, `DEMANDE_ACCEPTEE` envoyées.

Suggestions : `suivisParMesAbonnements` (tri par nombre de ponts), puis `lesPlusSuivis`, puis `findTop20ByActifTrueAndIdNotOrderByDateInscriptionDesc` — sans doublon, en excluant moi, les comptes déjà suivis ou demandés, les désactivés et les blocages ; 5 au plus. Les comptes sont chargés par `findAllById` (une requête).

- [ ] **Step 1: Write the failing test** — `UtilisateurControllerTest` :
  - recherche insensible à la casse ; exclut moi, un compte désactivé, un compte bloqué ; aucun `email` ;
  - profil d'un compte non suivi : `coordonnees` nul, `contenuVisible` vrai s'il est public, faux s'il est privé ; `$.email` absent ;
  - profil d'un compte suivi : `coordonnees.emailPro` et `coordonnees.instagram` présents, `$.email` et `$.coordonnees.email` absents, `peutEcrire` vrai ;
  - profil d'un compte qui m'a bloqué → 404 ; mon propre profil → coordonnées visibles, `peutEcrire` faux ;
  - `PUT /moi` avec `emailPro` invalide → 400 ; avec les réseaux → relus dans `/moi` ;
  - passer privé puis public accepte la demande en attente ;
  - suggestions : Alice suit Bob et Carol, qui suivent tous deux Dave → Dave premier avec `enCommun = 2` ; Bob et Carol n'apparaissent pas (déjà suivis).
- [ ] **Step 2:** échec. **Step 3:** implémentation. **Step 4:** vert + suite complète (le test existant `listeDesComptes_sansEmail` doit passer sans modification). **Step 5:** commit « Abonnements : profil public, recherche et suggestions ».

### Task B5: Blocages

**Files:** Create `controller/BlocageController.java` ; Test `controller/BlocageControllerTest.java`.

**Interfaces — Produces:** `PUT /api/blocages/{id}` (204 ; 400 soi-même ; 404 inconnu ; supprime abonnements et notifications entre les deux comptes), `DELETE /{id}` (204), `GET` → `List<AuteurPublic>`.

- [ ] **Step 1:** test — bloquer supprime les abonnements dans les deux sens et les notifications ; le bloqué ne peut plus suivre (404) ni voir le profil (404) ; débloquer ne restaure rien ; la liste contient le bloqué ; soi-même 400. **Steps 2-5** : échec, implémentation, vert, commit « Abonnements : blocages ».

### Task B6: Notifications

**Files:** Create `controller/NotificationController.java` ; Test `controller/NotificationControllerTest.java`.

**Interfaces — Produces:** `record NotificationVue(Long id, TypeNotification type, AuteurPublic acteur, Instant date, boolean lue)` ; `GET /api/notifications` (30 max), `GET /non-lues`, `PUT /lues` (204). Les acteurs désactivés ou en relation de blocage sont filtrés (via `VueRelations.bloque`).

- [ ] **Step 1:** test — suivre crée une non-lue pour le suivi ; `PUT /lues` la passe à lue et vide `/non-lues` ; une notification d'un acteur désactivé n'est pas renvoyée. **Steps 2-5** : échec, implémentation, vert, commit « Abonnements : notifications ».

### Task B7: Fil — filtres et visibilité

**Files:** Modify `repository/PublicationRepository.java` (`fil`), `controller/PublicationController.java` ; Test `controller/PublicationControllerTest.java`.

**Interfaces — Produces:** `GET /api/publications?categorie=&abonnements=&auteur=&avant=&taille=`.

```java
@Query("""
    SELECT p FROM Publication p JOIN FETCH p.auteur a
    WHERE (:categorie IS NULL OR p.categorie = :categorie)
      AND (:avant IS NULL OR p.id < :avant)
      AND (:auteurId IS NULL OR a.id = :auteurId)
      AND (:seulementAbonnements = false OR EXISTS (
            SELECT ab.id FROM Abonnement ab
            WHERE ab.abonne.id = :moi AND ab.suivi.id = a.id AND ab.statut = :accepte))
      AND NOT EXISTS (
            SELECT b.id FROM Blocage b
            WHERE (b.bloqueur.id = :moi AND b.bloque.id = a.id)
               OR (b.bloqueur.id = a.id AND b.bloque.id = :moi))
      AND (a.comptePrive = false OR a.id = :moi OR :admin = true OR EXISTS (
            SELECT ab2.id FROM Abonnement ab2
            WHERE ab2.abonne.id = :moi AND ab2.suivi.id = a.id AND ab2.statut = :accepte))
    ORDER BY p.id DESC""")
List<Publication> fil(Categorie categorie, Long avant, Long auteurId, boolean seulementAbonnements,
                      Long moi, boolean admin, StatutAbonnement accepte, Pageable limite);
```

`PUT /{id}/reaction` : 404 si `!vue.voitContenu(auteur)`.

- [ ] **Step 1:** tests ajoutés — `abonnements=true` ne rend que les comptes suivis (acceptés) ; `auteur=` ne rend que cet auteur ; compte privé invisible pour un non-abonné, visible pour un abonné accepté, pour l'administrateur et pour lui-même ; demande en attente ne suffit pas ; auteur bloqué exclu ; réaction sur publication invisible → 404. **Steps 2-5** : échec, implémentation, vert + suite complète, commit « Abonnements : filtre du fil et comptes privés ».

### Task B8: Messagerie

**Files:** Modify `controller/MessageController.java` ; Test `controller/MessageControllerTest.java`.

**Interfaces — Produces:** `record Interlocuteur(AuteurPublic compte, boolean peutEcrire)` ; `GET /api/messages/interlocuteurs` (suivis acceptés ∪ `idsQuiMOntEcrit` ∪ `idsAQuiJAiEcrit`, comptes actifs, non bloqués, tri par nom, chargés par `findAllById`) ; `POST` → 403 si `!vue.peutEcrire(destinataire)` ; `GET /non-lus` exclut les expéditeurs bloqués.

- [ ] **Step 1:** dans `MessageControllerTest.preparer()`, Alice et Bob se suivent (abonnements acceptés enregistrés en base) — les tests existants envoient entre eux. Nouveaux tests : Carol écrit à Alice sans la suivre → 403 ; Alice suit Carol → 200 ; Carol peut répondre à Alice qui lui a écrit → 200 ; interlocuteurs d'Alice contiennent Bob (`peutEcrire` vrai) ; après blocage, 403 et absence de la liste. **Steps 2-5** : échec, implémentation, vert + suite complète, commit « Abonnements : règles de la messagerie ».

### Task B9: Administration et vérification au `curl`

**Files:** Modify `controller/AdminController.java` ; Test `controller/AdminControllerTest.java`.

- [ ] **Step 1:** test — supprimer un compte qui suit, est suivi, a bloqué et a des notifications → 204, `flush`/`clear`, tables vides pour lui.
- [ ] **Step 2-4:** échec (clé étrangère), ajout de `notificationRepository.supprimerCellesDe`, `blocageRepository.supprimerCeuxDe`, `abonnementRepository.supprimerCeuxDe` avant la suppression du compte, suite complète au vert.
- [ ] **Step 5:** commit « Abonnements : suppression d'un compte ».
- [ ] **Step 6:** `curl` sur un port libre (8126, vérifié par `Get-NetTCPConnection`) : trois inscriptions, profil pro renseigné, profil public sans abonnement (pas de coordonnées), suivre, profil avec coordonnées, message 403 puis 200, compte privé et demande, acceptation, notifications, blocage, fil filtré ; arrêt du serveur.

---

## Frontend

### Task F1: Modèles, fabriques de test, réseaux sociaux généralisés

**Files:** Create `abonnement.model.ts` ; Modify `utilisateur.model.ts`, `contact.model.ts` (type `ReseauxRenseignes`), `donnees-test.ts`, `components/reseaux-sociaux/reseaux-sociaux.ts` (entrée `reseaux`), usages dans `pages/contact-detail/contact-detail.html` et `components/contact-list/contact-list.html`.

```ts
export type StatutRelation = 'AUCUN' | 'EN_ATTENTE' | 'ACCEPTE';
export interface CompteResume { id: number; nomAffichage: string; photoUrl?: string | null; comptePrive: boolean; statut: StatutRelation; ilMeSuit: boolean; }
export interface CoordonneesPro { emailPro: string | null; instagram: string | null; twitter: string | null; facebook: string | null; twitch: string | null; youtube: string | null; linkedin: string | null; }
export interface ProfilPublic { id: number; nomAffichage: string; photoUrl?: string | null; comptePrive: boolean; statut: StatutRelation; ilMeSuit: boolean; nombreAbonnes: number; nombreAbonnements: number; contenuVisible: boolean; peutEcrire: boolean; coordonnees: CoordonneesPro | null; }
export interface Suggestion { compte: CompteResume; enCommun: number; }
export type TypeNotification = 'NOUVEL_ABONNE' | 'DEMANDE_RECUE' | 'DEMANDE_ACCEPTEE';
export interface NotificationCompte { id: number; type: TypeNotification; acteur: AuteurPublic; date: string; lue: boolean; }
export interface Interlocuteur { compte: AuteurPublic; peutEcrire: boolean; }
export function texteNotification(n: NotificationCompte): string; // « Alice vous suit » / « Alice demande à vous suivre » / « Alice a accepté votre demande »
// contact.model.ts
export type ReseauxRenseignes = Partial<Record<CleReseau, string | null>>;
// donnees-test.ts : unUtilisateur(... comptePrive: false), unCompte(), uneNotification(), unProfil()
```

- [ ] **Step 1:** test `abonnement.model.spec.ts` sur les trois textes de `texteNotification`. **Steps 2-5** : échec, implémentation, suite complète au vert (les tests existants de `contact-detail`/`contact-list` couvrent le renommage), commit « Abonnements : modèles côté Angular ».

### Task F2: `AbonnementService` et `BlocageService`

**Files:** Create `services/abonnement.ts`, `services/blocage.ts` ; Test `services/abonnement.spec.ts`, `services/blocage.spec.ts`.

```ts
class AbonnementService {
  statutDe(id: number): StatutRelation;                    // lit le signal privé Map<number, StatutRelation>
  charger(): void;                                          // GET /api/abonnements, remplit le signal
  abonnements(): Observable<CompteResume[]>;               // idem, rend la liste
  suivre(id: number): Observable<CompteResume>;             // PUT, statut de la réponse dans le signal
  nePlusSuivre(id: number): Observable<void>;               // DELETE, retire du signal
  oublier(id: number): void;
  rechercher(terme: string): Observable<CompteResume[]>;
  suggestions(): Observable<Suggestion[]>;
  profil(id: number): Observable<ProfilPublic>;
  abonnes(): Observable<CompteResume[]>;
  demandes(): Observable<CompteResume[]>;
  accepter(id: number): Observable<void>;
  refuser(id: number): Observable<void>;
  retirerAbonne(id: number): Observable<void>;
}
class BlocageService {
  bloquer(id: number): Observable<void>;                    // + abonnementService.oublier(id)
  debloquer(id: number): Observable<void>;
  liste(): Observable<AuteurPublic[]>;
}
```

- [ ] **Step 1:** tests — `suivre` range le statut **de la réponse** (EN_ATTENTE pour un compte privé) ; `nePlusSuivre` remet `AUCUN` ; `charger` remplit ; un échec de `suivre` ne change pas le statut ; `bloquer` oublie le statut ; `rechercher` envoie `recherche`. **Steps 2-5** : échec, implémentation, vert, commit « Abonnements : services Angular ».

### Task F3: `ActiviteService`, bandeaux et navigation

**Files:** Create `services/activite.ts` ; Test `services/activite.spec.ts` ; Modify `services/notification.ts` (`notifier(titre, corps, lien = '/messages')`, `Bandeau.lien`, `tag` selon le lien), `app.ts`, `app.html`.

```ts
class ActiviteService {
  readonly nonLues: Signal<NotificationCompte[]>;
  demarrerSuivi(): void;   // timer(0, 15000) + switchMap + catchError intérieur, discret, gardes navigateur / double démarrage
  arreterSuivi(): void;    // désabonne, vide, remet la mémoire à zéro
  recentes(): Observable<NotificationCompte[]>;
  marquerLues(): void;     // PUT /api/notifications/lues puis vide nonLues
}
```

- [ ] **Step 1:** tests (minuteurs simulés, même structure que `message.spec.ts`) : premier tour silencieux ; nouvelle notification annoncée avec `texteNotification` et le lien `/abonnements` ; pas d'annonce en double ; remise à zéro après arrêt ; pas de double sondage.
- [ ] **Steps 2-4:** échec, implémentation ; `App` démarre / arrête ce suivi dans le même `effect()` que les messages ; lien « Abonnements » avec pastille `activite.nonLues().length` ; bandeau `[routerLink]="bandeau.lien"`. Suite complète au vert (les tests existants de `message.spec.ts` appellent `notifier` à deux arguments).
- [ ] **Step 5:** commit « Abonnements : notifications sondées et navigation ».

### Task F4: `bouton-suivre` et `carte-compte`

**Files:** Create `components/bouton-suivre/*`, `components/carte-compte/*` ; Test `components/bouton-suivre/bouton-suivre.spec.ts`.

```ts
// <app-bouton-suivre [compteId]="…" [comptePrive]="…" (statutChange)="…" />
class BoutonSuivre { compteId = input.required<number>(); comptePrive = input(false); statutChange = output<StatutRelation>(); }
// <app-carte-compte [compte]="…" [avecBouton]="true"> actions projetées </app-carte-compte>
class CarteCompte { compte = input.required<{ id: number; nomAffichage: string; photoUrl?: string | null; comptePrive?: boolean }>(); avecBouton = input(true); }
```

Libellés : `AUCUN` → « Suivre » (« Demander à suivre » si privé) ; `EN_ATTENTE` → « Demande envoyée » ; `ACCEPTE` → « Abonné·e ». Clic sur les deux derniers : annulation. Masqué pour son propre compte. Désactivé pendant l'appel.

- [ ] **Step 1:** tests — les quatre libellés ; clic sur « Suivre » → `PUT` et émission du nouveau statut ; clic sur « Abonné·e » → `DELETE` ; aucun bouton pour soi. **Steps 2-5** : échec, implémentation, vert, commit « Abonnements : bouton Suivre et carte de compte ».

### Task F5: Fil — filtre et auteurs cliquables

**Files:** Modify `services/publication.ts` (`charger(categorie, options: { abonnements?: boolean; auteurId?: number | null } = {})`, signaux `abonnements`, `auteurId`, paramètres `abonnements=true` / `auteur=`, `correspondAuFiltre` : ni publication propre sous « Abonnements », ni autre auteur sous `auteurId`), `pages/fil/*` (bascule « Tout le monde / Abonnements », état vide avec lien, `abonnementService.charger()`), `components/publication-carte/*` (nom lié à `/personne/:id`, `bouton-suivre`) ; Test `services/publication.spec.ts`.

- [ ] **Step 1:** tests ajoutés — paramètres envoyés ; publication propre non insérée sous « Abonnements » ; changement de filtre annule la tranche en vol (déjà couvert, doit rester vert). **Steps 2-5** : échec, implémentation, vert, commit « Abonnements : filtre du fil et auteurs cliquables ».

### Task F6: Pages « Abonnements » et « Personne »

**Files:** Create `pages/abonnements/*`, `pages/personne/*` ; Modify `app.routes.ts` ; Test `pages/personne/personne.spec.ts`.

- `abonnements` : notifications récentes (puis `marquerLues`), demandes reçues (Accepter / Refuser), recherche (`FormControl` + `debounceTime(300)` + `distinctUntilChanged` + `switchMap` + `takeUntilDestroyed`), suggestions, « Je suis », demandes envoyées, « Mes abonnés » (Retirer), « Comptes bloqués » (Débloquer). Chaque action recharge la liste concernée.
- `personne/:id` : `paramMap` suivi par `takeUntilDestroyed` (passer d'une personne à l'autre réutilise le composant) ; profil ; `bouton-suivre` (`statutChange` → rechargement du profil et des publications) ; « Écrire » → `/messages?avec=id` ; « Retirer de mes abonnés » ; « Bloquer » avec confirmation dans la page puis retour à `/abonnements` ; coordonnées pro ou explication ; publications via `publicationService.charger(null, { auteurId })` si `contenuVisible` ; 404 → « Compte introuvable » ; son propre profil → lien vers `/profil`.

- [ ] **Step 1:** tests `personne.spec.ts` — coordonnées affichées si présentes, explication sinon ; pas de chargement des publications si `contenuVisible` est faux ; 404 → message introuvable. **Steps 2-5** : échec, implémentation, vert, commit « Abonnements : pages Abonnements et Personne ».

### Task F7: Profil enrichi

**Files:** Modify `services/auth.ts` (`modifierProfil(demande: DemandeProfil)`, suppression de `autresUtilisateurs`), `pages/profil/*` ; Test `pages/profil/profil.spec.ts`.

- [ ] **Step 1:** test — le `PUT /api/utilisateurs/moi` envoie `emailPro`, les réseaux et `comptePrive` ; le formulaire est pré-rempli depuis le compte. **Steps 2-5** : échec, implémentation (`Validators.email` sur `emailPro`, boucle `RESEAUX`, case « Compte privé » et son explication, mention sur la visibilité), vert, commit « Abonnements : profil professionnel et compte privé ».

### Task F8: Messagerie

**Files:** Modify `services/message.ts` (`interlocuteurs(): Observable<Interlocuteur[]>`, `envoyer()` renvoie l'Observable), `pages/messages/*` ; Test `pages/messages/messages.spec.ts`.

- [ ] **Step 1:** tests — la liste vient de `/api/messages/interlocuteurs` ; `?avec=` ouvre la bonne conversation ; `peutEcrire` faux → pas de champ de saisie, explication affichée ; un 403 à l'envoi laisse le texte dans le champ.
- [ ] **Steps 2-4:** échec, implémentation, suite complète au vert, puis `npx ng build` (budgets).
- [ ] **Step 5:** commit « Abonnements : messagerie réservée aux abonnements ».

---

### Task D: Documentation

- [ ] Support : **section 37 « Relations entre comptes : abonnements, comptes privés et blocages »**, insérée après la section 36 — tables de liaison à statut, un objet de règles chargé une fois (et pourquoi pas une requête par élément), la même règle en Java et en JPQL, liste blanche et 403 / 404, requête d'agrégation (`GROUP BY`, `COUNT`) pour les suggestions, notifications persistées et sondées, `paramMap` et composant réutilisé par le routeur, `output()` pour prévenir le parent. Backend / Git / Pense-bête renumérotées 38 / 39 / 40, renvois corrigés (support et cours Angular). Pense-bête : erreurs réellement rencontrées.
- [ ] `progression-pedagogique.md` : **Partie 16**, numérotation continue à partir de 143 ; pistes suivantes mises à jour.
- [ ] Vérification des liens et des blocs de code, commit « Support : section 37 (abonnements), progression Partie 16 ».

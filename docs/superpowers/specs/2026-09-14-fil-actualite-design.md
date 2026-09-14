# Fil d'actualité — spécification

Date : 2026-09-14 · Statut : conception validée par l'utilisateur

## Objectif

Ajouter au carnet un fil d'actualité commun à tous les comptes, où chacun publie
autour de ses hobbies, classés par catégorie. Livraison complète en un bloc
(pas d'étapes validées une à une), testée et documentée dans le support.

Choix de l'utilisateur :

| Question | Réponse |
|---|---|
| Rythme | Livraison complète |
| Interactions | Réactions emoji (mécanique des messages) |
| Défauts de la revue | Corriger ceux liés au fil : DTO public, validation serveur |
| Schémas du cours | Mermaid |

## Décisions structurantes

1. **Pagination par curseur + « Voir plus »** (et non par numéro de page).
   Le fil est trié du plus récent au plus ancien ; une publication ajoutée
   pendant la lecture décalerait toutes les pages d'un cran. Le curseur
   (`avant=<id>`) demande « les N plus anciennes que celle-ci », insensible aux
   insertions en tête. Il permet aussi de mettre la liste à jour localement
   après une publication ou une suppression, sans recharger.
2. **Réactions : table dédiée `reaction_publication`**, et la logique de
   regroupement (« 👍 3, dont la mienne ») extraite de `MessageController` dans
   une classe `Reactions` partagée par les deux contrôleurs.
3. **Validation serveur par Bean Validation** (`spring-boot-starter-validation`,
   `@Valid`, `@NotNull`, `@NotBlank`, `@Size`, `@Pattern`). La dépendance n'est
   pas dans le dépôt Maven local : un téléchargement réseau est nécessaire.

## Backend

### Modèle

- `model/Categorie` — enum, stockée par nom (`@Enumerated(STRING)`), dans cet
  ordre : `SPORT, CULTURE, JEU_VIDEO, INFORMATIQUE, ACTUALITE, MUSIQUE, CUISINE,
  VOYAGE, NATURE, CREATIONS, AUTRE`.
- `model/Publication` — `id`, `auteur` (`@ManyToOne`, colonne `auteur_id`,
  obligatoire), `categorie` (obligatoire, longueur 20), `contenu` (obligatoire,
  longueur 2000), `imageUrl` (facultative, longueur 500), `datePublication`
  (`Instant`, obligatoire), `dateModification` (`Instant`, nulle tant que non
  modifiée).
- `model/ReactionPublication` — `publication` et `utilisateur` (`LAZY`), `emoji`
  (longueur 8), contrainte d'unicité `(publication_id, utilisateur_id)`.

### Dépôts

- `PublicationRepository`
  - `fil(Categorie categorie, Long avant, Pageable limite)` en JPQL :
    `(:categorie IS NULL OR p.categorie = :categorie) AND (:avant IS NULL OR p.id < :avant)
    ORDER BY p.id DESC`, retour `List<Publication>` (pas de requête de comptage).
    Tri par `id` décroissant : l'identifiant est croissant et unique, là où deux
    dates peuvent être égales.
  - `countByAuteurId`, `supprimerCellesDe(utilisateurId)`.
- `ReactionPublicationRepository` — `findByPublicationIdIn`,
  `findByPublicationIdAndUtilisateurId`, `supprimerCellesDeLaPublication`,
  `supprimerCellesDe(utilisateurId)`, `supprimerCellesDesPublicationsDe(utilisateurId)`.

### DTO et code partagé

- `controller/AuteurPublic(id, nomAffichage, photoUrl)` avec `de(Utilisateur)`.
- `controller/Reactions` — `EMOJIS_AUTORISES`, `estAutorise(emoji)`, record
  `ReactionResume(emoji, nombre, parMoi)`, et une méthode générique de
  regroupement utilisée par les messages et les publications. Les réactions
  d'un lot sont réparties par élément une seule fois (plus de filtrage de toute
  la liste pour chaque élément).

### API `/api/publications` (authentifiée)

| Route | Corps | Réponse | Refus |
|---|---|---|---|
| `GET ?categorie=&avant=&taille=` | — | `PageFil { publications, curseurSuivant }` | 400 catégorie inconnue |
| `POST` | `DemandePublication` | `PublicationVue` | 400 validation |
| `PUT /{id}` | `DemandePublication` | `PublicationVue` | 400, 403 non-auteur, 404 |
| `DELETE /{id}` | — | 204 | 403 ni auteur ni admin, 404 |
| `PUT /{id}/reaction` | `{ emoji }` | `PublicationVue` | 400 emoji, 404 |

- `taille` : défaut 10, bornée à [1, 30]. Le serveur lit `taille + 1` lignes ;
  s'il y en a plus que `taille`, `curseurSuivant` = id de la dernière renvoyée,
  sinon `null`.
- `DemandePublication(@NotNull categorie, @NotBlank @Size(max=2000) contenu,
  @Size(max=500) @Pattern("^$|^https?://\S+$") imageUrl)` ; une `imageUrl` vide
  est enregistrée `null`. Une catégorie inconnue dans le JSON est un 400.
- `PublicationVue(id, auteur: AuteurPublic, categorie, contenu, imageUrl,
  datePublication, dateModification, reactions, modifiable, supprimable)` —
  `modifiable` = auteur ; `supprimable` = auteur ou administrateur. Calculés par
  le serveur pour le compte qui regarde.
- **403, pas 404**, pour un non-auteur : une publication est publique, son
  existence n'est pas un secret (contraste voulu avec les messages privés).
- Suppression par un administrateur : le rôle est **relu en base**, pas pris
  dans le jeton (action destructrice). `@Transactional` ; les réactions partent
  avant la publication.
- Réaction : même bascule que les messages (poser / remplacer / retirer).

### Correctifs liés au fil

- **Confidentialité** : `MessageVu.expediteur/destinataire` et
  `GET /api/utilisateurs` renvoient `AuteurPublic` au lieu de l'entité
  `Utilisateur` (qui exposait email, rôle, statut). `GET /api/utilisateurs/moi`
  inchangé.
- **Validation des messages** : `DemandeMessage(@NotNull destinataireId,
  @NotBlank @Size(max=2000) contenu)` avec `@Valid` — un message trop long ou
  sans destinataire donne 400 au lieu de 500.
- **Administration** : la suppression d'un compte supprime aussi ses
  réactions aux publications, les réactions sur ses publications, puis ses
  publications. `LigneCompte` gagne `nombrePublications`.

## Frontend

### Modèles

- `reaction.model.ts` — `ReactionResume` et `EMOJIS_REACTION`, déplacés depuis
  `message.model.ts`.
- `utilisateur.model.ts` — `AuteurPublic { id, nomAffichage, photoUrl? }` ;
  `LigneCompte.nombrePublications`.
- `message.model.ts` — `expediteur` / `destinataire` typés `AuteurPublic`.
- `publication.model.ts` — `CATEGORIES` (`cle`, `libelle`, `emoji`, `couleur`)
  `as const`, `type Categorie` déduit du tableau (patron de `RESEAUX`),
  `Publication`, `PageFil`, `DemandePublication`.

### `services/publication.ts` — `PublicationService`

- Signaux privés + lecture seule : `publications`, `categorie` (filtre actif),
  `curseur` ; `computed` `aDesPlusAnciennes`.
- Un `Subject<{ suite: boolean }>` + `switchMap` + `catchError` intérieur : une
  nouvelle catégorie annule un « Voir plus » en vol, dont la réponse ajouterait
  sinon une page de l'ancienne catégorie.
- `charger(categorie)` : vide la liste, repart sans curseur.
- `chargerPlus()` : sans effet si `curseur` est `null` ; sinon ajoute à la fin.
- `publier(demande)` / `modifier(id, demande)` **renvoient l'Observable** (patron
  d'`AuthService`) : l'appelant ne vide ou ne ferme le formulaire qu'en cas de
  succès. `publier` insère en tête si la catégorie correspond au filtre ;
  `modifier` remplace, ou retire si la publication ne correspond plus au filtre.
- `supprimer(id)` : retire localement après succès. `reagir(id, emoji)` :
  requête discrète, remplace la publication.

### Composants et page

- `components/publication-form` — création ET modification.
  `publication = input<Publication | null>(null)`, `termine = output<Publication>()`,
  `annuler = output<void>()`. Catégorie (`<select>` natif), contenu (`<textarea>`,
  compteur /2000), image (URL, aperçu). Validateurs miroirs du serveur. Le
  formulaire injecte `PublicationService` et ne se vide qu'au succès.
- `components/publication-carte` — `publication = input.required<Publication>()`.
  Auteur (avatar ou initiale), pastille de catégorie colorée par une variable CSS
  pilotée depuis le composant, date (et « modifié »), texte, image
  (`loading="lazy"`), barre de réactions (palette + compteurs), actions selon
  `modifiable` / `supprimable`. Modification en place (réutilise le formulaire).
  Suppression avec **confirmation dans la carte** : pas de `p-confirmDialog`,
  le paquet initial étant à 926 kB pour une alerte à 1 MB.
- `pages/fil` — pastilles de filtre « Tout » + 11 catégories (`aria-pressed`),
  formulaire de publication, liste des cartes, bouton « Voir plus », états vides
  selon le filtre.
- Route `fil` (`authGuard`, rendu client via la route `**` existante) ; lien
  « Fil » dans la navigation, après « Contacts ».
- Administration : colonne « Publications » et compteur dans la confirmation.

## Tests

- Backend — `PublicationControllerTest` : publication lue dans le fil sans email
  d'auteur ; tri ; filtre ; curseur (pages successives sans doublon ; insertion
  entre deux pages sans décalage) ; taille bornée ; validations (400) ;
  modification et suppression (auteur, autre → 403, admin, inconnue → 404) ;
  drapeaux `modifiable` / `supprimable` ; réactions ; 401 sans jeton.
  `MessageControllerTest` : pas d'email dans l'expéditeur ni dans la liste des
  comptes ; 400 message trop long / sans destinataire. `AdminControllerTest` :
  suppression d'un compte qui a publié et réagi.
- Frontend — `publication.spec.ts` (paramètres, curseur, annulation par
  `switchMap`, insertion selon le filtre, suppression locale, réaction,
  modification hors filtre) ; `publication-form.spec.ts` (validation, formulaire
  conservé en cas d'échec, vidé au succès).
- Vérifications : `mvnw test`, `ng test`, `ng build` (budgets), parcours `curl`
  sur un port dédié après contrôle des ports occupés.

## Documentation

- Support : section 33 « Validation côté serveur (Bean Validation) », 34
  « Pagination par curseur », 35 « Fil d'actualité : catégories, droits et DTO
  public » ; Backend / Git / Pense-bête renumérotées 36 / 37 / 38 (liens internes
  compris) ; entrées au pense-bête pour les erreurs rencontrées.
- `progression-pedagogique.md` : Partie 14.
- `README.md` : fonctionnalités à jour, mise en forme Markdown réparée.
- Ensuite, chantier séparé : `cours-angular.md` refondu — schémas Mermaid ajoutés
  aux étapes 1 à 10, étapes 11 à 26 rédigées dans le même format, étape 27 sur le
  fil d'actualité ; renvois au support mis à jour après la renumérotation.

## Hors périmètre

Commentaires, défilement infini, alerte « nouvelles publications », envoi de
fichier image, et les autres défauts de la revue (accusés de lecture en
arrière-plan, saisie perdue des contacts, validation des contacts, emails non
normalisés, N+1 de l'administration, `utilisateurConnecte()` dupliquée).

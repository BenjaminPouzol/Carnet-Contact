# Abonnements, comptes privés, blocages et notifications — spécification

Date : 2026-09-15 · Statut : conception validée par l'utilisateur (les quatre
extensions — blocage et retrait d'un abonné, suggestions, notifications, comptes
privés — ont été demandées ensuite, avec l'autorisation de trancher les détails)

## Objectif

Permettre de suivre des comptes qu'on ne connaît pas, dans une liste distincte
des contacts, pour retrouver facilement leurs publications. Suivre quelqu'un
donne le droit de lui écrire et de voir son email professionnel et ses réseaux
sociaux — jamais son email de connexion. S'y ajoutent : comptes privés (suivre
demande un accord), blocage et retrait d'un abonné, suggestions de comptes, et
notifications d'abonnement. Livraison complète, après l'envoi d'images.

Choix de l'utilisateur :

| Question | Réponse |
|---|---|
| Profil | Email pro + 6 réseaux ajoutés au compte ; pas de téléphone |
| Messagerie | On écrit aux comptes qu'on suit, et on peut répondre à qui nous a écrit |
| Accord | Immédiat pour un compte public ; demande à accepter pour un compte privé |
| Découverte | Bouton sur les cartes du fil, page de recherche, page profil public |
| Extensions | Bloquer / retirer un abonné, suggestions, notifications, comptes privés |

## Décisions structurantes

1. **Tables de liaison écrites à la main** (`Abonnement`, `Blocage`), comme les
   réactions : elles portent des données (statut, dates) et s'interrogent dans
   les deux sens. Pas de `@ManyToMany`.
2. **Un abonnement a un statut** (`EN_ATTENTE`, `ACCEPTE`) plutôt qu'une table
   de demandes séparée : une demande acceptée *devient* l'abonnement, sans
   recopie. Suivre un compte public crée directement `ACCEPTE`.
3. **Toutes les règles de visibilité dans un seul service**, `Relations`. Les
   contrôleurs lui posent des questions (« peut-il écrire ? », « voit-il le
   contenu ? ») au lieu de recombiner abonnement, blocage et confidentialité à
   chaque route — la même raison que les intercepteurs : une règle recopiée
   finit par être oubliée quelque part.
4. **Liste blanche** : `ProfilPublic`, `CompteResume` et `CoordonneesPro` sont
   des records qui n'ont pas de champ email. L'email de connexion ne peut pas
   fuiter par oubli d'un `@JsonIgnore`.
5. **Un blocage coupe tout, dans les deux sens.** Entre deux comptes dont l'un a
   bloqué l'autre : pas de profil (404), pas de recherche, pas de publications
   dans le fil, pas d'abonnement, pas de message, pas de notification. Règle
   unique, plus facile à tester et à expliquer qu'une matrice « qui voit quoi ».
   Seul le bloqueur voit le bloqué, dans sa liste « Comptes bloqués ».
6. **Notifications persistées** (entité `Notification`) plutôt que déduites des
   abonnements : « demande acceptée » concerne le demandeur, pas le suivi, et
   une table dédiée garde l'état « lue ». Détection côté client par le sondage
   déjà en place (patron de la Partie 11 : `Set` d'identifiants vus, premier
   tour silencieux).
7. **403 pour un envoi de message refusé**, 404 pour un compte bloqué ou
   inconnu : un compte public existe aux yeux de tous, dire « introuvable »
   serait faux ; un compte en relation de blocage, lui, doit disparaître.

## Backend

### Modèle

- `Utilisateur` gagne `emailPro`, `instagram`, `twitter`, `facebook`, `twitch`,
  `youtube`, `linkedin` (chaînes facultatives) et `comptePrive`
  (`boolean`, non nul, `false` par défaut).
- `model/StatutAbonnement` — enum `EN_ATTENTE`, `ACCEPTE`.
- `model/Abonnement` — `abonne` et `suivi` (`@ManyToOne(LAZY)`, colonnes
  `abonne_id`, `suivi_id`), `statut` (`@Enumerated(STRING)`, longueur 12),
  `dateDemande` (`Instant`, non nul), `dateAcceptation` (`Instant`, nul tant que
  en attente). Unicité `(abonne_id, suivi_id)`.
- `model/Blocage` — `bloqueur`, `bloque` (`LAZY`), `dateBlocage`. Unicité
  `(bloqueur_id, bloque_id)`.
- `model/TypeNotification` — enum `NOUVEL_ABONNE`, `DEMANDE_RECUE`,
  `DEMANDE_ACCEPTEE`.
- `model/Notification` — `destinataire`, `acteur` (`LAZY`), `type`
  (`STRING`, longueur 20), `date`, `lue` (`boolean`).

### Service `abonnement/Relations`

| Méthode | Règle |
|---|---|
| `bloques(aId, bId)` | Un blocage existe dans un sens ou dans l'autre |
| `statut(abonneId, suiviId)` | `AUCUN`, `EN_ATTENTE` ou `ACCEPTE` (enum `StatutRelation` côté DTO) |
| `voitContenu(moi, auteur)` | Pas de blocage ET (moi = auteur OU compte public OU abonnement accepté OU moi administrateur) |
| `voitCoordonnees(moi, autre)` | Pas de blocage ET (moi = autre OU abonnement accepté) — un compte public ne suffit pas |
| `peutEcrire(moi, autre)` | Pas de blocage ET (abonnement accepté de moi vers autre OU autre m'a déjà écrit) |
| `notifier(destinataire, acteur, type)` | Crée une notification |

Le rôle administrateur est lu sur l'entité rechargée, jamais dans le jeton.

### DTO

- `StatutRelation` — `AUCUN`, `EN_ATTENTE`, `ACCEPTE`.
- `CompteResume(id, nomAffichage, photoUrl, comptePrive, statut, ilMeSuit)` —
  `statut` : ma relation vers ce compte ; `ilMeSuit` : abonnement accepté de lui
  vers moi.
- `CoordonneesPro(emailPro, instagram, twitter, facebook, twitch, youtube, linkedin)`.
- `ProfilPublic(id, nomAffichage, photoUrl, comptePrive, statut, ilMeSuit,
  nombreAbonnes, nombreAbonnements, contenuVisible, peutEcrire, coordonnees)` —
  `coordonnees` nulles si `voitCoordonnees` est faux ; compteurs sur les
  abonnements acceptés seulement.
- `Suggestion(compte: CompteResume, enCommun)`.
- `NotificationVue(id, type, acteur: AuteurPublic, date, lue)`.
- `Interlocuteur(compte: AuteurPublic, peutEcrire)`.

### API

**`/api/utilisateurs`**

| Route | Réponse | Refus / règles |
|---|---|---|
| `GET ?recherche=` | `List<CompteResume>`, 20 max, tri par nom | Exclut moi, les comptes désactivés et les blocages. Remplace l'ancienne liste de tous les comptes |
| `GET /suggestions` | `List<Suggestion>`, 5 max | D'abord les comptes suivis par ceux que je suis (tri par `enCommun` décroissant), complétés par les comptes les plus suivis. Exclut moi, déjà suivis ou demandés, désactivés, blocages |
| `GET /{id}` | `ProfilPublic` | 404 inconnu, désactivé ou blocage. Pour soi : coordonnées visibles |
| `GET /moi` | `Utilisateur` | inchangée (porte désormais les nouveaux champs) |
| `PUT /moi` | `Utilisateur` | `DemandeProfil` gagne `@Email @Size(max=255) emailPro`, les 6 réseaux `@Size(max=255)`, `Boolean comptePrive` ; `@Valid`. Passer de privé à public accepte toutes les demandes en attente (et notifie chaque demandeur) |

**`/api/abonnements`**

| Route | Réponse | Refus / règles |
|---|---|---|
| `PUT /{id}` | `CompteResume` | Suivre. Compte public → `ACCEPTE` + `NOUVEL_ABONNE` ; privé → `EN_ATTENTE` + `DEMANDE_RECUE`. Idempotent (aucune seconde notification). 400 soi-même, 404 inconnu / désactivé / blocage |
| `DELETE /{id}` | 204 | Ne plus suivre ou annuler sa demande ; supprime la notification associée. Idempotent |
| `GET` | `List<CompteResume>` | Mes abonnements sortants, acceptés et en attente |
| `GET /abonnes` | `List<CompteResume>` | Mes abonnés acceptés |
| `GET /demandes` | `List<CompteResume>` | Demandes reçues en attente |
| `PUT /demandes/{id}` | 204 | Accepter la demande de `{id}` ; notifie `DEMANDE_ACCEPTEE`, retire `DEMANDE_RECUE`. 404 sans demande |
| `DELETE /demandes/{id}` | 204 | Refuser ; retire `DEMANDE_RECUE`. Idempotent |
| `DELETE /abonnes/{id}` | 204 | Retirer un abonné ; il pourra redemander. Idempotent |

**`/api/blocages`**

| Route | Réponse | Refus / règles |
|---|---|---|
| `PUT /{id}` | 204 | Bloquer. Supprime les abonnements et les notifications entre les deux comptes, dans les deux sens. 400 soi-même, 404 inconnu. Idempotent |
| `DELETE /{id}` | 204 | Débloquer (aucune relation n'est restaurée). Idempotent |
| `GET` | `List<AuteurPublic>` | Comptes que j'ai bloqués |

**`/api/notifications`**

| Route | Réponse |
|---|---|
| `GET` | Les 30 plus récentes, lues ou non |
| `GET /non-lues` | Les non lues (sondée, requête discrète côté client) |
| `PUT /lues` | 204, marque tout comme lu |

Les notifications dont l'acteur est en relation de blocage ou désactivé sont
exclues des lectures.

**`/api/publications`** — `GET` gagne `abonnements` (booléen) et `auteur`
(identifiant), combinables avec `categorie`. La requête applique toujours la
visibilité : auteurs en relation de blocage exclus ; auteurs privés seulement
si moi, abonnement accepté, ou administrateur. `?auteur=` d'un compte invisible
rend une liste vide. `PUT /{id}/reaction` sur une publication invisible : 404.

**`/api/messages`**
- `POST` : 403 « Suivez cette personne pour lui écrire. » si `peutEcrire` est
  faux.
- `GET /interlocuteurs` : `List<Interlocuteur>` — abonnements acceptés ∪
  personnes avec qui une conversation existe, sans comptes désactivés ni
  blocages, tri par nom.
- `GET /non-lus` : exclut les expéditeurs en relation de blocage.
- `GET /{autreId}` et les réactions : inchangés.

**Administration** — la suppression d'un compte supprime aussi ses
notifications (destinataire ou acteur), ses blocages et ses abonnements, dans
les deux sens.

## Frontend

### Modèles

- `utilisateur.model.ts` — `Utilisateur` gagne `emailPro`, les 6 réseaux,
  `comptePrive`.
- `abonnement.model.ts` — `StatutRelation`, `CompteResume`, `CoordonneesPro`,
  `ProfilPublic`, `Suggestion`, `TypeNotification`, `NotificationVue`,
  `Interlocuteur`.

### Services

- `AbonnementService` — signal privé `statuts: Map<number, StatutRelation>`
  (chargé par `GET /api/abonnements`) et `statutDe(id)` ; `suivre(id)` et
  `nePlusSuivre(id)` mettent le signal à jour à la réponse ; `rechercher`,
  `suggestions`, `profil`, `abonnes`, `demandes`, `accepter`, `refuser`,
  `retirerAbonne` renvoient des Observables.
- `BlocageService` — `bloquer(id)` (retire aussi l'id des `statuts`),
  `debloquer(id)`, `liste()`.
- `ActiviteService` — sondage de `/api/notifications/non-lues` toutes les 15 s,
  mêmes gardes que le sondage des messages (navigateur seulement, arrêt à la
  déconnexion, pas de double démarrage) ; signal `nonLues` ; annonce chaque
  nouvelle notification par `NotificationService` (« Alice vous suit »,
  « Bob demande à vous suivre », « Alice a accepté votre demande ») avec la
  détection par `Set` et premier tour silencieux ; `marquerLues()`.
- `PublicationService.charger()` reçoit un filtre
  `{ categorie, abonnements, auteurId }`.
- `AuthService.modifierProfil()` reçoit un objet (nom, photo, email pro,
  réseaux, confidentialité).
- `MessageService` (ou la page) lit `/api/messages/interlocuteurs`.

### Composants

- `bouton-suivre` — entrées `compteId`, `comptePrive`. Libellés : « Suivre » /
  « Demande envoyée » / « Abonné·e », selon `statutDe`. Un clic sur les deux
  derniers annule, avec un `title` explicite.
- `carte-compte` — avatar, nom lié à `/personne/:id`, `bouton-suivre`, et
  `<ng-content>` pour des actions propres à la liste (Accepter / Refuser,
  Retirer, Débloquer).
- `reseaux-sociaux` — l'entrée accepte tout objet portant les 6 champs de
  réseaux (plus seulement un `Contact`).

### Pages et navigation

- `/abonnements` (lien « Abonnements » avec pastille = notifications non lues) :
  notifications récentes (marquées lues à l'ouverture), demandes reçues,
  recherche (délai de frappe, `switchMap`), suggestions, « Je suis », demandes
  envoyées, « Mes abonnés » (Retirer), « Comptes bloqués » (Débloquer).
- `/personne/:id` : en-tête (avatar, nom, compteurs, cadenas si privé),
  `bouton-suivre`, « Écrire » si `peutEcrire` (vers `/messages?avec=id`),
  « Retirer de mes abonnés » si `ilMeSuit`, « Bloquer » avec confirmation dans
  la page ; coordonnées pro (email pro en `mailto:`, `reseaux-sociaux`) ou
  explication ; publications si `contenuVisible`, sinon « Ce compte est privé ».
  404 → « Compte introuvable ».
- Fil : bascule « Tout le monde / Abonnements » au-dessus des catégories ; état
  vide des abonnements avec un lien vers `/abonnements` ; dans la carte, nom de
  l'auteur lié à `/personne/:id` et `bouton-suivre` si l'auteur n'est pas moi.
- Profil : email pro, 6 réseaux (réutilise `RESEAUX`), case « Compte privé »
  avec son explication ; mention « Visibles par les personnes qui vous suivent.
  Votre email de connexion ne l'est jamais. »
- Messages : colonne « Conversations » alimentée par les interlocuteurs ;
  `?avec=id` présélectionne ; si `peutEcrire` est faux, la saisie est remplacée
  par une explication et `bouton-suivre`. Un 403 à l'envoi conserve le texte
  saisi.
- Routes `abonnements` et `personne/:id` protégées par `authGuard`, rendues
  côté client.

## Tests

- Backend
  - `RelationsTest` (contexte Spring, `@Transactional`) : chaque ligne du
    tableau des règles, blocage dans les deux sens.
  - `AbonnementControllerTest` : suivre public / privé, idempotence sans
    seconde notification, soi-même 400, inconnu 404, bloqué 404, annuler,
    accepter, refuser, retirer un abonné, listes, passage privé → public qui
    accepte les demandes.
  - `UtilisateurControllerTest` : recherche (exclusions), profil public
    (coordonnées nulles sans abonnement, présentes avec, **aucun champ
    `email`** dans les deux cas), 404 bloqué, validation `emailPro`,
    suggestions (ordre par comptes en commun, exclusions).
  - `BlocageControllerTest` : effets du blocage (abonnements et notifications
    supprimés), 400, déblocage, liste.
  - `NotificationControllerTest` : création aux trois événements, non-lues,
    marquage, exclusion des bloqués.
  - `PublicationControllerTest` : filtres `abonnements` et `auteur`, compte
    privé invisible pour un non-abonné et visible pour un abonné accepté et
    pour l'administrateur, auteur bloqué exclu, réaction sur publication
    invisible 404.
  - `MessageControllerTest` : 403 sans abonnement, envoi avec abonnement,
    réponse autorisée, interlocuteurs et `peutEcrire`, blocage. Les tests
    existants qui envoyaient sans abonnement sont adaptés (abonnement créé dans
    la préparation).
  - `AdminControllerTest` : suppression d'un compte ayant abonnements, blocages
    et notifications.
- Frontend
  - `abonnement.spec.ts` (statuts mis à jour à la réponse), `blocage.spec.ts`,
    `activite.spec.ts` (détection par `Set`, premier tour silencieux),
    `bouton-suivre.spec.ts` (trois libellés), `publication.spec.ts` (paramètres
    de filtre), `messages` (état « ne peut pas écrire »), `profil` (envoi des
    nouveaux champs).
- Vérifications : `mvnw test`, `npm test`, `ng build` (budgets), parcours `curl`
  sur un port dédié après contrôle des ports.

## Documentation

- Support : section 37 « Relations entre comptes : abonnements, comptes privés
  et blocages » (tables de liaison à statut, service de règles centralisé,
  liste blanche, 403 / 404, suggestions par requête d'agrégation, notifications
  persistées) ; Backend / Git / Pense-bête renumérotées 38 / 39 / 40.
- `progression-pedagogique.md` : Partie 16.

## Hors périmètre

Notifications autres que les abonnements (réactions, mentions), abonnement à une
catégorie, signalement d'un compte à l'administration, import des contacts
comme abonnements.

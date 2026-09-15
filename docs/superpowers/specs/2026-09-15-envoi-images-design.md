# Envoi d'images — spécification

Date : 2026-09-15 · Statut : conception validée par l'utilisateur

## Objectif

Permettre d'envoyer une image depuis son appareil, **en plus** de la saisie
d'une URL qui reste possible, pour la photo de profil, la photo d'un contact et
l'image d'une publication. Les images qui ne servent plus sont supprimées
automatiquement. Livraison complète en un bloc, testée et documentée dans le
support.

Choix de l'utilisateur :

| Question | Réponse |
|---|---|
| Portée | Photo de profil, photo d'un contact, image d'une publication |
| Stockage | En base (colonne BLOB), cohérent avec H2 en mémoire |
| Limites | 5 Mo ; JPEG, PNG, WebP, GIF |
| Rythme | Livraison complète |
| Approche | Route d'envoi dédiée qui renvoie une URL |
| Nettoyage | Tâche planifiée seule |

## Décisions structurantes

1. **Une image envoyée devient une URL comme une autre.** `POST /api/images`
   stocke le fichier et renvoie son adresse absolue ; le frontend la place dans
   le champ URL existant. Aucune entité existante ne change, les huit `<img>`
   du frontend restent tels quels, et l'aperçu fonctionne avant
   l'enregistrement. Écartés : envoyer le fichier avec chaque formulaire (cinq
   contrats d'API à changer, aperçu difficile) et l'encodage base64 dans le
   champ (des mégaoctets recopiés dans chaque réponse JSON).
2. **Lecture publique par UUID.** Une balise `<img>` n'envoie pas le jeton JWT
   et ne passe pas par les intercepteurs : `GET /api/images/{id}` doit être
   public. L'identifiant est un UUID aléatoire, impossible à deviner — même
   niveau de confidentialité qu'une URL externe. Seul le `GET` est ouvert.
3. **URL absolue.** `<img src="/api/…">` partirait vers le serveur Angular
   (port 4200) : `baseUrlInterceptor` ne concerne que `HttpClient`. Le serveur
   construit l'adresse à partir de la requête reçue
   (`ServletUriComponentsBuilder.fromCurrentContextPath()`), sans écrire
   `localhost:8080` en dur. Une URL absolue `http://` satisfait déjà le
   `@Pattern` de `DemandePublication`.
4. **Format déterminé par la signature du fichier**, jamais par son nom ni par
   le `Content-Type` annoncé par le client. SVG exclu (texte pouvant contenir du
   JavaScript).
5. **Nettoyage par tâche planifiée unique**, avec délai de grâce. Une image
   fraîchement envoyée n'est encore référencée par rien tant que le formulaire
   n'est pas enregistré : on ne supprime que les images non référencées **et**
   envoyées depuis plus d'une heure. Une même URL pouvant servir à plusieurs
   endroits, la référence est cherchée dans les trois tables. Un seul endroit
   de code : aucune route future ne peut oublier de nettoyer. Contrepartie
   acceptée : une photo retirée reste accessible par son lien 1 h 30 au plus.

## Backend

### Configuration — `application.properties`

```properties
spring.servlet.multipart.max-file-size=5MB
spring.servlet.multipart.max-request-size=6MB
carnet.images.delai-grace-ms=3600000
carnet.images.nettoyage-intervalle-ms=1800000
```

Aucune dépendance à ajouter (multipart inclus dans le starter web).
`@EnableScheduling` posé sur la classe principale de l'application.

### Modèle — `model/Image`

| Champ | Détail |
|---|---|
| `id` | `String`, longueur 36, UUID généré par le serveur à la création |
| `donnees` | `@Lob byte[]`, obligatoire |
| `typeContenu` | `image/jpeg`, `image/png`, `image/webp` ou `image/gif`, longueur 20, obligatoire |
| `taille` | `long`, obligatoire |
| `dateEnvoi` | `Instant`, obligatoire |
| `proprietaire` | `@ManyToOne(LAZY)`, colonne `proprietaire_id`, obligatoire |

L'entité n'est jamais sérialisée en JSON (le contrôleur renvoie les octets ou
un record).

### Signature — `image/FormatImage`

Classe finale, constructeur privé, méthode statique
`Optional<String> typeDe(byte[] debut)` :

| Format | Octets |
|---|---|
| JPEG | `FF D8 FF` en tête |
| PNG | `89 50 4E 47 0D 0A 1A 0A` en tête |
| GIF | `GIF87a` ou `GIF89a` en tête |
| WebP | `RIFF` (octets 0-3) et `WEBP` (octets 8-11) |

Tout le reste — y compris un contenu trop court pour porter une signature —
renvoie `Optional.empty()`.

### Dépôt — `ImageRepository`

- `supprimerCellesDe(utilisateurId)` — `@Modifying(flushAutomatically = true,
  clearAutomatically = true)`, pour la suppression d'un compte.
- `supprimerOrphelinesAvant(Instant limite)` — `@Modifying`, JPQL `DELETE` des
  images dont `dateEnvoi < limite` et pour lesquelles il n'existe **aucun**
  `Contact.photoUrl`, `Utilisateur.photoUrl` ni `Publication.imageUrl` se
  terminant par `/api/images/<id>` (`NOT EXISTS … LIKE CONCAT('%/api/images/',
  i.id)`). Renvoie le nombre de lignes supprimées.

### API `/api/images`

| Route | Accès | Corps | Réponse | Refus |
|---|---|---|---|---|
| `POST` | jeton requis | multipart, partie `fichier` | 201 `{ "url": "http://…/api/images/<uuid>" }` | 400 partie absente ou vide, 413 > 5 Mo, 415 format non reconnu, 401 |
| `GET /{id}` | **public** | — | 200, octets, `Content-Type` stocké, `Cache-Control: max-age=31536000, immutable` | 404 |

- Le 413 est normalement produit par Spring avant l'appel du contrôleur
  (`MaxUploadSizeExceededException`). **À vérifier au `curl`** ; si la réponse
  n'est pas un 413, ajouter un `@RestControllerAdvice` qui le produit.
- `SecurityConfig` : `.requestMatchers(HttpMethod.GET, "/api/images/*").permitAll()`,
  placé avant `anyRequest().authenticated()`.

### Nettoyage — `image/NettoyageImages`

- `@Service` ; méthode `int nettoyer(Instant limite)` qui délègue à
  `supprimerOrphelinesAvant` (`@Transactional`) — c'est elle que les tests
  appellent, avec une limite choisie.
- Méthode `@Scheduled(fixedDelayString = "${carnet.images.nettoyage-intervalle-ms}",
  initialDelayString = "${carnet.images.nettoyage-intervalle-ms}")` qui appelle
  `nettoyer(Instant.now().minusMillis(delaiGrace))`. Le délai initial évite
  qu'elle ne s'exécute au démarrage des tests.

### Branchements

- `AdminController.supprimer` : `imageRepository.supprimerCellesDe(cible.getId())`
  ajouté à la chaîne, avant `utilisateurRepository.delete(cible)` (clé
  étrangère).
- Commentaires « pas d'envoi de fichier » corrigés dans `Contact.java`,
  `Utilisateur.java` et `Publication.java`.

## Frontend

### `services/image.ts` — `ImageService`

- `envoyer(fichier: File): Observable<string>` : `FormData` avec la partie
  `fichier`, `POST /api/images`, `contexte({ libelle: "Impossible d'envoyer
  l'image" })`, `map` vers `url`.
- **Aucun en-tête `Content-Type` écrit à la main** : le navigateur doit ajouter
  lui-même la frontière multipart.
- Constantes exportées `TAILLE_MAX_IMAGE` (5 Mo) et `FORMATS_IMAGE` (les quatre
  types MIME), partagées avec le composant.

### `components/champ-image` — composant réutilisable

- Entrées : `controle = input.required<FormControl<string | null>>()`,
  `identifiant = input.required<string>()` (pour `<label for>`),
  `libelle = input.required<string>()`, `placeholder = input('')`.
- Gabarit : libellé, champ texte lié par `[formControl]="controle()"`, bouton
  `type="button"` « Choisir une image » qui déclenche un
  `<input type="file" accept="image/jpeg,image/png,image/webp,image/gif">`
  caché, message d'erreur local.
- À la sélection : vérification locale de la taille et du type (message, aucune
  requête si refusé) ; sinon signal `envoiEnCours` à `true`, bouton désactivé
  avec « Envoi en cours… », puis `controle().setValue(url)` et
  `markAsDirty()` au succès. En cas d'échec, le contrôle **garde sa valeur
  précédente** (la bannière vient de l'intercepteur).
- L'input fichier est vidé après chaque sélection, pour que choisir deux fois
  le même fichier redéclenche `change`.
- `ControlValueAccessor` non retenu : plus lourd, même résultat ici.

### Intégration

| Fichier | Contrôle passé |
|---|---|
| `pages/profil/profil.html` | `formulaire.controls.photoUrl` |
| `components/contact-form/contact-form.html` | `contactForm.controls.photoUrl` |
| `pages/contact-edit/contact-edit.html` | `contactForm.controls.photoUrl` |
| `components/publication-form/publication-form.html` | `formulaire.controls.imageUrl` |

- Les aperçus existants (profil, publication) sont conservés et continuent de
  lire la valeur du contrôle. Le message d'erreur de format de la publication
  reste dans `publication-form`.
- Texte d'aide du profil : « Collez une adresse, ou choisissez une image sur
  votre appareil. »
- Pendant l'envoi, `chargementInterceptor` compte la requête : les boutons
  d'enregistrement déjà liés à `chargement()` sont grisés sans code
  supplémentaire.

### `erreurInterceptor`

`raisonTechnique()` gagne `413` → « l'image dépasse la taille autorisée (413) »
et `415` → « ce format de fichier n'est pas accepté (415) ».

## Tests

- Backend
  - `FormatImageTest` (unitaire) : quatre signatures reconnues
    (`@ParameterizedTest`), faux JPEG (texte renommé), SVG, contenu de 2 octets
    et contenu vide refusés.
  - `ImageControllerTest` (MockMvc `multipart()`) : 201 et URL absolue se
    terminant par l'id ; 415 ; 400 fichier vide ; 400 partie absente ; 401 sans
    jeton ; `GET` sans jeton → mêmes octets et bon `Content-Type` ; 404.
  - `NettoyageImagesTest` : ancienne non référencée → supprimée ; récente non
    référencée → gardée ; ancienne référencée par un contact, par un profil,
    par une publication → gardée (trois cas).
  - `AdminControllerTest` : suppression d'un compte qui a envoyé une image.
- Frontend
  - `image.spec.ts` : corps `FormData` avec la partie `fichier`, pas de
    `Content-Type` posé, URL extraite.
  - `champ-image.spec.ts` : fichier trop gros et format refusé → aucune requête,
    message affiché ; succès → contrôle mis à jour ; échec → valeur précédente
    conservée.
  - `erreur-interceptor.spec.ts` : messages 413 et 415.
- Vérifications : `mvnw test`, `npm test`, `ng build` (budgets), parcours `curl`
  sur un port dédié après contrôle des ports occupés — envoi, lecture sans
  jeton, 415 sur un faux JPEG, **413 réel** sur un fichier de 6 Mo.

## Documentation

- Support : section 36 « Envoi de fichiers » — multipart côté Spring
  (`MultipartFile`, limites), `@Lob`, signature de fichier, lecture publique par
  UUID, `@Scheduled` / `@EnableScheduling` et délai de grâce, `FormData` côté
  Angular, composant qui reçoit un `FormControl`. Backend / Git / Pense-bête
  renumérotées 37 / 38 / 39 (liens internes compris) ; entrées au pense-bête
  pour les erreurs rencontrées.
- `progression-pedagogique.md` : Partie 15, et retrait de « envoi de fichier »
  des pistes si mentionné.

## Hors périmètre

Redimensionnement ou compression des images, recadrage de l'avatar, glisser-
déposer, envoi de plusieurs images par publication, images privées servies avec
le jeton, stockage sur disque ou service externe, et les défauts de la revue
déjà listés (dont la page d'édition de contact qui navigue avant la réponse du
serveur).

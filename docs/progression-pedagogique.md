# Progression pédagogique — État d'avancement et méthode

Ce fichier explique la logique suivie jusqu'ici dans l'apprentissage, pour que 
tu (Claude Code) poursuives dans la même philosophie plutôt que de repartir 
sur une approche différente.

## Philosophie de progression

- **Un concept à la fois.** Chaque étape introduit une seule notion nouvelle 
  (un signal, un service, un Observable...), jamais plusieurs à la fois, 
  même si ça aurait été plus rapide de tout écrire en un bloc.
- **Le "pourquoi" avant le "comment".** Avant de donner une syntaxe, expliquer 
  le problème concret qu'elle résout, avec une comparaison ou une analogie 
  si utile (ex: signal vs variable classique, service = entrepôt / composant 
  = rayon de magasin).
- **Confirmation avant de continuer.** Après chaque étape, attendre que 
  l'utilisateur confirme que ça fonctionne (souvent via une capture d'écran 
  de son résultat) avant de passer à la suite. Ne pas enchaîner plusieurs 
  étapes sans validation intermédiaire.
- **Diagnostiquer avant de corriger.** Face à une erreur, poser des questions 
  ciblées (contenu de la console, du fichier concerné) plutôt que de deviner 
  et de donner un correctif à l'aveugle.
- **Adapter au fil de l'eau.** Le tutoriel de départ a été écrit pour une 
  syntaxe Angular générique ; la version réelle installée (Angular 20+, 
  standalone components sans NgModule, signals, nouveaux noms de fichiers 
  sans suffixe .component) a nécessité d'adapter chaque étape en temps réel 
  à ce que l'utilisateur avait réellement sous les yeux, plutôt que de lui 
  faire suivre une doc obsolète.

## Ce qui a été fait, dans l'ordre, et pourquoi

### Partie 1 — Angular seul (en mémoire, avant tout backend)
Objectif : apprendre les bases d'Angular (composants, signals, services, 
formulaires, communication parent/enfant) sans la complexité d'un vrai 
serveur, pour isoler les concepts frontend avant d'ajouter une couche réseau.

1. Installation environnement (Node, Angular CLI, `ng new`)
2. Structure d'un composant (`app.ts`/`.html`/`.css`), premier binding 
   d'interpolation
3. Modèle de données (`interface Contact`)
4. Service avec signal (`ContactService`), pour comprendre le partage de 
   données entre composants avant d'introduire la complexité HTTP
5. Composant `ContactList` : lecture du signal, `@for`/`@if`, injection avec 
   `inject()`
6. Affichage du composant dans `App`
7. Composant `ContactForm` : formulaires réactifs, `Validators`
8. Communication enfant → parent avec `output()` / `.emit()`
9. Passage d'un signal simple à un `BehaviorSubject`-like pattern, puis 
   simplification en gardant les signals (choix final : signals partagés 
   via le service, pas de RxJS pour cette partie — RxJS introduit seulement 
   à la Partie 2, avec HttpClient)

### Partie 2 — Connexion à un vrai backend Spring Boot
Objectif : faire vivre les mêmes données côté serveur, avec persistance en 
base, et apprendre le fonctionnement d'une API REST des deux côtés (client 
et serveur).

9. Backend Spring Boot généré via l'extension VS Code Spring Initializr 
   (Maven, Java 21, dépendances Web + JPA + H2). Création manuelle, fichier 
   par fichier, dans l'ordre : entité (`Contact.java`) → repository 
   (`ContactRepository`) → contrôleur REST (`ContactController`), pour 
   suivre la même logique de couches que côté Angular (modèle → service → 
   composant)
10. Connexion Angular ↔ backend : `provideHttpClient()`, réécriture du 
    service pour retourner des Observables au lieu de manipuler un signal 
    directement, introduction du concept Observable vs signal (valeur 
    future vs valeur présente), `.subscribe()`, `ngOnInit()`
11. Limite assumée et documentée : chaque composant avait alors son propre
    signal local (plus de signal partagé comme en Partie 1), donc
    `window.location.reload()` utilisé temporairement après un ajout.
    **Limite levée depuis** — voir Partie 3 ci-dessous.

### Mise en place de l'outillage
12. Dépôt Git unique (monorepo) avec `.gitignore` couvrant les deux projets, 
    poussé sur GitHub
13. Documents `CLAUDE.md` et `docs/support-apprentissage-angular-spring.md` 
    mis en place pour la suite du travail avec Claude Code

### Partie 3 — Consolidation : le signal partagé alimenté par HttpClient
Objectif : supprimer le `window.location.reload()` hérité de la Partie 2, en
recombinant deux notions déjà connues séparément — le signal partagé de la
Partie 1 et l'Observable de la Partie 2. Volontairement choisie avant le
routing parce qu'elle n'introduit presque aucune notion neuve : elle consolide
en articulant l'existant. Seule vraie nouveauté : c'est désormais le service
qui s'abonne, plus le composant.

Migration menée en quatre étapes, chacune validée par l'utilisateur avant de
passer à la suivante, l'application restant fonctionnelle à chaque palier :

14. Le service devient propriétaire de la donnée : signal privé,
    `asReadonly()`, et `chargerContacts()` qui s'abonne lui-même. Aucun
    changement visible — la nouvelle plomberie est posée à côté de l'ancienne
15. `ContactList` abandonne son signal local et pointe vers celui du service
    (référence partagée, pas copie). Template inchangé
16. `addContact()` alimente le signal avec la **réponse du serveur**, qui
    seule porte l'id généré par la base : le `reload()` disparaît
17. `deleteContact()` met à jour le signal localement par `.filter()` au lieu
    de refaire un GET, puis nettoyage du code devenu mort (`getContacts()`,
    import `Observable`)

À la demande de l'utilisateur, chaque bloc de code du support est désormais
marqué `[DÉFINITIF]` ou `[PROVISOIRE]`, pour distinguer à la relecture le
pattern lui-même de l'échafaudage de transition. Cette convention est à
reprendre pour les prochaines migrations progressives.

### Partie 4 — Routing et page de détail d'un contact
Objectif : donner une URL propre à chaque contact, pour l'afficher seul et
rendre le bouton Retour du navigateur utilisable. Prolonge directement la
Partie 3 : la page de détail lit le **même** signal partagé que la liste,
sans une ligne de synchronisation.

Trois notions neuves, découpées en étapes validées une à une :

18. `App` devient une coquille (titre + `<router-outlet />`) ; le contenu de
    la page d'accueil part dans un nouveau composant `pages/accueil`.
    Première route `''` → `Accueil`. Distinction `pages/` (associées à une
    URL) vs `components/` (briques réutilisables)
19. Route `contact/:id` → `ContactDetail` ; les noms de la liste deviennent
    des `[routerLink]="['/contact', id]"`. Pourquoi pas `href` : il recharge
    tout et détruit le signal partagé
20. `ContactDetail` lit l'id via `ActivatedRoute.snapshot`, puis retrouve le
    contact avec `computed()` sur le signal partagé — recalcul automatique
    quand la réponse HTTP arrive. Étapes 3 et 4 du plan fusionnées : un id
    affiché seul aurait été un palier sans intérêt visible
21. Contrainte SSR : la route paramétrée passe en `RenderMode.Client` dans
    `app.routes.server.ts` (les id n'existent qu'à l'exécution)

Support : `computed()` ajouté en sous-section de la section 3, et une
section 13 « Routing Angular » complète.

### Partie 5 — Modification d'un contact
Objectif : formulaire pré-rempli + `PUT`. Réutilise le formulaire réactif, le
signal partagé et le routing ; donne un vrai usage à la page de détail (bouton
« Modifier »).

22. Backend : `PUT /api/contacts/{id}` combinant `@PathVariable` (id de
    l'URL, qui fait autorité) et `@RequestBody`
23. Service : `modifierContact()` met à jour le signal partagé via `.map()`
24. Page `pages/contact-edit`, route `contact/:id/modifier` : `FormBuilder`
    pré-rempli avec `patchValue()`, déclenché par un `effect()` quand les
    données du signal arrivent (drapeau booléen pour ne le faire qu'une fois).
    Retour à la fiche via `Router.navigate()`

Notions ajoutées au support : `effect()` en sous-section de la section 3,
`patchValue()` en sous-section de la section 7, `Router.navigate()` en
sous-section de la section 13, et une **section 14 « Modification d'une
ressource »** complète. Sections Backend/Git/Pense-bête renumérotées 15/16/17.

Sur demande de l'utilisateur : chaque notion du support est suivie d'un
encadré **« Dans le projet »** donnant le fichier réel (lien Markdown relatif
depuis `docs/`) et l'extrait de code correspondant, pour lire le support et le
code source en parallèle. Convention ajoutée à CLAUDE.md. **Appliquée à tout
le document** (sections 2 à 14) — 25 encadrés au total. À maintenir pour
chaque nouvelle notion.

### Partie 6 — Gestion des erreurs HTTP
Objectif : ne plus échouer en silence quand le backend est éteint ou renvoie
une erreur. Prolonge le service de la Partie 3.

25. `ContactService` : chaque appel passe par `.pipe(catchError(...))`.
    `chargerContacts` retombe sur `of([])` (liste vide de repli) ; les
    écritures retournent `EMPTY` (l'état local n'est pas touché en cas
    d'échec). Un second signal `erreur` (privé + `readonly`) porte le
    dernier message
26. `App` (la coquille) injecte `ContactService` et affiche une bannière
    tant que `erreur()` n'est pas `null` — affichage transverse assumé

Notions ajoutées au support : **section 15 « Gestion des erreurs HTTP
(`catchError`) »** — `.pipe()`, `catchError`, `of()` / `EMPTY` /
`throwError`, la forme objet de `.subscribe({ next, error })`, le signal
d'état d'erreur. Sections Backend/Git/Pense-bête renumérotées 16/17/18.

### Partie 7 — Indicateur de chargement
Objectif : rendre visible le délai d'une requête, et surtout **empêcher le
double-envoi** en désactivant les boutons pendant l'attente. Prolonge encore
le même service (troisième signal, à côté de `contacts` et `erreur`).

27. `ContactService` : signal `chargement` (privé + `readonly`), passé à
    `true` avant chaque appel HTTP et remis à `false` dans un
    `finalize()` du `.pipe()` — placé **après `catchError`** pour couvrir
    succès ET erreur sans duplication
28. Bannière « Chargement… » dans la coquille (`App`), même logique
    transverse que la bannière d'erreur
29. Boutons « Ajouter » / « Enregistrer » / « Supprimer » en
    `[disabled]` tant que `chargement()` est vrai — `ContactForm` doit
    pour cela injecter le service

Diagnostic marquant de cette partie : un `delay(1500)` `[PROVISOIRE]` a
servi à isoler « câblage cassé » de « requête trop rapide pour être vue » ;
la vraie cause du symptôme initial était que `ng serve` n'avait pas rechargé
le bundle (redémarrage + `Ctrl+Shift+R` nécessaires). À retenir comme
réflexe de débogage. Également compris : l'indicateur ne peut pas s'afficher
au chargement de la page, le `GET` initial partant côté serveur (SSR).

Notions ajoutées au support : **section 16 « Indicateur de chargement
(`finalize`) »**. Sections Backend/Git/Pense-bête renumérotées 17/18/19.

### Partie 8 — Intercepteurs HTTP
Objectif : sortir du service métier la plomberie transverse (indicateur de
chargement, message d'erreur) accumulée aux Parties 6 et 7. Partie demandée
« d'un seul coup » par l'utilisateur, sans validation intermédiaire — première
fois depuis le début du projet ; le découpage en étapes a quand même été
présenté et expliqué avant d'écrire le code.

Motivation venue du code lui-même : les quatre méthodes de `ContactService`
répétaient les trois mêmes lignes (`erreurSignal.set(null)`,
`chargementSignal.set(true)`, `finalize(...)`). Le problème mis en avant n'est
pas le volume dupliqué mais **l'oubli** — rien n'oblige une cinquième méthode
à les recopier.

30. Nouveau service transverse `services/etat-http.ts` (`EtatHttpService`),
    sans aucune notion de métier : il porte le compteur de requêtes et le
    message d'erreur. Notion neuve : un **compteur** + `computed()` au lieu
    d'un booléen, parce que l'intercepteur voit désormais des requêtes qui
    peuvent se chevaucher. Justification aussi donnée pour ne pas laisser ces
    signaux dans `ContactService` : boucle de dépendances intercepteur →
    service → `HttpClient` → intercepteur
31. `chargementInterceptor` : cas « observer sans modifier ». Reprend le
    `finalize()` des quatre méthodes
32. `erreurInterceptor` : cas « intercepter sans avaler ». Traduit
    `HttpErrorResponse.status` en message, puis **relance** l'erreur avec
    `throwError` — le point clé de la partie : l'intercepteur décide du
    MESSAGE (générique), le service garde la décision de la VALEUR DE REPLI
    (`of([])` vs `EMPTY`), lui seul sachant s'il lisait ou écrivait
33. `baseUrlInterceptor` : cas « modifier la requête ». `req.clone()` et
    l'immuabilité de `HttpRequest`. Le service passe à une URL relative
    (`/api/contacts`) et ne connaît plus l'adresse du backend
34. Enregistrement par `provideHttpClient(withInterceptors([...]))`, avec
    l'ordre de la chaîne expliqué (aller de haut en bas, retour en sens
    inverse)
35. Répercussions : `ContactService` passe de 90 à 59 lignes ; `App` n'injecte
    plus `ContactService` du tout (un affichage transverse dépend maintenant
    d'un état transverse) ; les trois composants à bouton lisent `chargement`
    depuis `EtatHttpService` ; `ContactForm` perd son import de
    `ContactService`, devenu mort

Contrepartie assumée et documentée : les messages d'erreur perdent leur
nuance métier (« Impossible d'ajouter le contact ») pour une nuance technique
(« Serveur injoignable », « Erreur interne du serveur (500) »). Jugé
globalement gagnant — l'utilisateur apprend *pourquoi* l'appel a échoué — avec
la porte de sortie notée dans le support : le `catchError` du service
s'exécutant après celui de l'intercepteur, il peut toujours réécrire le
message.

Vérification : `npx ng build` passe, prérendu SSR de la route d'accueil
compris — ce qui valide que l'intercepteur de base URL réécrit bien l'URL
relative avant l'appel réseau côté serveur (une URL relative partant vers le
réseau depuis le serveur aurait échoué).

Notions ajoutées au support : **section 17 « Intercepteurs HTTP »** —
`HttpInterceptorFn`, la signature `(req, next)`, la chaîne et son ordre,
`withInterceptors`, `req.clone()` et l'immuabilité, `HttpErrorResponse.status`,
`throwError`, le compteur dérivé par `computed()`, un tableau « qui décide
quoi » entre intercepteur et service, et une liste « ce qu'un intercepteur ne
doit pas faire ». Six entrées ajoutées au pense-bête. Sections Backend/Git/
Pense-bête renumérotées 18/19/20.

### Partie 9 — Comptes, messagerie, champs enrichis et refonte visuelle
Demande groupée de l'utilisateur : « la connexion pour avoir accès à ses
contacts », une messagerie interne, l'email professionnel, les réseaux
sociaux avec logos, la photo de profil, et une interface plus plaisante en
bleu et rouge pétants. Soit l'équivalent de quatre ou cinq parties d'un coup.

Rupture méthodologique assumée : au lieu d'étapes validées une à une, un
seul tour de **questions préalables** (4 questions) puis une livraison
complète. Motif : quatre décisions changeaient l'architecture assez
profondément pour que deviner aurait fait construire la mauvaise chose.
Réponses retenues :
- Messagerie **entre utilisateurs inscrits** (pas un message vers un contact,
  ni un envoi SMTP)
- **Spring Security + BCrypt + JWT** (plutôt qu'une session cookie ou une
  version maison), choix cohérent avec la Partie 8 : l'intercepteur
  d'authentification est le prolongement direct de la section 17
- Photo de profil par **URL** (pas d'upload de fichier)
- Thème **clair**, bleu en couleur principale, rouge réservé au destructeur

#### Backend
36. Dépendances : `spring-boot-starter-security` et JJWT 0.13.0 (déjà
    présents dans le dépôt Maven local). Spring Boot 4.1.1 → Spring
    Security 7.0.5, dont l'API de configuration a été vérifiée sur place
    plutôt que supposée
37. Entité `Utilisateur` (table renommée `utilisateur`, `user` étant réservé
    en SQL), entité `Message` (deux `@ManyToOne` vers le même type, d'où des
    `@JoinColumn` nommés). `Contact` gagne `emailPro`, `photoUrl`, six
    champs de réseaux, et un `@ManyToOne` vers son propriétaire
38. Pas d'entité « Conversation » : le fil entre A et B se déduit des
    messages. Pas de collection inverse dans `Utilisateur` non plus — évite
    la récursion Jackson et une collection chargée pour rien
39. `JwtService`, `JwtAuthFilter` (`OncePerRequestFilter`), `SecurityConfig`
    (stateless, CORS centralisé, `/api/auth/**` public). Vérification du mot
    de passe faite à la main par `passwordEncoder.matches()` : pas besoin de
    câbler un `AuthenticationManager`, et c'est plus lisible
40. Contrôleurs réécrits autour de `@AuthenticationPrincipal` : le
    propriétaire est **imposé par le serveur**, et les accès par id passent
    par `findByIdAndProprietaireId`
41. `application.properties` complété : H2 en mémoire nommée + console H2
    activée (le manque relevé en début de Partie 8), secret JWT surchargeable
    par variable d'environnement, exclusion de
    `UserDetailsServiceAutoConfiguration`

#### Frontend
42. `SessionService` (jeton + compte, `localStorage` protégé par
    `isPlatformBrowser`) séparé de `AuthService` (appels HTTP) — application
    directe de la leçon de la section 17 sur la boucle de dépendances
43. `authInterceptor` (quatrième intercepteur), `authGuard` (`CanActivateFn`),
    routes protégées, `RenderMode.Client` pour tout sauf `/connexion`
44. `erreurInterceptor` étendu : déconnexion automatique sur 401 **si un
    jeton existait**, et silence sur les appels `/api/auth/` (le formulaire
    affiche son propre message)
45. Pages `connexion` (un composant pour connexion et inscription),
    `messages` (liste des comptes + fil avec bulles), `profil` (nom affiché,
    photo, aperçu live)
46. Composant `reseaux-sociaux` : `input.required()`, `@switch`, SVG en
    ligne colorés par `currentColor`, variable CSS pilotée depuis le
    TypeScript. Les six réseaux viennent d'une constante `RESEAUX`, parcourue
    aussi bien à l'affichage qu'à la saisie (`[formControlName]` dynamique)
47. Refonte visuelle : `styles.css` devient un petit système de design
    (variables de couleurs, rayons, ombres ; styles de boutons, champs,
    cartes, grille auto-fit). En-tête à dégradé bleu→rouge avec navigation
    et pastille de non-lus

#### Vérifications faites
- Backend testé au `curl` de bout en bout : 401 sans jeton et avec jeton
  invalide, 409 sur email déjà pris, isolation effective entre deux comptes
  (404 quand Bob visait un contact d'Alice), messagerie avec marquage des
  lus, 400 sur message vide, et absence du mot de passe dans tous les JSON
- Deux défauts trouvés **par ces tests** et corrigés : 403 au lieu de 401
  (pas d'`AuthenticationEntryPoint`) et 403 au lieu de 404 (redispatch vers
  `/error` que `OncePerRequestFilter` ne rejoue pas)
- Frontend : `ng build` passe, et `ng serve` vérifié à l'exécution — rendu
  SSR de `/connexion`, coquille client pour les routes protégées, aucune
  erreur `localStorage`
- Non vérifié : le parcours réel dans un navigateur (aucun navigateur
  disponible dans l'environnement). À faire au premier lancement.

Notions ajoutées au support : **section 18 « Authentification »**,
**section 19 « Relations entre entités JPA »**, **section 20 « Composants
réutilisables : input() et boucles de configuration »**, **section 21 « Mise
en forme : variables CSS et cohérence visuelle »**. 14 entrées ajoutées au
pense-bête. Sections Backend/Git/Pense-bête renumérotées 22/23/24.

### Partie 10 — Les quatre chantiers de consolidation, plus les tests
Demande de l'utilisateur : traiter d'un coup les axes d'amélioration 2 à 6
identifiés en début de session — tests, rafraîchissement du jeton, messagerie
en temps réel, pagination/recherche, nuance des messages d'erreur.

Seconde livraison groupée d'affilée, mais pour un motif différent de la
Partie 9 : ici rien n'était ambigu, il n'y avait donc pas de questions
préalables à poser. En revanche l'**ordre** a été choisi contre celui de la
demande. Les tests, listés en premier, ont été écrits en **dernier** : les
quatre autres chantiers changeaient la forme de la réponse de l'API, la
signature de `session.ouvrir()` et le contenu des services. Des tests écrits
d'abord auraient été réécrits quatre fois.

#### Pagination et recherche (axe 5)
48. Backend : `@Query` avec `Pageable`, recherche sur nom OU prénom OU email,
    `Math.clamp` pour borner la taille demandée. Réponse enveloppée dans un
    `record PageContacts` maison plutôt que le `Page<T>` de Spring Data, dont
    la forme JSON n'est pas un contrat stable
49. **Conséquence non demandée mais nécessaire** : découper la liste rend
    fausse l'hypothèse « le signal contient tous les contacts ». Les pages
    détail et édition, qui cherchaient avec un `computed()` (Partie 4),
    affichaient « introuvable » pour un contact situé sur une autre page. D'où
    un `GET /api/contacts/{id}` et un signal `contactCourant`
50. Frontend : `Subject` + `switchMap` dans le service (réponses dans le
    désordre), `debounceTime` + `distinctUntilChanged` + `takeUntilDestroyed`
    dans le composant (une requête par frappe). Point clé répété deux fois
    ensuite : `catchError` **à l'intérieur** du `switchMap`
51. Autre conséquence : `addContact` / `deleteContact` ne peuvent plus mettre
    le signal à jour à la main — le découpage appartient au serveur

#### Contexte HTTP et messages d'erreur (axe 6)
52. `HttpContextToken` : `LIBELLE_ACTION` et `DISCRET`, plus un raccourci
    `contexte({ libelle, discret })`. Le service déclare une DONNÉE (ce qu'il
    faisait), l'intercepteur garde toute la LOGIQUE — la troisième voie entre
    « message générique » et « retour du `catchError` métier partout »
53. `erreurInterceptor` découpé en `raisonTechnique()` (fragment de phrase) et
    `messagePour()` (recollage). La contrepartie assumée en Partie 8 est donc
    levée sans réintroduire la duplication qui l'avait motivée
54. `DISCRET` sert aussi au chantier suivant : sortie anticipée dans
    `chargementInterceptor`, silence dans `erreurInterceptor`

#### Renouvellement du jeton (axe 3)
55. Jeton d'accès ramené de 24 h à **15 min**, plus un jeton de
    rafraîchissement de 7 jours. Choix structurant : ce second jeton est une
    valeur aléatoire **stockée en base** (`SecureRandom`, entité
    `JetonRafraichissement`), pas un second JWT. Motif pédagogique explicite :
    un second JWT serait tout aussi irrévocable, et ferait s'effondrer l'intérêt
    du montage. C'est le stockage serveur qui rend la révocation possible
56. **Rotation** à chaque usage : l'ancien est révoqué. Un jeton volé devient
    détectable au lieu d'être exploitable sept jours en silence
57. La déconnexion devient réelle (`POST /api/auth/deconnexion`) : jusqu'ici
    elle ne faisait qu'oublier le jeton côté navigateur
58. Cinquième intercepteur, placé **en dernier** — donc au plus profond, donc
    premier à voir l'erreur au retour, avant qu'`erreurInterceptor` ne
    déconnecte. L'ordre fait partie du comportement, et un test le vérifie
59. La vraie difficulté n'est pas l'appel mais la **concurrence** : trois 401
    simultanés lanceraient trois rotations, dont deux échoueraient. Résolu par
    un Observable mutualisé (`shareReplay(1)` + `finalize`)

#### Messagerie en temps réel (axe 4)
60. Sondage périodique par `timer(0, N)` + `switchMap` : 5 s pour le fil
    ouvert, 15 s pour la pastille de non-lus. Le compteur émis par `timer` sert
    à ne rendre discrètes que les requêtes à partir de la deuxième — le premier
    chargement, lui, vient d'un clic et mérite l'indicateur
61. Le sujet réel de la partie est le **cycle de vie**, pas RxJS : arrêt à la
    déconnexion (sinon un 401 toutes les 15 s), arrêt dans `ngOnDestroy`,
    garde-fou contre le double démarrage, et surtout garde `isPlatformBrowser`
    — un timer côté SSR empêche l'application d'être « stable » et le rendu ne
    se termine jamais

#### Tests (axe 2)
62. Backend, 28 tests : `JwtServiceTest` (unitaire pur, sans contexte Spring —
    l'injection par constructeur permet de fabriquer un jeton déjà périmé avec
    une durée négative), `ContactControllerTest` et `AuthControllerTest`
    (`@SpringBootTest` + `@AutoConfigureMockMvc` + `@Transactional`)
63. Frontend, 32 tests : `provideHttpClientTesting` pour les services et
    intercepteurs, bouchon d'`ActivatedRoute` pour les pages. Les six `.spec.ts`
    générés par le CLI étaient tous obsolètes (l'un référençait une classe
    `Contact` inexistante, un autre cherchait « Hello, carnet-contact ») :
    remplacés, pas rafistolés
64. Critère de choix appliqué : tester ce qui est **invisible** (isolation
    entre comptes, absence du mot de passe dans le JSON), ce qu'on **ne sait pas
    provoquer à la main** (jeton expiré, réponses dans le désordre, trois 401
    simultanés), et ce dont on a **justifié la forme précise** dans un
    commentaire (position du `catchError`, ordre des intercepteurs)

#### Vérifications faites
- `.\mvnw.cmd test` : 28 tests au vert. `npm test` : 32 tests au vert
- `ng build` passe, prérendu SSR compris
- Bout en bout au `curl` contre le backend réel : pagination (2 pages sur 8
  contacts), recherche insensible à la casse et sur les trois champs, taille
  bornée à 50 malgré `?taille=99999`, page hors limites qui rend une liste vide,
  `GET /contacts/{id}` en 200 / 404, rotation du jeton (l'ancien refusé en 401),
  déconnexion serveur (204 puis 401), isolation entre comptes
- `ng serve` interrogé à l'exécution : `/connexion`, `/` et `/messages`
  répondent en 200 en quelques dizaines de millisecondes — ce qui **prouve** que
  la garde `isPlatformBrowser` fonctionne, un timer parti côté serveur aurait
  fait expirer ces requêtes
- Non vérifié, toujours : le parcours réel dans un navigateur (aucun navigateur
  disponible dans l'environnement)

Deux incidents d'environnement à retenir : la version de Surefire épinglée par
Spring Boot 4.1.1 (3.5.6) n'était pas complète dans le dépôt Maven local, il a
fallu autoriser le réseau pour `mvnw test` ; et Spring Boot 4 embarque Jackson 3,
qui n'expose plus de bean `ObjectMapper` — les tests lisent le JSON avec
`JsonPath.read`.

Notions ajoutées au support : **section 22 « Pagination et recherche côté
serveur »**, **section 23 « Contexte d'une requête HTTP (`HttpContext`) »**,
**section 24 « Renouvellement du jeton d'accès »**, **section 25
« Rafraîchissement automatique (sondage périodique) »**, **section 26 « Tests
automatisés »**. 24 entrées ajoutées au pense-bête. Sections Backend/Git/
Pense-bête renumérotées 27/28/29.

Nouveauté de forme dans le support : quatre encadrés **« Note de mise à jour »**
insérés sous des exemples devenus faux (le `computed()` des sections 3 et 13, le
`chargerContacts()` de la section 14, la requête dérivée des sections 18 et 19).
Ils disent ce qui a changé et surtout ce qui n'a **pas** changé, plutôt que de
réécrire la section — pour qu'une relecture du document dans l'ordre reste
cohérente avec le code actuel. Convention à reprendre.

### Partie 11 — Mot de passe et notifications
Trois demandes de l'utilisateur, formulées ensemble : conditions de solidité à
la création d'un compte, possibilité d'afficher le mot de passe tapé, et
notification à l'arrivée d'un message.

Les deux premières touchent le même formulaire et se sont donc traitées d'un
bloc ; la troisième se greffe sur le sondage déjà en place depuis la Partie 10,
ce qui a réduit le travail à la détection et à l'affichage.

#### Mot de passe : la règle des deux côtés
65. `PolitiqueMotDePasse` côté Java (classe finale, constructeur privé,
    méthodes statiques) : 10 caractères, minuscule, majuscule, chiffre,
    caractère spécial. Elle renvoie la LISTE de ce qui manque, pas un booléen —
    « refusé » sans dire pourquoi oblige à deviner
66. Point pédagogique central de la partie : la validation du navigateur est un
    **confort d'interface**, celle du serveur une **sécurité**. Les deux sont
    nécessaires, pour des raisons différentes. D'où deux fichiers isolés
    (`PolitiqueMotDePasse.java` / `validateurs/mot-de-passe.ts`), faciles à
    comparer, et des tests symétriques des deux côtés
67. Second point, plus subtil : les règles de composition mesurent la FORME, pas
    la solidité. « Motdepasse1! » coche les cinq critères. D'où une petite liste
    de mots de passe trop courants, dont certaines entrées passent exprès tous
    les critères de forme — c'est ce qui rend la démonstration parlante
68. Validateur personnalisé Angular (`ValidatorFn`), avec la convention
    contre-intuitive `null = valide`. Il transporte la liste des critères
    manquants, pas seulement un drapeau
69. Une seule constante `CRITERES_MOT_DE_PASSE` sert à valider ET à afficher la
    liste à cocher — même principe que `RESEAUX` en Partie 9. Un test veille
    explicitement à ce que les deux usages ne puissent pas diverger
70. `setValidators()` + `updateValueAndValidity()` : la règle s'applique à
    l'inscription et PAS à la connexion. Justification : un compte créé sous
    l'ancienne politique doit pouvoir entrer, et on ne peut pas revalider un
    mot de passe existant puisqu'on n'en stocke que le haché
71. Ordre de vérification dans le contrôleur : 400 (requête mal formée) avant
    409 (conflit d'état). Un test existant a dû être corrigé, son mot de passe
    bidon tombant désormais sur le 400 avant d'atteindre le 409

#### Afficher le mot de passe
72. Signal `motDePasseVisible` + `[type]` en binding de propriété. Deux détails
    qui valaient d'être écrits noir sur blanc : `type="button"` obligatoire
    (sans lui le bouton soumet le formulaire — piège classique et silencieux)
    et `[attr.aria-pressed]` plutôt que `[aria-pressed]`, les attributs `aria-*`
    n'ayant pas de propriété DOM correspondante

#### Notifications
73. `NotificationService` avec DEUX canaux, parce qu'aucun ne suffit : la
    notification système est la seule visible quand l'onglet est en
    arrière-plan, mais elle exige une permission refusable définitivement ; le
    bandeau interne marche toujours mais seulement si la page est regardée.
    Règle retenue : système si `document.hidden` ET permission accordée,
    bandeau sinon
74. Quatrième valeur « indisponible » ajoutée aux trois de l'API, pour le cas où
    `Notification` n'existe pas (SSR, vieux navigateur) — sinon il faudrait
    tester `typeof Notification` à chaque usage
75. La permission se demande depuis un CLIC (bouton dans la page Profil) : les
    navigateurs ignorent une demande qui ne suit pas un geste. Et un refus est
    définitif côté site, d'où un `@switch` à quatre cas qui explique à
    l'utilisateur où il en est, plutôt qu'un bouton inopérant
76. Le vrai piège n'était pas l'affichage mais la DÉTECTION : le sondage renvoie
    à chaque tour la liste complète des non-lus. Sans mémoire, une alerte
    toutes les quinze secondes. Résolu par un `Set` d'identifiants déjà vus,
    remplacé (et non complété) à chaque réponse, plus un drapeau « premier
    tour » qui observe sans annoncer — et remis à zéro à la déconnexion

#### Vérifications faites
- Backend : 38 tests (dont 9 nouveaux sur la politique, avec
  `@ParameterizedTest`). Frontend : 58 tests (dont 26 nouveaux — validateur,
  service de notification, détection des nouveaux messages, formulaire)
- `ng build` passe, prérendu SSR compris
- Politique vérifiée au `curl` contre le vrai serveur : chaque critère manquant
  produit bien son message, et `Motdepasse1!` est refusé pour la seule raison
  d'être trop courant
- Non vérifié : l'affichage réel des notifications système, qui demande un vrai
  navigateur et une permission accordée à la main

Incident à retenir : la première série de `curl` a interrogé le backend que
l'utilisateur avait lancé de son côté — donc l'ancien code — et semblait montrer
que la politique ne s'appliquait pas. Le journal du serveur, lui, disait « Port
8080 was already in use ». Réflexe à garder : quand un résultat contredit le
code qu'on vient d'écrire, vérifier D'ABORD que c'est bien ce code qui tourne.
La vérification a été refaite sur le port 8099 pour ne pas couper le serveur de
l'utilisateur.

Notions ajoutées au support : **section 27 « Saisie et validation d'un mot de
passe »** et **section 28 « Notifications du navigateur »**. 13 entrées ajoutées
au pense-bête. Sections Backend/Git/Pense-bête renumérotées 29/30/31.

## Ce qui était prévu ensuite (pas encore fait)

### Pistes suivantes envisagées (mentionnées mais non détaillées)
- Parcours complet dans un vrai navigateur — la seule vérification jamais faite
  depuis le début du projet, et la seule façon de voir les notifications
  système à l'œuvre
- Messagerie : passer du sondage à un vrai temps réel (WebSocket ou SSE), ce qui
  supprimerait les requêtes inutiles quand rien ne change
- Persistance réelle : H2 est en mémoire, tout disparaît au redémarrage
- Tests : rien ne couvre encore la messagerie ni le composant `reseaux-sociaux`
- Accessibilité et navigation au clavier, jamais examinées

## Comment poursuivre cette philosophie

Si l'utilisateur demande de l'aide sur une nouvelle fonctionnalité, ne pas 
la générer entièrement d'un coup : proposer de la découper en étapes selon 
la même logique que ci-dessus (une notion à la fois, le pourquoi avant le 
comment, confirmation avant de continuer), sauf s'il demande explicitement 
d'aller plus vite ou de tout faire directement.

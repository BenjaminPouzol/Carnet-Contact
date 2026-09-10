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

## Ce qui était prévu ensuite (pas encore fait)

### Pistes suivantes envisagées (mentionnées mais non détaillées)
- Pagination et recherche côté backend
- Tests unitaires (fichiers `.spec.ts` déjà générés par le CLI, jamais 
  exploités jusqu'ici)
- Console H2 non activée : `application.properties` ne contient que
  `spring.application.name`, alors que la dépendance est présente. Aucun
  moyen d'inspecter la table en direct pour l'instant

## Comment poursuivre cette philosophie

Si l'utilisateur demande de l'aide sur une nouvelle fonctionnalité, ne pas 
la générer entièrement d'un coup : proposer de la découper en étapes selon 
la même logique que ci-dessus (une notion à la fois, le pourquoi avant le 
comment, confirmation avant de continuer), sauf s'il demande explicitement 
d'aller plus vite ou de tout faire directement.

# Cours Angular — construit pas à pas sur le carnet de contacts

Ce document raconte Angular **dans l'ordre où on l'apprend**, et non dans l'ordre où on le classe. Chaque étape part d'un problème concret que l'application ne sait pas encore résoudre, introduit la notion qui le résout, montre le code réel du carnet de contacts à ce moment précis de son histoire, puis prépare le problème suivant.

## À qui il s'adresse

À quelqu'un qui **n'a jamais fait d'Angular**, et qui n'est pas nécessairement à l'aise avec le développement web en général.

Aucun prérequis n'est supposé. Chaque terme technique est défini la première fois qu'il apparaît — y compris ceux que les tutoriels considèrent comme acquis : ce qu'est une classe, un décorateur, une interface, une dépendance, un Observable, ce que veut dire « asynchrone ». Quand un mot est employé pour la première fois, il est en **gras** et expliqué dans la phrase qui suit ou juste après.

Le document est donc volontairement long et bavard. C'est assumé : il vaut mieux relire un paragraphe qu'on connaît déjà que buter sur un mot que personne n'a pris la peine d'expliquer.

## Comment lire ce document

Il existe deux supports pour ce projet, et ils ne servent pas à la même chose :

| Document | Usage |
|---|---|
| **Ce cours** | Se lit **en avançant**. Une notion à la fois, dans l'ordre où elle est devenue nécessaire. |
| [`support-apprentissage-angular-spring.md`](support-apprentissage-angular-spring.md) | Se consulte **par notion**. Quand `finalize` ou `computed()` redevient flou, on y va directement. |

Chaque étape renvoie à la section correspondante du support pour le détail exhaustif. Le cours explique *pourquoi et quand*, le support explique *tout le reste*.

## Un parti pris : montrer aussi le code qui a disparu

C'est le point le plus important de ce document, et ce qui le distingue d'un tutoriel ordinaire.

Le carnet de contacts, tel qu'il est aujourd'hui, ne ressemble pas à ce qu'il était à l'étape 10. Des lignes ont été écrites, ont servi à comprendre quelque chose, puis ont été **supprimées** parce qu'une meilleure solution est arrivée. Un `window.location.reload()`, par exemple. Ou un signal d'erreur dans le service, plus tard déplacé dans un intercepteur.

Lire seulement le code final donnerait une fausse impression : celle d'une application qui serait née comme ça, d'un coup, avec ses intercepteurs et ses signaux bien rangés. Ce n'est jamais ainsi qu'on apprend, et ce n'est jamais ainsi qu'on écrit du logiciel. **On comprend pourquoi un intercepteur existe seulement après avoir écrit trois fois la même ligne dans trois méthodes différentes.**

Ce cours montre donc les deux : l'état intermédiaire tel qu'il était réellement, et ce qu'il est devenu. Les blocs de code portent l'un de ces marqueurs :

| Marqueur | Signification |
|---|---|
| **[ÉTAPE]** | Le code tel qu'il était à ce moment-là. Il a disparu depuis — c'était un palier, pas une destination. |
| **[DÉFINITIF]** | Le code est encore là aujourd'hui, éventuellement enrichi. |
| **[REMPLACÉ]** | Le code a été remplacé par une autre approche, expliquée plus loin. |

Le code des étapes intermédiaires n'est pas inventé : il est extrait de l'historique Git du projet. Chaque bloc précise le commit d'où il vient, pour que tu puisses le retrouver :

```bash
# Voir un fichier tel qu'il etait a un commit donne
git show 85aae2e:carnet-contact_frontend/src/app/services/contact.ts

# Voir ce qu'un commit a change
git show 41b9bcf --stat
```

## La carte du parcours

| Partie | Étapes | Ce qu'on y apprend |
|---|---|---|
| I — Les fondations | 1 à 8 | Angular seul, sans serveur : composant, signal, service, formulaire |
| II — Parler à un serveur | 9 à 10 | HttpClient, Observables, et la source de vérité partagée |
| III — Une vraie application | 11 à 15 | Routing, modification, erreurs, chargement, intercepteurs |
| IV — Sécuriser et monter en charge | 16 à 21 | Authentification, pagination, jetons, temps réel |
| V — Fiabiliser et finir | 22 à 26 | Tests, notifications, rôles, bibliothèque de composants, thème sombre |

---

## Sommaire

**Partie I — Les fondations d'Angular**

1. [Le projet et ses fichiers](#étape-1--le-projet-et-ses-fichiers)
2. [Le composant, brique de base](#étape-2--le-composant-brique-de-base)
3. [Le modèle de données](#étape-3--le-modèle-de-données)
4. [Le signal : une valeur que l'interface surveille](#étape-4--le-signal--une-valeur-que-linterface-surveille)
5. [Le service et l'injection de dépendances](#étape-5--le-service-et-linjection-de-dépendances)
6. [Afficher une liste : `@for` et `@if`](#étape-6--afficher-une-liste--for-et-if)
7. [Le formulaire réactif](#étape-7--le-formulaire-réactif)
8. [Faire remonter l'information : `output()`](#étape-8--faire-remonter-linformation--output)

**Partie II — Parler à un serveur**

9. [HttpClient et les Observables](#étape-9--httpclient-et-les-observables)
10. [Le signal partagé alimenté par HttpClient](#étape-10--le-signal-partagé-alimenté-par-httpclient)

**Partie III — Une vraie application**

11. [Le routing](#étape-11--le-routing)
12. [Modifier une ressource : `PUT` et `effect()`](#étape-12--modifier-une-ressource--put-et-effect)
13. [Quand le serveur ne répond pas : `catchError`](#étape-13--quand-le-serveur-ne-répond-pas--catcherror)
14. [L'indicateur de chargement : `finalize`](#étape-14--lindicateur-de-chargement--finalize)
15. [Les intercepteurs : dégraisser le service](#étape-15--les-intercepteurs--dégraisser-le-service)

**Partie IV — Sécuriser et monter en charge**

16. [L'authentification côté Angular](#étape-16--lauthentification-côté-angular)
17. [Un composant réutilisable : `input()`](#étape-17--un-composant-réutilisable--input)
18. [La mise en forme : les variables CSS](#étape-18--la-mise-en-forme--les-variables-css)
19. [Pagination et recherche : quand une hypothèse s'écroule](#étape-19--pagination-et-recherche--quand-une-hypothèse-sécroule)
20. [Renouveler le jeton sans déconnecter personne](#étape-20--renouveler-le-jeton-sans-déconnecter-personne)
21. [Le temps réel par sondage](#étape-21--le-temps-réel-par-sondage)

**Partie V — Fiabiliser et finir**

22. [Les tests automatisés](#étape-22--les-tests-automatisés)
23. [Mot de passe solide et notifications](#étape-23--mot-de-passe-solide-et-notifications)
24. [Rôles, administration et chargement différé](#étape-24--rôles-administration-et-chargement-différé)
25. [PrimeNG, les couches CSS et le mode sombre](#étape-25--primeng-les-couches-css-et-le-mode-sombre)
26. [Réactions et accusés de lecture](#étape-26--réactions-et-accusés-de-lecture)

---

## Avant de commencer : le vocabulaire minimum

Cette section n'est pas une étape du cours. C'est un socle : cinq notions qui ne sont pas propres à Angular, mais sans lesquelles la suite serait incompréhensible. Si tu connais déjà tout ça, passe directement à l'étape 1.

### Ce qui tourne où : navigateur et serveur

Une application web vit à **deux** endroits en même temps, et c'est la première source de confusion.

Le **navigateur** (Chrome, Firefox, Edge) exécute le code qui dessine l'écran et réagit aux clics. Ce code est téléchargé depuis Internet et tourne sur *ta* machine. C'est ce qu'on appelle le **frontend**, ou le « côté client ».

Le **serveur** est un programme qui tourne sur une autre machine, quelque part, et qui garde les données. Lui seul a accès à la base de données. C'est le **backend**, ou le « côté serveur ».

Les deux ne se parlent qu'en s'envoyant des messages par le réseau. Le navigateur demande (« donne-moi la liste des contacts »), le serveur répond. Cette conversation est le sujet de toute la Partie II de ce cours.

Dans ce projet : Angular est le frontend, Spring Boot est le backend. Ils tournent sur deux ports différents de ta machine — `4200` pour Angular, `8080` pour Spring Boot — ce qui est une façon de simuler deux machines distinctes sur un seul ordinateur.

### JavaScript et TypeScript

Le navigateur ne comprend qu'un seul langage de programmation : **JavaScript**. Tout ce qui s'exécute dans une page web finit en JavaScript, quoi qu'on ait écrit au départ.

JavaScript a un défaut connu : il ne vérifie **pas les types**. Rien ne l'empêche d'écrire `"bonjour" + 5`, ou de lire une propriété qui n'existe pas sur un objet. L'erreur ne se manifeste qu'à l'exécution, parfois très loin de sa cause, parfois seulement chez l'utilisateur.

**TypeScript** est du JavaScript auquel on a ajouté les types. On écrit :

```typescript
let age: number = 30;     // age contiendra un NOMBRE, et rien d'autre
age = "trente";           // ERREUR signalee immediatement, avant meme d'executer
```

TypeScript ne s'exécute pas directement : il est **transpilé**, c'est-à-dire traduit automatiquement en JavaScript, avant d'être envoyé au navigateur. Ce qu'on y gagne : les erreurs de type apparaissent pendant qu'on écrit, dans l'éditeur, et non chez l'utilisateur.

Angular est écrit en TypeScript et impose de l'utiliser. Tous les fichiers `.ts` de ce projet sont du TypeScript.

### Une classe, et pourquoi Angular en est rempli

Une **classe** est un plan de fabrication : elle décrit ce qu'un objet contient (ses **propriétés**) et ce qu'il sait faire (ses **méthodes**).

```typescript
class Voiture {
  // Une PROPRIETE : une donnee que chaque voiture possede.
  marque: string = 'Renault';

  // Une METHODE : une action que chaque voiture sait faire.
  // Le mot-cle `this` designe « la voiture sur laquelle on agit ».
  klaxonner(): void {
    console.log(this.marque + ' fait pouet');
  }
}

// On FABRIQUE un objet a partir du plan : c'est une « instance ».
const maVoiture = new Voiture();
maVoiture.klaxonner();   // affiche « Renault fait pouet »
```

En Angular, presque tout est une classe : un composant est une classe, un service est une classe. Tu écriras donc beaucoup de classes, mais — et c'est une particularité importante d'Angular — **tu ne feras presque jamais `new` toi-même**. C'est le framework qui fabrique les objets pour toi. L'étape 5 explique pourquoi.

### Un décorateur

Un **décorateur** est une annotation qu'on place juste au-dessus d'une classe, précédée d'un `@`. Il ne change pas ce que la classe fait : il ajoute des informations *à propos* de la classe, à destination du framework.

```typescript
@Component({ selector: 'app-truc' })   // <- le decorateur
export class Truc { }
```

L'analogie la plus proche est l'étiquette sur un colis. Le contenu du colis est la classe ; l'étiquette dit au transporteur quoi en faire. Sans `@Component`, la classe `Truc` ne serait qu'une classe TypeScript ordinaire, qu'Angular ignorerait complètement. C'est le décorateur qui la déclare comme composant.

Les deux décorateurs de ce cours sont `@Component` (« ceci est un morceau d'interface ») et `@Injectable` (« ceci est un service »).

### Synchrone et asynchrone

Un code **synchrone** s'exécute ligne par ligne : la ligne 2 attend que la ligne 1 soit finie.

```typescript
const a = 2 + 2;          // fini instantanement
console.log(a);           // s'execute juste apres, a vaut 4
```

Certaines opérations, en revanche, prennent du temps sans qu'on sache combien : demander des données à un serveur, par exemple. Ça peut durer 20 millisecondes, ou trois secondes, ou échouer. Si le programme *attendait* la réponse, la page entière serait figée pendant ce temps — plus rien ne réagirait, ni les clics ni le défilement.

D'où l'**asynchrone** : on lance l'opération, on continue immédiatement à autre chose, et on fournit à l'avance un morceau de code à exécuter *quand* la réponse arrivera.

```typescript
console.log('1 : je demande les contacts');
demanderAuServeur(reponse => {
  // Ce bloc s'executera PLUS TARD, quand la reponse arrivera.
  console.log('3 : les voici', reponse);
});
console.log('2 : je continue sans attendre');

// Ordre reel d'affichage : 1, puis 2, puis 3.
```

Ce décalage — « 2 » s'affiche avant « 3 » alors qu'il est écrit après — est le point qui surprend le plus au début. Il explique aussi une grande partie de ce cours : à l'étape 9, une liste s'affichera vide pendant un instant avant de se remplir, et il faudra que l'interface sache gérer ça.

---

# Partie I — Les fondations d'Angular

Cette première partie construit une application qui **ne parle à aucun serveur**. Les contacts vivent en mémoire, dans le navigateur, et disparaissent au moindre rafraîchissement de la page.

C'est volontaire, et c'est un choix pédagogique qu'il faut comprendre. Ajouter un serveur dès le départ introduirait d'un coup le réseau, l'asynchrone, la gestion des pannes et le format des échanges — quatre difficultés qui n'ont rien à voir avec Angular lui-même. En les remettant à la Partie II, on peut apprendre les vraies briques d'Angular (composant, signal, service, formulaire) sur des données qu'on maîtrise entièrement.

> **Note sur le code de cette partie.** L'état « tout en mémoire » n'a jamais été enregistré dans Git : le premier commit du projet contenait déjà le backend. Les blocs de code de cette partie sont donc **reconstitués** à partir du parcours réellement suivi, tel que [`progression-pedagogique.md`](progression-pedagogique.md) le décrit (Partie 1, étapes 1 à 9). À partir de l'étape 9, tout le code présenté est extrait de l'historique Git et vérifiable.

---

## Étape 1 — Le projet et ses fichiers

### Le problème

Une application Angular, ce n'est pas un fichier HTML qu'on ouvre dans un navigateur. C'est une arborescence d'une centaine de fichiers, dont la plupart sont de la configuration. Avant d'écrire une seule ligne utile, il faut savoir **lesquels comptent** et par où le programme démarre.

### Les outils : Node, npm, et le CLI

Trois noms reviennent constamment.

**Node.js** est un programme qui sait exécuter du JavaScript *en dehors* d'un navigateur. On ne s'en sert pas pour l'application elle-même — celle-ci tourne dans le navigateur — mais pour tous les outils de développement : transpiler le TypeScript, lancer un serveur local, exécuter les tests.

**npm** (*Node Package Manager*) est installé avec Node. C'est le gestionnaire de **dépendances** : les bibliothèques de code écrites par d'autres dont ton projet a besoin. Angular lui-même est une dépendance. Elles sont listées dans un fichier `package.json`, et `npm install` les télécharge toutes dans un dossier `node_modules/` — dossier qu'on ne versionne jamais dans Git, puisqu'il se reconstruit à la commande.

**Le CLI Angular** (`ng`) est l'outil en ligne de commande d'Angular. Son rôle est d'éviter d'écrire à la main la plomberie répétitive. Créer un composant demanderait sinon d'écrire soi-même le décorateur, les trois fichiers, et de les relier correctement ; `ng generate` fait tout en une commande, et surtout **de la même façon à chaque fois**. C'est autant un gain de temps qu'une garantie de cohérence.

```bash
# Creer un projet (a faire une seule fois)
ng new mon-projet

# Installer les dependances listees dans package.json
npm install

# Lancer le serveur de developpement : il recompile a chaque sauvegarde
ng serve

# Fabriquer la version optimisee, prete a etre mise en ligne
ng build

# Generer un composant, un service (voir les etapes suivantes)
ng generate component components/mon-composant
ng generate service services/mon-service
```

Quand `ng serve` a fini de démarrer, il affiche une ligne de ce genre :

```
➜  Local:   http://localhost:4200/
Watch mode enabled. Watching for file changes...
```

`localhost` désigne ta propre machine, et `4200` est le **port** — un numéro qui permet à plusieurs programmes de cohabiter sur la même machine sans se gêner. « Watch mode » signifie que l'outil surveille tes fichiers : à chaque sauvegarde, il recompile et le navigateur se met à jour tout seul.

### Les fichiers qui comptent

```
carnet-contact_frontend/
├── package.json          <- la liste des dependances
├── angular.json          <- la configuration de compilation
└── src/
    ├── index.html        <- LA page HTML, unique
    ├── main.ts           <- le point de depart du programme
    ├── styles.css        <- les styles GLOBAUX
    └── app/
        ├── app.config.ts <- la configuration de l'application
        ├── app.routes.ts <- la correspondance URL -> page
        ├── app.ts        <- le composant racine
        ├── app.html
        └── app.css
```

### Comment le programme démarre, dans l'ordre

C'est une chaîne de quatre maillons, et la comprendre évite beaucoup de perplexité par la suite.

**1. `index.html`** est la seule page HTML de toute l'application. Son corps est presque vide :

```html
<body>
  <app-root></app-root>
</body>
```

`<app-root>` n'est pas une balise HTML standard — elle n'existe dans aucune spécification. C'est un **emplacement** : Angular va y injecter toute l'interface. Cette idée d'une page unique dans laquelle tout est construit par du code porte un nom, **SPA** (*Single Page Application*) : la page n'est jamais rechargée, c'est son contenu qui change.

**2. `main.ts`** est le premier fichier exécuté :

```typescript
import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { App } from './app/app';

// « bootstrap » : demarrer. On dit a Angular « lance l'application, en
// partant du composant App, avec cette configuration ».
bootstrapApplication(App, appConfig)
  .catch((err) => console.error(err));
```

**3. `app.config.ts`** liste ce dont l'application a besoin pour fonctionner. Chaque entrée du tableau `providers` active une capacité :

```typescript
export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),      // active la navigation par URL (etape 11)
    provideHttpClient()         // active les appels reseau (etape 9)
  ]
};
```

Ce tableau est un fil rouge du cours : il va s'allonger à presque chaque partie. À la fin, il contiendra aussi les intercepteurs, le thème de la bibliothèque de composants, et l'hydratation côté serveur.

**4. `app.ts`** est le **composant racine**, le sommet de l'arbre. Tout le reste de l'interface sera contenu dedans, directement ou indirectement.

### Ce que cette étape a introduit

| Notion | Rôle |
|---|---|
| `npm install` | Télécharger les dépendances listées dans `package.json` |
| `ng serve` | Serveur de développement, recompilation automatique |
| `ng build` | Version optimisée pour la mise en ligne |
| `ng generate` | Créer un composant ou un service sans écrire la plomberie |
| `index.html` + `<app-root>` | L'unique page, et l'emplacement où Angular s'installe |
| `main.ts` | Le point de départ : `bootstrapApplication` |
| `app.config.ts` | Ce que l'application active (`providers`) |

> Pour le détail des commandes et de leurs options : section 1 du [support](support-apprentissage-angular-spring.md#1-outils-en-ligne-de-commande).

---

## Étape 2 — Le composant, brique de base

### Le problème

Une interface complète — barre de navigation, formulaire, liste, fiche de détail — écrite dans un seul fichier devient très vite illisible. Et si deux endroits de l'application doivent afficher la même chose, il faudrait dupliquer le code, avec la certitude qu'une des deux copies finira par diverger de l'autre.

### La notion

Un **composant** est une portion d'écran autonome, avec son affichage, sa logique et son style réunis. Une application Angular n'est au fond rien d'autre qu'un **arbre de composants** imbriqués : le composant racine en contient d'autres, qui en contiennent d'autres à leur tour.

Chaque composant sépare toujours trois responsabilités dans trois fichiers :

```
mon-composant.ts     -> la logique : que faire quand on clique, ou chercher les donnees
mon-composant.html   -> l'affichage : le « template », a quoi ca ressemble
mon-composant.css    -> le style, ISOLE a ce composant
```

L'isolation du CSS mérite qu'on s'y arrête, parce qu'elle résout un problème réel. En CSS ordinaire, une règle `.titre { color: red }` s'applique à *tous* les éléments de classe `titre` de toute la page. Deux développeurs qui choisissent le même nom de classe dans deux parties différentes du site se marchent dessus. Angular évite ça : le CSS écrit dans `mon-composant.css` ne peut toucher que les éléments de `mon-composant.html`, jamais ceux d'un autre composant. Le mécanisme exact est expliqué à l'étape 25.

### La syntaxe

```typescript
import { Component } from '@angular/core';

@Component({
  // Le nom de la balise HTML que ce composant cree.
  selector: 'app-mon-composant',

  // Ce dont SON PROPRE template a besoin (voir plus bas).
  imports: [],

  templateUrl: './mon-composant.html',
  styleUrl: './mon-composant.css'
})
export class MonComposant {
  // La logique vient ici : proprietes et methodes.
}
```

Le `selector` définit une balise personnalisée. Une fois déclarée, on l'utilise dans n'importe quel autre template exactement comme une balise HTML native :

```html
<app-mon-composant></app-mon-composant>
```

Le tableau `imports` demande une explication, parce qu'il est la cause de l'erreur la plus fréquente en début d'apprentissage. Chaque composant Angular moderne — dit **standalone**, « autonome » — déclare lui-même, dans ce tableau, tout ce dont *son propre template* a besoin : les autres composants qu'il affiche, les directives qu'il utilise.

C'était différent avant : tout se déclarait une fois pour toutes dans un fichier central appelé `NgModule`. L'approche autonome rend chaque composant compréhensible isolément — en lisant ses `imports`, on sait immédiatement de quoi il dépend, sans remonter dans une configuration globale.

**Utiliser un composant dans un autre demande donc trois gestes indissociables :**

```typescript
// 1. L'importer (au sens TypeScript : rendre le nom disponible dans ce fichier)
import { ContactList } from './components/contact-list/contact-list';

@Component({
  // 2. Le declarer dans imports (au sens Angular : autoriser le template a l'employer)
  imports: [ContactList],
  templateUrl: './accueil.html'
})
export class Accueil { }
```

```html
<!-- 3. Utiliser la balise -->
<app-contact-list></app-contact-list>
```

Oublier le geste 2 est extrêmement courant, d'autant que l'éditeur ajoute souvent le geste 1 automatiquement. Angular refuse alors de compiler : il ne « connaît » pas cette balise. Le message d'erreur mentionne le nom de la balise inconnue — c'est le réflexe à avoir : vérifier les `imports`.

### Afficher une valeur : l'interpolation

Pour afficher dans le template une valeur venue de la classe, on l'entoure de doubles accolades. Ça s'appelle l'**interpolation**.

```typescript
export class MonComposant {
  titre = 'Mon carnet';
}
```

```html
<h1>{{ titre }}</h1>
```

Angular lit la propriété `titre` de la classe et la place dans le HTML. Et surtout : si sa valeur change plus tard, l'affichage se met à jour tout seul. Comment Angular sait-il qu'elle a changé ? C'est exactement le sujet de l'étape 4.

### Dans le projet

L'arbre des composants du carnet, à la fin du parcours : `App` (la coquille : en-tête, navigation) → `Accueil` (la page) → `ContactForm` + `ContactList` (les briques).

**[DÉFINITIF]** — [`components/contact-list/contact-list.ts`](../carnet-contact_frontend/src/app/components/contact-list/contact-list.ts)

```typescript
@Component({
  selector: 'app-contact-list',
  imports: [RouterLink, ReactiveFormsModule, ReseauxSociaux, /* ... */],
  templateUrl: './contact-list.html',
  styleUrl: './contact-list.css'
})
export class ContactList implements OnInit { /* ... */ }
```

Les trois gestes se lisent côte à côte dans [`pages/accueil/accueil.ts`](../carnet-contact_frontend/src/app/pages/accueil/accueil.ts) et [`accueil.html`](../carnet-contact_frontend/src/app/pages/accueil/accueil.html) :

```typescript
// accueil.ts
imports: [ContactList, ContactForm],
```

```html
<!-- accueil.html -->
<app-contact-form (contactAjoute)="ajouterContact($event)"></app-contact-form>
<app-contact-list></app-contact-list>
```

> Le `(contactAjoute)` de cette dernière ligne est une notion de l'étape 8. Pour l'instant, retenir seulement que le formulaire et la liste sont deux composants distincts, posés l'un sous l'autre.

### Une convention de nommage à connaître

Le projet range ses composants dans deux dossiers, et la distinction est utile :

| Dossier | Contenu | Exemple |
|---|---|---|
| `pages/` | Un composant associé à une **URL** | `pages/accueil`, `pages/contact-detail` |
| `components/` | Une brique **réutilisable**, sans URL propre | `components/contact-list` |

Rien dans Angular n'impose cette séparation. C'est une convention de projet, et elle aide : en voyant un dossier, on sait si le composant est une destination de navigation ou un morceau assemblable.

### Ce que cette étape a introduit

| Notion | Rôle |
|---|---|
| `@Component` | Décorateur qui déclare une classe comme composant |
| `selector` | Le nom de la balise HTML créée |
| `imports` | Ce que le template du composant est autorisé à employer |
| `templateUrl` / `styleUrl` | Les fichiers HTML et CSS associés |
| `{{ valeur }}` | Interpolation : afficher une donnée de la classe |
| CSS isolé | Le style d'un composant ne fuit pas vers les autres |

> Détail complet : section 2 du [support](support-apprentissage-angular-spring.md#2-composants-angular).

---

## Étape 3 — Le modèle de données

### Le problème

Un contact, dans le code, c'est un objet avec un nom, un prénom, un email. Rien n'empêche d'écrire :

```typescript
const contact = { nom: 'Dupont', prenom: 'Jean', emial: 'jean@exemple.fr' };
//                                                ^^^^^ faute de frappe
```

En JavaScript, ce code fonctionne. L'objet aura une propriété `emial`, et le jour où l'affichage cherchera `contact.email`, il trouvera `undefined` — une valeur vide. Aucune erreur, aucun avertissement : juste un champ vide à l'écran, et une demi-heure à chercher pourquoi.

### La notion

Une **interface** TypeScript est un contrat : elle décrit la forme qu'un objet doit avoir. Elle ne contient aucun code exécutable et disparaît complètement à la transpilation — elle n'existe que pour le compilateur, qui s'en sert pour vérifier ton travail pendant que tu écris.

```typescript
export interface Contact {
  id: number;         // obligatoire, doit etre un nombre
  nom: string;        // obligatoire, doit etre une chaine
  telephone?: string; // le ? signifie FACULTATIF : peut etre absent
}
```

Avec cette interface, la faute de frappe précédente est signalée immédiatement dans l'éditeur, avant même d'enregistrer le fichier :

```typescript
const contact: Contact = { nom: 'Dupont', emial: '...' };
// Erreur : la propriete 'emial' n'existe pas sur le type 'Contact'.
// Erreur : la propriete 'id' est manquante.
```

Le mot-clé `export` mérite un mot : il rend l'interface utilisable depuis d'autres fichiers. Sans lui, elle resterait privée à son fichier. C'est le pendant de l'`import` vu à l'étape 2 — l'un expose, l'autre consomme.

### Le `?` et ce qu'il implique

Marquer une propriété facultative avec `?` a une conséquence qu'on oublie souvent : TypeScript t'obligera désormais à envisager son absence.

```typescript
interface Contact {
  nom: string;
  telephone?: string;
}

function afficher(c: Contact) {
  console.log(c.telephone.length);   // ERREUR : c.telephone peut etre undefined
  console.log(c.telephone?.length);  // OK : le ?. renvoie undefined au lieu de planter
}
```

Le `?.` s'appelle le **chaînage optionnel**. Il signifie « si ce qui précède existe, continue ; sinon, donne `undefined` et arrête-toi là ». C'est un outil qu'on retrouvera partout dans ce projet, notamment dans les templates : `auth.utilisateur()?.nomAffichage`.

### Dans le projet

**[ÉTAPE]** — le modèle initial, au premier commit (`b66a87a`). Cinq champs, tous obligatoires.

```typescript
// contact.model.ts
export interface Contact {
  id: number;
  nom: string;
  prenom: string;
  email: string;
  telephone: string;
}
```

**[DÉFINITIF]** — [`contact.model.ts`](../carnet-contact_frontend/src/app/contact.model.ts) aujourd'hui. Le modèle a grossi : email professionnel, photo, six réseaux sociaux — et presque tout est devenu facultatif, parce qu'exiger six réseaux sociaux pour enregistrer un contact n'aurait aucun sens.

Ce grossissement illustre une règle utile : **un modèle de données n'est jamais figé**. Il suit les besoins de l'application, et TypeScript est là précisément pour que chaque ajout de champ signale tous les endroits du code qui doivent en tenir compte. On en verra une démonstration spectaculaire à l'étape 22, quand deux champs ajoutés casseront quatre fichiers de test d'un coup.

### Où ranger ces fichiers

Le projet place ses interfaces à la racine de `src/app/`, dans des fichiers `*.model.ts` :

```
src/app/
├── contact.model.ts
├── utilisateur.model.ts
└── message.model.ts
```

C'est une convention, pas une obligation. Son intérêt : un modèle est partagé par les composants, les services et les tests — le mettre à la racine évite de le faire appartenir à l'un d'eux en particulier.

### Ce que cette étape a introduit

| Notion | Rôle |
|---|---|
| `interface` | Décrire la forme d'un objet ; vérifiée à la compilation, absente à l'exécution |
| `export` | Rendre une déclaration utilisable depuis un autre fichier |
| `?` sur une propriété | La rendre facultative — et forcer à envisager son absence |
| `?.` | Chaînage optionnel : lire sans planter si l'objet est absent |

---

## Étape 4 — Le signal : une valeur que l'interface surveille

### Le problème

L'étape 2 affichait une valeur avec `{{ titre }}`. Mais que se passe-t-il quand cette valeur **change** ?

```typescript
export class MonComposant {
  titre = 'Mon carnet';

  renommer(): void {
    this.titre = 'Autre titre';   // le HTML se met-il a jour ?
  }
}
```

La question paraît triviale, elle ne l'est pas. Pour rafraîchir l'écran, le framework doit *savoir* que quelque chose a changé. Or une affectation ordinaire (`this.titre = ...`) ne prévient personne : c'est une écriture en mémoire, silencieuse.

Historiquement, Angular résolvait ça en **re-vérifiant tout**. À chaque clic, chaque réponse réseau, chaque minuteur, il reparcourait l'ensemble des valeurs affichées pour voir lesquelles avaient bougé. Ça fonctionne, mais c'est du travail proportionnel à la taille de l'application entière, même quand une seule ligne a changé.

### La notion

Un **signal** est une valeur qui **prévient quand elle change**. Au lieu de stocker la donnée dans une propriété muette, on la range dans un petit conteneur qui tient la liste de ceux qui la lisent.

C'est un renversement complet : ce n'est plus Angular qui part à la recherche des changements, c'est la donnée qui signale le sien. Le travail devient proportionnel à ce qui a réellement bougé.

L'analogie la plus juste est celle d'un abonnement. Une variable ordinaire est un tableau noir : si personne ne regarde au bon moment, le changement passe inaperçu. Un signal est une liste de diffusion : quiconque l'a lu est prévenu automatiquement.

### La syntaxe

```typescript
import { signal } from '@angular/core';

// CREER un signal, avec sa valeur de depart.
compteur = signal(0);

// LIRE : on APPELLE le signal comme une fonction. Les parentheses sont
// obligatoires — sans elles, on recupere le conteneur, pas son contenu.
console.log(this.compteur());        // 0

// ECRIRE, deux facons :
this.compteur.set(5);                // remplacer par une valeur connue
this.compteur.update(n => n + 1);    // calculer a partir de l'ancienne
```

Dans un template, on lit un signal exactement pareil, avec ses parenthèses :

```html
<p>{{ compteur() }}</p>
```

C'est la source d'erreur numéro un avec les signals. Écrire `{{ compteur }}` sans parenthèses n'affiche pas d'erreur : ça affiche quelque chose comme `function computed()`, ou `[object Object]`. Le réflexe à prendre : **un signal se lit toujours avec `()`**.

### `set` ou `update` : lequel choisir

```typescript
// set : la nouvelle valeur ne depend pas de l'ancienne.
this.contacts.set(listeVenueDuServeur);

// update : la nouvelle valeur est CALCULEE depuis l'ancienne.
this.contacts.update(liste => [...liste, nouveauContact]);
```

`update` n'est pas qu'un raccourci. Il garantit qu'on part de la valeur réellement en place au moment de l'exécution. Avec `set`, on écrirait :

```typescript
this.contacts.set([...this.contacts(), nouveau]);   // lire PUIS ecrire
```

et on introduit un intervalle — court, mais réel — entre la lecture et l'écriture, pendant lequel la valeur pourrait avoir changé. `update` supprime cet intervalle.

### La règle d'immutabilité

Voici un piège qui coûte cher, et qu'il faut comprendre une fois pour toutes.

```typescript
// CE QUI NE MARCHE PAS
this.contacts.update(liste => {
  liste.push(nouveau);   // on modifie le tableau EXISTANT
  return liste;          // ... et on renvoie le meme tableau
});
```

Ce code ajoute bien l'élément, mais **l'écran ne se met pas à jour**. La raison : pour décider s'il doit rafraîchir, Angular compare l'ancienne valeur et la nouvelle. Pour un tableau, cette comparaison porte sur la **référence** — l'adresse en mémoire — et non sur le contenu. Le tableau modifié sur place a la même adresse qu'avant : Angular conclut que rien n'a changé.

Il faut donc renvoyer un **nouveau** tableau :

```typescript
// CE QUI MARCHE
this.contacts.update(liste => [...liste, nouveau]);            // ajout
this.contacts.update(liste => liste.filter(c => c.id !== id)); // suppression
this.contacts.update(liste => liste.map(c => c.id === maj.id ? maj : c)); // remplacement
```

Le `...` s'appelle l'**étalement** (*spread*) : `[...liste, nouveau]` crée un tableau neuf contenant tous les éléments de `liste`, puis `nouveau`. Et `filter` comme `map` renvoient d'eux-mêmes des tableaux neufs — ce sont les bons outils ici, là où `push`, `splice` ou `sort` modifient sur place.

Cette règle vaut aussi pour les objets :

```typescript
// Nouvel objet, avec une propriete changee.
this.utilisateur.update(u => ({ ...u, nomAffichage: 'Nouveau nom' }));
```

> Les parenthèses autour de `{ ... }` ne sont pas décoratives : sans elles, JavaScript lirait l'accolade comme le début d'un bloc de code et non comme un objet.

### `asReadonly()` : donner à lire sans donner à écrire

```typescript
// Le signal modifiable reste PRIVE : seule cette classe peut l'ecrire.
private contactsSignal = signal<Contact[]>([]);

// La version exposee : lisible par tous, modifiable par personne d'autre.
readonly contacts = this.contactsSignal.asReadonly();
```

Ce couple de deux lignes revient partout dans le projet, et son intérêt est architectural. Si n'importe quel composant pouvait écrire dans la liste des contacts, une incohérence à l'écran deviendrait très difficile à diagnostiquer : il faudrait inspecter tous les composants pour trouver le coupable. Avec cette discipline, **il n'existe qu'un seul endroit au monde qui puisse modifier cette donnée**. Le bug est forcément là.

### La valeur dérivée : `computed()`

Certaines valeurs ne sont pas des données mais des **conséquences**. « Combien de contacts ? » n'est pas une information à stocker : c'est la longueur de la liste. La stocker séparément créerait deux sources qu'il faudrait penser à synchroniser — et qu'on oubliera.

```typescript
import { computed, signal } from '@angular/core';

contacts = signal<Contact[]>([]);

// computed() calcule une valeur DERIVEE. Il se recalcule tout seul des qu'un
// des signaux qu'il lit change, et JAMAIS autrement.
nombre = computed(() => this.contacts().length);
connecte = computed(() => this.jeton() !== null);
```

Un `computed` se lit comme un signal (`nombre()`), mais **ne s'écrit pas** : il n'a ni `set` ni `update`. C'est voulu — sa valeur n'appartient pas à celui qui la lit, elle découle de ses sources.

| | `signal()` | `computed()` |
|---|---|---|
| Contient | Une valeur qu'on fixe soi-même | Une valeur déduite d'autres signaux |
| Se modifie | `.set()`, `.update()` | Jamais directement — recalcul automatique |
| Rôle | Source de vérité | Vue dérivée d'une ou plusieurs sources |

La règle de décision : **si tu peux la calculer, ne la stocke pas.** Deux sources qui pourraient se désynchroniser valent toujours moins qu'une seule dont on dérive.

### L'effet de bord : `effect()`

Troisième et dernier outil de la famille. `computed()` *calcule* une valeur ; `effect()` *fait* quelque chose.

```typescript
import { effect } from '@angular/core';

constructor() {
  // Ce bloc s'execute une premiere fois, puis A CHAQUE FOIS qu'un signal
  // qu'il lit change.
  effect(() => {
    console.log('Le theme est maintenant', this.theme());
  });
}
```

Un `effect` sert quand il faut agir **hors d'Angular** en réaction à un changement : écrire dans le navigateur, démarrer un minuteur, poser un attribut sur la page. On le verra à l'étape 12 (pré-remplir un formulaire à l'arrivée des données) et à l'étape 25 (appliquer le thème sombre).

Deux précautions valables dès maintenant :

- un `effect()` doit être créé dans un **contexte d'injection** — en pratique, dans le `constructor` de la classe, ou comme propriété de classe ;
- il est **différé** : il ne s'exécute pas à la ligne où on l'écrit, mais un peu après. C'est ce qui piégera les tests à l'étape 22.

### Dans le projet

**[ÉTAPE]** — au tout début, le titre de l'application était un signal, dans [`app.ts`](../carnet-contact_frontend/src/app/app.ts) :

```typescript
export class App {
  protected readonly title = signal('carnet-contact');
}
```

```html
<h1>{{ title() }}</h1>
```

Ce signal a disparu depuis (l'en-tête affiche un titre fixe), mais il illustre bien le point de départ : **la première chose qu'on met dans un signal, c'est la plus simple possible.**

**[DÉFINITIF]** — le couple privé/lecture seule de [`services/contact.ts`](../carnet-contact_frontend/src/app/services/contact.ts) :

```typescript
private contactsSignal = signal<Contact[]>([]);
readonly contacts = this.contactsSignal.asReadonly();
```

Et un `computed` de [`services/session.ts`](../carnet-contact_frontend/src/app/services/session.ts), qui dit bien « ceci est une conséquence, pas une donnée » :

```typescript
// « Connecte » n'est pas une donnee a stocker, c'est une consequence de la
// presence d'un jeton.
readonly connecte = computed(() => this.jetonSignal() !== null);
```

### Ce que cette étape a introduit

| Notion | Rôle |
|---|---|
| `signal(valeur)` | Créer une valeur qui prévient de ses changements |
| `monSignal()` | Lire — les parenthèses sont obligatoires |
| `.set(v)` | Remplacer par une valeur connue |
| `.update(f)` | Calculer la nouvelle valeur depuis l'ancienne |
| `[...liste, x]` | Renvoyer un **nouveau** tableau : sans ça, pas de rafraîchissement |
| `.asReadonly()` | Exposer en lecture seule, garder l'écriture privée |
| `computed(f)` | Valeur dérivée, recalculée automatiquement |
| `effect(f)` | Effet de bord déclenché par un changement |

> Détail complet : section 3 du [support](support-apprentissage-angular-spring.md#3-signals).

---

## Étape 5 — Le service et l'injection de dépendances

### Le problème

La liste des contacts doit être visible par le composant qui l'affiche, et modifiable par celui qui ajoute un contact. Si chacun garde sa propre copie, les deux divergent immédiatement : le formulaire ajoute à sa liste, la liste affiche la sienne, et rien ne bouge à l'écran.

Il faut donc que la donnée vive **ailleurs** que dans les composants — à un endroit unique auquel tous accèdent.

### La notion

Un **service** est une classe qui ne dessine rien. Elle détient des données et de la logique, et les met à disposition de qui en a besoin.

L'analogie qui marche bien : le service est l'**entrepôt**, les composants sont les **rayons du magasin**. Plusieurs rayons peuvent présenter le même produit ; il n'y a qu'un seul stock. Quand le stock bouge, tous les rayons le reflètent.

```typescript
import { Injectable, signal } from '@angular/core';

@Injectable({
  // « providedIn: root » : Angular cree UNE SEULE instance de ce service pour
  // toute l'application, et la donne a tous ceux qui la demandent. C'est ce
  // qu'on appelle un « singleton » — un seul exemplaire.
  providedIn: 'root'
})
export class ContactService {
  private contactsSignal = signal<Contact[]>([]);
  readonly contacts = this.contactsSignal.asReadonly();

  ajouter(contact: Contact): void {
    this.contactsSignal.update(liste => [...liste, contact]);
  }
}
```

`providedIn: 'root'` est le point clé. Sans lui, chaque composant recevrait sa *propre* instance du service — et on retomberait exactement sur le problème qu'on voulait résoudre.

### L'injection de dépendances

Une **dépendance** est simplement quelque chose dont ton code a besoin pour fonctionner. Un composant qui affiche des contacts dépend de `ContactService`.

La façon naïve de se la procurer serait de la fabriquer soi-même :

```typescript
export class ContactList {
  private contactService = new ContactService();   // NE JAMAIS FAIRE CA
}
```

Ce code compile et paraît raisonnable. Il est pourtant faux, et pour une raison qui va au cœur de l'architecture Angular : `new` crée un **nouvel** objet. Chaque composant aurait le sien, avec sa propre liste vide. Le singleton serait anéanti.

Angular fonctionne autrement. On ne fabrique pas ses dépendances : on les **demande**, et le framework les fournit. C'est l'**injection de dépendances**.

```typescript
import { inject } from '@angular/core';

export class ContactList {
  // « Donne-moi le ContactService de l'application. » Angular le fabrique s'il
  // n'existe pas encore, et renvoie l'exemplaire existant dans le cas contraire.
  private contactService = inject(ContactService);
}
```

Trois bénéfices, dont le dernier n'apparaîtra qu'à l'étape 22 :

1. **Le singleton est respecté** : tous reçoivent le même exemplaire, donc la même donnée.
2. **Le composant ne sait pas comment construire son service.** Si `ContactService` gagne demain une dépendance supplémentaire, aucun composant n'a à changer.
3. **On peut remplacer une dépendance.** Dans un test, on fournira un faux service, et le composant testé ne s'en apercevra pas.

`inject()` ne s'appelle pas n'importe où : il lui faut un **contexte d'injection**. En pratique, ça veut dire comme propriété de classe (le cas courant) ou dans le `constructor`. L'appeler dans une méthode ordinaire lève une erreur.

### Dans le projet

**[ÉTAPE — reconstitué]** — la toute première version du service, avant tout serveur. La liste vit en mémoire et rien de plus.

```typescript
@Injectable({ providedIn: 'root' })
export class ContactService {
  private contactsSignal = signal<Contact[]>([
    { id: 1, nom: 'Dupont', prenom: 'Jean', email: 'jean@exemple.fr', telephone: '0601020304' }
  ]);
  readonly contacts = this.contactsSignal.asReadonly();

  addContact(contact: Contact): void {
    this.contactsSignal.update(liste => [...liste, contact]);
  }

  deleteContact(id: number): void {
    this.contactsSignal.update(liste => liste.filter(c => c.id !== id));
  }
}
```

Cette version est **simple et fausse** — fausse parce que tout disparaît au rafraîchissement de la page. Mais elle est pédagogiquement précieuse : elle isole la notion de service et de signal partagé, sans le bruit du réseau.

Et voici ce qui va être instructif pour la suite : à l'étape 9, cette architecture propre va être **cassée** par l'arrivée du serveur, avant d'être reconstruite à l'étape 10.

**[DÉFINITIF]** — [`services/contact.ts`](../carnet-contact_frontend/src/app/services/contact.ts) et [`components/contact-list/contact-list.ts`](../carnet-contact_frontend/src/app/components/contact-list/contact-list.ts) :

```typescript
export class ContactList implements OnInit {
  private contactService = inject(ContactService);

  // Une REFERENCE vers le signal du service, pas une copie.
  contacts = this.contactService.contacts;
}
```

Cette ligne mérite d'être lue lentement. `contacts` n'est pas une copie de la liste : c'est le même objet signal. Quand le service l'écrit, le composant lit la nouvelle valeur — sans une ligne de synchronisation.

### Ce que cette étape a introduit

| Notion | Rôle |
|---|---|
| `@Injectable({ providedIn: 'root' })` | Déclarer un service, en un seul exemplaire pour l'application |
| `inject(MonService)` | Demander une dépendance au framework |
| Singleton | Un exemplaire unique, donc une seule source de vérité |
| Ne jamais faire `new` sur un service | `new` casse le singleton |

> Détail complet : section 4 du [support](support-apprentissage-angular-spring.md#4-services-et-injection-de-dépendances).

---

## Étape 6 — Afficher une liste : `@for` et `@if`

### Le problème

On a une liste de contacts dans un signal. Il faut en afficher un bloc HTML par contact — sans savoir combien il y en aura, ni s'il y en aura.

Le HTML ne sait pas faire de boucle. C'est un langage de description, pas de programmation : il n'a ni `for` ni `if`.

### La notion

Angular étend le HTML avec des **blocs de contrôle**, reconnaissables à leur `@`. Ils s'écrivent directement dans le template et pilotent ce qui est affiché.

```html
<!-- Afficher un bloc PAR ELEMENT d'une liste -->
@for (element of maListe(); track element.id) {
  <p>{{ element.nom }}</p>
}

<!-- Afficher conditionnellement -->
@if (condition) {
  <p>Vrai</p>
} @else {
  <p>Faux</p>
}
```

> Cette syntaxe est récente. Beaucoup de tutoriels et de réponses trouvées en ligne utilisent encore l'ancienne forme, `*ngFor` et `*ngIf`, écrite comme un attribut de balise. Les deux font la même chose ; la forme `@` est celle à employer aujourd'hui. Si tu tombes sur `*ngIf`, tu sais que la source est antérieure à Angular 17.

### `track` : la partie qui compte vraiment

`track` est obligatoire dans un `@for`, et ce n'est pas une formalité. Il indique à Angular **comment reconnaître un élément** d'un affichage au suivant.

Sans cette information, Angular ne pourrait pas savoir ce qui s'est passé entre deux versions de la liste. Supposons qu'on supprime le premier de trois contacts. Angular reçoit une ancienne liste de trois éléments et une nouvelle de deux. A-t-on supprimé le premier ? Modifié les deux premiers et supprimé le dernier ? Sans repère, il ne peut que tout détruire et tout reconstruire.

Avec `track contact.id`, il compare des identités : « l'élément 1 a disparu, les éléments 2 et 3 sont inchangés ». Il retire une seule ligne du document et ne touche pas aux autres.

Les conséquences sont très concrètes :

- **Les performances** : reconstruire mille lignes à chaque changement est perceptible.
- **L'état perdu** : si une ligne contient un champ de saisie à demi rempli, ou une case cochée, la reconstruction l'efface. Avec un `track` correct, la ligne n'est pas touchée et l'état survit.

```html
<!-- BIEN : un identifiant unique et stable -->
@for (contact of contacts(); track contact.id) { }

<!-- ACCEPTABLE en dernier recours : la position dans la liste -->
@for (contact of contacts(); track $index) { }
```

`$index` est un repli médiocre : la position d'un élément change dès qu'on trie ou insère. On l'utilise seulement quand les éléments n'ont aucun identifiant — une liste de chaînes de caractères, par exemple.

### Les variables utiles d'un `@for`

```html
@for (contact of contacts(); track contact.id) {
  <p>{{ $index }} — {{ contact.nom }}</p>
}
```

| Variable | Contenu |
|---|---|
| `$index` | La position, à partir de 0 |
| `$first` / `$last` | `true` pour le premier / le dernier |
| `$even` / `$odd` | Position paire / impaire — pratique pour alterner les couleurs |
| `$count` | Le nombre total d'éléments |

### `@if` avec `as` : éviter de tester deux fois

Cette forme est très employée dans le projet et vaut la peine d'être comprise :

```html
@if (contact(); as c) {
  <h2>{{ c.prenom }} {{ c.nom }}</h2>
  <p>{{ c.email }}</p>
}
```

On lit : « si `contact()` a une valeur, range-la dans `c` et affiche ce bloc ». Sans le `as`, il faudrait écrire `contact()!.prenom`, `contact()!.email`... en appelant le signal à chaque ligne, et en ajoutant un `!` pour promettre à TypeScript que la valeur n'est pas vide — une promesse que le compilateur ne peut pas vérifier.

Avec `as`, TypeScript **sait** que `c` existe dans ce bloc. Le code est plus court et mieux vérifié.

### Le bloc `@empty`

```html
@for (contact of contacts(); track contact.id) {
  <li>{{ contact.nom }}</li>
} @empty {
  <p>Aucun contact enregistré.</p>
}
```

`@empty` s'affiche quand la liste est vide. Il remplace avantageusement un `@if (liste().length === 0)` écrit à côté : la condition et son contraire restent dans la même structure, donc impossible de modifier l'un en oubliant l'autre.

### Dans le projet

**[ÉTAPE]** — le premier template de la liste, au commit `b66a87a`. Volontairement sans mise en forme : à ce stade, seule la mécanique d'affichage était en jeu.

```html
<!-- contact-list.html, version initiale -->
<h2>Mes contacts</h2>

@if (contacts().length === 0) {
  <p>Aucun contact enregistré.</p>
} @else {
  <ul>
    @for (contact of contacts(); track contact.id) {
      <li>
        {{ contact.prenom }} {{ contact.nom }} — {{ contact.email }} — {{ contact.telephone }}
        <button (click)="supprimer(contact.id)">Supprimer</button>
      </li>
    }
  </ul>
}
```

Noter le `@if ... @else` autour du `@for` : c'est ce qu'on écrit spontanément avant de connaître `@empty`.

**[DÉFINITIF]** — [`contact-list.html`](../carnet-contact_frontend/src/app/components/contact-list/contact-list.html) aujourd'hui. Le même `@for`, avec un affichage bien plus riche : photo ou initiale, coordonnées, liens vers les réseaux sociaux, et un lien vers la fiche de détail.

**[DÉFINITIF]** — un `@if ... as` typique, dans [`pages/contact-detail/contact-detail.html`](../carnet-contact_frontend/src/app/pages/contact-detail/contact-detail.html) :

```html
@if (contact(); as c) {
  <h2>{{ c.prenom }} {{ c.nom }}</h2>
} @else {
  <p>Contact introuvable (ou en cours de chargement).</p>
}
```

Le « ou en cours de chargement » de ce message est révélateur : à ce stade du projet, on ne savait pas distinguer « le contact n'existe pas » de « la réponse du serveur n'est pas encore arrivée ». C'est le genre d'imprécision qui se résoudra plus tard, aux étapes 13 et 14.

### Ce que cette étape a introduit

| Notion | Rôle |
|---|---|
| `@for (x of liste(); track x.id)` | Répéter un bloc par élément |
| `track` | Comment reconnaître un élément — performances **et** état préservé |
| `@empty` | Le cas de la liste vide, dans la même structure |
| `@if` / `@else` | Affichage conditionnel |
| `@if (x(); as y)` | Tester et nommer en une fois, avec le type garanti |
| `$index`, `$first`, `$last`… | Les variables implicites d'une boucle |

> Détail complet : section 5 du [support](support-apprentissage-angular-spring.md#5-syntaxe-de-template-if--for).

---

## Étape 7 — Les bindings et le formulaire réactif

Cette étape est double, parce que les deux notions sont indissociables en pratique : un formulaire n'est qu'un assemblage de bindings. On commence donc par la mécanique générale, puis on l'applique.

### 7.1 — Les bindings : faire circuler l'information

#### Le problème

Jusqu'ici, l'information ne circulait que dans un sens et d'une seule façon : `{{ valeur }}` prend une donnée de la classe et l'affiche comme **texte**.

Ça ne suffit pas. Il faut aussi pouvoir :

- donner une valeur à un **attribut** HTML (l'adresse d'une image, l'état désactivé d'un bouton) ;
- réagir à ce que fait l'utilisateur (un clic, une saisie).

#### La notion

Un **binding** (« liaison ») est un pont entre la classe TypeScript et le template. Angular en propose trois formes, et chacune a une syntaxe distincte qu'on reconnaît d'un coup d'œil.

```html
<!-- 1. INTERPOLATION : la classe -> le template, comme texte -->
<h1>{{ titre }}</h1>

<!-- 2. PROPERTY BINDING : la classe -> le template, comme VALEUR -->
<img [src]="urlPhoto" />
<button [disabled]="formulaireInvalide">Envoyer</button>

<!-- 3. EVENT BINDING : le template -> la classe -->
<button (click)="supprimer(contact.id)">Supprimer</button>
<form (ngSubmit)="onSubmit()">
```

Les crochets et les parenthèses ne sont pas décoratifs, et on peut les mémoriser par leur forme : les **crochets `[ ]` pointent vers l'intérieur** de l'élément — la donnée y entre ; les **parenthèses `( )` sont ouvertes vers l'extérieur** — l'événement en sort.

#### Pourquoi `[src]` et pas `src="{{ ... }}"`

La question se pose légitimement : les deux semblent marcher.

```html
<img src="{{ urlPhoto }}" />    <!-- fonctionne, mais... -->
<img [src]="urlPhoto" />        <!-- a preferer -->
```

La différence est que l'interpolation produit toujours une **chaîne de caractères**. Pour une URL, ça passe. Mais pour une valeur qui n'est pas du texte, ça casse :

```html
<!-- CE QUI NE MARCHE PAS -->
<button disabled="{{ false }}">    <!-- l'attribut vaut la chaine "false"... -->
<!-- ...et en HTML, la simple PRESENCE de l'attribut disabled desactive le
     bouton, quelle que soit sa valeur. Le bouton reste donc desactive. -->

<!-- CE QUI MARCHE -->
<button [disabled]="false">        <!-- la PROPRIETE recoit le booleen false -->
```

Le fond de l'affaire : un **attribut** HTML est du texte écrit dans le document, alors qu'une **propriété** est une valeur de l'objet que le navigateur construit à partir du document. `[x]` écrit la propriété, avec son vrai type. C'est presque toujours ce qu'on veut.

#### Le cas particulier des attributs `aria-*`

Il existe une exception, et on la rencontre dès qu'on soigne l'accessibilité :

```html
<!-- Les attributs aria-* n'ont PAS de propriete correspondante sur l'objet
     DOM. [attr.] force l'ecriture de l'attribut lui-meme. -->
<button [attr.aria-pressed]="motDePasseVisible()">Afficher</button>
```

#### `$event` : récupérer ce qui s'est passé

```html
<app-contact-form (contactAjoute)="ajouterContact($event)"></app-contact-form>
```

`$event` est une variable fournie par Angular qui contient **la donnée transportée par l'événement**. Pour un `(click)`, c'est l'événement de souris du navigateur. Pour un événement personnalisé — c'est le cas ici, on le verra à l'étape 8 — c'est la valeur qu'on a choisi d'émettre.

### 7.2 — Le formulaire réactif

#### Le problème

Un formulaire n'est pas seulement un ensemble de champs. Il faut, pour chacun d'eux, savoir : quelle est sa valeur à cet instant ? est-elle valide ? l'utilisateur l'a-t-il seulement touché ? Et pour le formulaire entier : peut-on l'envoyer ?

Écrire ça à la main, champ par champ, produit énormément de code répétitif et facile à désynchroniser.

#### La notion

Angular propose deux approches des formulaires. Ce projet emploie exclusivement les **formulaires réactifs**, et c'est le bon choix par défaut.

L'idée : le formulaire est décrit **dans la classe TypeScript**, pas dans le HTML. Le template ne fait que se brancher sur cette description. Ainsi, la structure, les règles de validation et les valeurs vivent à un seul endroit — et sont testables sans même afficher le formulaire.

```typescript
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

@Component({
  // ReactiveFormsModule doit figurer dans les imports, sinon [formGroup] et
  // formControlName ne sont pas reconnus dans le template.
  imports: [ReactiveFormsModule],
  // ...
})
export class MonFormulaire {
  // FormBuilder est un service : il se demande par inject(), comme tout service.
  private fb = inject(FormBuilder);

  contactForm = this.fb.group({
    // Pour chaque champ : [valeur de depart, regles de validation]
    nom: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    telephone: ['']                  // aucune regle : facultatif
  });
}
```

Côté template, trois branchements suffisent :

```html
<!-- [formGroup] relie le <form> a l'objet decrit dans la classe -->
<form [formGroup]="contactForm" (ngSubmit)="onSubmit()">

  <!-- formControlName relie CE champ a CETTE cle du groupe -->
  <input formControlName="nom" placeholder="Nom" />
  <input formControlName="email" placeholder="Email" />

  <!-- Le bouton se desactive tout seul tant que le formulaire est invalide -->
  <button type="submit" [disabled]="contactForm.invalid">Ajouter</button>
</form>
```

> `(ngSubmit)` plutôt que `(click)` sur le bouton : `ngSubmit` se déclenche aussi quand l'utilisateur appuie sur Entrée dans un champ. C'est le comportement attendu d'un formulaire, et l'obtenir gratuitement évite d'y penser.

#### Les états d'un champ

Chaque champ — et le formulaire entier — porte en permanence plusieurs drapeaux :

| Propriété | Signification |
|---|---|
| `valid` / `invalid` | Les règles de validation sont-elles satisfaites ? |
| `touched` / `untouched` | L'utilisateur a-t-il visité puis quitté le champ ? |
| `dirty` / `pristine` | La valeur a-t-elle été modifiée ? |
| `value` | La valeur actuelle |

`touched` a un usage précis et important pour la qualité de l'interface :

```html
@if (contactForm.get('email')?.invalid && contactForm.get('email')?.touched) {
  <p class="erreur-champ">Format d'adresse invalide.</p>
}
```

Sans la condition `touched`, le message « format invalide » s'afficherait **dès la première lettre tapée** — ce qui est agressif et faux : l'utilisateur est en train d'écrire, il n'a pas encore terminé. En attendant qu'il ait *quitté* le champ, on ne signale l'erreur qu'au moment où elle en est vraiment une.

#### Lire et écrire dans le formulaire depuis le code

```typescript
// Lire l'ensemble des valeurs
const valeurs = this.contactForm.value;

// Tout remplacer (tous les champs doivent etre fournis)
this.contactForm.setValue({ nom: 'Dupont', email: 'x@y.fr', telephone: '' });

// Remplir PARTIELLEMENT : les cles absentes sont laissees telles quelles
this.contactForm.patchValue({ nom: 'Dupont' });

// Vider le formulaire
this.contactForm.reset();
```

`patchValue` sera l'outil de l'étape 12, pour pré-remplir un formulaire d'édition avec les données venues du serveur.

#### Dans le projet

**[ÉTAPE]** — le premier formulaire, au commit `b66a87a`. Quatre champs, aucune mise en forme.

```typescript
// contact-form.ts, version initiale
export class ContactForm {
  private fb = inject(FormBuilder);

  contactForm = this.fb.group({
    nom: ['', Validators.required],
    prenom: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    telephone: ['']
  });

  onSubmit(): void {
    if (this.contactForm.valid) {
      this.contactAjoute.emit(this.contactForm.value as Contact);
      this.contactForm.reset();
    }
  }
}
```

```html
<!-- contact-form.html, version initiale -->
<form [formGroup]="contactForm" (ngSubmit)="onSubmit()">
  <div><input formControlName="nom" placeholder="Nom" /></div>
  <div><input formControlName="prenom" placeholder="Prénom" /></div>
  <div><input formControlName="email" placeholder="Email" /></div>
  <div><input formControlName="telephone" placeholder="Téléphone" /></div>
  <button type="submit" [disabled]="contactForm.invalid">Ajouter</button>
</form>
```

**[DÉFINITIF]** — [`components/contact-form/contact-form.ts`](../carnet-contact_frontend/src/app/components/contact-form/contact-form.ts). Le même squelette, avec treize champs, une section repliable pour les champs facultatifs, et des messages d'erreur conditionnés par `touched`.

Le formulaire le plus intéressant du projet est cependant celui de la connexion, à l'étape 23 : ses règles de validation **changent en cours de route** selon qu'on se connecte ou qu'on crée un compte.

### Ce que cette étape a introduit

| Notion | Rôle |
|---|---|
| `{{ x }}` | Interpolation : afficher une donnée comme texte |
| `[propriete]="x"` | Property binding : donner une valeur typée à un élément |
| `[attr.aria-x]="y"` | Écrire un vrai attribut, pour ceux qui n'ont pas de propriété |
| `(evenement)="methode()"` | Event binding : réagir à une action |
| `$event` | La donnée transportée par l'événement |
| `FormBuilder` + `fb.group()` | Décrire un formulaire dans la classe |
| `Validators.required`, `.email` | Les règles de validation |
| `[formGroup]`, `formControlName` | Brancher le template sur la description |
| `(ngSubmit)` | Soumission, clavier compris |
| `valid`, `touched`, `value` | Les états qui pilotent l'affichage |
| `patchValue`, `reset` | Écrire dans le formulaire depuis le code |

> Détail complet : sections 6 et 7 du [support](support-apprentissage-angular-spring.md#6-bindings).

---

## Étape 8 — Faire remonter l'information : `output()`

### Le problème

Le formulaire et la liste sont deux composants **frères** : ni l'un ni l'autre ne contient l'autre. Quand le formulaire produit un nouveau contact, comment la liste l'apprend-elle ?

La tentation serait que le formulaire écrive directement dans la liste. C'est une mauvaise idée, et pas seulement par principe : le formulaire deviendrait inutilisable ailleurs. Un composant qui connaît son voisin ne peut plus être déplacé.

### La notion

Un composant enfant ne connaît pas son parent, et c'est ce qui le rend réutilisable. Il se contente d'**annoncer** qu'il s'est passé quelque chose, en émettant un événement. Le parent décide de ce qu'il en fait.

```typescript
import { output } from '@angular/core';

export class ContactForm {
  // Declare un evenement personnalise, qui transportera un Contact.
  // Le nom choisi ici devient le nom de l'evenement dans le template parent.
  contactAjoute = output<Contact>();

  onSubmit(): void {
    if (this.contactForm.valid) {
      // « J'ai fini, voici le resultat. » Ce composant ignore
      // completement ce qui va en etre fait.
      this.contactAjoute.emit(this.contactForm.value as Contact);
      this.contactForm.reset();
    }
  }
}
```

Côté parent, on écoute cet événement comme n'importe quel autre — avec des parenthèses :

```html
<app-contact-form (contactAjoute)="ajouterContact($event)"></app-contact-form>
```

```typescript
export class Accueil {
  private contactService = inject(ContactService);

  // $event contient ce qui a ete passe a .emit() : ici, le Contact.
  ajouterContact(contact: Contact): void {
    this.contactService.addContact(contact);
  }
}
```

Noter la symétrie avec les bindings de l'étape 7 : `(click)` écoute un événement du navigateur, `(contactAjoute)` écoute un événement qu'on a créé. Angular ne fait aucune différence entre les deux, et c'est ce qui rend la notion facile à retenir.

### Le sens de circulation

C'est le point à graver :

| Sens | Outil | Étape |
|---|---|---|
| Parent → enfant | `input()` | Étape 17 |
| Enfant → parent | `output()` | Celle-ci |
| Entre composants quelconques | Un **service** partagé | Étape 5 |

La troisième ligne est la plus importante en pratique. `input` et `output` conviennent quand les composants sont **voisins directs**. Dès qu'il faut traverser trois niveaux, ou communiquer entre deux branches éloignées de l'arbre, faire remonter puis redescendre l'information devient un cauchemar — chaque composant intermédiaire devrait relayer une donnée qui ne le concerne pas.

C'est exactement pour ça que le service existe. Et c'est la raison pour laquelle, à l'étape 10, l'événement `contactAjoute` **cessera de transporter la donnée jusqu'à la liste** : le service deviendra la source de vérité, et la liste n'aura plus besoin d'être prévenue.

### Dans le projet

**[ÉTAPE]** — au commit `b66a87a`, la chaîne complète était : `ContactForm` émet → `App` reçoit → `App` appelle le service → et, faute de mieux, **rechargeait toute la page**.

```typescript
// app.ts, version initiale — le reload qu'on va supprimer a l'etape 10
ajouterContact(contact: Contact): void {
  this.contactService.addContact(contact).subscribe(() => {
    window.location.reload();
  });
}
```

**[DÉFINITIF]** — [`components/contact-form/contact-form.ts`](../carnet-contact_frontend/src/app/components/contact-form/contact-form.ts) et [`pages/accueil/accueil.ts`](../carnet-contact_frontend/src/app/pages/accueil/accueil.ts). L'`output` est toujours là, mais le parent se contente désormais de passer la main au service :

```typescript
// accueil.ts
ajouterContact(contact: Contact): void {
  this.contactService.addContact(contact);
}
```

### Ce que cette étape a introduit

| Notion | Rôle |
|---|---|
| `output<T>()` | Déclarer un événement personnalisé, qui transporte un `T` |
| `.emit(valeur)` | Émettre — sans savoir qui écoute |
| `(monEvenement)="..."` | Écouter côté parent, comme un événement natif |
| Découplage | L'enfant annonce, le parent décide : l'enfant reste réutilisable |

> Détail complet : section 8 du [support](support-apprentissage-angular-spring.md#8-communication-entre-composants).

---

### Bilan de la Partie I

À ce stade, l'application sait afficher une liste de contacts, en ajouter et en supprimer. Tout fonctionne — et tout disparaît au moindre rafraîchissement de la page, puisque les données ne vivent que dans la mémoire du navigateur.

Les briques acquises :

```
Composant  -> une portion d'ecran autonome (@Component, selector, imports)
Modele     -> la forme des donnees (interface)
Signal     -> une valeur qui previent de ses changements
Service    -> la source de verite unique, obtenue par inject()
Template   -> @for, @if, {{ }}, [x], (y)
Formulaire -> decrit dans la classe, branche sur le HTML
output()   -> l'enfant annonce, le parent decide
```

La Partie II ajoute le serveur. Et la première chose qu'elle va faire, c'est **casser l'architecture propre** qu'on vient de construire — avant de la reconstruire en mieux.

---

# Partie II — Parler à un serveur

Les deux étapes qui suivent sont le vrai passage à niveau du cours. Tout ce qui précède se passait dans une seule mémoire, où les choses sont instantanées et certaines. À partir d'ici, les données vivent ailleurs, arrivent avec du retard, et peuvent ne pas arriver du tout.

---

## Étape 9 — HttpClient et les Observables

### Le problème

Les contacts disparaissent à chaque rafraîchissement de la page. Il faut les ranger dans une base de données — donc les confier à un serveur, et les lui redemander.

### Côté serveur : une API REST en trois routes

> **Encadré backend.** Ce cours porte sur Angular, mais certaines étapes n'ont aucun sens sans leur contrepartie serveur. Ces encadrés donnent le minimum pour comprendre ce à quoi Angular parle. Le détail Spring Boot est dans la [section 33 du support](support-apprentissage-angular-spring.md#33-backend-spring-boot).

Une **API REST** est une convention : on expose des **ressources** (ici, des contacts) à des **URL**, et on agit dessus avec les **verbes** HTTP.

| Verbe | URL | Signification |
|---|---|---|
| `GET` | `/api/contacts` | Donne-moi tous les contacts |
| `POST` | `/api/contacts` | Crée un contact (envoyé dans le corps de la requête) |
| `PUT` | `/api/contacts/5` | Remplace le contact 5 |
| `DELETE` | `/api/contacts/5` | Supprime le contact 5 |

Côté Spring Boot, cela tient en une classe :

```java
@RestController                                    // « cette classe repond a des requetes HTTP »
@RequestMapping("/api/contacts")                   // prefixe commun a toutes ses routes
@CrossOrigin(origins = "http://localhost:4200")    // autorise le navigateur Angular (voir plus bas)
public class ContactController {

    private final ContactRepository contactRepository;

    public ContactController(ContactRepository contactRepository) {
        this.contactRepository = contactRepository;
    }

    @GetMapping
    public List<Contact> getAllContacts() {
        return contactRepository.findAll();
    }

    @PostMapping
    public Contact createContact(@RequestBody Contact contact) {
        // save() renvoie le contact ENRICHI de l'id genere par la base.
        // Cette valeur de retour va devenir importante a l'etape 10.
        return contactRepository.save(contact);
    }

    @DeleteMapping("/{id}")
    public void deleteContact(@PathVariable Long id) {
        contactRepository.deleteById(id);
    }
}
```

Une précision qui évite une perte de temps classique : `@CrossOrigin`. Par sécurité, un navigateur interdit à une page servie par `localhost:4200` d'appeler `localhost:8080` — deux ports différents sont considérés comme deux **origines** différentes. Cette règle s'appelle la *same-origin policy*, et `@CrossOrigin` est la façon dont le serveur déclare qu'il accepte cet appel. Sans elle, toutes les requêtes échouent avec une erreur « CORS » dans la console du navigateur.

### La notion : `HttpClient`

Angular fournit un service pour parler au réseau. Comme tout service, il s'obtient par `inject()` — mais il faut d'abord l'avoir activé dans la configuration :

```typescript
// app.config.ts
providers: [
  provideHttpClient()     // sans cette ligne, inject(HttpClient) echoue
]
```

```typescript
import { HttpClient } from '@angular/common/http';

export class ContactService {
  private http = inject(HttpClient);
  private apiUrl = 'http://localhost:8080/api/contacts';

  // Le <Contact[]> dit a TypeScript ce que la reponse contiendra. C'est une
  // PROMESSE qu'on lui fait, pas une verification : Angular ne controle pas
  // que le serveur renvoie vraiment ca.
  getContacts(): Observable<Contact[]> {
    return this.http.get<Contact[]>(this.apiUrl);
  }

  addContact(contact: Contact): Observable<Contact> {
    return this.http.post<Contact>(this.apiUrl, contact);
  }

  deleteContact(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
```

### La notion : l'Observable

Voici le concept qui demande le plus d'effort, et le seul vraiment nouveau de cette étape.

`this.http.get(...)` ne renvoie **pas** les contacts. Il ne peut pas : la réponse mettra quelques dizaines de millisecondes à arriver, et le programme ne va pas s'arrêter en attendant (revoir la section « synchrone et asynchrone » du vocabulaire).

Il renvoie un **Observable** : la promesse qu'une valeur arrivera, plus tard.

La comparaison qui éclaire le mieux :

| | Signal | Observable |
|---|---|---|
| Contient | Une valeur, **maintenant** | Une valeur qui arrivera **plus tard** |
| Se lit | `monSignal()` | `.subscribe(v => ...)` |
| Analogie | Le contenu d'un tiroir | Un colis commandé |
| Sert à | L'état de l'interface | Une opération asynchrone |

Un point capital : **un Observable ne fait rien tant que personne ne s'y abonne.**

```typescript
// Ceci n'envoie AUCUNE requete. On a juste decrit ce qu'il faudrait faire.
const requete = this.http.get<Contact[]>(this.apiUrl);

// C'est CECI qui declenche l'appel reseau.
requete.subscribe(contacts => {
  console.log('arrives :', contacts);
});
```

C'est déroutant au début — on s'attend à ce qu'appeler une fonction fasse quelque chose — mais c'est très utile : on peut construire une requête, la transformer, la passer à d'autres fonctions, et ne déclencher l'appel qu'au dernier moment. On s'en servira massivement à partir de l'étape 19.

Le corollaire, tout aussi important : **un Observable oublié ne s'exécute jamais**. Une requête dont personne ne fait `.subscribe()` ne part pas. C'est une cause fréquente de « mais pourquoi mon appel ne se fait pas ? ».

### `ngOnInit` : le bon moment pour charger

Où appeler `.subscribe()` ? Pas n'importe où.

Un composant Angular traverse un **cycle de vie** : il est construit, puis affiché, puis un jour retiré de l'écran. Angular propose des méthodes qu'on peut implémenter pour être prévenu à chaque étape.

```typescript
import { Component, OnInit, inject } from '@angular/core';

export class ContactList implements OnInit {
  private contactService = inject(ContactService);
  contacts = signal<Contact[]>([]);

  // ngOnInit est appelee UNE FOIS, apres la construction du composant et une
  // fois ses entrees disponibles. C'est l'endroit ou l'on declenche ce qui
  // demande du temps.
  ngOnInit(): void {
    this.contactService.getContacts().subscribe(data => {
      this.contacts.set(data);
    });
  }
}
```

`implements OnInit` est une aide de TypeScript : il vérifie que la méthode existe bien et qu'elle est correctement orthographiée. Angular, lui, appellerait `ngOnInit` de toute façon. Mais une faute de frappe — `ngOninit` — produirait une méthode jamais appelée, sans le moindre avertissement. L'`implements` évite ça.

**Pourquoi pas dans le constructeur ?** Le `constructor` sert à recevoir les dépendances et rien d'autre. Il s'exécute avant qu'Angular ait fini de préparer le composant, et y lancer un appel réseau rend le composant très difficile à tester. La règle : *construire* dans le constructeur, *démarrer* dans `ngOnInit`.

| Méthode | Quand | Usage typique |
|---|---|---|
| `constructor` | À la création de l'objet | Recevoir les dépendances |
| `ngOnInit` | Une fois, après l'initialisation | Charger des données |
| `ngOnDestroy` | Au retrait de l'écran | Arrêter ce qu'on a démarré (étape 21) |

### Dans le projet : le code réel, avec ses défauts

**[ÉTAPE]** — commit `b66a87a`. Voici ce que donnait l'arrivée du serveur. Lis-le en cherchant ce qui cloche : trois choses vont être corrigées à l'étape suivante.

```typescript
// components/contact-list/contact-list.ts
export class ContactList implements OnInit {
  private contactService = inject(ContactService);

  // (1) Un signal LOCAL au composant. Le service ne detient plus rien.
  contacts = signal<Contact[]>([]);

  ngOnInit(): void {
    this.chargerContacts();
  }

  chargerContacts(): void {
    this.contactService.getContacts().subscribe(data => {
      this.contacts.set(data);
    });
  }

  supprimer(id: number): void {
    this.contactService.deleteContact(id).subscribe(() => {
      // (2) On redemande TOUTE la liste au serveur apres une suppression,
      //     alors qu'on sait deja a quoi elle doit ressembler.
      this.chargerContacts();
    });
  }
}
```

```typescript
// app.ts
ajouterContact(contact: Contact): void {
  this.contactService.addContact(contact).subscribe(() => {
    // (3) Le formulaire vit dans App, la liste dans ContactList. Aucun moyen
    //     de prevenir la seconde... alors on recharge la PAGE ENTIERE.
    window.location.reload();
  });
}
```

Ce `window.location.reload()` est le genre de ligne qu'on n'ose pas montrer dans un tutoriel. Il est pourtant précieux : il rend **visible** un problème d'architecture qui, sinon, resterait abstrait.

Ce qui s'est passé, c'est que l'arrivée d'`HttpClient` a fait **régresser** la conception. À la Partie I, le service détenait la liste et tous les composants la voyaient. Ici, le service s'est transformé en simple passe-plat : il fabrique des Observables et les rend, sans rien retenir. Chaque composant s'est donc remis à gérer sa propre copie — et le formulaire, qui ne peut plus prévenir la liste, n'a d'autre recours que de tout recharger.

Le rechargement de page a un coût bien réel : tout le JavaScript est retéléchargé et réexécuté, la page blanchit pendant un instant, la position de défilement est perdue. Pour l'utilisateur, c'est un clignotement à chaque ajout.

### Ce que cette étape a introduit

| Notion | Rôle |
|---|---|
| API REST | Ressources aux URL, actions par verbes HTTP |
| `provideHttpClient()` | Activer les appels réseau dans `app.config.ts` |
| `http.get<T>(url)` | Construire une requête — **sans l'envoyer** |
| `Observable<T>` | Une valeur qui arrivera plus tard |
| `.subscribe(v => ...)` | S'abonner, et **déclencher** la requête |
| `ngOnInit` | Le moment où l'on charge |
| `implements OnInit` | Protection contre la faute de frappe |
| `@CrossOrigin` (serveur) | Autoriser le navigateur d'une autre origine |

> Détail complet : sections 10 et 11 du [support](support-apprentissage-angular-spring.md#10-httpclient-et-observables).

---

## Étape 10 — Le signal partagé alimenté par HttpClient

### Le problème

Celui qu'on vient de constater : trois symptômes d'une seule cause.

1. Chaque composant garde sa propre copie de la liste.
2. Une suppression provoque un rechargement complet depuis le serveur.
3. Un ajout recharge la page entière.

### L'idée

Aucune notion nouvelle dans cette étape. C'est une **recomposition** : on reprend le signal partagé de la Partie I et l'Observable de l'étape 9, et on les articule.

La règle à retenir tient en une phrase : **le service détient la donnée, et c'est lui qui s'abonne.**

Avant, le composant s'abonnait et rangeait le résultat chez lui. Désormais le service s'abonne et range le résultat dans son propre signal ; les composants se contentent de lire ce signal. Comme il n'y en a qu'un pour toute l'application, tout le monde voit la même chose, immédiatement.

Une conséquence de forme : les méthodes du service ne renvoient plus d'`Observable`, mais `void`. Elles ne rendent plus rien à l'appelant — elles **agissent** sur l'état partagé.

### La migration, en quatre temps

Elle a réellement été menée en quatre paliers, chacun laissant l'application fonctionnelle. Cette façon de procéder mérite d'être retenue pour elle-même : on ne réécrit pas tout d'un bloc en espérant que ça remarche à la fin.

#### Temps 1 — Le service devient propriétaire

On pose la nouvelle plomberie **à côté** de l'ancienne. Rien ne change à l'écran.

```typescript
private contactsSignal = signal<Contact[]>([]);
readonly contacts = this.contactsSignal.asReadonly();

// Ne renvoie plus rien : s'abonne elle-meme et remplit le signal.
chargerContacts(): void {
  this.http.get<Contact[]>(this.apiUrl).subscribe(data => {
    this.contactsSignal.set(data);
  });
}
```

#### Temps 2 — Le composant abandonne sa copie

```typescript
export class ContactList implements OnInit {
  private contactService = inject(ContactService);

  // Une REFERENCE vers le signal du service, plus un signal local.
  contacts = this.contactService.contacts;

  ngOnInit(): void {
    this.contactService.chargerContacts();
  }
}
```

Le template n'a **pas changé d'un caractère** : il écrivait déjà `contacts()`. C'est un bénéfice discret mais réel des signaux — l'affichage ne sait pas d'où vient la donnée qu'il lit.

#### Temps 3 — L'ajout alimente le signal, et le `reload` disparaît

```typescript
addContact(contact: Contact): void {
  this.http.post<Contact>(this.apiUrl, contact).subscribe(contactCree => {
    // On ajoute la reponse du SERVEUR, pas l'objet qu'on a envoye.
    this.contactsSignal.update(liste => [...liste, contactCree]);
  });
}
```

Le détail à ne pas rater : on ajoute `contactCree`, **la réponse du serveur**, et non le `contact` qu'on lui a transmis. Les deux ne sont pas identiques — c'est la base de données qui attribue l'`id`, et l'objet envoyé ne l'a donc pas encore. Ajouter l'objet envoyé mettrait dans la liste un contact sans identifiant, qu'on ne pourrait ni supprimer ni ouvrir.

C'est un principe général : **après une écriture, la réponse du serveur fait autorité sur ce qu'on a envoyé.** Il peut avoir complété, normalisé ou corrigé la donnée.

Côté `App`, le `reload` peut alors disparaître :

```typescript
ajouterContact(contact: Contact): void {
  // Plus de .subscribe(), plus de window.location.reload() :
  // le service met a jour le signal partage, ContactList suit toute seule.
  this.contactService.addContact(contact);
}
```

#### Temps 4 — La suppression se met à jour localement

```typescript
deleteContact(id: number): void {
  this.http.delete<void>(`${this.apiUrl}/${id}`).subscribe(() => {
    // Inutile de redemander la liste : on sait deja a quoi elle ressemble.
    this.contactsSignal.update(liste => liste.filter(c => c.id !== id));
  });
}
```

L'asymétrie entre l'ajout et la suppression est instructive. Pour un ajout, on a **besoin** de la réponse du serveur (l'`id`). Pour une suppression, on n'a besoin de rien : savoir que ça a réussi suffit à déduire la nouvelle liste. Redemander l'ensemble serait une requête entière pour une information qu'on possède déjà.

### Le code complet obtenu

**[DÉFINITIF]** — commit `85aae2e`. Ce fichier est encore, dans les grandes lignes, celui d'aujourd'hui.

```typescript
@Injectable({ providedIn: 'root' })
export class ContactService {
  private http = inject(HttpClient);
  private apiUrl = 'http://localhost:8080/api/contacts';

  // La liste vit ICI, dans le service singleton : c'est la source de
  // verite cote client. Privee : seul le service a le droit de l'ecrire.
  private contactsSignal = signal<Contact[]>([]);

  // Version exposee aux composants : lisible, mais pas modifiable.
  readonly contacts = this.contactsSignal.asReadonly();

  chargerContacts(): void {
    this.http.get<Contact[]>(this.apiUrl).subscribe(data => {
      this.contactsSignal.set(data);
    });
  }

  addContact(contact: Contact): void {
    this.http.post<Contact>(this.apiUrl, contact).subscribe(contactCree => {
      this.contactsSignal.update(liste => [...liste, contactCree]);
    });
  }

  deleteContact(id: number): void {
    this.http.delete<void>(`${this.apiUrl}/${id}`).subscribe(() => {
      this.contactsSignal.update(liste => liste.filter(c => c.id !== id));
    });
  }
}
```

### Ce qu'il faut retenir de cette étape

C'est le patron d'architecture le plus important du cours, et il resservira pour chaque service du projet :

```
Le SERVICE detient le signal, s'abonne aux requetes, et met a jour l'etat.
Le COMPOSANT lit le signal et declenche des actions.
Le TEMPLATE affiche le signal, sans savoir d'ou il vient.
```

Il faut aussi noter ce qui vient de se produire : **l'étape 9 a dégradé la conception, l'étape 10 l'a réparée.** C'est une trajectoire normale. Une notion nouvelle s'installe d'abord maladroitement, en cassant ce qui marchait, puis trouve sa place. Le même mouvement se reproduira presque à l'identique aux étapes 13 à 15, puis à l'étape 19.

### Ce que cette étape a introduit

| Notion | Rôle |
|---|---|
| Service propriétaire de l'état | Le service s'abonne, le composant lit |
| Méthodes qui renvoient `void` | Elles agissent sur l'état, elles ne rendent rien |
| `update(liste => [...liste, reponse])` | Ajouter **la réponse du serveur** |
| `update(liste => liste.filter(...))` | Déduire localement, sans nouvelle requête |
| Migration par paliers | Poser le neuf à côté, basculer, retirer l'ancien |

> Détail complet : section 12 du [support](support-apprentissage-angular-spring.md#12-signal-partagé-alimenté-par-httpclient).

---


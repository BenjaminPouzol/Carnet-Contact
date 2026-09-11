# Support d'apprentissage — Angular + Spring Boot

Document de référence détaillé, organisé par notion. Chaque section combine un paragraphe explicatif (le "pourquoi", le contexte, la logique derrière la notion) et la syntaxe précise à retenir (le "comment", réutilisable telle quelle dans un futur projet). Pense à t'y référer quand un concept redevient flou, et à le compléter toi-même au fil de tes futures découvertes.

---

## Sommaire

1. [Outils en ligne de commande](#1-outils-en-ligne-de-commande)
2. [Composants Angular](#2-composants-angular)
3. [Signals](#3-signals)
4. [Services et injection de dépendances](#4-services-et-injection-de-dépendances)
5. [Syntaxe de template (`@if` / `@for`)](#5-syntaxe-de-template-if--for)
6. [Bindings](#6-bindings)
7. [Formulaires réactifs](#7-formulaires-réactifs)
8. [Communication entre composants](#8-communication-entre-composants)
9. [HTML sémantique](#9-html-sémantique)
10. [HttpClient et Observables](#10-httpclient-et-observables)
11. [Cycle de vie d'un composant](#11-cycle-de-vie-dun-composant)
12. [Signal partagé alimenté par HttpClient](#12-signal-partagé-alimenté-par-httpclient)
13. [Routing Angular](#13-routing-angular)
14. [Modification d'une ressource (PUT, formulaire pré-rempli)](#14-modification-dune-ressource-put-formulaire-pré-rempli)
15. [Gestion des erreurs HTTP (`catchError`)](#15-gestion-des-erreurs-http-catcherror)
16. [Indicateur de chargement (`finalize`)](#16-indicateur-de-chargement-finalize)
17. [Intercepteurs HTTP](#17-intercepteurs-http)
18. [Authentification (Spring Security, BCrypt, JWT)](#18-authentification-spring-security-bcrypt-jwt)
19. [Relations entre entités JPA](#19-relations-entre-entités-jpa)
20. [Composants réutilisables : `input()` et boucles de configuration](#20-composants-réutilisables--input-et-boucles-de-configuration)
21. [Mise en forme : variables CSS et cohérence visuelle](#21-mise-en-forme--variables-css-et-cohérence-visuelle)
22. [Pagination et recherche côté serveur](#22-pagination-et-recherche-côté-serveur)
23. [Contexte d'une requête HTTP (`HttpContext`)](#23-contexte-dune-requête-http-httpcontext)
24. [Renouvellement du jeton d'accès](#24-renouvellement-du-jeton-daccès)
25. [Rafraîchissement automatique (sondage périodique)](#25-rafraîchissement-automatique-sondage-périodique)
26. [Tests automatisés](#26-tests-automatisés)
27. [Saisie et validation d'un mot de passe](#27-saisie-et-validation-dun-mot-de-passe)
28. [Notifications du navigateur](#28-notifications-du-navigateur)
29. [Rôles et autorisations](#29-rôles-et-autorisations)
30. [PrimeNG, couches CSS et chargement différé](#30-primeng-couches-css-et-chargement-différé)
31. [Mode sombre](#31-mode-sombre)
32. [Réactions et accusés de lecture](#32-réactions-et-accusés-de-lecture)
33. [Backend Spring Boot](#33-backend-spring-boot)
34. [Git et GitHub](#34-git-et-github)
35. [Pense-bête de dépannage](#35-pense-bête-de-dépannage)

---

## 1. Outils en ligne de commande

Angular et Spring Boot fournissent chacun un outil en ligne de commande dont le rôle est d'éviter d'écrire à la main toute la "plomberie" répétitive d'un projet : structure de dossiers, fichiers de configuration, squelette de code respectant les conventions du framework. Sans ces outils, créer un composant Angular obligerait à écrire soi-même le décorateur `@Component`, les trois fichiers liés, et à les relier correctement — l'outil fait tout ça en une commande. C'est un gain de temps, mais surtout une garantie de cohérence : tous les projets Angular généré par `ng` partagent la même structure, ce qui les rend plus faciles à comprendre pour n'importe quel développeur habitué au framework.

### Angular CLI (`ng`)

| Commande | Rôle |
|---|---|
| `ng new <nom>` | Crée un nouveau projet Angular complet |
| `ng serve` | Lance le serveur de développement (`localhost:4200`), avec rechargement automatique |
| `ng generate component <chemin>` (raccourci `ng g c`) | Génère un composant (fichiers ts/html/css/spec) |
| `ng generate service <chemin>` (raccourci `ng g s`) | Génère un service |
| `ng build` | Compile l'application pour la production (fichiers optimisés dans `dist/`) |
| `ng --help` | Liste toutes les commandes disponibles |

`ng serve` mérite une attention particulière : il ne se contente pas de lancer un serveur, il surveille aussi en permanence les fichiers du projet (le "watch mode"). Dès qu'une sauvegarde est détectée, Angular recompile automatiquement le code concerné et pousse la mise à jour vers le navigateur — c'est ce qui permet de voir le résultat d'une modification quasi instantanément, sans jamais avoir à relancer quoi que ce soit manuellement. C'est un outil de développement uniquement : pour livrer une vraie application, on utilise `ng build`, qui produit des fichiers statiques optimisés, prêts à être déposés sur un serveur web classique.

### Maven Wrapper (`mvnw`) — l'équivalent côté Java

| Commande | Rôle |
|---|---|
| `./mvnw spring-boot:run` | Compile et lance le serveur Spring Boot (`localhost:8080`) — forme macOS / Linux / Git Bash |
| `.\mvnw.cmd spring-boot:run` | La même commande sous Windows PowerShell (antislash, et extension `.cmd`) |
| `./mvnw clean` | Supprime le dossier `target/`, pour repartir d'une compilation vierge |

Le "wrapper" (`mvnw` au lieu de `mvn`) existe pour une raison précise : il télécharge et utilise automatiquement la bonne version de Maven attendue par le projet, sans que tu aies besoin d'installer Maven toi-même sur ta machine ni de te soucier des versions. C'est un peu l'équivalent de ce que fait `package-lock.json` côté npm — garantir que tout le monde travaille avec les mêmes versions d'outils, projet par projet.

### Lancer les deux serveurs en parallèle

Une application séparée en un frontend et un backend n'est pas un programme unique que l'on démarre d'une seule commande : ce sont **deux serveurs indépendants**, qui tournent chacun dans son propre terminal, écoutent chacun sur son propre port, et communiquent uniquement par des requêtes HTTP. C'est une conséquence directe de l'architecture choisie — le navigateur charge l'interface depuis le serveur de développement du frontend, puis appelle le backend séparément pour obtenir les données.

Concrètement, il faut ouvrir deux terminaux et laisser les deux commandes tourner en permanence pendant toute la session de travail. Un terminal qui semble "bloqué" après avoir lancé un serveur est le comportement normal et attendu : le serveur occupe ce terminal tant qu'il fonctionne, et l'interrompre (`Ctrl + C`) revient à éteindre le serveur.

| Terminal | Dossier | Commande | Port |
|---|---|---|---|
| 1 — Backend | `<projet>-backend/` | `.\mvnw.cmd spring-boot:run` | 8080 |
| 2 — Frontend | `<projet>_frontend/` | `npm start` (alias de `ng serve`) | 4200 |

L'ordre de démarrage a son importance en pratique : mieux vaut lancer le backend en premier, pour qu'il soit prêt à répondre quand le frontend enverra ses premières requêtes. Si le frontend démarre seul, la page s'affichera correctement mais toutes les données resteront vides, avec une erreur réseau visible dans la console du navigateur (`F12`).

Une différence de comportement distingue les deux serveurs au quotidien. Le serveur Angular surveille les fichiers et recompile automatiquement à chaque sauvegarde (le "watch mode") : une modification TypeScript ou HTML apparaît seule dans le navigateur en une ou deux secondes. Le serveur Spring Boot, lui, ne surveille rien par défaut : toute modification d'un fichier Java exige de l'arrêter (`Ctrl + C`) puis de le relancer pour être prise en compte.

| Vérification | Ce qu'on doit obtenir |
|---|---|
| Backend démarré | `Tomcat started on port 8080` puis `Started ...Application in X seconds` |
| Backend répond | `http://localhost:8080/api/<ressource>` renvoie du JSON (`[]` si la base est vide) |
| Frontend démarré | `Local: http://localhost:4200/` puis `Watch mode enabled` |

---

## 2. Composants Angular

Un composant est la brique de base de toute interface Angular. L'idée centrale est de découper l'application en petites unités autonomes et réutilisables, chacune responsable d'une portion précise de l'écran, plutôt que d'écrire une seule énorme page qui gère tout. Une application Angular complète n'est au fond rien d'autre qu'un arbre de composants imbriqués les uns dans les autres — le composant racine (`App`) contient d'autres composants, qui peuvent eux-mêmes en contenir d'autres, et ainsi de suite.

Concrètement, un composant sépare toujours trois responsabilités dans trois fichiers distincts : la logique (que faire quand on clique sur un bouton, comment récupérer des données), l'affichage (à quoi ressemble le composant), et le style (comment il est présenté visuellement, sans affecter les autres composants de la page grâce à l'isolation du CSS).

### Structure

```
mon-composant.ts     → logique (classe TypeScript)
mon-composant.html   → template affiché
mon-composant.css    → style, isolé à ce composant
```

### Syntaxe de base

```typescript
import { Component } from '@angular/core';

@Component({
  selector: 'app-mon-composant',
  imports: [],
  templateUrl: './mon-composant.html',
  styleUrl: './mon-composant.css'
})
export class MonComposant {
  // logique ici
}
```

Le `@Component({...})` est ce qu'on appelle un **décorateur** : une annotation placée juste avant une classe, qui donne des informations supplémentaires au framework sur la façon de traiter cette classe. Sans lui, `MonComposant` ne serait qu'une classe TypeScript ordinaire — c'est ce décorateur qui la transforme en composant Angular reconnu comme tel.

Le `selector` définit le nom de la balise HTML personnalisée que ce composant crée. Une fois déclaré, on peut l'utiliser n'importe où dans un autre template, exactement comme une balise HTML native (`<div>`, `<p>`...), mais celle-ci affichera tout ce que le composant définit.

Le tableau `imports` mérite d'être bien compris : chaque composant Angular moderne (dit "standalone") déclare explicitement, dans ce tableau, tous les autres composants ou modules dont **son propre template** a besoin. C'est différent de l'ancienne approche Angular, où tout était déclaré une fois pour toutes dans un fichier central appelé `NgModule`. L'approche standalone rend chaque composant plus autonome et plus facile à comprendre isolément : en lisant ses `imports`, on sait immédiatement de quoi il dépend, sans devoir remonter dans une configuration globale.

### Générer un composant

```bash
ng generate component components/mon-composant
```

### Utiliser un composant dans un autre

1. L'importer : `import { MonComposant } from './components/mon-composant/mon-composant';`
2. L'ajouter aux `imports: [...]` du composant parent
3. L'utiliser dans le template parent : `<app-mon-composant></app-mon-composant>`

Ces trois étapes sont indissociables. Oublier l'une d'elles est une source d'erreur très fréquente en début d'apprentissage : si tu utilises la balise dans le HTML sans avoir ajouté le composant aux `imports`, Angular affichera une erreur au moment de la compilation, car il ne "connaît" pas cette balise.

**Dans le projet** — le composant racine [`app.ts`](../carnet-contact_frontend/src/app/app.ts), et un composant enfant typique [`components/contact-list/contact-list.ts`](../carnet-contact_frontend/src/app/components/contact-list/contact-list.ts)

```typescript
// contact-list.ts — les trois fichiers ts / html / css, un selector, des imports
@Component({
  selector: 'app-contact-list',
  imports: [RouterLink],
  templateUrl: './contact-list.html',
  styleUrl: './contact-list.css'
})
export class ContactList implements OnInit { /* ... */ }
```

L'arbre des composants du projet : `App` (coquille) → `Accueil` (page) → `ContactForm` + `ContactList` (briques). Les trois étapes « importer / déclarer dans `imports` / utiliser la balise » se lisent dans [`pages/accueil/accueil.ts`](../carnet-contact_frontend/src/app/pages/accueil/accueil.ts) et [`accueil.html`](../carnet-contact_frontend/src/app/pages/accueil/accueil.html) :

```typescript
// accueil.ts
imports: [ContactList, ContactForm],
```
```html
<!-- accueil.html -->
<app-contact-form (contactAjoute)="ajouterContact($event)"></app-contact-form>
<app-contact-list></app-contact-list>
```

---

## 3. Signals

### Le concept

Les signals répondent à un problème très concret : comment faire en sorte que l'affichage d'une page se mette à jour automatiquement dès qu'une donnée change, sans avoir à écrire soi-même du code pour détecter ce changement et rafraîchir manuellement le DOM ? Un signal est une "boîte" qui contient une valeur, et qui a la particularité de prévenir Angular chaque fois que cette valeur est modifiée. Angular peut alors recalculer uniquement les parties de l'affichage qui dépendent de cette valeur précise, de façon très ciblée et performante — sans avoir besoin de rafraîchir toute la page.

C'est un changement de paradigme important par rapport à la programmation "classique" où l'on manipule des variables normales : avec une variable ordinaire, modifier sa valeur ne déclenche rien d'automatique côté affichage. Avec un signal, la mise à jour de l'interface devient une conséquence directe et automatique de la mise à jour de la donnée.

### Syntaxe

```typescript
import { signal } from '@angular/core';

// Créer un signal
const monSignal = signal<Type>(valeurInitiale);
const compteur = signal(0);
const contacts = signal<Contact[]>([]);

// Lire la valeur — TOUJOURS avec des parenthèses, comme un appel de fonction
console.log(compteur());  // 0

// Modifier avec .set() — remplace complètement la valeur
compteur.set(5);

// Modifier avec .update() — reçoit l'ancienne valeur, retourne la nouvelle
compteur.update(ancienneValeur => ancienneValeur + 1);

// Exposer en lecture seule (empêche la modification depuis l'extérieur)
readonly monSignalPublic = monSignal.asReadonly();
```

La distinction entre `.set()` et `.update()` correspond à deux besoins différents. `.set()` s'utilise quand on connaît déjà la nouvelle valeur complète et qu'on veut simplement l'imposer (par exemple, remplacer toute une liste par les résultats reçus d'un serveur). `.update()` s'utilise quand la nouvelle valeur dépend de l'ancienne — typiquement pour ajouter un élément à un tableau existant, ou incrémenter un compteur : on ne peut pas "deviner" la nouvelle valeur sans regarder l'ancienne d'abord.

`.asReadonly()` répond à une préoccupation d'encapsulation : un service qui expose directement son signal modifiable permettrait à n'importe quel composant de le modifier n'importe comment, sans passer par une méthode contrôlée. En exposant une version en lecture seule, on force tous les composants extérieurs à passer par les méthodes définies explicitement par le service (comme `addContact()` ou `deleteContact()`), qui elles seules ont le droit de modifier la donnée réelle. Cela centralise la logique de modification à un seul endroit, plus facile à comprendre et à déboguer.

**Dans le projet** — [`services/contact.ts`](../carnet-contact_frontend/src/app/services/contact.ts)

```typescript
private contactsSignal = signal<Contact[]>([]);   // création, valeur initiale []
readonly contacts = this.contactsSignal.asReadonly();

// .set() : remplacer toute la liste (réponse du serveur)
this.contactsSignal.set(data);
// .update() : la nouvelle valeur dépend de l'ancienne (ajout)
this.contactsSignal.update(liste => [...liste, contactCree]);
```

Le détail de ce fichier, étape par étape, est en section 12.

### Dans un template HTML

```html
<p>{{ compteur() }}</p>
<!-- toujours avec les parenthèses ! -->
```

Un piège fréquent en début d'apprentissage : oublier les parenthèses en lisant un signal. Écrire `{{ compteur }}` sans parenthèses n'affichera pas la valeur, mais une représentation de la fonction elle-même — un signal doit systématiquement être "appelé" comme une fonction pour en extraire la valeur actuelle, aussi bien dans le TypeScript que dans le HTML.

### Immutabilité — règle importante

```typescript
// ✅ Correct — crée un nouveau tableau
listeSignal.update(liste => [...liste, nouvelElement]);
listeSignal.update(liste => liste.filter(item => item.id !== idASupprimer));

// ❌ Incorrect — mutation directe, Angular peut ne pas détecter le changement
listeSignal().push(nouvelElement);
```

Cette règle de l'immutabilité (ne jamais modifier un tableau ou un objet "en place", mais toujours en recréer une nouvelle version) découle directement de la façon dont Angular détecte les changements : il compare les **références** des objets, pas leur contenu en détail. Si l'on modifie un tableau directement avec `.push()`, la référence du tableau reste exactement la même en mémoire — Angular, en comparant l'ancienne et la nouvelle référence, ne verra aucune différence et risque de ne pas déclencher la mise à jour de l'affichage, même si le contenu a réellement changé. En utilisant le spread operator (`...`) pour créer un tout nouveau tableau à chaque modification, on garantit que la référence change également, ce qui permet à Angular de détecter fiablement le changement.

### Valeurs dérivées : `computed()`

Il arrive souvent qu'une valeur affichée ne soit pas stockée telle quelle, mais se déduise d'autres données : un total qui dépend d'une liste, un élément qu'on retrouve dans un tableau à partir d'un identifiant, un libellé qui change selon un état. Écrire cette déduction « à la main » obligerait à la recalculer soi-même à chaque endroit où l'une des données sources change — et à ne jamais en oublier un.

`computed()` répond à ce besoin : il crée un signal en **lecture seule** dont la valeur est le résultat d'un calcul, et Angular réexécute ce calcul automatiquement dès que l'un des signals lus à l'intérieur change. On ne l'écrit jamais avec `.set()` ni `.update()` — sa valeur n'est jamais imposée, seulement déduite.

```typescript
import { signal, computed } from '@angular/core';

const prix = signal(100);
const quantite = signal(2);

// Le calcul lit prix() et quantite() : computed() « retient » cette
// dépendance et se recalcule si l'un des deux change.
const total = computed(() => prix() * quantite());

console.log(total());   // 200
quantite.set(3);
console.log(total());   // 300 — recalculé tout seul, sans intervention
```

Un cas d'usage fréquent : retrouver un élément précis dans une liste qui, elle, est chargée de façon asynchrone (réponse d'un serveur). Au premier affichage la liste est encore vide, donc la recherche ne renvoie rien ; quand la réponse arrive et remplit le signal de la liste, le `computed()` se recalcule et trouve enfin l'élément — sans qu'aucun `.subscribe()` ni code de synchronisation n'ait été écrit.

```typescript
// items() est un signal alimenté plus tard par un appel HTTP
elementCourant = computed(() =>
  this.service.items().find(item => item.id === this.idRecherche)
);
```

**Dans le projet** — [`pages/contact-detail/contact-detail.ts`](../carnet-contact_frontend/src/app/pages/contact-detail/contact-detail.ts)

```typescript
private contactId = Number(this.route.snapshot.paramMap.get('id'));

contact = computed(() =>
  this.contactService.contacts().find(c => c.id === this.contactId)
);
```

> **Note de mise à jour.** Ce `computed()` a depuis été retiré du projet : la pagination (section 22) a rendu fausse l'hypothèse dont il dépendait — le signal `contacts` ne contient plus *tous* les contacts, seulement une page. L'exemple reste juste en tant qu'illustration de `computed()` ; c'est son hypothèse de départ qui a changé, pas l'opérateur. `ContactService` conserve d'ailleurs deux `computed()` bien vivants (`premierePage`, `dernierePage`).

| | `signal()` | `computed()` |
|---|---|---|
| Contient | Une valeur qu'on fixe soi-même | Une valeur déduite d'autres signals |
| Se modifie | `.set()`, `.update()` | Jamais directement — recalcul automatique |
| Rôle | Source de vérité | Vue dérivée d'une ou plusieurs sources |

### Réagir à un changement : `effect()`

`computed()` produit une **valeur**. Parfois, on ne veut pas calculer une valeur mais **déclencher une action** quand un signal change : écrire dans `localStorage`, envoyer un log, ou remplir un formulaire dès que la donnée qui doit l'alimenter est disponible. C'est le rôle d'`effect()`.

```typescript
import { signal, effect } from '@angular/core';

const utilisateur = signal<string | null>(null);

// La fonction est réexécutée à CHAQUE changement d'un signal qu'elle lit.
effect(() => {
  const u = utilisateur();
  if (u) {
    localStorage.setItem('dernierUtilisateur', u);
  }
});

utilisateur.set('alice');   // l'effect s'exécute, écrit dans localStorage
```

`effect()` s'écrit dans le `constructor` d'un composant (ou dans un champ de classe) : Angular a besoin d'être dans son « contexte d'injection » pour l'enregistrer et le nettoyer automatiquement quand le composant disparaît.

Un piège fréquent : un `effect()` qui modifie quelque chose à chaque exécution alors qu'on ne le voulait qu'une fois (par exemple pré-remplir un formulaire). La parade habituelle est un drapeau booléen qui mémorise que l'action a déjà eu lieu.

| | `computed()` | `effect()` |
|---|---|---|
| Produit | Une valeur (signal en lecture seule) | Rien — un effet de bord |
| Sert à | Dériver une donnée d'autres signals | Synchroniser avec l'extérieur (stockage, réseau, formulaire, log) |
| Se lit | `maValeur()` | ne se lit pas |

**Dans le projet** — [`pages/contact-edit/contact-edit.ts`](../carnet-contact_frontend/src/app/pages/contact-edit/contact-edit.ts)

```typescript
private formulaireRempli = false;

constructor() {
  effect(() => {
    const c = this.contact();                 // lit le signal partagé
    if (c && !this.formulaireRempli) {
      this.contactForm.patchValue(c);          // action : remplir le formulaire
      this.formulaireRempli = true;            // garde-fou : une seule fois
    }
  });
}
```

---

## 4. Services et injection de dépendances

### Le concept

Un service répond à une question de conception logicielle : où doit vivre la logique et les données qui ne concernent pas directement l'affichage, mais qui doivent être partagées entre plusieurs composants ? Si l'on écrivait toute cette logique directement dans un composant, on se retrouverait rapidement avec du code dupliqué dès qu'un deuxième composant aurait besoin des mêmes données, et il deviendrait très difficile de garder plusieurs affichages synchronisés entre eux.

Un service résout ce problème en centralisant cette responsabilité dans une classe à part, dédiée uniquement à la donnée et à la logique métier (pas à l'affichage). N'importe quel composant peut ensuite "demander" ce service et l'utiliser. C'est le principe de séparation des responsabilités : les composants s'occupent de l'affichage, les services s'occupent de la donnée et de la logique.

### Créer un service

```bash
ng generate service services/mon-service
```

### Syntaxe

```typescript
import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class MonService {
  // logique ici
}
```

Le `providedIn: 'root'` est une précision cruciale : il indique à Angular de ne créer **qu'une seule instance** de ce service pour toute l'application (ce qu'on appelle un singleton), plutôt qu'une nouvelle instance à chaque fois qu'un composant en a besoin. C'est cette unicité qui permet le partage réel de données : si deux composants différents injectent le même service, ils reçoivent tous les deux une référence vers exactement la même instance, avec les mêmes données internes. Si l'un des deux modifie une donnée via une méthode du service, l'autre composant "voit" immédiatement ce changement, puisqu'ils travaillent en réalité sur le même objet en mémoire.

### Utiliser un service dans un composant (injection)

```typescript
import { inject } from '@angular/core';
import { MonService } from '../../services/mon-service';

export class MonComposant {
  private monService = inject(MonService);
}
```

L'injection de dépendances est un mécanisme qui inverse la responsabilité de création des objets : plutôt que d'écrire soi-même `new MonService()` (ce qui créerait une nouvelle instance, allant à l'encontre du principe de singleton), on demande à Angular de nous **fournir** l'instance déjà existante. `inject()` est la syntaxe moderne pour formuler cette demande — elle remplace l'ancienne approche qui consistait à recevoir le service en paramètre du constructeur de la classe.

**Dans le projet** — le service [`services/contact.ts`](../carnet-contact_frontend/src/app/services/contact.ts), injecté dans plusieurs composants

```typescript
// contact.ts — un seul exemplaire pour toute l'application
@Injectable({ providedIn: 'root' })
export class ContactService {
  private http = inject(HttpClient);   // le service injecte lui-même une dépendance
  // ...
}
```

```typescript
// contact-list.ts, contact-detail.ts, contact-edit.ts, accueil.ts :
// tous reçoivent LA MÊME instance
private contactService = inject(ContactService);
```

C'est cette unicité qui fait fonctionner le signal partagé de la section 12 : quatre composants, un seul service, un seul signal.

---

## 5. Syntaxe de template (`@if` / `@for`)

Les templates Angular ont besoin d'une syntaxe spécifique pour exprimer de la logique conditionnelle ou des boucles directement dans le HTML, puisque le HTML natif ne sait pas faire ça. Les versions récentes d'Angular (17 et suivantes) ont introduit une nouvelle syntaxe de contrôle de flux, `@if` et `@for`, qui remplace l'ancienne écriture basée sur des directives (`*ngIf`, `*ngFor`). Cette nouvelle syntaxe est volontairement plus proche de ce qu'on écrirait en JavaScript classique, ce qui la rend plus intuitive à lire et à retenir.

### `@if` / `@else`

```html
@if (condition) {
  <p>Vrai</p>
} @else {
  <p>Faux</p>
}
```

### `@for`

```html
@for (item of liste(); track item.id) {
  <li>{{ item.nom }}</li>
}
```

Le mot-clé `track` est obligatoire avec `@for`, et il mérite une explication : quand une liste change (un élément ajouté, supprimé, ou réordonné), Angular a besoin d'un moyen fiable d'identifier quel élément du DOM correspond à quel élément de la liste, pour éviter de tout recréer inutilement à chaque changement. En donnant un identifiant unique et stable (typiquement l'`id` de chaque élément), Angular peut, par exemple, comprendre qu'un seul élément a été supprimé au milieu de la liste, et retirer uniquement le `<li>` correspondant, sans toucher aux autres — une optimisation de performance importante sur de longues listes.

### `@for` avec bloc vide

```html
@for (item of liste(); track item.id) {
  <li>{{ item.nom }}</li>
} @empty {
  <p>Liste vide</p>
}
```

**Dans le projet** — [`components/contact-list/contact-list.html`](../carnet-contact_frontend/src/app/components/contact-list/contact-list.html)

```html
@if (contacts().length === 0) {
  <p>Aucun contact enregistré.</p>
} @else {
  <ul>
    @for (contact of contacts(); track contact.id) {
      <li>
        <a [routerLink]="['/contact', contact.id]">{{ contact.prenom }} {{ contact.nom }}</a>
        — {{ contact.email }} — {{ contact.telephone }}
        <button (click)="supprimer(contact.id)">Supprimer</button>
      </li>
    }
  </ul>
}
```

Le projet utilise `@if/@else` plutôt que le bloc `@empty`, mais le résultat est le même : un message quand la liste est vide.

---

## 6. Bindings

Le terme "binding" (liaison) désigne la façon dont le HTML d'un template se connecte à la logique TypeScript du composant. Angular propose trois syntaxes différentes, chacune correspondant à un sens de circulation de l'information bien précis, et il est important de bien les distinguer visuellement grâce à leur ponctuation caractéristique.

### Interpolation `{{ }}`

Affiche une valeur TypeScript dans le texte du HTML. La donnée circule dans un seul sens : du composant vers l'affichage.
```html
<h1>{{ title() }}</h1>
<p>{{ contact.nom }}</p>
```

### Binding de propriété `[ ]`

Lie un attribut ou une propriété HTML à une valeur TypeScript. Comme l'interpolation, la donnée circule du composant vers le HTML, mais cette fois-ci elle contrôle un attribut plutôt qu'un texte affiché.
```html
<button [disabled]="formulaire.invalid">Envoyer</button>
<form [formGroup]="monFormulaire">
```

### Binding d'événement `( )`

Exécute une méthode du composant en réaction à un événement du DOM (un clic, une soumission de formulaire...). Ici, la circulation s'inverse : c'est une action de l'utilisateur dans le navigateur qui déclenche l'exécution de code côté TypeScript.
```html
<button (click)="maMethode()">Cliquer</button>
<button (click)="supprimer(contact.id)">Supprimer</button>
<form (ngSubmit)="onSubmit()">
```

Retenir la logique visuelle aide à se souvenir de laquelle utiliser : les crochets `[ ]` "font entrer" une donnée dans l'élément HTML (comme une fenêtre par laquelle on regarde vers l'intérieur), tandis que les parenthèses `( )` "font sortir" une action vers le TypeScript (comme un signal qui part de l'élément).

**Dans le projet** — les trois bindings sont visibles à la lecture des templates

| Binding | Fichier | Ligne |
|---|---|---|
| `{{ }}` interpolation | [`app.html`](../carnet-contact_frontend/src/app/app.html) | `<h1>{{ title() }}</h1>` |
| `[ ]` propriété | [`contact-form.html`](../carnet-contact_frontend/src/app/components/contact-form/contact-form.html) | `[formGroup]="contactForm"`, `[disabled]="contactForm.invalid"` |
| `( )` événement | [`contact-list.html`](../carnet-contact_frontend/src/app/components/contact-list/contact-list.html) | `(click)="supprimer(contact.id)"` |
| `( )` événement | [`accueil.html`](../carnet-contact_frontend/src/app/pages/accueil/accueil.html) | `(contactAjoute)="ajouterContact($event)"` (événement personnalisé, section 8) |

---

## 7. Formulaires réactifs

### Principe

Angular propose deux façons de gérer les formulaires : l'approche "template-driven" (où la logique de validation est écrite directement dans le HTML) et l'approche "réactive" (où toute la structure du formulaire — les champs, leurs valeurs initiales, leurs règles de validation — est définie dans la classe TypeScript, le HTML se contentant de s'y "brancher"). L'approche réactive est généralement préférée dès que le formulaire devient un peu complexe, car elle centralise toute la logique à un seul endroit, testable et prévisible, plutôt que de l'éparpiller dans le template.

### Syntaxe TypeScript

```typescript
import { inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

@Component({
  selector: 'app-mon-formulaire',
  imports: [ReactiveFormsModule],
  templateUrl: './mon-formulaire.html',
  styleUrl: './mon-formulaire.css'
})
export class MonFormulaire {
  private fb = inject(FormBuilder);

  monFormulaire = this.fb.group({
    nom: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    telephone: ['']  // optionnel, pas de Validators
  });

  onSubmit(): void {
    if (this.monFormulaire.valid) {
      console.log(this.monFormulaire.value);
      this.monFormulaire.reset();
    }
  }
}
```

`FormBuilder` est un service fourni par Angular qui simplifie l'écriture d'un formulaire réactif — on pourrait construire la même structure sans lui, mais avec une syntaxe plus verbeuse. Chaque champ déclaré dans `.group({...})` prend la forme d'un tableau : sa valeur de départ, suivie de ses règles de validation (une seule règle directement, ou plusieurs regroupées dans un tableau). L'absence de règle, comme pour `telephone`, signifie simplement que ce champ est optionnel.

La propriété `monFormulaire.valid` (et son inverse `.invalid`) est recalculée automatiquement par Angular à chaque changement de valeur dans les champs, en fonction des règles de validation déclarées. On n'a jamais besoin de la calculer soi-même : elle vaut `true` uniquement si absolument tous les champs respectent leurs règles respectives.

### Validateurs courants

| Validateur | Rôle |
|---|---|
| `Validators.required` | Champ obligatoire |
| `Validators.email` | Format email valide |
| `Validators.minLength(n)` | Longueur minimale |
| `Validators.maxLength(n)` | Longueur maximale |
| `Validators.pattern(regex)` | Doit respecter une expression régulière |

### Syntaxe HTML

```html
<form [formGroup]="monFormulaire" (ngSubmit)="onSubmit()">
  <input formControlName="nom" placeholder="Nom" />
  <input formControlName="email" placeholder="Email" />
  <button type="submit" [disabled]="monFormulaire.invalid">Envoyer</button>
</form>
```

Chacune de ces quatre lignes illustre bien la combinaison des bindings vus plus haut : `[formGroup]` est un binding de propriété qui relie tout le `<form>` à l'objet `FormGroup` du TypeScript ; `formControlName` fait le lien fin entre un `<input>` précis et le champ correspondant à l'intérieur de ce groupe ; `(ngSubmit)` est un binding d'événement qui capture la soumission du formulaire (que ce soit par clic sur le bouton ou par la touche Entrée) ; et `[disabled]` est encore un binding de propriété qui désactive dynamiquement le bouton tant que le formulaire entier n'est pas valide.

**Dans le projet** — [`components/contact-form/contact-form.ts`](../carnet-contact_frontend/src/app/components/contact-form/contact-form.ts) et [`contact-form.html`](../carnet-contact_frontend/src/app/components/contact-form/contact-form.html)

```typescript
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
```

```html
<form [formGroup]="contactForm" (ngSubmit)="onSubmit()">
  <div><input formControlName="nom" placeholder="Nom" /></div>
  <!-- ... prenom, email, telephone ... -->
  <button type="submit" [disabled]="contactForm.invalid">Ajouter</button>
</form>
```

Le formulaire d'édition [`contact-edit.ts`](../carnet-contact_frontend/src/app/pages/contact-edit/contact-edit.ts) réutilise exactement la même structure, avec `patchValue()` en plus.

### Pré-remplir un formulaire existant

Les valeurs déclarées dans `.group({...})` ne servent qu'au démarrage. Pour afficher un formulaire déjà rempli (cas d'une page d'édition), on injecte les valeurs actuelles après coup avec `patchValue()` :

```typescript
this.monFormulaire.patchValue(objetExistant);
```

`patchValue()` n'affecte que les champs dont le nom correspond à un contrôle et ignore les clés en trop (un `id`, par exemple). Sa variante `setValue()` exige un objet correspondant exactement aux contrôles. Le cas concret — pré-remplir depuis un signal partagé chargé de façon asynchrone — est traité en section 14.

---

## 8. Communication entre composants

Un composant enfant, dans l'architecture Angular, n'a normalement aucun moyen direct de "parler" à son parent — l'information ne circule naturellement que du parent vers l'enfant (via des `@Input()`, que tu n'as pas encore vus en détail). Pour permettre l'inverse, c'est-à-dire qu'un enfant signale un événement ou transmette une donnée à son parent, Angular propose un mécanisme d'événements personnalisés.

### Enfant → Parent : `output()`

**Dans le composant enfant** :
```typescript
import { output } from '@angular/core';

export class ComposantEnfant {
  monEvenement = output<TypeDeLaDonnee>();

  declencherEvenement(): void {
    this.monEvenement.emit(laDonnee);
  }
}
```

**Dans le composant parent (HTML)** :
```html
<app-composant-enfant (monEvenement)="maMethode($event)"></app-composant-enfant>
```

Ce mécanisme fonctionne en deux temps distincts. D'abord, l'enfant déclare, avec `output<Type>()`, qu'il est capable d'émettre un événement personnalisé nommé comme la propriété (`monEvenement` ici), transportant une donnée d'un type précis. Ensuite, quelque part dans sa logique interne (typiquement en réaction à une action utilisateur), il appelle `.emit(laDonnee)` pour effectivement déclencher cet événement avec une valeur donnée. Côté parent, on écoute cet événement exactement comme on écouterait un événement natif du DOM (`(click)`, `(ngSubmit)`...), sauf qu'ici le nom entre parenthèses correspond au nom choisi pour l'`output`. La variable spéciale `$event` récupère automatiquement la donnée qui a été passée à `.emit(...)`.

**Dans le projet** — l'enfant [`contact-form.ts`](../carnet-contact_frontend/src/app/components/contact-form/contact-form.ts), le parent [`accueil.ts`](../carnet-contact_frontend/src/app/pages/accueil/accueil.ts) / [`accueil.html`](../carnet-contact_frontend/src/app/pages/accueil/accueil.html)

```typescript
// contact-form.ts (enfant) — déclare l'événement, puis l'émet à la soumission
contactAjoute = output<Contact>();
// ...
this.contactAjoute.emit(this.contactForm.value as Contact);
```

```html
<!-- accueil.html (parent) — écoute l'événement, $event porte le Contact émis -->
<app-contact-form (contactAjoute)="ajouterContact($event)"></app-contact-form>
```

```typescript
// accueil.ts (parent) — reçoit la donnée et la transmet au service
ajouterContact(contact: Contact): void {
  this.contactService.addContact(contact);
}
```

---

## 9. HTML sémantique

Le HTML sémantique consiste à choisir ses balises en fonction du **rôle** du contenu qu'elles contiennent, et non uniquement en fonction de leur apparence visuelle par défaut. Cette distinction compte pour plusieurs raisons concrètes : l'accessibilité (les lecteurs d'écran utilisés par les personnes malvoyantes s'appuient sur cette structure pour naviguer efficacement dans une page), le référencement (les moteurs de recherche utilisent la hiérarchie des titres pour comprendre l'organisation du contenu), et la maintenabilité (un autre développeur, ou toi-même dans plusieurs mois, comprend immédiatement la structure logique d'une page en lisant simplement son HTML, sans avoir besoin du rendu visuel).

| Balise | Rôle |
|---|---|
| `<h1>` à `<h6>` | Titres hiérarchisés (h1 = le plus important). Structure la page, indépendamment de l'apparence visuelle par défaut du navigateur |
| `<p>` | Paragraphe de texte simple, sans rôle de titre |
| `<ul>` | Liste à puces (*unordered list*), pour des éléments sans ordre particulier |
| `<ol>` | Liste numérotée (*ordered list*), pour des éléments dont l'ordre a un sens |
| `<li>` | Élément de liste (*list item*), doit obligatoirement être placé à l'intérieur d'un `<ul>` ou d'un `<ol>` |

**Dans le projet** — [`contact-list.html`](../carnet-contact_frontend/src/app/components/contact-list/contact-list.html) : titre `<h2>`, liste `<ul>` / `<li>` (une puce par contact, sans ordre imposé), et `<p>` pour le message de liste vide.

---

## 10. HttpClient et Observables

### Activer HttpClient

```typescript
import { provideHttpClient } from '@angular/common/http';

export const appConfig: ApplicationConfig = {
  providers: [
    // ...autres providers
    provideHttpClient()
  ]
};
```

Ce `provideHttpClient()` doit être ajouté une seule fois, dans la configuration globale de l'application (`app.config.ts`). C'est ce qu'on appelle un "provider" : une fonction qui active une fonctionnalité pour toute l'application. Sans cette ligne, tenter d'injecter `HttpClient` dans un service provoquerait une erreur au démarrage, puisque cette fonctionnalité ne serait tout simplement pas configurée.

**Dans le projet** — [`app.config.ts`](../carnet-contact_frontend/src/app/app.config.ts)

```typescript
export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideClientHydration(withEventReplay()),
    provideHttpClient()
  ]
};
```

### Le concept d'Observable

La notion d'Observable répond à un problème temporel : comment représenter une donnée qui n'existe pas encore au moment où on écrit le code, mais qui arrivera plus tard, après un appel réseau dont on ne connaît pas la durée à l'avance ? Un signal, tel qu'on l'a vu, contient toujours une valeur immédiatement disponible. Un Observable, à l'inverse, représente un flux potentiel de valeurs futures — dans le cas d'une requête HTTP classique, ce flux ne contiendra qu'une seule valeur (la réponse du serveur), mais le mécanisme des Observables est en réalité plus général et peut gérer des flux de plusieurs valeurs successives dans le temps (utile par exemple pour des mises à jour en temps réel).

Un point de fonctionnement souvent déroutant au premier abord : appeler `this.http.get(...)` ne déclenche **pas** immédiatement la requête réseau. Cette méthode retourne un Observable qui décrit la requête à effectuer, mais rien ne part réellement tant que personne ne s'y "abonne". Ce comportement est qualifié de "lazy" (paresseux). C'est l'appel à `.subscribe(...)` qui déclenche véritablement l'envoi de la requête.

| | Signal | Observable |
|---|---|---|
| Contient | Une valeur **présente** | Une valeur **future** (potentiellement plusieurs dans le temps) |
| Se lit | `monSignal()` | `.subscribe(callback)` |
| Déclenchement | Immédiat | "Lazy" — rien ne se passe tant que personne ne s'abonne |

### Syntaxe du service avec HttpClient

```typescript
import { inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class MonService {
  private http = inject(HttpClient);
  private apiUrl = 'http://localhost:8080/api/ressource';

  getTout(): Observable<Type[]> {
    return this.http.get<Type[]>(this.apiUrl);
  }

  creer(item: Type): Observable<Type> {
    return this.http.post<Type>(this.apiUrl, item);
  }

  supprimer(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  modifier(id: number, item: Type): Observable<Type> {
    return this.http.put<Type>(`${this.apiUrl}/${id}`, item);
  }
}
```

Ce service illustre un pattern important : il ne stocke plus lui-même aucune donnée, contrairement au service à base de signals vu plus tôt. Son unique rôle est désormais de construire et retourner des Observables représentant chaque type de requête possible vers le backend. C'est un changement de responsabilité : la véritable source de vérité des données devient le serveur (et sa base de données), le service Angular n'étant plus qu'un intermédiaire chargé de formuler les bonnes requêtes.

### S'abonner à un Observable (dans un composant)

```typescript
this.monService.getTout().subscribe(data => {
  this.monSignalLocal.set(data);
});

this.monService.supprimer(id).subscribe(() => {
  // exécuté une fois la suppression confirmée par le serveur
  this.rechargerListe();
});
```

Ce qui se passe concrètement, dans l'ordre chronologique : l'appel à `this.monService.getTout()` ne fait que préparer et retourner un Observable, sans effet immédiat. L'appel enchaîné à `.subscribe(callback)` déclenche alors réellement l'envoi de la requête HTTP vers le serveur. Le navigateur attend ensuite la réponse — pendant ce temps, le reste du code du composant continue de s'exécuter normalement, sans être bloqué. Ce n'est que lorsque la réponse arrive effectivement que la fonction `callback` passée à `.subscribe()` est exécutée, avec la donnée reçue en paramètre. C'est à ce moment précis, souvent bien plus tard dans le temps par rapport aux lignes de code qui l'entourent, qu'on peut par exemple mettre à jour un signal local avec cette donnée.

**Dans le projet** — [`services/contact.ts`](../carnet-contact_frontend/src/app/services/contact.ts)

Le service du carnet a d'abord ressemblé exactement au bloc générique ci-dessus (des méthodes retournant `Observable<Contact[]>`, l'abonnement fait dans les composants). Il a ensuite évolué vers le pattern de la **section 12** : l'`Observable` de `http.get/post/put/delete` est toujours là, mais l'abonnement (`.subscribe()`) a été remonté dans le service, qui range le résultat dans un signal. Les méthodes retournent désormais `void`.

```typescript
// état actuel — l'Observable est un détail interne, plus une valeur de retour
chargerContacts(): void {
  this.http.get<Contact[]>(this.apiUrl).subscribe(data => {
    this.contactsSignal.set(data);
  });
}
```

---

## 11. Cycle de vie d'un composant

### `ngOnInit()`

```typescript
import { Component, OnInit } from '@angular/core';

export class MonComposant implements OnInit {
  ngOnInit(): void {
    // exécuté UNE SEULE FOIS, juste après la création du composant
    // endroit conventionnel pour déclencher un premier chargement de données
  }
}
```

Angular appelle automatiquement, à des moments précis et prévisibles de l'existence d'un composant, un certain nombre de méthodes spéciales appelées "hooks de cycle de vie" — `ngOnInit()` en est la plus utilisée. Elle est déclenchée une seule fois, juste après qu'Angular ait fini de créer le composant et d'initialiser ses propriétés de base, mais avant que l'utilisateur ne voie quoi que ce soit à l'écran.

La question qui revient souvent est : pourquoi ne pas simplement mettre cette logique directement dans le constructeur de la classe ? La convention Angular réserve le constructeur à une initialisation très basique (typiquement, recevoir des dépendances injectées), et déconseille d'y placer une logique plus complexe comme un appel réseau. `ngOnInit()` garantit que le composant est déjà pleinement construit et prêt, ce qui le rend plus fiable comme point de départ pour charger des données ou effectuer d'autres opérations d'initialisation qui dépendent de l'état complet du composant.

**Dans le projet** — [`components/contact-list/contact-list.ts`](../carnet-contact_frontend/src/app/components/contact-list/contact-list.ts), et aussi `contact-detail.ts` / `contact-edit.ts`

```typescript
export class ContactList implements OnInit {
  ngOnInit(): void {
    // premier chargement : demande au service de remplir le signal partagé
    this.contactService.chargerContacts();
  }
}
```

`contact-edit.ts` montre la complémentarité `ngOnInit()` / `constructor` : le `constructor` y installe l'`effect()` (enregistrement, pas de logique réseau), tandis que `ngOnInit()` déclenche le chargement des données.

---

## 12. Signal partagé alimenté par HttpClient

### Le problème : plusieurs copies d'une même vérité

Dès qu'une application dépasse un seul composant, une question se pose : où ranger les données reçues du serveur ? La solution la plus immédiate consiste à donner à chaque composant son propre signal local, qu'il remplit lui-même en s'abonnant au service. Cela fonctionne parfaitement tant qu'un seul composant affiche la donnée — mais le jour où un deuxième composant la modifie, le défaut de conception apparaît : chaque composant détient sa propre copie de la liste, et rien ne prévient les autres qu'une copie vient de changer.

Le symptôme typique est un affichage devenu périmé : la donnée est bien enregistrée côté serveur, mais l'écran continue d'afficher l'état d'avant. Le réflexe de contournement — recharger toute la page avec `window.location.reload()` — fonctionne, mais il détruit et reconstruit l'application entière pour resynchroniser une simple liste, ce qui annule l'intérêt même d'une application monopage.

Le pattern du signal partagé résout ce problème en déplaçant la donnée du composant vers le service. Comme un service `providedIn: 'root'` est un singleton, tous les composants qui l'injectent reçoivent la même instance, donc le même signal : ils regardent littéralement la même boîte en mémoire. Un composant écrit, les autres voient le changement immédiatement, sans qu'aucun code de synchronisation n'ait besoin d'être écrit.

### La répartition des rôles entre Observable et signal

Ce pattern ne remplace pas les Observables par des signals : il donne à chacun le rôle pour lequel il est fait, au lieu de les mettre en concurrence.

| Outil | Rôle dans ce pattern |
|---|---|
| **Observable** | Le *transport* — il représente la réponse future du serveur, le temps que la requête réseau aboutisse |
| **Signal** | Le *stockage* — il contient la valeur présente, celle que les composants affichent à l'instant T |

Le service devient le point de conversion de l'un vers l'autre : il reçoit un Observable, en extrait la valeur, et la dépose dans le signal. Les composants, eux, ne manipulent plus que des signals et ne voient plus jamais passer d'Observable.

### Qui s'abonne : le service, pas le composant

C'est le changement de pratique le plus important de ce pattern. L'approche habituelle consiste à appeler `.subscribe()` dans le composant, puis à ranger le résultat dans un signal local. Ici, l'abonnement remonte dans le service.

La raison est directe : si chaque composant s'abonnait de son côté et stockait le résultat chez lui, on recréerait exactement le problème que l'on cherche à supprimer — autant de copies de la vérité que de composants abonnés. Il faut donc **un seul endroit** qui reçoive la réponse du serveur et l'écrive dans la boîte partagée. Ce seul endroit est le service, puisqu'il est aussi le propriétaire du signal.

Une conséquence pratique agréable : les méthodes du service ne retournent plus d'`Observable` mais `void`. L'appelant ne dit plus « donne-moi les données pour que je les range », il dit simplement « mets la liste à jour ».

### Convention de lecture des étapes ci-dessous

Migrer vers ce pattern ne se fait pas d'un bloc : on procède par étapes, et l'application doit rester fonctionnelle à chacune d'elles. Cela suppose de faire cohabiter temporairement l'ancienne et la nouvelle façon de faire. Une partie du code écrit en cours de route n'a donc pas vocation à survivre — c'est de l'échafaudage, utile le temps de la transition, puis retiré.

Les blocs de code qui suivent portent une mention en commentaire pour lever cette ambiguïté :

| Mention | Signification |
|---|---|
| `[DÉFINITIF]` | Fait partie du résultat final et restera dans le projet |
| `[PROVISOIRE]` | Échafaudage : nécessaire pour que l'application fonctionne à cette étape précise, mais destiné à disparaître plus loin |

À la fin de chaque étape, un encadré **« Dans le projet »** donne le ou les fichiers réellement modifiés du carnet de contacts, avec l'extrait de code correspondant. Le bloc générique au-dessus (`MonService`, `Item`, `ItemList`…) reste la version réutilisable dans un futur projet ; l'encadré « Dans le projet » est sa traduction concrète, à ouvrir en parallèle du fichier pour suivre le code source.

### Étape 1 — Le service devient propriétaire de la donnée

La première étape ne modifie aucun composant : elle se contente d'ajouter au service la boîte partagée et la méthode qui la remplit. Rien ne change à l'écran, puisque personne ne s'en sert encore. C'est volontaire : on installe la nouvelle plomberie à côté de l'ancienne, sans rien casser.

Le signal est déclaré en deux temps — une version privée et modifiable, une version publique en lecture seule. Ce dédoublement est le mécanisme d'encapsulation permis par `asReadonly()` : les composants pourront lire la liste, mais le compilateur TypeScript leur refusera toute écriture directe, ce qui force le passage par les méthodes du service.

```typescript
@Injectable({ providedIn: 'root' })
export class MonService {
  private http = inject(HttpClient);
  private apiUrl = 'http://localhost:8080/api/ressource';

  // [DÉFINITIF] La donnée partagée vit ici, dans le service singleton.
  // Version privée : seul le service peut l'écrire.
  private itemsSignal = signal<Item[]>([]);

  // [DÉFINITIF] Version exposée aux composants : lisible, non modifiable.
  readonly items = this.itemsSignal.asReadonly();

  // [DÉFINITIF] Le service s'abonne lui-même et range la réponse.
  // Retourne void : l'appelant n'a rien à faire du résultat.
  chargerItems(): void {
    this.http.get<Item[]>(this.apiUrl).subscribe(data => {
      this.itemsSignal.set(data);   // .set() car on remplace toute la liste
    });
  }

  // [PROVISOIRE] Ancienne méthode retournant l'Observable brut.
  // Conservée tant que des composants s'y abonnent encore ;
  // supprimée une fois la migration terminée.
  getItems(): Observable<Item[]> {
    return this.http.get<Item[]>(this.apiUrl);
  }
}
```

On utilise `.set()` et non `.update()` parce qu'on remplace intégralement la liste par ce que le serveur vient d'envoyer : il n'est pas nécessaire de consulter l'ancienne valeur pour construire la nouvelle.

**Dans le projet** — [`carnet-contact_frontend/src/app/services/contact.ts`](../carnet-contact_frontend/src/app/services/contact.ts)

```typescript
private contactsSignal = signal<Contact[]>([]);
readonly contacts = this.contactsSignal.asReadonly();

chargerContacts(): void {
  this.http.get<Contact[]>(this.apiUrl).subscribe(data => {
    this.contactsSignal.set(data);
  });
}
```

### Étape 2 — Le composant lit le signal du service

Le composant abandonne son signal local et pointe vers celui du service. La ligne clé mérite d'être lue attentivement, car elle est souvent mal comprise : elle ne **copie pas** la liste, elle donne un second nom à la même boîte en mémoire. Les deux propriétés — celle du composant et celle du service — désignent le même objet ; quand le service écrit dedans, le composant n'a strictement rien à faire pour en être informé.

```typescript
export class ItemList implements OnInit {
  // [DÉFINITIF] L'ORDRE COMPTE : le service doit être injecté avant d'être
  // utilisé par la propriété suivante (les champs d'une classe sont
  // initialisés dans leur ordre de déclaration).
  private monService = inject(MonService);

  // [DÉFINITIF] Référence vers le signal du service, PAS une copie.
  // Type obtenu : Signal<Item[]>, en lecture seule.
  items = this.monService.items;

  ngOnInit(): void {
    // [DÉFINITIF] On demande au service de remplir la boîte partagée.
    // Plus aucun .subscribe() ici, plus aucun Observable visible.
    this.monService.chargerItems();
  }

  supprimer(id: number): void {
    // [PROVISOIRE] Ancienne façon de faire : on s'abonne dans le composant,
    // puis on recharge toute la liste depuis le serveur.
    // Sera remplacé par une mise à jour locale du signal.
    this.monService.supprimerItem(id).subscribe(() => {
      this.monService.chargerItems();
    });
  }
}
```

Deux observations utiles à ce stade. D'abord, le template HTML n'a besoin d'aucune modification : il lisait déjà le signal avec des parenthèses (`items()`), et ce contrat de lecture est inchangé — seul le propriétaire du signal a bougé. Ensuite, l'import de `signal` disparaît du composant, puisqu'il n'en crée plus aucun : le composant redevient un pur consommateur d'affichage.

Un piège concret guette sur l'ordre des déclarations. Écrire la propriété `items` **avant** la ligne `inject()` provoquerait une erreur à l'exécution (`cannot read properties of undefined`), car les champs d'une classe TypeScript sont initialisés dans leur ordre d'écriture : au moment d'évaluer `this.monService.items`, le service ne serait pas encore injecté.

**Dans le projet** — [`carnet-contact_frontend/src/app/components/contact-list/contact-list.ts`](../carnet-contact_frontend/src/app/components/contact-list/contact-list.ts)

```typescript
private contactService = inject(ContactService);

// Référence vers le signal du service, pas une copie.
contacts = this.contactService.contacts;

ngOnInit(): void {
  this.contactService.chargerContacts();
}
```

Le template [`contact-list.html`](../carnet-contact_frontend/src/app/components/contact-list/contact-list.html), lui, n'a pas changé à cette étape : il faisait déjà `@for (contact of contacts(); track contact.id)`.

### Étape 3 — L'écriture met à jour le signal partagé

C'est l'étape où le bénéfice devient visible. Jusqu'ici, la donnée avait simplement changé de propriétaire ; maintenant, les opérations d'écriture vont alimenter la boîte partagée, et tous les composants qui la regardent se mettront à jour d'eux-mêmes. Le rechargement complet de la page devient inutile et peut être supprimé.

La méthode d'écriture du service suit la même transformation que la méthode de lecture : elle ne retourne plus d'`Observable`, elle s'abonne elle-même et dépose le résultat dans le signal.

Un point mérite une attention particulière, car il est source d'un bug discret : **il faut ajouter à la liste la réponse du serveur, et non l'objet qu'on lui a envoyé.** Les deux ne sont pas identiques. L'objet construit par le formulaire ne possède pas encore d'identifiant — c'est la base de données qui l'attribue au moment de l'enregistrement (côté JPA, via `@GeneratedValue`). La réponse du serveur, elle, contient l'entité complète, identifiant inclus. Ajouter l'objet envoyé placerait dans la liste un élément dont l'`id` vaut `undefined`, ce qui casserait le `track` de la boucle d'affichage et rendrait inopérant tout bouton agissant sur cet identifiant (suppression, modification, navigation vers une page de détail).

```typescript
// Dans le service

// [DÉFINITIF] Ne retourne plus d'Observable : le service fait le travail
// complet — requête, attente de la réponse, mise à jour de l'état.
ajouterItem(item: Item): void {
  this.http.post<Item>(this.apiUrl, item).subscribe(itemCree => {
    // On ajoute la RÉPONSE DU SERVEUR (itemCree), pas l'objet envoyé (item) :
    // seule la réponse porte l'id généré par la base de données.
    // .update() car la nouvelle valeur dépend de l'ancienne.
    // Spread [...] pour créer un nouveau tableau (règle d'immutabilité).
    this.itemsSignal.update(liste => [...liste, itemCree]);
  });
}
```

Côté composant, la méthode se réduit à une seule ligne. Elle ne s'abonne plus, n'attend plus rien et ne recharge plus la page : elle transmet la demande au service, qui se charge de tout le reste.

```typescript
// Dans le composant parent

// [DÉFINITIF] Plus de .subscribe(), plus de window.location.reload().
ajouter(item: Item): void {
  this.monService.ajouterItem(item);
}
```

Le résultat à observer est caractéristique de ce pattern : le composant qui **affiche** la liste n'a pas été modifié du tout, et se met pourtant à jour instantanément après un ajout effectué par un **autre** composant. Aucun code de synchronisation n'a été écrit entre les deux — ils partagent simplement le même signal, et Angular se charge de rafraîchir l'affichage qui en dépend.

Un bon test de vérification consiste à ajouter un élément, puis à agir immédiatement dessus (le supprimer, par exemple) sans recharger la page. Si l'opération fonctionne, c'est la preuve que l'identifiant généré par la base a bien été récupéré depuis la réponse du serveur ; si elle échoue, c'est le signe que l'objet envoyé a été ajouté à la place de la réponse.

**Dans le projet** — [`carnet-contact_frontend/src/app/services/contact.ts`](../carnet-contact_frontend/src/app/services/contact.ts)

```typescript
addContact(contact: Contact): void {
  this.http.post<Contact>(this.apiUrl, contact).subscribe(contactCree => {
    // On ajoute la réponse du SERVEUR : elle porte l'id généré par la base.
    this.contactsSignal.update(liste => [...liste, contactCree]);
  });
}
```

**Dans le projet** — [`carnet-contact_frontend/src/app/pages/accueil/accueil.ts`](../carnet-contact_frontend/src/app/pages/accueil/accueil.ts)

```typescript
ajouterContact(contact: Contact): void {
  this.contactService.addContact(contact);
}
```

Au moment où cette étape a été faite, cette méthode se trouvait dans `app.ts` (voir section 13 : elle a ensuite été déplacée dans le composant de page `Accueil` lors de la mise en place du routing).

### Étape 4 — La suppression met à jour le signal localement

Une opération de suppression écrite naïvement enchaîne deux requêtes : d'abord le `DELETE`, puis un `GET` complet pour récupérer la liste à jour. Le second appel demande pourtant au serveur une information déjà connue du client — la liste précédente, moins l'élément retiré. On peut donc l'économiser en modifiant le signal directement.

```typescript
// Dans le service

// [DÉFINITIF]
supprimerItem(id: number): void {
  this.http.delete<void>(`${this.apiUrl}/${id}`).subscribe(() => {
    // Mise à jour locale : inutile de redemander la liste au serveur,
    // on sait déjà à quoi elle doit ressembler.
    // .filter() renvoie un nouveau tableau (règle d'immutabilité).
    this.itemsSignal.update(liste => liste.filter(item => item.id !== id));
  });
}
```

```typescript
// Dans le composant

// [DÉFINITIF] Le composant ne fait plus que transmettre la demande.
supprimer(id: number): void {
  this.monService.supprimerItem(id);
}
```

La mise à jour locale mérite d'être comprise comme un arbitrage plutôt que comme une règle absolue. Elle repose sur une hypothèse : le serveur a fait exactement ce qui lui était demandé, et personne d'autre n'a modifié les données entre-temps. Sur une application mono-utilisateur, cette hypothèse est sûre, et l'économie d'une requête réseau est un gain net. Sur une application où plusieurs personnes travaillent simultanément sur les mêmes données, la liste locale peut en revanche diverger de celle du serveur — on préfère alors recharger depuis le serveur après chaque écriture, ou mettre en place un mécanisme de synchronisation plus élaboré.

**Dans le projet** — [`carnet-contact_frontend/src/app/services/contact.ts`](../carnet-contact_frontend/src/app/services/contact.ts) et [`contact-list.ts`](../carnet-contact_frontend/src/app/components/contact-list/contact-list.ts)

```typescript
// contact.ts
deleteContact(id: number): void {
  this.http.delete<void>(`${this.apiUrl}/${id}`).subscribe(() => {
    this.contactsSignal.update(liste => liste.filter(c => c.id !== id));
  });
}

// contact-list.ts
supprimer(id: number): void {
  this.contactService.deleteContact(id);
}
```

### Le nettoyage final

Une fois toutes les opérations migrées, l'échafaudage doit être retiré. C'est une étape à part entière : du code de transition laissé en place devient du code mort, qui laisse croire à un lecteur futur qu'il existe deux façons valides de faire les choses dans le projet.

| À supprimer | Pourquoi |
|---|---|
| Les anciennes méthodes du service retournant un `Observable` | Plus aucun composant ne s'y abonne ; vérifier par une recherche globale sur leur nom avant de les retirer |
| L'import `Observable` dans le service | Plus aucune signature de méthode ne l'utilise |
| Les signals locaux déclarés dans les composants | Remplacés par la référence au signal du service |
| Les appels à `window.location.reload()` | Le signal partagé propage désormais seul les changements |

Un point peut surprendre au moment de supprimer l'import `Observable` : RxJS n'a pas disparu du projet pour autant. Les méthodes `http.get()`, `http.post()` et `http.delete()` retournent toujours des Observables, et le service s'y abonne toujours. Ce qui a changé, c'est que les Observables ne font plus partie du **vocabulaire** de l'application : ils sont devenus un détail d'implémentation interne au service, invisible depuis les composants. C'est précisément le but recherché — chaque outil à sa place, derrière une frontière claire.

### Comment savoir que la migration est terminée

| Signe | Ce qu'il confirme |
|---|---|
| Aucun `.subscribe()` dans un composant | Tous les abonnements sont remontés dans le service |
| Aucune méthode du service ne retourne d'`Observable` | Le service expose un état, plus des requêtes |
| Aucun `window.location.reload()` | La réactivité repose entièrement sur le signal partagé |
| Les composants d'affichage n'ont pas été modifiés lors des étapes 3 et 4 | Le partage fonctionne : ils se mettent à jour sans code de synchronisation |

Ce dernier point est le meilleur indicateur de réussite du pattern. Un composant qui affiche une liste ne devrait avoir été touché qu'une seule fois — à l'étape 2, pour pointer vers le signal du service. Toutes les écritures effectuées ensuite par d'autres composants se répercutent sur son affichage sans qu'une seule ligne n'ait été ajoutée chez lui.

### Récapitulatif du statut de chaque élément

| Élément | Statut | Devenir |
|---|---|---|
| `private itemsSignal = signal<Item[]>([])` | Définitif | Cœur du pattern — la source de vérité côté client |
| `readonly items = this.itemsSignal.asReadonly()` | Définitif | Cœur du pattern — la vitrine en lecture seule |
| `chargerItems(): void` dans le service | Définitif | Remplace le chargement fait par chaque composant |
| `ajouterItem()` et `supprimerItem()` retournant `void` | Définitif | Écrivent dans le signal partagé au lieu de retourner un Observable |
| Import `Observable` dans le service | Supprimé | Retiré au nettoyage final : plus aucune signature ne l'utilise |
| `items = this.monService.items` dans le composant | Définitif | Remplace le signal local du composant |
| Signal local `items = signal<Item[]>([])` dans le composant | Supprimé | Remplacé dès l'étape 2 par la référence au signal du service |
| `getItems(): Observable<Item[]>` dans le service | Supprimé | Retiré au nettoyage final, une fois plus aucun composant abonné |
| `.subscribe()` écrit dans un composant | Supprimé | Retiré à l'étape 4 : plus aucun composant ne s'abonne |
| `window.location.reload()` après une écriture | Supprimé | Retiré à l'étape 3, dès que le signal partagé propage seul les changements |

> Les extraits « Dans le projet » de cette section montrent le service tel qu'il était à la fin de la migration. Chaque méthode a ensuite reçu un `.pipe(catchError(...))` pour gérer les échecs réseau — voir section 15.

---

## 13. Routing Angular

### Le problème résolu

Une application à écran unique fonctionne, mais l'URL du navigateur y reste figée : elle n'indique jamais *où* l'on se trouve dans l'application. Cela ferme trois portes concrètes. Impossible d'afficher un seul élément en pleine page sans le reste autour. Impossible de partager ou de mettre en favori un état précis de l'application, puisqu'il n'a pas d'adresse. Et le bouton « Retour » du navigateur devient inutilisable, faute d'historique de navigation interne.

Le routing établit une correspondance entre **une URL et un composant à afficher**. L'URL cesse d'être décorative : elle devient une partie de l'état de l'application, lisible et modifiable par l'utilisateur. Tout cela sans jamais recharger la page — le navigateur ne redemande rien au serveur, c'est le routeur qui remplace le contenu affiché.

### Mise en place

`provideRouter(routes)` s'ajoute une seule fois dans `app.config.ts`, aux côtés des autres providers. La constante `routes` est un tableau qui associe chaque chemin à un composant.

```typescript
// app.routes.ts
import { Routes } from '@angular/router';
import { Accueil } from './pages/accueil/accueil';
import { Detail } from './pages/detail/detail';

export const routes: Routes = [
  // Chemin vide = la racine du site (http://localhost:4200/).
  // pathMatch: 'full' : ne correspondre QUE si l'URL est entièrement vide
  // (sans lui, '' correspond par simple préfixe, donc à presque tout).
  { path: '', component: Accueil, pathMatch: 'full' },

  // ':id' est un SEGMENT VARIABLE : il capture n'importe quelle valeur
  // rencontrée à cette position et la range sous le nom "id".
  // /element/5, /element/42... correspondent tous à cette route.
  { path: 'element/:id', component: Detail },
];
```

Les chemins s'écrivent **sans barre oblique initiale** : `path: ''`, `path: 'element/:id'`, jamais `path: '/'`. La barre est implicite.

**Dans le projet** — [`carnet-contact_frontend/src/app/app.routes.ts`](../carnet-contact_frontend/src/app/app.routes.ts)

```typescript
export const routes: Routes = [
  { path: '', component: Accueil, pathMatch: 'full' },
  { path: 'contact/:id', component: ContactDetail },
  { path: 'contact/:id/modifier', component: ContactEdit }
];
```

Le `provideRouter(routes)`, lui, est dans [`app.config.ts`](../carnet-contact_frontend/src/app/app.config.ts) — il y était déjà, laissé par `ng new`.

### `<router-outlet />` — l'emplacement d'insertion

Le composant racine cesse d'afficher directement du contenu : il devient une **coquille** qui ne contient que ce qui est commun à toutes les pages (un titre, un menu de navigation), plus un `<router-outlet />`. C'est à cet endroit précis que le routeur insère le composant correspondant à l'URL courante.

```html
<!-- app.html -->
<h1>Mon application</h1>
<nav><!-- liens de navigation communs --></nav>

<!-- Le composant de la page active s'insère ici. -->
<router-outlet />
```

```typescript
// app.ts — importe RouterOutlet, ne connaît plus aucune donnée métier
import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.html',
})
export class App {}
```

Une distinction d'organisation utile en découle : les composants associés à une URL se rangent dans un dossier `pages/`, tandis que `components/` garde les briques réutilisables insérées *à l'intérieur* d'une page. Un composant de liste n'est pas une page ; la page qui l'affiche en est une.

**Dans le projet** — [`app.ts`](../carnet-contact_frontend/src/app/app.ts) et [`app.html`](../carnet-contact_frontend/src/app/app.html)

```typescript
// app.ts — réduit à une coquille : plus aucune donnée métier
export class App {
  protected readonly title = signal('carnet-contact');
}
```

```html
<!-- app.html -->
<h1>{{ title() }}</h1>
<router-outlet />
```

Le contenu de l'ancienne page unique (le formulaire + la liste + la méthode `ajouterContact`) a été déplacé dans [`pages/accueil/accueil.ts`](../carnet-contact_frontend/src/app/pages/accueil/accueil.ts), désormais une page à part entière branchée sur la route `''`.

### Naviguer : `routerLink` plutôt que `href`

Écrire `<a href="/element/5">` serait une erreur de fond. Un `href` classique demande au navigateur d'aller chercher une nouvelle page auprès du serveur : il détruit l'application en cours, la retélécharge et la redémarre entièrement. Tous les services sont recréés, leurs signals repartent vides, les appels réseau sont à refaire.

`routerLink` intercepte le clic, change l'URL affichée sans rien recharger, et demande au routeur de remplacer le contenu du `<router-outlet />`. L'application reste vivante, les services gardent leur instance et leurs données.

```html
<!-- Forme statique : l'URL est connue à l'écriture -->
<a routerLink="/">Accueil</a>

<!-- Forme dynamique : binding de propriété, l'URL est construite à partir
     d'un tableau de segments. ['/element', 5] produit /element/5 -->
<a [routerLink]="['/element', element.id]">{{ element.nom }}</a>
```

Dans les deux cas, il faut ajouter `RouterLink` aux `imports` du composant qui utilise la directive.

| Élément | Rôle |
|---|---|
| `provideRouter(routes)` | Active le routeur pour toute l'application (dans `app.config.ts`) |
| `Routes` | Type du tableau associant chemins et composants |
| `path: ''` | Chemin racine ; `pathMatch: 'full'` pour une correspondance exacte |
| `path: 'x/:id'` | Segment variable `:id`, capturé pour être relu dans le composant |
| `<router-outlet />` | Emplacement où le composant de la route active est inséré |
| `routerLink="/x"` | Lien de navigation interne, sans rechargement de page |
| `[routerLink]="['/x', v]"` | Même chose, URL construite à partir de segments dynamiques |

**Dans le projet** — [`contact-list.html`](../carnet-contact_frontend/src/app/components/contact-list/contact-list.html)

```html
@for (contact of contacts(); track contact.id) {
  <li>
    <a [routerLink]="['/contact', contact.id]">
      {{ contact.prenom }} {{ contact.nom }}
    </a>
    — {{ contact.email }} — {{ contact.telephone }}
    <button (click)="supprimer(contact.id)">Supprimer</button>
  </li>
}
```

`contact-list.ts` a dû ajouter `RouterLink` à ses `imports` pour que le template ait le droit d'utiliser la directive.

### Lire un paramètre d'URL : `ActivatedRoute`

Le routeur capture la valeur du segment `:id`, mais elle ne se retrouve pas d'elle-même dans la classe du composant. `ActivatedRoute` est un service — on l'injecte comme n'importe quel autre — dont le rôle est de représenter la route actuellement active et de donner accès à ses paramètres.

```typescript
import { Component, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

export class Detail {
  private route = inject(ActivatedRoute);

  // .snapshot : une PHOTO figée de la route au moment de la création du
  // composant. paramMap.get('id') renvoie toujours une chaîne (une URL
  // n'a pas de type) — d'où la conversion si on attend un nombre.
  private id = Number(this.route.snapshot.paramMap.get('id'));
}
```

Le `.snapshot` suffit tant qu'aucun lien ne mène **directement** d'une page paramétrée à une autre page de la même route (par exemple d'un `/element/5` vers un `/element/6`). Dans ce cas particulier, Angular réutilise l'instance du composant au lieu de la recréer, et le `.snapshot` — lu une seule fois — ne se met pas à jour. Pour gérer ce cas, on lit `route.paramMap` sous forme d'Observable et on s'y abonne ; tant qu'il ne se présente pas, le `.snapshot` reste la solution la plus simple.

### Afficher la donnée : `ActivatedRoute` + signal partagé + `computed()`

Une page de détail combine naturellement les trois notions : l'id vient de l'URL (`ActivatedRoute`), les données viennent du service (signal partagé, section 12), et la fiche à afficher se déduit des deux (`computed()`, section 3).

```typescript
export class Detail implements OnInit {
  private route = inject(ActivatedRoute);
  private service = inject(MonService);

  private id = Number(this.route.snapshot.paramMap.get('id'));

  // Se recalcule seul quand items() change : si la liste est encore vide
  // au premier affichage (réponse serveur en attente), element() vaut
  // undefined, puis se remplit dès l'arrivée des données.
  element = computed(() =>
    this.service.items().find(item => item.id === this.id)
  );

  ngOnInit(): void {
    // Nécessaire en cas d'accès direct à l'URL (lien partagé, F5) :
    // le signal partagé serait alors vide, personne ne l'ayant rempli.
    this.service.chargerItems();
  }
}
```

```html
<!-- '; as e' capture le résultat de element() dans une variable locale e,
     réutilisable dans tout le bloc, et évite de forcer le typage à
     chaque ligne (element() pouvant valoir undefined). -->
@if (element(); as e) {
  <h2>{{ e.nom }}</h2>
  <p>{{ e.description }}</p>
} @else {
  <p>Élément introuvable (ou en cours de chargement).</p>
}

<a routerLink="/">Retour</a>
```

**Dans le projet** — [`pages/contact-detail/contact-detail.ts`](../carnet-contact_frontend/src/app/pages/contact-detail/contact-detail.ts) et [`contact-detail.html`](../carnet-contact_frontend/src/app/pages/contact-detail/contact-detail.html)

```typescript
private route = inject(ActivatedRoute);
private contactService = inject(ContactService);

private contactId = Number(this.route.snapshot.paramMap.get('id'));

contact = computed(() =>
  this.contactService.contacts().find(c => c.id === this.contactId)
);

ngOnInit(): void {
  this.contactService.chargerContacts();
}
```

> **Note de mise à jour.** Depuis la pagination (section 22), cette page appelle `chargerContact(id)` et lit un signal dédié `contactCourant`, au lieu de fouiller la liste. Le raisonnement de cette section reste entier — lire l'id dans l'URL, afficher un signal, laisser le gabarit se réafficher tout seul à l'arrivée de la réponse ; seule la **source** du signal a changé, parce que la liste ne contient plus qu'une page.

```html
@if (contact(); as c) {
  <h2>{{ c.prenom }} {{ c.nom }}</h2>
  <p>Email : {{ c.email }}</p>
  <p>Téléphone : {{ c.telephone }}</p>
  <a [routerLink]="['/contact', c.id, 'modifier']">Modifier</a>
} @else {
  <p>Contact introuvable (ou en cours de chargement).</p>
}
<p><a routerLink="/">Retour à la liste</a></p>
```

### Naviguer depuis le code : `Router.navigate()`

`routerLink` déclenche une navigation sur un **clic** de l'utilisateur. Il arrive qu'on veuille naviguer depuis le code TypeScript, après qu'une action se soit terminée : rediriger vers la fiche d'un élément une fois enregistré, renvoyer vers l'accueil après une déconnexion, etc. C'est le rôle du service `Router`.

```typescript
import { inject } from '@angular/core';
import { Router } from '@angular/router';

export class MonComposant {
  private router = inject(Router);

  apresAction(): void {
    // Même tableau de segments que [routerLink].
    this.router.navigate(['/element', id]);
  }
}
```

| | Déclenché par | Où on l'écrit |
|---|---|---|
| `routerLink` / `[routerLink]` | Un clic sur un `<a>` | Le template HTML |
| `Router.navigate([...])` | Du code | La classe TypeScript, en fin de traitement |

### Routing et rendu côté serveur (SSR)

Un projet généré avec le SSR activé possède un fichier `app.routes.server.ts` qui indique, pour chaque route, *comment* la page doit être produite. Par défaut, toutes les routes sont pré-générées au moment du `build` (`RenderMode.Prerender`) — excellent pour des pages au contenu fixe.

Une route paramétrée comme `element/:id` pose problème : au moment du build, les identifiants n'existent pas encore (ils vivent en base, à l'exécution). Angular ne peut pas deviner quelles pages fabriquer. On déclare donc cette route en `RenderMode.Client` : le navigateur la construira lui-même.

```typescript
// app.routes.server.ts
import { RenderMode, ServerRoute } from '@angular/ssr';

export const serverRoutes: ServerRoute[] = [
  // Les id n'existent qu'à l'exécution : pas de prérendu possible.
  { path: 'element/:id', renderMode: RenderMode.Client },

  // Toutes les autres routes restent pré-générées au build.
  { path: '**', renderMode: RenderMode.Prerender },
];
```

Sans cet ajustement, `ng build` échoue en réclamant la liste des identifiants à pré-générer.

**Dans le projet** — [`carnet-contact_frontend/src/app/app.routes.server.ts`](../carnet-contact_frontend/src/app/app.routes.server.ts)

```typescript
export const serverRoutes: ServerRoute[] = [
  { path: 'contact/:id', renderMode: RenderMode.Client },
  { path: 'contact/:id/modifier', renderMode: RenderMode.Client },
  { path: '**', renderMode: RenderMode.Prerender }
];
```

## 14. Modification d'une ressource (PUT, formulaire pré-rempli)

### Vue d'ensemble

Modifier une ressource existante réunit des briques déjà vues et en ajoute quelques-unes. Le formulaire réactif (section 7) est réutilisé, mais pré-rempli au lieu d'être vide. Le service (section 12) gagne une méthode d'écriture de plus, sur le même modèle que l'ajout. Le routing (section 13) fournit l'id à modifier et le retour vers la fiche. Les notions réellement nouvelles sont au nombre de quatre : le verbe HTTP `PUT` côté client et côté serveur, la combinaison `@PathVariable` + `@RequestBody` dans une même méthode Java, `patchValue()` pour remplir un formulaire, et `effect()` pour réagir à l'arrivée asynchrone des données.

### Côté serveur : `@PutMapping` et deux sources d'entrée

Une méthode de contrôleur peut lire **plusieurs entrées de nature différente** dans la même signature. Pour une mise à jour, il en faut deux : *quel* enregistrement modifier (dans l'URL) et *avec quelles valeurs* (dans le corps de la requête).

```java
// @PathVariable  : lit un morceau de l'URL  (/api/ressource/5 -> 5)
// @RequestBody   : désérialise le JSON reçu en objet Java
@PutMapping("/{id}")
public Ressource update(@PathVariable Long id, @RequestBody Ressource recue) {
    // On impose l'id de l'URL à l'objet reçu : le client ne peut pas,
    // via le corps JSON, viser un autre enregistrement que celui de l'URL.
    recue.setId(id);
    // save() fait un INSERT si l'id est absent, un UPDATE s'il correspond
    // à une ligne existante — la même méthode pour les deux cas.
    return repository.save(recue);
}
```

| Annotation | Rôle |
|---|---|
| `@PutMapping("/{id}")` | Associe la méthode aux requêtes `PUT` sur `/base/{id}` |
| `@PathVariable Long id` | Injecte le segment `{id}` de l'URL dans le paramètre |
| `@RequestBody Ressource recue` | Convertit le corps JSON de la requête en objet Java |

### Côté service : une écriture de plus, sur le modèle de l'ajout

```typescript
// [DÉFINITIF] Même forme que la méthode d'ajout : s'abonne, puis met à
// jour le signal partagé avec la réponse du serveur.
modifierRessource(r: Ressource): void {
  this.http.put<Ressource>(`${this.apiUrl}/${r.id}`, r).subscribe(maj => {
    // .map() renvoie un NOUVEAU tableau : l'élément modifié est remplacé
    // par la réponse du serveur, tous les autres restent identiques.
    this.itemsSignal.update(liste =>
      liste.map(item => (item.id === maj.id ? maj : item))
    );
  });
}
```

`.map()` complète la panoplie des mises à jour immuables d'un signal-liste, aux côtés du spread `[...liste, x]` (ajout) et de `.filter()` (suppression) déjà vus. Les trois ont un point commun : elles renvoient un nouveau tableau, jamais l'ancien modifié en place.

| Opération | Transformation immuable |
|---|---|
| Ajouter | `liste => [...liste, nouvel]` |
| Modifier | `liste => liste.map(x => x.id === maj.id ? maj : x)` |
| Supprimer | `liste => liste.filter(x => x.id !== id)` |

### Pré-remplir le formulaire : `patchValue()`

Un `FormGroup` (section 7) démarre avec les valeurs passées à sa création — vides pour un formulaire d'ajout. Pour un formulaire d'édition, il faut y injecter les valeurs actuelles de la ressource. `patchValue()` fait exactement cela : il affecte les champs dont le nom correspond, et ignore les clés en trop dans l'objet fourni.

```typescript
// L'objet peut contenir plus de champs que le formulaire (ici 'id') :
// patchValue ne garde que ceux qui correspondent à un contrôle.
this.form.patchValue(ressource);
```

`patchValue()` est tolérant (champs manquants acceptés) ; sa variante `setValue()` exige un objet correspondant **exactement** aux contrôles du formulaire, ni plus ni moins.

### Réagir à l'arrivée des données : `effect()`

Sur une page d'édition, les valeurs à pré-remplir viennent du signal partagé, qui est encore vide au premier affichage (la réponse HTTP n'est pas arrivée). Il faut donc pré-remplir le formulaire *au moment* où la donnée apparaît, pas à la construction du composant.

`effect()` est le pendant « effet de bord » de `computed()` : là où `computed()` **calcule une valeur** à partir de signals, `effect()` **exécute une action** chaque fois qu'un signal qu'il lit change.

```typescript
import { effect } from '@angular/core';

export class Edition {
  private rempli = false;

  constructor() {
    // Relancé à chaque changement de ressource() (donc du signal partagé).
    effect(() => {
      const r = this.ressource();
      if (r && !this.rempli) {
        this.form.patchValue(r);
        this.rempli = true;   // garde-fou : ne pré-remplir qu'une fois,
                              // sinon une MAJ de la liste écraserait la saisie
      }
    });
  }
}
```

`effect()` s'écrit dans le `constructor` (ou via le champ d'une classe), là où le contexte d'injection Angular est disponible. Le garde-fou booléen évite qu'un rechargement ultérieur de la liste (provoqué par une autre action) ne réécrase les modifications en cours de saisie.

| | `computed()` | `effect()` |
|---|---|---|
| Produit | Une valeur (signal en lecture seule) | Rien — déclenche une action |
| Sert à | Dériver une donnée d'autres signals | Synchroniser avec l'extérieur (formulaire, log, `localStorage`…) |
| Se lit avec | `maValeur()` | ne se lit pas |

### Dans le projet

**Serveur** — [`carnet-contact-backend/.../controller/ContactController.java`](../carnet-contact-backend/src/main/java/com/example/carnet_contact_backend/controller/ContactController.java)

```java
@PutMapping("/{id}")
public Contact updateContact(@PathVariable Long id, @RequestBody Contact contact) {
    contact.setId(id);
    return contactRepository.save(contact);
}
```

**Service** — [`carnet-contact_frontend/src/app/services/contact.ts`](../carnet-contact_frontend/src/app/services/contact.ts)

```typescript
modifierContact(contact: Contact): void {
  this.http.put<Contact>(`${this.apiUrl}/${contact.id}`, contact).subscribe(contactMaj => {
    this.contactsSignal.update(liste =>
      liste.map(c => (c.id === contactMaj.id ? contactMaj : c))
    );
  });
}
```

**Page d'édition** — [`carnet-contact_frontend/src/app/pages/contact-edit/contact-edit.ts`](../carnet-contact_frontend/src/app/pages/contact-edit/contact-edit.ts)

```typescript
private id = Number(this.route.snapshot.paramMap.get('id'));

contact = computed(() =>
  this.contactService.contacts().find(c => c.id === this.id)
);

contactForm = this.fb.group({
  nom: ['', Validators.required],
  prenom: ['', Validators.required],
  email: ['', [Validators.required, Validators.email]],
  telephone: ['']
});

private formulaireRempli = false;

constructor() {
  effect(() => {
    const c = this.contact();
    if (c && !this.formulaireRempli) {
      this.contactForm.patchValue(c);
      this.formulaireRempli = true;
    }
  });
}

ngOnInit(): void {
  this.contactService.chargerContacts();
}

onSubmit(): void {
  if (this.contactForm.invalid) return;
  this.contactService.modifierContact({ id: this.id, ...this.contactForm.value } as Contact);
  this.router.navigate(['/contact', this.id]);
}
```

Le template [`contact-edit.html`](../carnet-contact_frontend/src/app/pages/contact-edit/contact-edit.html) est le même formulaire réactif que `contact-form.html`, avec un bouton « Enregistrer » et un lien « Annuler » qui ramène à la fiche.

> **Note de mise à jour.** Comme la page de détail, celle-ci lit désormais le signal `contactCourant` du service et appelle `chargerContact(id)` dans son `ngOnInit` — conséquence de la pagination (section 22). L'`effect()` de pré-remplissage et son drapeau `formulaireRempli`, eux, n'ont pas bougé d'une ligne : c'est exactement le mécanisme décrit ci-dessus, et un test le protège désormais (section 26).

> `modifierContact` a ensuite reçu un `.pipe(catchError(...))`, comme les autres méthodes du service — voir section 15.

## 15. Gestion des erreurs HTTP (`catchError`)

### Le problème : l'échec silencieux

Un `.subscribe(valeur => …)` ne passe qu'une seule fonction : celle du **succès**. Or un Observable a trois issues possibles — émettre une valeur, émettre une **erreur**, ou se terminer. Une requête HTTP échoue pour toutes sortes de raisons : serveur éteint, réponse `500`, `404`, réseau coupé, requête refusée. Sans traitement de l'erreur, rien ne l'attrape : elle finit en erreur non gérée dans la console, et l'utilisateur ne voit **rien** — une liste vide, un bouton sans effet apparent.

Gérer l'erreur, c'est décider ce qui doit se passer à sa place : afficher un message, retomber sur une valeur par défaut, ou réessayer.

### `.pipe()` : insérer des opérateurs avant l'abonnement

`.pipe()` est un « tube » que le flux de données traverse avant d'arriver au `.subscribe()`. On y place des **opérateurs** RxJS qui transforment, filtrent ou — c'est le cas ici — interceptent les erreurs du flux.

```typescript
this.http.get<T[]>(url).pipe(
  operateur1(),
  operateur2(),
).subscribe(valeur => { /* ... */ });
```

### `catchError` : attraper l'erreur et fournir un flux de remplacement

`catchError` ne s'active que si le flux part en erreur. Sa contrainte est stricte : **il doit retourner un Observable**, car il remplace le flux cassé par un flux de secours. Trois retours possibles selon l'intention :

| Retour de `catchError` | Effet sur le `.subscribe()` | Quand l'utiliser |
|---|---|---|
| `of(valeurParDefaut)` | Reçoit `valeurParDefaut` comme si tout allait bien | Une valeur de repli a du sens (liste vide, objet neutre) |
| `EMPTY` | Ne s'exécute pas : le flux se termine sans rien émettre | Un échec d'écriture — on ne veut surtout pas modifier l'état local |
| `throwError(() => err)` | Reçoit l'erreur (déclenche son callback `error`) | On préfère gérer l'erreur plus loin dans la chaîne |

`of(x)` et `EMPTY` viennent de `rxjs` : `of(x)` crée un Observable qui émet `x` puis se termine ; `EMPTY` est un Observable qui se termine immédiatement sans rien émettre.

```typescript
import { of, EMPTY, catchError } from 'rxjs';

// Lecture : une liste vide vaut mieux qu'un plantage
chargerTout(): void {
  this.http.get<T[]>(this.apiUrl).pipe(
    catchError(() => {
      this.erreurSignal.set('Chargement impossible.');
      return of([]);          // le .subscribe() reçoit []
    })
  ).subscribe(data => this.itemsSignal.set(data));
}

// Écriture : en cas d'échec, ne pas toucher à l'état local
ajouter(item: T): void {
  this.http.post<T>(this.apiUrl, item).pipe(
    catchError(() => {
      this.erreurSignal.set("Ajout impossible.");
      return EMPTY;           // le .subscribe() ne s'exécute pas
    })
  ).subscribe(cree => this.itemsSignal.update(l => [...l, cree]));
}
```

### La forme complète de `.subscribe()`

Sans `catchError`, on peut aussi traiter l'erreur directement dans le `.subscribe()`, qui accepte un objet à trois clés :

```typescript
this.http.get<T[]>(url).subscribe({
  next: data => { /* succès */ },
  error: err => { /* échec */ },
  complete: () => { /* flux terminé (rare à utiliser pour du HTTP) */ },
});
```

`catchError` et le callback `error` ne s'excluent pas. La différence : `catchError` agit **dans le flux** (il peut fournir une valeur de repli, réessayer, transformer l'erreur), alors que le callback `error` ne fait que **réagir** une fois l'erreur arrivée au bout. On utilise `catchError` quand le service doit rester maître de ce qui remplace l'échec.

### Exposer l'erreur à l'affichage : un signal d'état

Le service tient un second signal, à côté de celui des données : le dernier message d'erreur, ou `null` s'il n'y a rien à signaler. Même pattern d'encapsulation que pour les données (privé modifiable + vitrine `readonly`).

```typescript
private erreurSignal = signal<string | null>(null);
readonly erreur = this.erreurSignal.asReadonly();
```

Chaque méthode le remet à `null` en début d'appel (on efface l'erreur précédente) et le renseigne dans son `catchError`. Un composant affiche ensuite une bannière conditionnée à ce signal.

```html
@if (service.erreur(); as message) {
  <p class="erreur">{{ message }}</p>
}
```

Une bannière d'erreur (comme un indicateur de chargement) est un affichage **transverse** : il concerne toutes les pages, pas une en particulier. Le composant racine — la coquille du routing, section 13 — est l'endroit légitime pour l'héberger, même s'il doit pour cela injecter le service métier.

### Dans le projet

**Service** — [`carnet-contact_frontend/src/app/services/contact.ts`](../carnet-contact_frontend/src/app/services/contact.ts)

```typescript
import { EMPTY, of, catchError } from 'rxjs';

private erreurSignal = signal<string | null>(null);
readonly erreur = this.erreurSignal.asReadonly();

chargerContacts(): void {
  this.erreurSignal.set(null);
  this.http.get<Contact[]>(this.apiUrl).pipe(
    catchError(() => {
      this.erreurSignal.set('Impossible de charger les contacts. Le serveur est-il démarré ?');
      return of([]);
    })
  ).subscribe(data => this.contactsSignal.set(data));
}

addContact(contact: Contact): void {
  this.erreurSignal.set(null);
  this.http.post<Contact>(this.apiUrl, contact).pipe(
    catchError(() => {
      this.erreurSignal.set("Impossible d'ajouter le contact.");
      return EMPTY;
    })
  ).subscribe(contactCree => {
    this.contactsSignal.update(liste => [...liste, contactCree]);
  });
}
```

`modifierContact` et `deleteContact` suivent le même schéma (retour `EMPTY`).

**Coquille** — [`app.ts`](../carnet-contact_frontend/src/app/app.ts) et [`app.html`](../carnet-contact_frontend/src/app/app.html)

```typescript
// app.ts — la coquille injecte le service pour lire son signal d'erreur
protected contactService = inject(ContactService);
```

```html
<!-- app.html -->
<h1>{{ title() }}</h1>
@if (contactService.erreur(); as message) {
  <p class="erreur">{{ message }}</p>
}
<router-outlet />
```

### Pourquoi le POST met plus longtemps à signaler l'échec que le GET

Serveur éteint : la bannière du `GET` (au chargement) apparaît presque instantanément, celle d'un `POST` d'ajout met quelques secondes. Ce n'est pas un bug du code. Un `POST` qui transporte du JSON est une requête « non anodine » : le navigateur envoie d'abord une requête `OPTIONS` de vérification (le *preflight*, section 33). Quand le serveur ne répond pas, le navigateur laisse ce preflight expirer avant de conclure à l'échec. Le `GET`, requête « simple », part directement et échoue tout de suite.

## 16. Indicateur de chargement (`finalize`)

### Le problème : l'attente invisible

Entre l'instant où `.subscribe()` déclenche une requête et celui où la réponse arrive, l'application ne montre rien. En local avec un serveur rapide, ce trou dure quelques millisecondes — invisible. Mais dès que le réseau ralentit ou que le serveur réfléchit, l'utilisateur fait face à une interface figée et réagit mal : il reclique sur « Valider » (deux requêtes, parfois deux enregistrements), ou il recharge la page en croyant que c'est bloqué (et perd l'état de l'application).

Rendre l'attente visible n'est pas qu'un confort : coupler cet état à un `[disabled]` sur les boutons **empêche** le double-envoi.

### Un signal booléen, allumé avant, éteint après

Le service tient un troisième signal, à côté des données et de l'erreur : `true` tant qu'une requête est en cours. Même encapsulation (privé modifiable + vitrine `readonly`).

```typescript
private chargementSignal = signal(false);
readonly chargement = this.chargementSignal.asReadonly();
```

On le passe à `true` juste avant l'appel HTTP. Reste à le remettre à `false` **quoi qu'il arrive** — succès comme erreur.

### `finalize` : s'exécuter à la fin du flux, pour n'importe quelle raison

Mettre `set(false)` uniquement dans le `.subscribe(next)` ne suffit pas : ce callback ne s'exécute pas si le flux part en erreur — l'indicateur resterait allumé indéfiniment. Le mettre à deux endroits (`.subscribe` **et** `catchError`) fonctionne, mais c'est dupliqué et on oublie vite un cas.

`finalize(callback)` est l'opérateur fait pour ça : son `callback` tourne quand l'Observable se termine, **quelle que soit l'issue** — valeur émise puis complétion, ou erreur. On le place dans le `.pipe()`, **après `catchError`**, pour qu'il s'exécute aussi après le flux de repli.

```typescript
import { catchError, finalize, of } from 'rxjs';

chargerTout(): void {
  this.chargementSignal.set(true);

  this.http.get<T[]>(this.apiUrl).pipe(
    catchError(() => {
      this.erreurSignal.set('Chargement impossible.');
      return of([]);
    }),
    finalize(() => this.chargementSignal.set(false)), // succès OU erreur
  ).subscribe(data => this.itemsSignal.set(data));
}
```

| Emplacement de `set(false)` | Couvre le succès | Couvre l'erreur | Sans duplication |
|---|---|---|---|
| Dans `.subscribe(next)` | oui | **non** | oui |
| Dans `.subscribe(next)` + `catchError` | oui | oui | **non** |
| Dans `finalize()` | oui | oui | oui |

### Afficher l'indicateur et bloquer les boutons

L'indicateur lui-même est transverse : il vit dans la coquille, comme la bannière d'erreur (section 15).

```html
@if (service.chargement()) {
  <p class="chargement">Chargement…</p>
}
```

Le garde-fou anti double-clic, lui, se pose sur chaque bouton qui déclenche une requête. Le composant lit le signal du service (référence, pas copie) et l'ajoute à la condition de `[disabled]` :

```typescript
chargement = this.service.chargement;
```

```html
<button type="submit" [disabled]="form.invalid || chargement()">Valider</button>
<button (click)="supprimer(x.id)" [disabled]="chargement()">Supprimer</button>
```

### Dans le projet

**Service** — [`carnet-contact_frontend/src/app/services/contact.ts`](../carnet-contact_frontend/src/app/services/contact.ts)

```typescript
import { EMPTY, of, catchError, finalize } from 'rxjs';

private chargementSignal = signal(false);
readonly chargement = this.chargementSignal.asReadonly();

chargerContacts(): void {
  this.erreurSignal.set(null);
  this.chargementSignal.set(true);

  this.http.get<Contact[]>(this.apiUrl).pipe(
    catchError(() => {
      this.erreurSignal.set('Impossible de charger les contacts. Le serveur est-il démarré ?');
      return of([]);
    }),
    finalize(() => this.chargementSignal.set(false))
  ).subscribe(data => this.contactsSignal.set(data));
}
```

Les trois écritures (`addContact`, `modifierContact`, `deleteContact`) portent le même `chargementSignal.set(true)` avant l'appel et le même `finalize(...)` dans le `.pipe()`.

**Coquille** — [`app.html`](../carnet-contact_frontend/src/app/app.html)

```html
@if (contactService.chargement()) {
  <p class="chargement">Chargement…</p>
}
```

**Boutons** — [`contact-form.ts`](../carnet-contact_frontend/src/app/components/contact-form/contact-form.ts) / [`contact-form.html`](../carnet-contact_frontend/src/app/components/contact-form/contact-form.html) (et de même dans [`contact-list`](../carnet-contact_frontend/src/app/components/contact-list/contact-list.ts) et [`contact-edit`](../carnet-contact_frontend/src/app/pages/contact-edit/contact-edit.ts))

```typescript
// contact-form.ts
private contactService = inject(ContactService);
chargement = this.contactService.chargement;
```

```html
<!-- contact-form.html -->
<button type="submit" [disabled]="contactForm.invalid || chargement()">Ajouter</button>
```

### Pourquoi l'indicateur reste invisible au chargement de la page (SSR)

Au premier affichage, `chargerContacts()` s'exécute **côté serveur** (rendu SSR, section 13) : `chargement` passe à `true` puis revient à `false` sur le serveur, avant même que le HTML ne parte vers le navigateur. La page arrive déjà remplie — l'indicateur n'a jamais eu, côté client, une image d'écran où s'afficher. Il n'apparaît que sur les requêtes **déclenchées par une action** (ajout, modification, suppression), qui partent forcément du navigateur.

## 17. Intercepteurs HTTP

### Le problème : la plomberie transverse recopiée à chaque appel

À la fin de la section 16, chacune des quatre méthodes du service ouvrait sur les deux mêmes lignes et refermait sur le même opérateur :

```typescript
this.erreurSignal.set(null);                          // ×4, identique
this.chargementSignal.set(true);                      // ×4, identique
finalize(() => this.chargementSignal.set(false))      // ×4, identique
```

Douze lignes strictement dupliquées. Mais le vrai coût n'est pas le volume de code : c'est **l'oubli**. Le jour où l'on ajoute une cinquième méthode, rien ne force à recopier ces lignes. Le bouton restera actif pendant la requête, l'erreur passera en silence — et le défaut ne se verra qu'à l'usage. Une règle qui doit valoir pour **toutes** les requêtes ne peut pas reposer sur la discipline du développeur à chaque appel.

Il y a aussi un problème de responsabilité. Un service métier a un sujet : gérer des contacts, des factures, des utilisateurs. « Allumer un indicateur pendant une requête » n'appartient à aucun de ces sujets : c'est une règle de l'application entière. Ce code était au mauvais endroit.

### Ce qu'est un intercepteur

Un intercepteur est une fonction qu'Angular insère **entre `HttpClient` et le réseau**. Toute requête émise par n'importe quel service la traverse, sans que ce service en sache quoi que ce soit.

C'est le même principe qu'un `.pipe()` (section 15), mais monté un étage plus haut : au lieu d'être branché sur *un* appel, il est branché sur *tous*.

```
Service ──► HttpClient ──► intercepteur A ──► intercepteur B ──► réseau
                                 ▲                  ▲              │
                                 └──── réponse ─────┴──────────────┘
```

À l'aller la requête descend la chaîne, au retour la réponse la remonte **en sens inverse**. Un intercepteur voit donc les deux : il peut agir avant l'envoi et après la réception.

### La signature `(req, next)`

Un intercepteur moderne est une simple fonction, typée `HttpInterceptorFn`, et non plus une classe :

```typescript
import { HttpInterceptorFn } from '@angular/common/http';

export const monIntercepteur: HttpInterceptorFn = (req, next) => {
  // 1. Ici : avant que la requête ne parte.
  //    req est la requête sortante (url, méthode, en-têtes, corps).

  // 2. next(req) la transmet au maillon suivant — autre intercepteur, ou
  //    le réseau — et renvoie l'Observable de la réponse.
  return next(req).pipe(
    // 3. Ici : les opérateurs qui traitent la réponse au retour.
  );
};
```

Deux règles à retenir : il faut **toujours** appeler `next(...)` (sinon la requête ne part jamais), et il faut **retourner** l'Observable qu'il rend (sinon `HttpClient` n'a rien à quoi s'abonner).

| Élément | Rôle |
|---|---|
| `req` | La requête sortante — objet **immuable** (`HttpRequest`) |
| `next(req)` | Transmet au maillon suivant et rend l'`Observable` de la réponse |
| `req.clone({ … })` | Copie de la requête avec des champs remplacés — la seule façon de la « modifier » |
| `inject(MonService)` | Fonctionne dans un intercepteur : il s'exécute dans un contexte d'injection |
| `.pipe(finalize(…))` | Agir à la fin, quelle que soit l'issue |
| `.pipe(catchError(…))` | Traiter l'erreur au retour |

### Les enregistrer : `withInterceptors`

On les déclare une fois pour toutes à côté de `provideHttpClient()`. L'ordre du tableau est l'ordre de la chaîne à l'aller.

```typescript
import { provideHttpClient, withInterceptors } from '@angular/common/http';

export const appConfig: ApplicationConfig = {
  providers: [
    provideHttpClient(
      withInterceptors([
        premierIntercepteur,   // voit la requête en premier
        secondIntercepteur,    // ...et la réponse en dernier
      ])
    )
  ]
};
```

Mieux vaut **un intercepteur par responsabilité** qu'un gros intercepteur qui fait tout : chacun reste lisible seul, et on peut en retirer un sans toucher aux autres.

### Cas 1 — observer : compter les requêtes en vol

Le premier usage est le plus simple : ne rien modifier, seulement constater. L'intercepteur incrémente un compteur avant, le décrémente dans un `finalize` après.

Un détail change par rapport à la section 16, et il est important. Tant que chaque méthode gérait son propre indicateur, un **booléen** suffisait, parce que les requêtes ne se chevauchaient jamais. Maintenant que l'intercepteur les voit toutes, deux peuvent être en vol simultanément — et avec un booléen, la première qui se termine éteindrait l'indicateur alors que la seconde tourne encore. La réponse est de **compter**, puis de dériver le booléen avec un `computed()` (section 3) :

```typescript
// Le service qui porte l'état, sans aucune notion de métier
@Injectable({ providedIn: 'root' })
export class EtatHttpService {
  private requetesEnCours = signal(0);
  // Le booléen est DÉRIVÉ du compteur : impossible de les désynchroniser.
  readonly chargement = computed(() => this.requetesEnCours() > 0);

  debutRequete(): void { this.requetesEnCours.update(n => n + 1); }
  finRequete(): void  { this.requetesEnCours.update(n => Math.max(0, n - 1)); }
}
```

```typescript
export const chargementInterceptor: HttpInterceptorFn = (req, next) => {
  const etat = inject(EtatHttpService);

  etat.debutRequete();

  return next(req).pipe(
    // Même raisonnement que dans le service auparavant : finalize couvre le
    // succès comme l'erreur. Il couvre même un cas de plus — le
    // désabonnement, quand une requête est annulée parce que le composant
    // qui l'attendait a été détruit.
    finalize(() => etat.finRequete())
  );
};
```

**Pourquoi un service dédié plutôt que les signaux déjà présents dans le service métier ?** Pour deux raisons. D'abord parce que cet état est transverse, comme la bannière qui l'affiche : il n'a rien à faire dans un service qui parle de contacts. Ensuite pour une raison technique : un intercepteur qui injecterait `ContactService`, lequel injecte `HttpClient`, lequel appelle l'intercepteur, formerait une boucle de dépendances — Angular s'en sort, mais le code devient difficile à suivre.

### Cas 2 — intercepter sans avaler : l'erreur et `throwError`

Deuxième usage : centraliser le message d'erreur. L'intercepteur ne connaît pas l'intention métier de la requête (« ajouter un contact »), mais il connaît son **code de statut**, et c'est souvent l'information la plus utile à l'utilisateur.

| `status` | Signification |
|---|---|
| `0` | Aucune réponse reçue : serveur éteint, réseau coupé, CORS refusé. Ce n'est pas un code du serveur, c'est son absence |
| `400` | Requête mal formée, refusée par le serveur |
| `401` / `403` | Non authentifié / non autorisé |
| `404` | La ressource demandée n'existe pas |
| `500` | Le serveur a planté en traitant la requête |

Le point délicat est ailleurs. L'intercepteur **ne doit pas décider de la valeur de repli** : une lecture veut `of([])`, une écriture veut `EMPTY` (section 15), et seul le service appelant sait dans quel cas il est. L'intercepteur note donc le message, puis **relance** l'erreur avec `throwError` pour que le `catchError` du service continue de faire son travail.

```typescript
function messagePour(erreur: HttpErrorResponse): string {
  switch (erreur.status) {
    case 0:   return 'Serveur injoignable. Est-il bien démarré ?';
    case 404: return 'Ressource introuvable (404).';
    case 500: return 'Erreur interne du serveur (500).';
    default:  return `Erreur inattendue (${erreur.status}).`;
  }
}

export const erreurInterceptor: HttpInterceptorFn = (req, next) => {
  const etat = inject(EtatHttpService);

  etat.effacerErreur();   // nouvelle requête : on repart d'un état sain

  return next(req).pipe(
    catchError((erreur: HttpErrorResponse) => {
      etat.signalerErreur(messagePour(erreur));

      // throwError relance l'erreur telle quelle. SANS cette ligne,
      // l'intercepteur « avalerait » l'échec : le service ne saurait jamais
      // que sa requête a raté, et son catchError ne s'exécuterait pas.
      return throwError(() => erreur);
    })
  );
};
```

C'est la répartition qui compte, et elle vaut bien au-delà de ce cas : **le générique en haut, le spécifique en bas.**

| Décision | Qui la prend | Pourquoi |
|---|---|---|
| Le message affiché à l'utilisateur | L'intercepteur | Dépend du code HTTP, pas du métier |
| Allumer / éteindre l'indicateur | L'intercepteur | Règle identique pour toute requête |
| La valeur de repli (`of([])` / `EMPTY`) | Le service | Seul lui sait s'il lisait ou écrivait |
| Mettre à jour le signal des données | Le service | C'est son métier |

Contrepartie assumée : le message perd sa nuance métier (« Impossible d'ajouter le contact ») au profit d'une nuance technique (« Serveur injoignable »). C'est souvent un gain — l'utilisateur apprend *pourquoi* ça a échoué. Si un message métier est vraiment nécessaire, le service peut toujours le réécrire dans son propre `catchError`, qui s'exécute **après** celui de l'intercepteur.

### Cas 3 — modifier la requête : `clone()` et l'immuabilité

Les deux premiers intercepteurs observaient sans toucher. Le troisième usage est celui pour lequel les intercepteurs sont surtout connus : **modifier la requête au passage**. C'est le mécanisme derrière l'ajout automatique d'un jeton d'authentification sur chaque appel.

Un `HttpRequest` est **immuable** : `req.url = …` ou `req.headers.set(…)` ne modifient rien. Il faut passer par `clone()`, qui rend une copie avec les champs remplacés — et transmettre la copie à `next()`. Cette immuabilité est volontaire : elle garantit qu'un maillon de la chaîne ne peut pas altérer une requête que les autres ont déjà vue.

```typescript
// Le cas d'usage classique : authentifier toutes les requêtes d'un coup
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const jeton = inject(AuthService).jeton();
  if (!jeton) {
    return next(req);   // pas connecté : on laisse passer tel quel
  }

  // clone() ne modifie pas req : il en rend une COPIE modifiée.
  const requeteAuthentifiee = req.clone({
    setHeaders: { Authorization: `Bearer ${jeton}` }
  });

  return next(requeteAuthentifiee);
};
```

| Option de `clone()` | Effet |
|---|---|
| `url` | Remplace l'URL de la requête |
| `setHeaders: { … }` | Ajoute ou remplace des en-têtes, en gardant les autres |
| `headers` | Remplace **tout** le jeu d'en-têtes |
| `setParams: { … }` | Ajoute des paramètres de requête (`?cle=valeur`) |
| `body` | Remplace le corps envoyé |

Une variante utile du même mécanisme : préfixer les URL relatives par l'adresse du serveur, pour que plus aucun service ne connaisse le nom d'hôte du backend.

```typescript
const BASE_URL = 'https://api.exemple.com';

export const baseUrlInterceptor: HttpInterceptorFn = (req, next) => {
  // Toute requête ne va pas forcément vers notre API : on ne réécrit que
  // ce qu'on reconnaît, et on laisse le reste passer intact.
  if (!req.url.startsWith('/api')) {
    return next(req);
  }
  return next(req.clone({ url: BASE_URL + req.url }));
};
```

### Dans le projet

**État transverse** — [`carnet-contact_frontend/src/app/services/etat-http.ts`](../carnet-contact_frontend/src/app/services/etat-http.ts)

Nouveau service, sans aucune notion de contact : il ne porte que le compteur de requêtes et le dernier message d'erreur.

```typescript
@Injectable({ providedIn: 'root' })
export class EtatHttpService {
  private requetesEnCours = signal(0);
  readonly chargement = computed(() => this.requetesEnCours() > 0);

  private erreurSignal = signal<string | null>(null);
  readonly erreur = this.erreurSignal.asReadonly();

  debutRequete(): void { this.requetesEnCours.update(n => n + 1); }
  finRequete(): void { this.requetesEnCours.update(n => Math.max(0, n - 1)); }
  signalerErreur(message: string): void { this.erreurSignal.set(message); }
  effacerErreur(): void { this.erreurSignal.set(null); }
}
```

**Les trois intercepteurs** — [`interceptors/chargement-interceptor.ts`](../carnet-contact_frontend/src/app/interceptors/chargement-interceptor.ts), [`interceptors/erreur-interceptor.ts`](../carnet-contact_frontend/src/app/interceptors/erreur-interceptor.ts), [`interceptors/base-url-interceptor.ts`](../carnet-contact_frontend/src/app/interceptors/base-url-interceptor.ts)

**Enregistrement** — [`app.config.ts`](../carnet-contact_frontend/src/app/app.config.ts)

```typescript
provideHttpClient(
  withInterceptors([
    baseUrlInterceptor,      // réécrit l'URL avant que les autres la voient
    chargementInterceptor,
    erreurInterceptor        // son catchError passe avant le finalize ci-dessus
  ])
)
```

**Service métier allégé** — [`services/contact.ts`](../carnet-contact_frontend/src/app/services/contact.ts)

Le service est passé de 90 à 59 lignes. Il ne reste dans chaque méthode que ce qui lui est propre : l'appel, la valeur de repli, la mise à jour du signal.

```typescript
// URL relative : baseUrlInterceptor y ajoute l'adresse du backend.
private apiUrl = '/api/contacts';

chargerContacts(): void {
  this.http.get<Contact[]>(this.apiUrl).pipe(
    catchError(() => of([]))       // lecture : une liste vide reste exploitable
  ).subscribe(data => this.contactsSignal.set(data));
}

addContact(contact: Contact): void {
  this.http.post<Contact>(this.apiUrl, contact).pipe(
    catchError(() => EMPTY)        // écriture : ne pas toucher l'état local
  ).subscribe(contactCree => {
    this.contactsSignal.update(liste => [...liste, contactCree]);
  });
}
```

**Coquille** — [`app.ts`](../carnet-contact_frontend/src/app/app.ts) et [`app.html`](../carnet-contact_frontend/src/app/app.html)

`App` n'injecte plus `ContactService` du tout : un affichage transverse dépend maintenant d'un état transverse.

```typescript
protected etatHttp = inject(EtatHttpService);
```

```html
@if (etatHttp.chargement()) { <p class="chargement">Chargement…</p> }
@if (etatHttp.erreur(); as message) { <p class="erreur">{{ message }}</p> }
```

**Boutons** — [`contact-form.ts`](../carnet-contact_frontend/src/app/components/contact-form/contact-form.ts), [`contact-list.ts`](../carnet-contact_frontend/src/app/components/contact-list/contact-list.ts), [`contact-edit.ts`](../carnet-contact_frontend/src/app/pages/contact-edit/contact-edit.ts)

```typescript
private etatHttp = inject(EtatHttpService);
chargement = this.etatHttp.chargement;
```

Effet de bord notable : `ContactForm` n'injecte plus `ContactService` (il n'y recourait que pour le chargement, et remonte l'ajout au parent par `output()`), son import a donc disparu.

### Ce qu'un intercepteur ne doit pas faire

- **Avaler une erreur sans la relancer**, sauf intention explicite : les appelants croiraient leur requête réussie.
- **Porter de la logique métier.** Si le code a besoin de savoir *quelle* fonctionnalité a déclenché la requête, il est au mauvais endroit.
- **Oublier `next()`** ou ne pas retourner son résultat : la requête ne part jamais, sans aucun message d'erreur.
- **Supposer qu'il ne tourne que dans le navigateur.** Avec le SSR (section 13), les intercepteurs s'exécutent aussi côté serveur, sur le `GET` initial : rien qui touche `window` ou `localStorage` ne doit y figurer sans précaution.

## 18. Authentification (Spring Security, BCrypt, JWT)

### Le problème : une API ouverte à tous

Jusqu'ici, `GET /api/contacts` renvoyait **toute** la table à quiconque la demandait. Il n'y avait ni comptes, ni propriétaire : le carnet était unique et public. Deux besoins apparaissent en même temps, et ils sont liés :

- **Authentifier** : savoir *qui* fait la requête.
- **Autoriser** : décider ce que cette personne peut voir et modifier.

Un point est à poser d'emblée, parce qu'il commande tout le reste : **la seule protection réelle est côté serveur**. Tout ce qu'on écrit dans Angular — cacher un bouton, bloquer une route — améliore l'expérience mais ne protège rien : n'importe qui peut appeler l'API directement avec `curl`. Le frontend rend l'application agréable, le backend la rend sûre.

### Hacher un mot de passe, et pourquoi ce n'est pas chiffrer

Un mot de passe ne doit **jamais** être stocké en clair, ni même chiffré. Chiffrer est réversible : celui qui possède la clé retrouve la valeur d'origine. **Hacher** est à sens unique — il n'existe aucun moyen de remonter du haché au mot de passe.

Vérifier une connexion ne consiste donc pas à déchiffrer, mais à **rehacher** ce que l'utilisateur vient de taper et à comparer les deux hachés.

BCrypt ajoute deux propriétés indispensables :

| Propriété | Ce qu'elle empêche |
|---|---|
| Un **sel** aléatoire par mot de passe | Deux comptes ayant le même mot de passe ont des hachés différents : on ne peut pas repérer les mots de passe communs, ni utiliser une table pré-calculée |
| Une **lenteur volontaire** (calcul coûteux) | Tester des milliards de combinaisons devient impraticable — là où un hachage rapide (MD5, SHA-1) en permet des milliards par seconde |

```java
// Déclaré une fois comme bean, injecté partout où c'est nécessaire
@Bean
PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}

// À l'inscription : on hache AVANT d'atteindre la base
utilisateur.setMotDePasse(passwordEncoder.encode(motDePasseEnClair));

// À la connexion : on compare, on ne déchiffre pas
boolean correct = passwordEncoder.matches(motDePasseEnClair, utilisateur.getMotDePasse());
```

### Le JWT : une identité qui se transporte

Une fois le mot de passe vérifié, il faut que les requêtes suivantes n'aient pas à le redemander. La méthode retenue ici est le **JWT** (JSON Web Token).

Un JWT est une chaîne en trois parties séparées par des points : `en-tête.charge_utile.signature`. Les deux premières sont du JSON encodé en base64, donc **lisibles par n'importe qui** — on n'y met jamais de secret. La troisième est une signature calculée avec une clé que seul le serveur connaît : elle ne rend pas le contenu secret, elle rend son **falsification** impossible.

L'intérêt tient en une phrase : **le serveur n'a rien à stocker**. Il ne tient aucune liste de sessions ouvertes ; il lui suffit de vérifier que la signature du jeton présenté correspond à sa clé. On parle d'authentification « sans état » (*stateless*).

| Notion | Rôle |
|---|---|
| `subject` | Le champ standard désignant à qui appartient le jeton (ici, l'email) |
| `issuedAt` / `expiration` | Dates d'émission et de péremption — un jeton volé ne vaut pas éternellement |
| `signWith(cle)` | Appose la signature ; sans la clé, impossible d'en produire une valide |
| `verifyWith(cle)` | Vérifie signature ET expiration à la lecture |

```java
@Service
public class JwtService {
    private final SecretKey cle;

    public JwtService(@Value("${app.jwt.secret}") String secret) {
        // HS256 exige une clé d'au moins 256 bits, soit 32 caractères
        this.cle = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String genererJeton(String email) {
        Instant maintenant = Instant.now();
        return Jwts.builder()
                .subject(email)
                .issuedAt(Date.from(maintenant))
                .expiration(Date.from(maintenant.plusMillis(86_400_000)))
                .signWith(cle)
                .compact();
    }

    /** Rend l'email, ou null si le jeton est expiré ou trafiqué. */
    public String emailDuJeton(String jeton) {
        try {
            return Jwts.parser().verifyWith(cle).build()
                    .parseSignedClaims(jeton).getPayload().getSubject();
        } catch (JwtException | IllegalArgumentException e) {
            // Un jeton invalide n'est pas une panne : c'est un cas normal,
            // auquel l'appelant doit réagir en refusant l'accès.
            return null;
        }
    }
}
```

> **Note de mise à jour.** `genererJeton` prend aujourd'hui un second paramètre, le rôle, qu'elle place dans le jeton sous forme de *claim* — et `JwtService` expose une méthode `roleDuJeton` symétrique (section 29). Le mécanisme décrit ici n'a pas changé d'un iota : c'est toujours la même clé, la même signature, et la même vérification. Le jeton porte simplement une information de plus.

`@Value` injecte une valeur venue de `application.properties` (et non un autre bean). La syntaxe `${VARIABLE:defaut}` permet de surcharger par une variable d'environnement — une clé de signature ne doit **jamais** être versionnée dans un vrai projet.

### Le filtre : relire le jeton à chaque requête entrante

Côté serveur, un **filtre** joue exactement le rôle qu'un intercepteur joue côté client (section 17) : il voit passer toutes les requêtes et traite une préoccupation transverse à un seul endroit. Côté Angular un intercepteur **ajoute** le jeton aux requêtes sortantes ; côté Spring un filtre le **relit** sur les requêtes entrantes. Même idée, appliquée aux deux bouts de la chaîne.

```java
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest requete,
                                    HttpServletResponse reponse,
                                    FilterChain chaine) throws ServletException, IOException {
        String entete = requete.getHeader("Authorization");

        if (entete != null && entete.startsWith("Bearer ")) {
            String email = jwtService.emailDuJeton(entete.substring(7));

            if (email != null) {
                // Le SecurityContext : un porte-clés propre à la requête en
                // cours, que Spring Security consulte pour autoriser, et que
                // les contrôleurs lisent pour savoir QUI parle.
                var auth = new UsernamePasswordAuthenticationToken(email, null, List.of());
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        // Toujours passer la main, même sans jeton valide : ce filtre
        // AUTHENTIFIE, il n'autorise pas. Sans cet appel, les routes
        // publiques (connexion, inscription) seraient bloquées aussi.
        chaine.doFilter(requete, reponse);
    }
}
```

`OncePerRequestFilter` garantit une seule exécution par requête, même quand le conteneur effectue des redirections internes.

### La configuration : ce qui est ouvert, ce qui est fermé

Dès que `spring-boot-starter-security` est présent dans le `pom.xml`, **tout est fermé par défaut** et un mot de passe aléatoire s'affiche au démarrage. La classe de configuration reprend la main.

```java
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .requestMatchers("/api/auth/**").permitAll()
                    .anyRequest().authenticated())
            .addFilterBefore(monFiltre, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
```

| Réglage | Pourquoi |
|---|---|
| `cors(...)` | Quand Spring Security est en place, c'est **lui** qui gère le CORS. Les `@CrossOrigin` des contrôleurs doivent disparaître, sinon le preflight `OPTIONS` est rejeté avant d'atteindre le contrôleur |
| `csrf().disable()` | La protection CSRF sert aux applications à session par cookie, où le navigateur envoie l'identité automatiquement. Ici l'identité voyage dans un en-tête que seul notre JavaScript ajoute : l'attaque visée n'existe pas |
| `STATELESS` | Aucune session serveur, aucun cookie. Chaque requête se justifie seule, par son jeton |
| `OPTIONS` en `permitAll` | Le preflight du navigateur ne porte jamais de jeton |
| `/api/auth/**` en `permitAll` | On ne peut pas exiger un jeton pour venir en chercher un |
| `anyRequest().authenticated()` | Tout le reste est fermé — la règle par défaut est « interdit », jamais « autorisé » |

Deux réglages de plus, dont l'absence produit des symptômes déroutants :

```java
    // Sans point d'entrée explicite, Spring répond 403 à une requête non
    // authentifiée. Or les deux codes ne disent pas la même chose :
    // 401 = « je ne sais pas qui tu es », 403 = « je sais, mais c'est interdit ».
    .exceptionHandling(ex -> ex.authenticationEntryPoint(
            (req, res, err) -> res.sendError(HttpServletResponse.SC_UNAUTHORIZED)))

    // /error doit rester ouvert : quand un contrôleur lève une
    // ResponseStatusException, Spring réachemine la requête vers /error — et
    // sur ce second passage, OncePerRequestFilter ne rejoue PAS le filtre
    // JWT. L'identité perdue, un 404 ressort transformé en 403.
    .requestMatchers("/error").permitAll()
```

### Savoir qui parle, dans le contrôleur

`@AuthenticationPrincipal` injecte le « principal » posé par le filtre. C'est la **seule** source d'identité digne de confiance : jamais un identifiant lu dans l'URL ou le corps de la requête.

```java
@GetMapping
public List<Contact> mesContacts(@AuthenticationPrincipal String email) {
    Long moi = utilisateurRepository.findByEmail(email).orElseThrow().getId();
    return contactRepository.findByProprietaireIdOrderByNomAsc(moi);
}

@PostMapping
public Contact creer(@RequestBody Contact contact, @AuthenticationPrincipal String email) {
    // Le propriétaire est IMPOSÉ par le serveur, pas lu dans la requête :
    // sinon un client pourrait écrire dans le carnet d'un autre.
    contact.setProprietaire(utilisateurConnecte(email));
    return contactRepository.save(contact);
}
```

Pour les accès par identifiant, la protection tient dans la requête elle-même : on cherche par id **et** par propriétaire. Demander la ressource 42 quand elle n'est pas à soi ne renvoie pas 42 — cela ne renvoie rien.

> **Note de mise à jour.** Le `GET` ci-dessus renvoie aujourd'hui une page plutôt qu'une liste (section 22), et l'appel au repository a changé de nom. Le point de cette sous-section est ailleurs et reste intact : quelle que soit la requête, l'identifiant du propriétaire vient du **jeton**, jamais de la requête HTTP. C'est exactement ce que vérifient les tests d'isolation de la section 26.

```java
Optional<Contact> findByIdAndProprietaireId(Long id, Long proprietaireId);
```

### Côté Angular : où ranger le jeton

Le client doit conserver le jeton entre deux chargements de page, sinon un simple F5 déconnecte. `localStorage` remplit ce rôle — avec une contrainte : **il n'existe pas côté serveur**. Avec le SSR (section 13), y toucher pendant le rendu serveur fait échouer la page.

`PLATFORM_ID` est le jeton d'injection qui dit sur quelle plateforme le code tourne.

```typescript
@Injectable({ providedIn: 'root' })
export class SessionService {
  private navigateur = isPlatformBrowser(inject(PLATFORM_ID));

  private jetonSignal = signal<string | null>(null);
  // « connecté » n'est pas une donnée à stocker : c'est une conséquence.
  readonly connecte = computed(() => this.jetonSignal() !== null);

  constructor() {
    if (this.navigateur) {
      const jeton = localStorage.getItem('jeton');
      if (jeton) this.jetonSignal.set(jeton);
    }
  }

  jetonActuel(): string | null {
    return this.jetonSignal();
  }
}
```

Ce service est délibérément **séparé** du service qui appelle l'API. L'intercepteur d'authentification doit lire le jeton ; s'il injectait un service dépendant de `HttpClient`, on retomberait sur la boucle de dépendances évitée en section 17.

### L'intercepteur d'authentification

C'est le cas d'usage annoncé en section 17 comme « le plus connu des intercepteurs », cette fois pour de vrai.

```typescript
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const jeton = inject(SessionService).jetonActuel();

  // Pas de jeton : on laisse passer tel quel. Envoyer « Bearer null »
  // ferait échouer la requête au lieu de la laisser anonyme.
  if (!jeton) {
    return next(req);
  }

  return next(req.clone({
    setHeaders: { Authorization: `Bearer ${jeton}` }
  }));
};
```

Son symétrique est la réaction au **401**. Un jeton a une durée de vie ; à son expiration, *toutes* les requêtes échouent d'un coup. Traiter ce cas dans chaque service serait exactement la duplication que les intercepteurs ont supprimée.

```typescript
catchError((erreur: HttpErrorResponse) => {
  // La condition sur le jeton est essentielle : sans elle, un mot de passe
  // erroné sur la page de connexion (401 aussi) déclencherait une
  // redirection vers... la page de connexion.
  if (erreur.status === 401 && session.jetonActuel() !== null) {
    session.vider();
    router.navigate(['/connexion']);
  }
  return throwError(() => erreur);
})
```

### La garde de route (`CanActivateFn`)

Une **garde** est une fonction qu'Angular appelle avant d'activer une route, et qui répond `true` (on passe) ou `false` (on bloque).

Le problème qu'elle résout : sans elle, taper `/contact/3` sans être connecté afficherait une page vide et une bannière 401 — techniquement correct, incompréhensible pour l'utilisateur.

```typescript
export const authGuard: CanActivateFn = () => {
  const session = inject(SessionService);
  const router = inject(Router);

  if (session.connecte()) {
    return true;
  }

  // Rediriger plutôt que renvoyer false sèchement : l'utilisateur atterrit
  // sur un écran qui lui dit quoi faire.
  router.navigate(['/connexion']);
  return false;
};
```

```typescript
// canActivate prend un TABLEAU : on peut enchaîner plusieurs gardes,
// et toutes doivent dire oui.
{ path: '', component: Accueil, canActivate: [authGuard] }
```

**À ne jamais oublier : une garde n'est pas une sécurité.** Elle guide l'utilisateur ; elle n'empêche personne d'appeler l'API à la main.

### Conséquence sur le SSR

Les routes protégées dépendent de `localStorage`, absent côté serveur. Les prérendre produirait le HTML de la page de connexion pour chacune, servi ensuite à des utilisateurs déjà connectés.

```typescript
export const serverRoutes: ServerRoute[] = [
  // Ne dépend d'aucune donnée : pré-générable au build
  { path: 'connexion', renderMode: RenderMode.Prerender },
  // Tout le reste : construit par le navigateur, quand la session est connue
  { path: '**', renderMode: RenderMode.Client }
];
```

### Dans le projet

**Backend** — [`security/JwtService.java`](../carnet-contact-backend/src/main/java/com/example/carnet_contact_backend/security/JwtService.java), [`security/JwtAuthFilter.java`](../carnet-contact-backend/src/main/java/com/example/carnet_contact_backend/security/JwtAuthFilter.java), [`security/SecurityConfig.java`](../carnet-contact-backend/src/main/java/com/example/carnet_contact_backend/security/SecurityConfig.java), [`controller/AuthController.java`](../carnet-contact-backend/src/main/java/com/example/carnet_contact_backend/controller/AuthController.java)

```java
// AuthController — un seul message pour « email inconnu » et « mot de passe
// faux » : distinguer les deux renseignerait un attaquant sur les comptes
// qui existent réellement.
if (trouve.isEmpty()
        || !passwordEncoder.matches(demande.motDePasse(), trouve.get().getMotDePasse())) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body("Email ou mot de passe incorrect.");
}
```

**Configuration** — [`application.properties`](../carnet-contact-backend/src/main/resources/application.properties)

```properties
carnet.jwt.secret=${CARNET_JWT_SECRET:cle-de-developpement-a-remplacer-en-production-32c}
carnet.jwt.duree-ms=86400000

# Sans cet exclude, Spring Boot cree un utilisateur "user" en memoire avec un
# mot de passe aleatoire affiche a chaque demarrage — trompeur, puisque notre
# authentification passe entierement par le filtre JWT.
spring.autoconfigure.exclude=org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration
```

**Frontend** — [`services/session.ts`](../carnet-contact_frontend/src/app/services/session.ts), [`services/auth.ts`](../carnet-contact_frontend/src/app/services/auth.ts), [`interceptors/auth-interceptor.ts`](../carnet-contact_frontend/src/app/interceptors/auth-interceptor.ts), [`auth-guard.ts`](../carnet-contact_frontend/src/app/auth-guard.ts), [`pages/connexion/`](../carnet-contact_frontend/src/app/pages/connexion/)

`AuthService` **retourne** ses Observables au lieu de s'y abonner, contrairement à `ContactService` — parce que l'appelant a besoin de savoir quand ça a réussi, pour naviguer :

```typescript
connexion(email: string, motDePasse: string): Observable<ReponseAuth> {
  return this.http.post<ReponseAuth>(`${this.apiUrl}/connexion`, { email, motDePasse }).pipe(
    // tap() observe le flux sans le modifier : idéal pour un effet de bord
    // (mémoriser la session) en laissant la réponse continuer vers l'appelant.
    tap(reponse => this.session.ouvrir(reponse.jeton, reponse.utilisateur))
  );
}
```

## 19. Relations entre entités JPA

### Le problème : des données qui se pointent l'une l'autre

Tant qu'une entité vivait seule, une table suffisait. Dès qu'un contact **appartient** à un utilisateur, et qu'un message **relie** deux utilisateurs, il faut exprimer ces liens — en base sous forme de clés étrangères, et en Java sous forme de champs.

### `@ManyToOne` : le côté qui porte la clé étrangère

« Plusieurs contacts pointent vers un utilisateur. » C'est le côté **propriétaire** de la relation : c'est la table `contact` qui reçoit une colonne supplémentaire.

```java
@Entity
public class Contact {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proprietaire_id")
    @JsonIgnore
    private Utilisateur proprietaire;
}
```

| Élément | Rôle |
|---|---|
| `@ManyToOne` | Plusieurs entités de ce côté pour une seule de l'autre |
| `@JoinColumn(name = …)` | Nomme explicitement la colonne de clé étrangère. **Obligatoire** quand une entité a deux relations vers le même type, sinon Hibernate ne peut pas les distinguer |
| `fetch = LAZY` | L'entité liée n'est chargée que si on la demande vraiment, au lieu d'une jointure systématique |
| `optional = false` | La colonne est `NOT NULL` : un contact sans propriétaire n'a pas de sens |
| `@JsonIgnore` | Le champ ne part pas dans le JSON |

Le cas des deux relations vers le même type est celui du message :

```java
@Entity
public class Message {
    @ManyToOne(optional = false)
    @JoinColumn(name = "expediteur_id")
    private Utilisateur expediteur;

    @ManyToOne(optional = false)
    @JoinColumn(name = "destinataire_id")
    private Utilisateur destinataire;
}
```

Remarquez qu'il n'y a **pas** d'entité « Conversation ». La conversation entre A et B, c'est simplement l'ensemble des messages où (expéditeur = A et destinataire = B) ou l'inverse. Une table de moins, et aucune information dupliquée à maintenir cohérente.

### `@JsonIgnore` : ce qui ne doit pas sortir du serveur

Une entité JPA sert deux maîtres : elle décrit une table, et elle est sérialisée en JSON. Ces deux rôles n'ont pas les mêmes besoins, et `@JsonIgnore` règle l'écart.

Deux motifs bien distincts de l'utiliser :

| Motif | Exemple |
|---|---|
| **Ne jamais divulguer** | Le mot de passe haché. Sans l'annotation, il partirait dans chaque réponse où un utilisateur apparaît — y compris un simple fil de discussion |
| **Éviter une récursion infinie** | Si `Contact` sérialise son propriétaire et que `Utilisateur` sérialisait ses contacts, Jackson tournerait en boucle jusqu'au plantage |

Le second motif se règle aussi en **ne déclarant pas** la collection inverse. C'est le choix fait ici : `Utilisateur` n'a pas de champ `List<Contact>` — le repository interroge par identifiant du propriétaire quand il en a besoin. Moins de code, aucun piège de sérialisation, et pas de collection chargée pour rien.

### Requêtes dérivées : Spring Data lit les noms de méthodes

On l'utilisait déjà sans le nommer. Le principe : Spring Data **analyse le nom** de la méthode d'interface et en écrit le SQL. Aucune implémentation à fournir.

```java
public interface ContactRepository extends JpaRepository<Contact, Long> {

    // WHERE proprietaire_id = ? ORDER BY nom ASC
    List<Contact> findByProprietaireIdOrderByNomAsc(Long proprietaireId);

    // WHERE id = ? AND proprietaire_id = ?
    Optional<Contact> findByIdAndProprietaireId(Long id, Long proprietaireId);
}
```

> **Note de mise à jour.** La première de ces deux méthodes n'existe plus dans le projet : elle a été remplacée par une `@Query` paginée lors de l'ajout de la recherche (section 22), la dérivation par nom ne sachant exprimer ni « nom OU prénom OU email » ni un terme de recherche optionnel. C'est précisément la limite décrite dans la sous-section suivante. La seconde, `findByIdAndProprietaireId`, est toujours là — comme `existsByEmail` et `findByDestinataireIdAndLuFalse`.

| Fragment du nom | Traduction SQL |
|---|---|
| `findBy…` / `existsBy…` / `countBy…` | `SELECT` / `SELECT EXISTS` / `SELECT COUNT` |
| `…And…` / `…Or…` | `AND` / `OR` |
| `…Not` | `<>` |
| `…True` / `…False` | `= true` / `= false` |
| `…OrderByChampAsc` | `ORDER BY champ ASC` |
| Chemin composé (`ProprietaireId`) | Traverse la relation : `proprietaire.id` |

`Optional<T>` plutôt que `null` : le type **dit** que le résultat peut être absent, et le compilateur force à traiter ce cas au lieu de laisser filer une erreur à l'exécution.

### `@Query` : quand le nom deviendrait illisible

La dérivation a une limite. Pour « les messages entre A et B, dans un sens ou dans l'autre », le nom dérivé serait `findByExpediteurIdAndDestinataireIdOrDestinataireIdAndExpediteurIdOrderByDateEnvoiAsc` — impossible à relire, et ambigu sur la priorité du `Or`.

`@Query` permet d'écrire la requête à la main, en **JPQL** : le même langage que SQL, mais qui parle d'*entités* et de leurs champs Java (`Message`, `m.expediteur.id`) au lieu de tables et de colonnes.

```java
@Query("""
        SELECT m FROM Message m
        WHERE (m.expediteur.id = :moi AND m.destinataire.id = :autre)
           OR (m.expediteur.id = :autre AND m.destinataire.id = :moi)
        ORDER BY m.dateEnvoi ASC
        """)
List<Message> conversation(@Param("moi") Long moi, @Param("autre") Long autre);
```

`:moi` et `:autre` sont des **paramètres nommés**, associés par `@Param`. Ils sont transmis séparément de la requête, ce qui exclut par construction toute injection SQL — on ne concatène jamais une valeur dans une requête.

Le `"""…"""` est un *text block* Java : une chaîne sur plusieurs lignes, sans concaténation ni `\n`.

### Les `record` : des classes de données en une ligne

Un `record` est une classe **immuable** dont le compilateur génère le constructeur, les accesseurs, `equals()`, `hashCode()` et `toString()`.

```java
public record DemandeInscription(String email, String motDePasse, String nomAffichage) {}
public record ReponseAuth(String jeton, Utilisateur utilisateur) {}
```

Les accesseurs portent le nom du champ, **sans** préfixe `get` : `demande.email()`, pas `demande.getEmail()`.

Pourquoi ne pas recevoir directement l'entité ? Parce qu'une requête d'inscription **n'est pas** un utilisateur : elle contient un mot de passe en clair, qui n'existe nulle part dans l'entité. Des types distincts pour des choses distinctes — c'est ce qu'on appelle un DTO (*Data Transfer Object*), et un record en est la forme la plus économique.

### `Instant` : horodater sans ambiguïté

```java
private Instant dateEnvoi;   // rempli par Instant.now()
```

`Instant` est un point précis dans le temps, en UTC, sans fuseau horaire. C'est le type juste pour un horodatage technique : la conversion vers l'heure locale de l'utilisateur est l'affaire de l'affichage, pas du stockage.

Côté client, Jackson le sérialise en chaîne ISO 8601 (`"2026-09-10T14:25:45.990Z"`) : en TypeScript c'est donc un `string`, converti en `Date` seulement au moment de l'afficher.

### Dans le projet

**Entités** — [`model/Utilisateur.java`](../carnet-contact-backend/src/main/java/com/example/carnet_contact_backend/model/Utilisateur.java), [`model/Contact.java`](../carnet-contact-backend/src/main/java/com/example/carnet_contact_backend/model/Contact.java), [`model/Message.java`](../carnet-contact-backend/src/main/java/com/example/carnet_contact_backend/model/Message.java)

```java
// Utilisateur.java — "user" est un mot réservé en SQL : la table serait
// refusée par H2 sans ce renommage explicite.
@Entity
@Table(name = "utilisateur")
public class Utilisateur {

    @Column(nullable = false, unique = true)
    private String email;

    // Ce champ ne sort JAMAIS du serveur.
    @JsonIgnore
    @Column(nullable = false)
    private String motDePasse;
}
```

**Repositories** — [`repository/MessageRepository.java`](../carnet-contact-backend/src/main/java/com/example/carnet_contact_backend/repository/MessageRepository.java), [`repository/UtilisateurRepository.java`](../carnet-contact-backend/src/main/java/com/example/carnet_contact_backend/repository/UtilisateurRepository.java)

```java
// Tous les comptes sauf le sien : "Not" devient WHERE id <> ?
List<Utilisateur> findByIdNotOrderByNomAffichageAsc(Long id);

// Les messages reçus non lus, pour la pastille de notification
List<Message> findByDestinataireIdAndLuFalse(Long destinataireId);
```

## 20. Composants réutilisables : `input()` et boucles de configuration

### Le problème : six fois le même bloc

Six réseaux sociaux à saisir, puis à afficher avec leur logo. Écrit naïvement, cela donne six champs de formulaire quasi identiques, six `@if` dans le gabarit d'affichage, et six blocs à modifier chaque fois qu'on ajoute un réseau.

La solution tient en un principe : **décrire la variabilité comme une donnée**, puis boucler dessus.

```typescript
// Une seule source de vérité pour la liste
export const RESEAUX = [
  { cle: 'instagram', nom: 'Instagram', couleur: '#E1306C' },
  { cle: 'linkedin', nom: 'LinkedIn', couleur: '#0A66C2' }
] as const;

// Le type « une des clés » : 'instagram' | 'linkedin'. Déduit du tableau,
// donc toujours à jour — impossible d'oublier de le mettre à jour.
export type CleReseau = typeof RESEAUX[number]['cle'];
```

`as const` demande à TypeScript de traiter le tableau comme figé : il en déduit alors les valeurs exactes (`'instagram'`) au lieu du vague `string`.

Ajouter un septième réseau devient **une ligne** dans ce tableau, plus un `@case` pour son logo.

### `input()` : recevoir une donnée du parent

`input()` est le pendant de `output()` (section 8) : la donnée descend du parent vers l'enfant, là où `output()` la fait remonter.

Un `input()` est un **signal en lecture seule** : il se lit avec des parenthèses, et tout `computed()` qui en dépend se recalcule quand le parent passe une nouvelle valeur.

```typescript
@Component({ selector: 'app-fiche', /* … */ })
export class Fiche {
  // .required : le composant ne peut pas être utilisé sans lui fournir la
  // valeur, et TypeScript le sait — pas de « | undefined » à gérer partout.
  element = input.required<MonType>();

  // Optionnel, avec valeur par défaut
  compact = input(false);

  // Dérivé de l'entrée : recalculé quand le parent change la valeur
  titre = computed(() => this.element().nom.toUpperCase());
}
```

```html
<!-- Côté parent : la même syntaxe de binding que n'importe quel attribut -->
<app-fiche [element]="contact" [compact]="true" />
```

### `@switch` dans un gabarit

L'équivalent du `switch`/`case` de TypeScript, côté template. Utile quand une même position doit accueillir un contenu différent selon une valeur.

```html
@switch (type()) {
  @case ('a') { <p>Contenu A</p> }
  @case ('b') { <p>Contenu B</p> }
  @default { <p>Autre</p> }
}
```

### Champs de formulaire générés par boucle

`formControlName` accepte une valeur dynamique entre crochets, ce qui permet de générer les champs depuis la liste de configuration.

```html
@for (reseau of reseaux; track reseau.cle) {
  <div>
    <label [for]="reseau.cle">{{ reseau.nom }}</label>
    <input [id]="reseau.cle" [formControlName]="reseau.cle" />
  </div>
}
```

Le `FormGroup` doit bien sûr contenir un contrôle par clé — c'est le seul endroit où la liste reste écrite en dur.

### SVG en ligne plutôt qu'images

Les logos sont des `<svg>` écrits directement dans le gabarit, pas des fichiers téléchargés. Trois raisons :

| Avantage | Détail |
|---|---|
| Aucune requête réseau | Le logo arrive avec le HTML, pas en dix requêtes supplémentaires |
| Aucune dépendance externe | Rien à charger depuis un site tiers qui pourrait tomber ou pister l'utilisateur |
| Colorable en CSS | `fill: currentColor` fait prendre au tracé la couleur du texte — impossible avec un PNG |

```css
.icone svg {
  width: 1.15rem;
  fill: currentColor;   /* le SVG suit la couleur du texte */
}
```

### Piloter une variable CSS depuis le composant

Chaque réseau a sa couleur de marque. Plutôt que six règles CSS presque identiques, on passe la couleur en **variable CSS** depuis le TypeScript.

```html
<a [style.--couleur-marque]="element.couleur">…</a>
```

```css
.lien {
  /* Avec une valeur de repli, au cas où la variable ne serait pas fournie */
  background: var(--couleur-marque, var(--bleu));
}
```

### Dans le projet

**Modèle** — [`contact.model.ts`](../carnet-contact_frontend/src/app/contact.model.ts) porte la constante `RESEAUX` et le type `CleReseau`.

**Composant** — [`components/reseaux-sociaux/reseaux-sociaux.ts`](../carnet-contact_frontend/src/app/components/reseaux-sociaux/reseaux-sociaux.ts) et son [gabarit](../carnet-contact_frontend/src/app/components/reseaux-sociaux/reseaux-sociaux.html)

```typescript
contact = input.required<Contact>();

// Ne garde que les réseaux renseignés : le gabarit n'a plus aucun @if à faire.
liens = computed<LienReseau[]>(() => {
  const c = this.contact();
  return RESEAUX
    .map(r => ({ ...r, url: this.versUrl(c[r.cle], r.cle) }))
    .filter(lien => lien.url !== '');
});
```

Le composant accepte aussi bien une URL complète qu'un pseudo, et reconstruit l'adresse dans le second cas — sans quoi un pseudo seul produirait un lien relatif cassé.

**Formulaires** — [`contact-form.html`](../carnet-contact_frontend/src/app/components/contact-form/contact-form.html) et [`contact-edit.html`](../carnet-contact_frontend/src/app/pages/contact-edit/contact-edit.html) génèrent leurs six champs par `@for`.

## 21. Mise en forme : variables CSS et cohérence visuelle

### Le problème : des couleurs éparpillées

Écrire `#0b5fff` dans quinze fichiers CSS fonctionne — jusqu'au jour où il faut changer la teinte. Il faut alors les retrouver toutes, sans en oublier une, et sans modifier par erreur un bleu qui n'était pas celui-là.

Les **variables CSS** (ou *custom properties*) règlent cela : on déclare chaque couleur une fois, sur `:root` (la racine du document), et on la référence partout par `var(--nom)`.

```css
:root {
  --bleu: #0b5fff;
  --bleu-fonce: #0740b5;
  --rouge: #ff1f3d;

  --rayon: 12px;
  --ombre: 0 1px 2px rgb(19 28 43 / 0.06), 0 4px 12px rgb(19 28 43 / 0.05);
}

.bouton {
  background: var(--bleu);
  border-radius: var(--rayon);
}
```

Elles ne servent pas qu'aux couleurs : rayons d'arrondi, ombres, largeur maximale du contenu. Tout ce qui doit rester **cohérent** d'un écran à l'autre gagne à être nommé une fois.

À la différence d'une variable de préprocesseur (Sass), une variable CSS existe dans le navigateur à l'exécution : elle est lisible par le JavaScript, surchargeable sur un élément précis (voir section 20), et modifiable à chaud.

### Choisir un rôle par couleur, pas juste une teinte

Deux couleurs vives se neutralisent si on les emploie au hasard. Le principe qui les rend lisibles : **une couleur, un rôle**.

| Couleur | Rôle | Où elle apparaît |
|---|---|---|
| Bleu | Action **courante**, navigation, identité | Boutons de validation, liens, en-tête, liserés de cartes |
| Rouge | Action **destructrice** et alerte | Bouton Supprimer, déconnexion, bannière d'erreur |
| Gris | Structure et texte secondaire | Bordures, libellés, informations de second plan |

Un rouge partout ne veut plus rien dire ; un rouge **rare** se remarque. C'est ce qui permet à l'œil de repérer le bouton dangereux sans le lire.

### Rendre les états visibles

Un état d'interface qui n'est pas visible n'existe pas pour l'utilisateur. Trois cas méritent une règle explicite :

```css
/* Bouton désactivé : c'est lui qui explique pourquoi un clic ne fait rien
   pendant une requête (section 16). Sans style distinct, l'utilisateur croit
   à un bug. */
button:disabled {
  background: #b9c3d4;
  cursor: not-allowed;
}

/* Focus clavier : ne JAMAIS supprimer un contour de focus sans le remplacer,
   sous peine de rendre le site inutilisable sans souris. */
input:focus {
  outline: none;
  border-color: var(--bleu);
  box-shadow: 0 0 0 3px rgb(11 95 255 / 0.15);
}
```

### Grilles qui s'adaptent sans média-requête

`auto-fit` associé à `minmax()` produit une grille qui se réorganise seule : autant de colonnes que la largeur le permet, chacune d'au moins 220 px.

```css
.grille {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 0.85rem;
}
```

Aucun `@media` n'est nécessaire pour ce cas : sur téléphone, la grille tombe naturellement à une colonne.

### Bouton ou lien ?

La distinction n'est pas décorative, elle est **fonctionnelle** :

| Balise | Quand | Conséquence |
|---|---|---|
| `<a>` | L'élément **navigue** vers une URL | Clic droit, ouvrir dans un onglet, copier l'adresse fonctionnent |
| `<button>` | L'élément **déclenche une action** | Activable à la barre d'espace, annoncé comme bouton aux lecteurs d'écran |

On peut styler l'un comme l'autre, mais il faut choisir la balise selon le comportement, pas selon l'apparence voulue.

### Dans le projet

**Système global** — [`src/styles.css`](../carnet-contact_frontend/src/styles.css) déclare toutes les variables et les styles de base des boutons, champs et cartes (`.carte`, `.grille`, `.pastille`).

**En-tête** — [`app.css`](../carnet-contact_frontend/src/app/app.css)

```css
.entete {
  /* Les deux couleurs de l'application : le bleu domine largement,
     le rouge n'apparaît qu'en fin de course. */
  background: linear-gradient(100deg, var(--bleu-fonce) 0%, var(--bleu) 55%, var(--rouge) 160%);
}

/* Classe posée automatiquement par routerLinkActive sur le lien courant */
.entete nav a.actif {
  color: var(--bleu-fonce);
  background: #fff;
}
```

`routerLinkActive` ajoute une classe au lien de la route active : c'est Angular qui suit la navigation, aucun état « onglet courant » n'est à gérer à la main.

**Fil de discussion** — [`pages/messages/messages.css`](../carnet-contact_frontend/src/app/pages/messages/messages.css)

```css
/* Bulle reçue : à gauche, en gris. */
.bulle { align-self: flex-start; background: #eef1f7; }

/* Bulle envoyée : à droite, en bleu pétant. Le côté et la couleur sont le
   seul indice qui permet de suivre un dialogue. */
.bulle.de-moi { align-self: flex-end; color: #fff; background: var(--bleu); }
```

> **Note de mise à jour.** Cet investissement a été remboursé d'un coup à l'arrivée du mode sombre (section 31) : **aucune règle CSS existante n'a eu à changer**. Le thème sombre n'est que le même fichier avec d'autres *valeurs* derrière les mêmes noms. C'est le meilleur argument possible pour la discipline décrite ici — et sa contrepartie exacte : une seule couleur écrite en dur quelque part reste claire sur fond noir. Le projet a d'ailleurs dû en corriger plusieurs, dans `messages.css` et `connexion.css`.

## 22. Pagination et recherche côté serveur

### Le problème : tout charger, toujours

Jusqu'ici, `GET /api/contacts` renvoyait **tous** les contacts. C'est parfait avec douze contacts, insoutenable avec dix mille : le serveur les lit tous en mémoire, les convertit tous en JSON, le réseau les transporte tous, le navigateur les affiche tous — pour montrer les six premiers.

La pagination consiste à ne demander qu'une **tranche**. Et pendant qu'on y est, à la filtrer : chercher « dupont » dans une liste qu'on a déjà entièrement téléchargée est possible côté client, mais cela suppose justement de l'avoir téléchargée. Les deux problèmes se règlent au même endroit, dans la même requête.

### Côté serveur : `Pageable` et `Page<T>`

Spring Data reconnaît un paramètre de type `Pageable` et se charge lui-même de traduire la demande en `LIMIT` / `OFFSET` / `ORDER BY`. La requête qu'on écrit ne parle donc **pas** de pages : on décrit *quelles lignes*, le découpage vient par-dessus.

```java
public interface RessourceRepository extends JpaRepository<Ressource, Long> {

    // Pageable n'est pas un paramètre comme les autres : Spring Data le
    // reconnaît et ajoute lui-même le découpage à la requête.
    // Le terme vide est traité DANS la requête plutôt que par une seconde
    // méthode « sans filtre » : un seul chemin de code à maintenir.
    @Query("""
            SELECT r FROM Ressource r
            WHERE r.proprietaire.id = :proprietaireId
              AND (:recherche = ''
                   OR LOWER(r.titre) LIKE LOWER(CONCAT('%', :recherche, '%')))
            """)
    Page<Ressource> rechercher(
            @Param("proprietaireId") Long proprietaireId,
            @Param("recherche") String recherche,
            Pageable pagination);
}
```

| Élément | Rôle |
|---|---|
| `Pageable` | La demande : quel numéro de page, quelle taille, quel tri |
| `PageRequest.of(page, taille, Sort)` | Fabrique un `Pageable` |
| `Page<T>` | La réponse : le contenu **plus** le total et le nombre de pages |
| `page.getContent()` | Les éléments de la tranche |
| `page.getTotalElements()` | Combien d'éléments au total, toutes pages confondues |
| `page.getTotalPages()` | Combien de pages, d'après la taille demandée |
| `LIKE CONCAT('%', :t, '%')` | « contient » ; `LOWER()` des deux côtés pour ignorer la casse |

Dans le contrôleur, les critères arrivent par la **query string**, et non par le chemin :

```java
/**
 * @RequestParam lit un paramètre de la query string
 * (/api/ressources?page=2&recherche=x), là où @PathVariable lit un morceau du
 * chemin. Règle habituelle : le chemin IDENTIFIE la ressource, la query string
 * la FILTRE ou la DÉCOUPE.
 *
 * defaultValue évite d'avoir à gérer le cas absent : un appel sans paramètre
 * reste valide et donne la première page.
 */
@GetMapping
public PageRessources lister(
        @RequestParam(defaultValue = "") String recherche,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "6") int taille) {

    // Le client fixe la taille, donc on la borne : sans ce garde-fou,
    // ?taille=1000000 ferait charger toute la table d'un coup.
    int tailleBornee = Math.clamp(taille, 1, 50);

    Page<Ressource> resultat = repository.rechercher(
            /* ... */, recherche.trim(),
            // Le tri appartient à la pagination : trier APRÈS avoir découpé
            // n'aurait aucun sens.
            PageRequest.of(Math.max(0, page), tailleBornee, Sort.by("titre").ascending()));

    return new PageRessources(
            resultat.getContent(), resultat.getNumber(), resultat.getSize(),
            resultat.getTotalElements(), resultat.getTotalPages());
}
```

### Pourquoi un DTO plutôt que le `Page<T>` de Spring

On pourrait renvoyer directement l'objet `Page<T>`. C'est déconseillé : il sérialise une douzaine de champs internes (`pageable`, `sort`, `first`, `numberOfElements`…) dont la forme n'est pas garantie d'une version de Spring à l'autre. Un `record` maison **fige le contrat d'API** et ne publie que ce que le client utilise vraiment.

```java
// Un DTO de réponse : cinq champs choisis, et rien d'autre.
public record PageRessources(
        List<Ressource> contenu, int page, int taille, long total, int totalPages) {}
```

### La conséquence oubliée : découper oblige à offrir l'accès unitaire

Une page de détail qui cherchait son élément dans la liste déjà chargée devient **fausse** dès que la liste est paginée : l'élément demandé peut se trouver sur une autre page. Il faut donc ajouter un `GET /api/ressources/{id}`. Ce n'est pas un détail cosmétique — c'est la conséquence logique directe du découpage.

```java
@GetMapping("/{id}")
public Ressource une(@PathVariable Long id, @AuthenticationPrincipal String email) {
    return repository.findByIdAndProprietaireId(id, /* ... */)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
}
```

### Côté Angular : `params`, `debounceTime`, `switchMap`

Trois notions arrivent ensemble, chacune réglant un problème distinct de la saisie au clavier.

```typescript
// 1. Les critères de la page vivent dans le SERVICE, pas dans le composant :
//    après un ajout, c'est lui qui doit savoir quelle page recharger.
private rechercheSignal = signal('');
private pageSignal = signal(0);

// 2. Un Subject : un Observable qu'on alimente à la main avec next().
//    Il sert de point de rendez-vous entre des appels ponctuels et UN flux
//    durable, monté une fois dans le constructeur.
private demandes = new Subject<void>();

constructor() {
  this.demandes.pipe(
    // 3. switchMap : chaque nouvelle demande ANNULE la précédente, réponse
    //    comprise. Sans lui, une réponse lente à « dup » pourrait arriver
    //    APRÈS celle de « dupont » et réécrire la liste avec un résultat périmé.
    switchMap(() => this.http.get<PageRessources>(this.apiUrl, {
      // Angular assemble et ÉCHAPPE la query string : ?recherche=x&page=0
      params: { recherche: this.rechercheSignal(), page: this.pageSignal(), taille: 6 }
    }).pipe(
      // catchError À L'INTÉRIEUR du switchMap — c'est capital. À l'extérieur,
      // il attraperait l'erreur du flux EXTERNE, qui se terminerait alors
      // définitivement : la première panne réseau condamnerait la recherche
      // pour le reste de la session.
      catchError(() => EMPTY)
    ))
  ).subscribe(page => { /* remplir les signaux */ });
}

rechercher(terme: string): void {
  this.rechercheSignal.set(terme);
  // Sans cette remise à zéro, chercher depuis la page 3 afficherait une page
  // vide : trois résultats existent, mais on en demande les 19e à 24e.
  this.pageSignal.set(0);
  this.demandes.next();
}
```

Dans le composant, deux opérateurs de plus filtrent la frappe avant qu'elle n'atteigne le réseau :

```typescript
champRecherche = new FormControl('', { nonNullable: true });

constructor() {
  this.champRecherche.valueChanges.pipe(
    // N'émettre qu'après 300 ms de silence. Sans lui, taper « dupont »
    // lancerait six requêtes — une par lettre — dont cinq déjà périmées.
    debounceTime(300),
    // Ignorer une valeur identique à la précédente (taper une lettre puis
    // l'effacer pendant le délai).
    distinctUntilChanged(),
    // Se désabonner à la destruction du composant. Sans argument, il exige un
    // contexte d'injection — le constructeur en est un.
    takeUntilDestroyed()
  ).subscribe(terme => this.service.rechercher(terme));
}
```

| Opérateur | Problème qu'il règle |
|---|---|
| `debounceTime(ms)` | Une requête par frappe de touche |
| `distinctUntilChanged()` | Une requête pour une valeur qui n'a pas changé |
| `switchMap(fn)` | Une réponse périmée qui écrase une réponse récente |
| `takeUntilDestroyed()` | Un abonnement qui survit au composant |
| `Subject` | Transformer des appels ponctuels en un flux unique |

### Dans le projet

**Requête paginée** — [`carnet-contact-backend/src/main/java/.../repository/ContactRepository.java`](../carnet-contact-backend/src/main/java/com/example/carnet_contact_backend/repository/ContactRepository.java)

```java
@Query("""
        SELECT c FROM Contact c
        WHERE c.proprietaire.id = :proprietaireId
          AND (:recherche = ''
               OR LOWER(c.nom)    LIKE LOWER(CONCAT('%', :recherche, '%'))
               OR LOWER(c.prenom) LIKE LOWER(CONCAT('%', :recherche, '%'))
               OR LOWER(c.email)  LIKE LOWER(CONCAT('%', :recherche, '%')))
        """)
Page<Contact> rechercher(
        @Param("proprietaireId") Long proprietaireId,
        @Param("recherche") String recherche,
        Pageable pagination);
```

> **Note de mise à jour.** `findByProprietaireIdOrderByNomAsc(...)`, cité en sections 18 et 19 comme exemple de requête dérivée, a été **remplacé** par cette méthode : la dérivation par nom ne sait pas exprimer « nom OU prénom OU email », ni recevoir un `Pageable` avec un terme optionnel. Les autres exemples de requêtes dérivées de la section 19 (`findByIdAndProprietaireId`, `existsByEmail`, `findByDestinataireIdAndLuFalse`) restent d'actualité.

**Contrôleur** — [`carnet-contact-backend/src/main/java/.../controller/ContactController.java`](../carnet-contact-backend/src/main/java/com/example/carnet_contact_backend/controller/ContactController.java)

```java
public record PageContacts(
        List<Contact> contenu, int page, int taille, long total, int totalPages) {}

@GetMapping
public PageContacts getMesContacts(
        @AuthenticationPrincipal String email,
        @RequestParam(defaultValue = "") String recherche,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "6") int taille) {

    int tailleBornee = Math.clamp(taille, 1, 50);
    Page<Contact> resultat = contactRepository.rechercher(
            utilisateurConnecte(email).getId(), recherche.trim(),
            PageRequest.of(Math.max(0, page), tailleBornee, Sort.by("nom").ascending()));

    return new PageContacts(resultat.getContent(), resultat.getNumber(),
            resultat.getSize(), resultat.getTotalElements(), resultat.getTotalPages());
}

// Nouveau : la page de détail ne peut plus fouiller la liste, qui ne contient
// plus qu'une page.
@GetMapping("/{id}")
public Contact getContact(@PathVariable Long id, @AuthenticationPrincipal String email) {
    return contactRepository
            .findByIdAndProprietaireId(id, utilisateurConnecte(email).getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
}
```

**Service Angular** — [`carnet-contact_frontend/src/app/services/contact.ts`](../carnet-contact_frontend/src/app/services/contact.ts)

Conséquence directe du découpage : `addContact` et `deleteContact` ne peuvent plus modifier le signal à la main (comme en section 12), parce que la répartition en pages est calculée par le serveur.

```typescript
addContact(contact: Contact): void {
  this.http.post<Contact>(this.apiUrl, contact, { /* ... */ }).pipe(
    catchError(() => EMPTY)
  ).subscribe(() => {
    // Selon son nom, le nouveau contact appartient peut-être à une autre page,
    // et le total a changé de toute façon.
    this.chargerContacts();
  });
}
```

**Page de détail** — [`carnet-contact_frontend/src/app/pages/contact-detail/contact-detail.ts`](../carnet-contact_frontend/src/app/pages/contact-detail/contact-detail.ts)

```typescript
// Avant : computed() qui cherchait dans le signal partagé (section 13).
// Depuis la pagination, la liste ne contient plus que six contacts : la page
// réclame désormais sa propre fiche. Le principe est inchangé — le gabarit lit
// un signal et se réaffiche quand la réponse arrive — seule la source diffère.
contact = this.contactService.contactCourant;

ngOnInit(): void {
  this.contactService.chargerContact(this.contactId);
}
```

**Champ de recherche et pagination** — [`carnet-contact_frontend/src/app/components/contact-list/contact-list.ts`](../carnet-contact_frontend/src/app/components/contact-list/contact-list.ts), [`contact-list.html`](../carnet-contact_frontend/src/app/components/contact-list/contact-list.html)

```html
@if (totalPages() > 1) {
  <nav class="pagination" aria-label="Pagination des contacts">
    <button (click)="pagePrecedente()" [disabled]="premierePage() || chargement()">← Précédent</button>
    <span>Page {{ page() + 1 }} sur {{ totalPages() }}</span>
    <button (click)="pageSuivante()" [disabled]="dernierePage() || chargement()">Suivant →</button>
  </nav>
}
```

> **Note de mise à jour.** Ces deux boutons ont depuis été remplacés par le `p-paginator` de PrimeNG, qui apporte les numéros de page, le saut au début et à la fin, et la navigation au clavier (section 30). Le raisonnement de cette section est intact — c'est le serveur qui pagine, le client ne fait qu'indiquer quelle page il veut ; seule la façon de le lui demander a changé. Une conversion est apparue au passage, le paginateur raisonnant en index d'élément là où le service raisonne en numéro de page.

---

## 23. Contexte d'une requête HTTP (`HttpContext`)

### Le problème : l'intercepteur ne sait pas ce qu'il intercepte

La section 17 a sorti la gestion d'erreur des services, et c'était un gain. Mais cela a coûté quelque chose : l'intercepteur ne connaît que le **code de statut**, pas l'intention. « Erreur interne du serveur (500) » a remplacé « Impossible d'ajouter le contact ». Exact, mais muet sur ce que l'utilisateur venait de tenter.

La tentation serait de remettre un `catchError` métier dans chaque service — et de recréer exactement la duplication qu'on venait de supprimer. Le **contexte** offre la troisième voie : le service déclare *une donnée* (ce qu'il était en train de faire), l'intercepteur garde *toute la logique*.

### `HttpContextToken` : une valise attachée à la requête

```typescript
// La fonction passée donne la valeur PAR DÉFAUT, utilisée quand personne n'a
// rien attaché. Le contexte n'est donc jamais « absent » : il y a toujours une
// valeur à lire, ce qui évite les tests de nullité partout.
export const LIBELLE_ACTION = new HttpContextToken<string | null>(() => null);
export const DISCRET = new HttpContextToken<boolean>(() => false);

// Raccourci de construction : sans lui, chaque appel s'écrirait
// `{ context: new HttpContext().set(LIBELLE_ACTION, '…') }` — exact, mais
// assez verbeux pour décourager de s'en servir.
export function contexte(options: { libelle?: string; discret?: boolean } = {}): HttpContext {
  let resultat = new HttpContext();
  if (options.libelle !== undefined) resultat = resultat.set(LIBELLE_ACTION, options.libelle);
  if (options.discret !== undefined) resultat = resultat.set(DISCRET, options.discret);
  return resultat;
}
```

**Pourquoi pas un simple en-tête HTTP ?** Parce qu'un en-tête part sur le réseau : on enverrait au serveur du texte français qui ne le regarde pas, à chaque requête. Le contexte, lui, ne quitte jamais le navigateur — c'est un canal entre le code appelant et les intercepteurs, rien de plus.

### Poser, puis lire

```typescript
// Côté appelant : une option de plus, à côté de params et headers.
this.http.post<Ressource>(url, corps, {
  context: contexte({ libelle: "Impossible d'ajouter la ressource" })
});

// Côté intercepteur : req.context.get(JETON) rend toujours une valeur.
const libelle = req.context.get(LIBELLE_ACTION);
const estDiscret = req.context.get(DISCRET);
```

| Élément | Rôle |
|---|---|
| `new HttpContextToken<T>(() => défaut)` | Déclare une clé typée, avec sa valeur de repli |
| `new HttpContext().set(jeton, valeur)` | Construit un contexte (immuable : `set` rend une **copie**) |
| `req.context.get(jeton)` | Lit la valeur, ou le défaut si rien n'a été posé |
| `{ context: … }` | L'option à passer à `get`/`post`/`put`/`delete` |

### Recomposer le message

L'astuce est de découper le message en deux moitiés, chacune fournie par celui qui la connaît :

```typescript
// La RAISON technique, formulée comme un fragment de phrase (pas de majuscule,
// pas de point) : elle est destinée à être recollée derrière le libellé.
function raisonTechnique(erreur: HttpErrorResponse): string {
  switch (erreur.status) {
    case 0:   return 'le serveur est injoignable';
    case 404: return 'la ressource est introuvable (404)';
    case 500: return 'le serveur a rencontré une erreur interne (500)';
    default:  return `une erreur inattendue s'est produite (${erreur.status})`;
  }
}

function messagePour(erreur: HttpErrorResponse, libelle: string | null): string {
  const raison = raisonTechnique(erreur);
  // Avec libellé : « Impossible d'ajouter la ressource : le serveur est injoignable. »
  if (libelle) return `${libelle} : ${raison}.`;
  // Sans libellé : « Le serveur est injoignable. »
  return raison.charAt(0).toUpperCase() + raison.slice(1) + '.';
}
```

Le service fournit la moitié qu'il est seul à connaître (l'intention), l'intercepteur fournit celle qu'il est seul à connaître (le statut). Aucun des deux ne fait le travail de l'autre — et ajouter un cinquième appel ne demande qu'une chaîne de caractères, pas un bloc de gestion d'erreur.

### Le second usage : les requêtes que l'utilisateur n'a pas demandées

Le drapeau `discret` répond à un besoin différent, apparu avec le rafraîchissement automatique (section 25) : une requête de fond ne doit ni allumer l'indicateur de chargement, ni afficher de bannière d'erreur.

```typescript
// Dans l'intercepteur de chargement : sortie anticipée.
if (req.context.get(DISCRET)) {
  return next(req);   // on transmet quand même — un intercepteur qui n'appelle
}                     // pas next() bloque la requête pour de bon
```

### Dans le projet

**Les deux jetons** — [`carnet-contact_frontend/src/app/interceptors/http-contexte.ts`](../carnet-contact_frontend/src/app/interceptors/http-contexte.ts)

**Lecture** — [`carnet-contact_frontend/src/app/interceptors/erreur-interceptor.ts`](../carnet-contact_frontend/src/app/interceptors/erreur-interceptor.ts)

```typescript
const estDiscret = req.context.get(DISCRET);

if (!estDiscret) {
  etatHttp.effacerErreur();
}

return next(req).pipe(
  catchError((erreur: HttpErrorResponse) => {
    if (!estAppelAuth && !estDiscret) {
      etatHttp.signalerErreur(messagePour(erreur, req.context.get(LIBELLE_ACTION)));
    }
    return throwError(() => erreur);
  })
);
```

**Écriture** — [`carnet-contact_frontend/src/app/services/contact.ts`](../carnet-contact_frontend/src/app/services/contact.ts)

```typescript
deleteContact(id: number): void {
  this.http.delete<void>(`${this.apiUrl}/${id}`, {
    context: contexte({ libelle: 'Impossible de supprimer le contact' })
  }).pipe(catchError(() => EMPTY)).subscribe(() => this.chargerContacts());
}
```

---

## 24. Renouvellement du jeton d'accès

### Le problème : un JWT ne s'annule pas

Un JWT est vérifié par un calcul de signature, sans rien demander à la base — c'est ce qui le rend rapide, et c'est exactement ce qui le rend **irrévocable**. Tant qu'il n'a pas expiré, il ouvre la porte, même si le compte a été compromis entre-temps. Sa durée de vie *est* la durée pendant laquelle un vol reste exploitable.

La parade est de lui donner une vie courte : quinze minutes au lieu de vingt-quatre heures. Mais personne ne veut retaper son mot de passe quatre fois par heure. D'où un **second jeton**, long et lui, révocable.

| | Jeton d'accès | Jeton de rafraîchissement |
|---|---|---|
| Forme | JWT signé | Valeur aléatoire « opaque » (rien à lire dedans) |
| Durée | 15 minutes | 7 jours |
| Stocké côté serveur | Non | **Oui**, en base |
| Vérification | Calcul de signature | Un `SELECT` |
| Envoyé à chaque requête | Oui (`Authorization`) | Non — seulement à `/auth/rafraichir` |
| Annulable | Non | **Oui** |

Le partage des rôles : le jeton court paie le prix de la vitesse (aucun accès base par requête), le jeton long paie le prix du contrôle (un accès base, mais toutes les quinze minutes seulement).

### Côté serveur : une entité, et la rotation

```java
@Entity
public class JetonRafraichissement {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String valeur;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utilisateur_id")
    private Utilisateur utilisateur;

    @Column(nullable = false) private Instant expiration;
    // On MARQUE plutôt qu'on supprime : une ligne révoquée garde une trace.
    @Column(nullable = false) private boolean revoque;

    public boolean estUtilisable() {
        return !revoque && expiration.isAfter(Instant.now());
    }
}
```

```java
/**
 * SecureRandom, et non Random. Les deux produisent des nombres « au hasard »,
 * mais Random est PRÉVISIBLE : à partir de quelques valeurs observées, on
 * retrouve sa graine, donc toutes les suivantes. Disqualifiant pour un secret.
 */
private final SecureRandom aleatoire = new SecureRandom();

@Transactional
public JetonRafraichissement emettre(Utilisateur utilisateur) {
    byte[] octets = new byte[32];
    aleatoire.nextBytes(octets);

    JetonRafraichissement jeton = new JetonRafraichissement();
    // withoutPadding : évite les « = » finaux, gênants dans une URL.
    jeton.setValeur(Base64.getUrlEncoder().withoutPadding().encodeToString(octets));
    /* ... */
    return repository.save(jeton);
}

/**
 * La ROTATION : vérifier, révoquer l'ancien, en émettre un neuf.
 *
 * Pourquoi ne pas laisser réutiliser le même pendant sept jours ? Parce qu'un
 * jeton volé serait alors exploitable sept jours sans que rien ne le trahisse.
 * Avec la rotation, le voleur et le vrai utilisateur se disputent un jeton à
 * usage unique : dès que l'un s'en sert, l'autre se voit déconnecté —
 * l'anomalie devient VISIBLE.
 */
@Transactional
public Optional<JetonRafraichissement> faireTourner(String valeurPresentee) {
    return repository.findByValeur(valeurPresentee)
            .filter(JetonRafraichissement::estUtilisable)
            .map(ancien -> {
                ancien.setRevoque(true);
                repository.save(ancien);
                return emettre(ancien.getUtilisateur());
            });
}
```

Le point d'entrée `/auth/rafraichir` est **public**, et c'est normal : il est appelé justement quand le jeton d'accès n'est plus valable. Exiger une authentification pour venir se réauthentifier n'aurait aucun sens — c'est le même raisonnement que pour `/auth/connexion`. La preuve d'identité, ici, c'est la possession du jeton long.

### La déconnexion devient réelle

Jusqu'ici, se déconnecter revenait à jeter le jeton côté navigateur ; le serveur n'en savait rien. Avec un jeton stocké en base, `POST /auth/deconnexion` le révoque : même recopié ailleurs, il n'ouvre plus rien. Le jeton d'accès déjà émis, lui, reste valable jusqu'à son expiration — contrepartie assumée du « sans état ».

### Côté Angular : rattraper le 401 et rejouer

Le bénéfice se mesure du point de vue de l'utilisateur : il clique sur « Enregistrer » vingt minutes après s'être connecté, et **ça marche**. Sans cet intercepteur, il serait éjecté vers la page de connexion en perdant sa saisie.

```typescript
export const rafraichissementInterceptor: HttpInterceptorFn = (req, next) => {
  // Les appels d'authentification ne se rejouent pas. /rafraichir surtout : il
  // se rappellerait lui-même à l'infini sur un 401.
  if (req.url.includes('/api/auth/')) return next(req);

  return next(req).pipe(
    catchError((erreur: HttpErrorResponse) => {
      if (erreur.status !== 401 || session.jetonRafraichissementActuel() === null) {
        return throwError(() => erreur);
      }

      return rafraichissement.obtenirNouveauJeton().pipe(
        // switchMap : « quand le nouveau jeton arrive, abandonne ce flux-ci et
        // continue avec celui de la requête rejouée ». L'appelant d'origine
        // reçoit la vraie réponse, sans savoir qu'un détour a eu lieu.
        switchMap(nouveauJeton => next(req.clone({
          // La requête porte déjà l'ancien en-tête, posé plus haut dans la
          // chaîne. setHeaders l'écrase.
          setHeaders: { Authorization: `Bearer ${nouveauJeton}` }
        }))),
        // Relancer l'erreur 401 D'ORIGINE, pas celle du rafraîchissement :
        // c'est elle qui a du sens pour la suite de la chaîne.
        catchError(() => throwError(() => erreur))
      );
    })
  );
};
```

**Sa place dans la chaîne fait partie du comportement.** Il est enregistré **en dernier**, donc c'est le maillon le plus profond : à l'aller la requête le traverse en dernier, au retour l'erreur l'atteint en **premier** — avant l'intercepteur d'erreur, qui déconnecte sur 401. Inversé, la déconnexion se produirait avant toute tentative de renouvellement.

### La vraie difficulté : plusieurs 401 en même temps

Quand un jeton expire, ce n'est presque jamais une requête qui échoue : c'est la page entière. Trois appels partis ensemble reçoivent trois 401 quasi simultanés. Sans précaution, chacun lancerait son propre rafraîchissement — trois rotations en chaîne, dont les deux dernières présenteraient un jeton que la première vient de révoquer. Résultat : l'utilisateur déconnecté alors que tout allait bien.

```typescript
// L'appel en cours, s'il y en a un : la mémoire qui permet aux appelants
// suivants de se greffer sur le premier au lieu d'en lancer un second.
private enCours: Observable<string> | null = null;

obtenirNouveauJeton(): Observable<string> {
  if (this.enCours) return this.enCours;   // on rend le MÊME Observable

  this.enCours = this.http.post<ReponseAuth>('/api/auth/rafraichir', { /* ... */ }).pipe(
    // La rotation a émis un nouveau jeton long : mémoriser les DEUX, sinon la
    // prochaine expiration présenterait une valeur déjà révoquée.
    tap(reponse => this.session.ouvrir(reponse.jeton, reponse.jetonRafraichissement, reponse.utilisateur)),
    map(reponse => reponse.jeton),
    catchError(erreur => {
      this.session.vider();              // plus de porte de sortie
      return throwError(() => erreur);
    }),
    // Efface la mémoire quand l'appel est terminé, dans un sens ou dans
    // l'autre. Sans cela, `enCours` garderait pour toujours le résultat du
    // premier rafraîchissement.
    finalize(() => { this.enCours = null; }),
    // Une seule requête réseau, partagée par tous les abonnés. Sans lui, un
    // Observable HttpClient étant « froid », chaque abonnement relancerait un
    // appel : exactement ce qu'on cherche à éviter.
    shareReplay(1)
  );

  return this.enCours;
}
```

| Opérateur | Rôle dans ce montage |
|---|---|
| `shareReplay(1)` | Une requête pour N abonnés, et sa valeur rejouée aux retardataires |
| `finalize(fn)` | Libère la mémoire à la fin, succès ou échec |
| `tap(fn)` | Effet de bord (mémoriser la session) sans modifier le flux |
| `switchMap(fn)` | Enchaîner sur la requête rejouée |

### Dans le projet

**Entité et rotation** — [`carnet-contact-backend/src/main/java/.../model/JetonRafraichissement.java`](../carnet-contact-backend/src/main/java/com/example/carnet_contact_backend/model/JetonRafraichissement.java), [`security/RafraichissementService.java`](../carnet-contact-backend/src/main/java/com/example/carnet_contact_backend/security/RafraichissementService.java)

**Points d'entrée** — [`carnet-contact-backend/src/main/java/.../controller/AuthController.java`](../carnet-contact-backend/src/main/java/com/example/carnet_contact_backend/controller/AuthController.java)

```java
@PostMapping("/rafraichir")
public ResponseEntity<?> rafraichir(@RequestBody DemandeRafraichissement demande) {
    return rafraichissementService.faireTourner(demande.jetonRafraichissement())
            .<ResponseEntity<?>>map(nouveau -> ResponseEntity.ok(new ReponseAuth(
                    jwtService.genererJeton(nouveau.getUtilisateur().getEmail()),
                    nouveau.getValeur(), nouveau.getUtilisateur())))
            // 401 et non 403 : le client doit comprendre « reconnecte-toi ».
            .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Jeton de rafraîchissement invalide ou expiré."));
}
```

**Durées** — [`carnet-contact-backend/src/main/resources/application.properties`](../carnet-contact-backend/src/main/resources/application.properties)

```properties
carnet.jwt.duree-ms=900000                    # 15 minutes
carnet.jwt.rafraichissement-duree-ms=604800000 # 7 jours
```

**Côté Angular** — [`services/rafraichissement.ts`](../carnet-contact_frontend/src/app/services/rafraichissement.ts), [`interceptors/rafraichissement-interceptor.ts`](../carnet-contact_frontend/src/app/interceptors/rafraichissement-interceptor.ts), [`app.config.ts`](../carnet-contact_frontend/src/app/app.config.ts)

```typescript
provideHttpClient(
  withInterceptors([
    baseUrlInterceptor,
    authInterceptor,
    chargementInterceptor,
    erreurInterceptor,
    // En dernier = au plus profond : il voit l'erreur en PREMIER au retour,
    // donc avant qu'erreurInterceptor ne déconnecte sur 401.
    rafraichissementInterceptor
  ])
)
```

**Déconnexion** — [`carnet-contact_frontend/src/app/services/auth.ts`](../carnet-contact_frontend/src/app/services/auth.ts)

```typescript
deconnexion(): void {
  const jetonRafraichissement = this.session.jetonRafraichissementActuel();
  // On vide LOCALEMENT d'abord, sans attendre : l'interface doit réagir au
  // clic, et une panne réseau ne doit pas laisser l'utilisateur connecté
  // malgré lui. L'appel serveur part en parallèle, son échec est ignoré.
  this.session.vider();

  if (jetonRafraichissement) {
    this.http.post<void>(`${this.apiUrl}/deconnexion`, { jetonRafraichissement })
      .pipe(catchError(() => EMPTY)).subscribe();
  }
}
```

---

## 25. Rafraîchissement automatique (sondage périodique)

### Le problème : une page qui ne sait pas qu'elle a vieilli

La messagerie n'affichait les nouveaux messages qu'au rechargement du fil. Un interlocuteur pouvait répondre trois fois sans que rien ne bouge à l'écran.

La solution la plus simple est le **sondage** (*polling*) : redemander régulièrement. Ce n'est pas la technique la plus élégante — un WebSocket laisserait le serveur *pousser* les nouveautés au lieu de les attendre — mais elle ne demande aucune infrastructure nouvelle, réutilise l'authentification déjà en place, et tient en un opérateur RxJS.

### `timer` : un flux qui émet tout seul

```typescript
// timer(0, 5000) émet immédiatement, puis toutes les 5 secondes : 0, 1, 2, 3…
// Le premier zéro est important : sans lui, ouvrir un fil laisserait l'écran
// vide pendant cinq secondes.
this.suivi = timer(0, 5000).pipe(
  // timer émet un COMPTEUR : on s'en sert pour distinguer le premier
  // chargement (déclenché par un clic, donc l'utilisateur attend et mérite
  // l'indicateur) des suivants, silencieux.
  switchMap(tour => this.http.get<T[]>(url, {
    context: contexte({ discret: tour > 0 })
  }).pipe(
    // Toujours à l'INTÉRIEUR du switchMap : dehors, la première coupure
    // réseau terminerait le flux du timer et arrêterait le rafraîchissement
    // pour de bon.
    catchError(() => EMPTY)
  ))
).subscribe(donnees => this.signal.set(donnees));
```

`switchMap` sert ici une seconde fonction, en plus de celle de la section 22 : si une réponse tarde plus que l'intervalle, le tour suivant annule le précédent au lieu d'empiler les requêtes.

### Le vrai piège : savoir s'arrêter

Un `timer` tourne **indéfiniment** tant que personne ne se désabonne. Trois arrêts sont à prévoir, et chacun correspond à un bug concret si on l'oublie :

| Ce qu'on oublie | Ce qui se passe |
|---|---|
| Arrêter à la déconnexion | Le sondage continue avec un jeton invalide : un 401 toutes les 15 secondes |
| Arrêter à la destruction du composant | Des requêtes partent pour alimenter un écran que plus personne ne regarde |
| Empêcher un second démarrage | Deux timers empilés, donc deux fois plus de requêtes — puis quatre, puis huit |

```typescript
// On garde l'abonnement sous la main pour pouvoir l'arrêter.
private suivi?: Subscription;

demarrer(): void {
  // Garde-fou : ne pas empiler un second timer.
  if (!this.navigateur || this.suivi) return;
  this.suivi = timer(0, INTERVALLE).pipe(/* ... */).subscribe(/* ... */);
}

arreter(): void {
  this.suivi?.unsubscribe();
  this.suivi = undefined;
}
```

Dans un composant, `ngOnDestroy` est l'endroit prévu pour cela — le pendant de `ngOnInit` (section 11), appelé quand Angular retire le composant de l'écran :

```typescript
export class MaPage implements OnInit, OnDestroy {
  ngOnDestroy(): void {
    this.service.arreter();
  }
}
```

### Et le SSR : ne rien démarrer côté serveur

Même précaution qu'en section 18 pour `localStorage`, mais pour une raison plus grave. Angular attend que l'application soit « stable » avant de renvoyer le HTML rendu côté serveur. Un flux qui ne se termine jamais l'en empêche : le rendu **ne se termine pas**, et la page ne s'affiche jamais.

```typescript
private navigateur = isPlatformBrowser(inject(PLATFORM_ID));

demarrer(): void {
  if (!this.navigateur) return;   // côté serveur : on ne démarre rien
  /* ... */
}
```

### Choisir un rythme

Deux valeurs différentes pour deux usages différents : on regarde un fil de discussion en attendant une réponse (5 s), alors qu'une pastille de notification n'est qu'une information d'ambiance (15 s). Sonder trop souvent coûte des requêtes pour rien ; pas assez donne une application qui paraît figée.

### Dans le projet

**Service** — [`carnet-contact_frontend/src/app/services/message.ts`](../carnet-contact_frontend/src/app/services/message.ts)

```typescript
const INTERVALLE_FIL_MS = 5000;
const INTERVALLE_NON_LUS_MS = 15000;

private navigateur = isPlatformBrowser(inject(PLATFORM_ID));
private suiviFil?: Subscription;
private suiviNonLus?: Subscription;

suivreFil(autreId: number): void {
  // Changer d'interlocuteur doit arrêter le suivi précédent, sinon deux
  // timers écriraient tour à tour dans le même signal.
  this.arreterSuiviFil();
  if (!this.navigateur) return;

  this.suiviFil = timer(0, INTERVALLE_FIL_MS).pipe(
    switchMap(tour => this.http.get<Message[]>(`${this.apiUrl}/${autreId}`, {
      context: contexte({ discret: tour > 0, libelle: 'Impossible de charger la conversation' })
    }).pipe(catchError(() => EMPTY)))
  ).subscribe(messages => {
    this.filSignal.set(messages);
    // Le serveur a marqué ces messages comme lus en répondant : la pastille
    // doit suivre tout de suite, sans attendre le prochain tour du second timer.
    this.nonLusSignal.update(liste => liste.filter(m => m.expediteur.id !== autreId));
  });
}
```

**Démarrage / arrêt selon la connexion** — [`carnet-contact_frontend/src/app/app.ts`](../carnet-contact_frontend/src/app/app.ts)

```typescript
constructor() {
  // Cet effect() ne déclenche plus un appel : il DÉMARRE et ARRÊTE un suivi.
  // Le cas « arrêter » est le plus important — sans lui, le sondage
  // continuerait après la déconnexion, avec un jeton devenu invalide.
  effect(() => {
    if (this.auth.connecte()) {
      this.messages.demarrerSuiviNonLus();
    } else {
      this.messages.arreterSuiviNonLus();
    }
  });
}
```

**Arrêt en quittant la page** — [`carnet-contact_frontend/src/app/pages/messages/messages.ts`](../carnet-contact_frontend/src/app/pages/messages/messages.ts)

```typescript
ouvrir(utilisateur: Utilisateur): void {
  this.selection.set(utilisateur);
  this.messageService.suivreFil(utilisateur.id);
}

ngOnDestroy(): void {
  this.messageService.arreterSuiviFil();
}
```

---

## 26. Tests automatisés

### Le problème : vérifier à la main ne passe pas à l'échelle

Jusqu'ici, chaque fonctionnalité était validée en la manipulant : cliquer, regarder, recommencer. Cela marche une fois. Le problème n'est pas de vérifier que le code écrit aujourd'hui fonctionne — c'est de vérifier que celui d'il y a trois mois fonctionne **encore** après la modification d'aujourd'hui. Personne ne reteste vingt écrans à la main après chaque changement.

Certaines règles sont en plus **invisibles** à l'œil nu. « Un utilisateur ne peut pas lire les contacts d'un autre » ne se voit pas en regardant l'écran : on ne voit que ce qui s'affiche, jamais ce qui aurait pu s'afficher. Il suffit d'un `findById` à la place d'un `findByIdAndProprietaireId` pour ouvrir tout le carnet de tout le monde, sans le moindre symptôme visible.

### Les deux formes de test

| | Test unitaire | Test d'intégration |
|---|---|---|
| Portée | Une classe isolée | Plusieurs couches assemblées |
| Démarrage | Quelques millisecondes | Quelques secondes (contexte Spring) |
| Répond à | « Ce calcul est-il juste ? » | « Le système protège-t-il vraiment ? » |
| Exemple ici | `JwtServiceTest` | `ContactControllerTest` |

Le choix n'est pas une question de goût. Tester un contrôleur *seul*, avec un faux repository, prouverait seulement que le code fait ce qu'on a écrit — pas qu'il protège quoi que ce soit, puisque la protection naît de la coopération du filtre JWT, de la configuration de sécurité et de la requête SQL.

### Côté Java : JUnit et AssertJ

Un test unitaire pur n'a besoin d'aucune annotation Spring. On construit la classe à la main, on l'interroge.

```java
class MonServiceTest {

    @Test
    @DisplayName("Une phrase lisible qui décrit le comportement attendu")
    void nomTechniqueDuTest() {
        MonService service = new MonService("paramètre", 60_000);

        String resultat = service.faireQuelqueChose();

        // assertThat(...).isEqualTo(...) : la forme « fluide » d'AssertJ. Elle
        // se lit comme une phrase, et son message d'échec est plus parlant
        // qu'un simple « expected true ».
        assertThat(resultat).isEqualTo("attendu");
    }
}
```

L'injection par constructeur, adoptée en section 33 pour d'autres raisons, se révèle ici un avantage inattendu : elle permet de fabriquer l'objet avec **les valeurs qu'on veut**, y compris des valeurs impossibles autrement.

```java
// Durée négative : le jeton naît déjà périmé. Impossible à obtenir en
// conditions réelles sans attendre quinze minutes.
JwtService service = new JwtService(SECRET, -1_000);
assertThat(service.emailDuJeton(service.genererJeton("a@b.fr"))).isNull();
```

### Côté Java : MockMvc pour les tests d'intégration

```java
@SpringBootTest        // démarre le contexte Spring complet, comme en vrai
@AutoConfigureMockMvc  // fournit MockMvc : des requêtes HTTP sans ouvrir de port
@Transactional         // chaque test dans une transaction ANNULÉE à la fin
class MonControleurTest {

    @Autowired private MockMvc mockMvc;

    // @BeforeEach s'exécute avant CHAQUE méthode, pas une fois pour toutes :
    // c'est ce qui garantit que deux tests ne partagent jamais un objet que
    // l'un aurait modifié.
    @BeforeEach
    void preparer() { /* ... */ }

    @Test
    void sansJeton_renvoie401() throws Exception {
        mockMvc.perform(get("/api/ressources"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void avecJeton_renvoieLaPremierePage() throws Exception {
        mockMvc.perform(get("/api/ressources")
                        .param("page", "0")
                        .header("Authorization", "Bearer " + jeton))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.contenu[0].titre").value("Alpha"));
    }
}
```

`@Transactional` sur un test **ne veut pas dire la même chose** que sur un service : ici, Spring annule systématiquement la transaction à la fin. Chaque méthode repart d'une base propre, et l'ordre d'exécution cesse d'avoir la moindre importance — un test qui ne passe que s'il tourne en premier n'a aucune valeur.

| Élément | Rôle |
|---|---|
| `@SpringBootTest` | Démarre l'application entière |
| `@AutoConfigureMockMvc` | Injecte `MockMvc` |
| `@Transactional` (sur un test) | Annule tout à la fin : base propre à chaque méthode |
| `@BeforeEach` | Préparation rejouée avant chaque test |
| `@DisplayName("…")` | Le libellé lisible affiché dans le rapport |
| `mockMvc.perform(...)` | Envoie une requête sans réseau |
| `.andExpect(status().isOk())` | Vérifie le code de statut |
| `.andExpect(jsonPath("$.x").value(y))` | Vérifie un morceau du JSON de réponse |
| `JsonPath.read(corps, "$.x")` | **Récupère** une valeur (pour la requête suivante) |
| `src/test/resources/application.properties` | Configuration propre aux tests (base dédiée, `create-drop`) |

Un fichier `application.properties` placé dans `src/test/resources` prend le pas sur celui de `src/main/resources` : les tests tournent dans leur propre monde. Attention, il le **remplace**, il ne s'y ajoute pas — il doit donc être complet.

### Côté Angular : vitest et le faux serveur HTTP

Un test ne doit dépendre ni du réseau ni d'un backend démarré. S'il échoue, on veut savoir que c'est le code qui est faux — pas que le serveur était éteint. `provideHttpClientTesting()` remplace la couche qui parle au réseau : le service continue d'utiliser `HttpClient` exactement comme en vrai, sans savoir qu'il est testé.

```typescript
describe('MonService', () => {
  let service: MonService;
  let backend: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(MonService);
    backend = TestBed.inject(HttpTestingController);
  });

  // Le garde-fou qui attrape les requêtes parties sans qu'on s'y attende —
  // souvent le signe d'un appel en trop.
  afterEach(() => backend.verify());

  it('remplit le signal à partir de la réponse', () => {
    service.charger();

    const requete = backend.expectOne('/api/ressources');
    expect(requete.request.params.get('page')).toBe('0');
    requete.flush({ contenu: [], total: 0 });

    expect(service.donnees()).toEqual([]);
  });
});
```

| Méthode | Rôle |
|---|---|
| `expectOne(url)` | Affirme qu'une requête **et une seule** attend, et la rend |
| `expectNone(url)` | Affirme qu'aucune requête n'est partie vers cette URL |
| `match(critère)` | Rend toutes les requêtes en attente qui correspondent |
| `req.flush(corps)` | Répond avec succès |
| `req.flush('', { status: 500, statusText: '…' })` | Répond avec une erreur |
| `req.cancelled` | `true` si le flux a été annulé (preuve d'un `switchMap`) |
| `verify()` | Échoue s'il reste une requête sans réponse |

Pour un composant, on règle l'état par les services puis on interroge le HTML produit — jamais les variables internes :

```typescript
const fixture = TestBed.createComponent(MonComposant);
await fixture.whenStable();          // laisse Angular afficher

backend.expectOne('/api/ressources/7').flush({ id: 7, titre: 'Alpha' });
await fixture.whenStable();

expect((fixture.nativeElement as HTMLElement).textContent).toContain('Alpha');
```

Quand un composant dépend du routeur ou de l'URL, on fournit le minimum nécessaire — un **bouchon** (*stub*), pas un routeur complet :

```typescript
providers: [
  provideRouter([]),
  {
    provide: ActivatedRoute,
    useValue: { snapshot: { paramMap: convertToParamMap({ id: '7' }) } }
  }
]
```

### Ce qu'il vaut la peine de tester

Pas tout, et surtout pas « que la classe s'instancie » — c'est ce que génère le CLI par défaut, et cela ne protège de rien. Les bons candidats sont :

- **Ce qui est invisible.** L'isolation entre comptes, l'absence du mot de passe dans une réponse JSON.
- **Ce qu'on ne sait pas provoquer à la main.** Un jeton expiré, deux réponses qui arrivent dans le désordre, trois 401 simultanés.
- **Ce dont on a justifié la forme précise.** Si un commentaire dit « `catchError` doit être à l'intérieur du `switchMap` », un test doit échouer quand quelqu'un le déplace.

### Dans le projet

**Lancer les tests**

| Commande | Portée |
|---|---|
| `.\mvnw.cmd test` (backend) | 38 tests : JWT, politique de mot de passe, contacts, authentification |
| `npm test` (frontend) | 58 tests : services, intercepteurs, validateurs, composants |

**Test unitaire pur** — [`carnet-contact-backend/src/test/java/.../security/JwtServiceTest.java`](../carnet-contact-backend/src/test/java/com/example/carnet_contact_backend/security/JwtServiceTest.java)

```java
@Test
@DisplayName("Un jeton signé avec une autre clé est refusé")
void jetonDUneAutreCle_rendNull() {
    JwtService emetteur = new JwtService("une-tout-autre-cle-de-32-caracteres-au-moins", 60_000);
    JwtService verificateur = new JwtService(SECRET, 60_000);

    // LE test qui justifie tout le mécanisme : n'importe qui peut fabriquer un
    // JWT, mais seule la bonne clé produit une signature qu'on accepte.
    assertThat(verificateur.emailDuJeton(emetteur.genererJeton("mallory@exemple.fr"))).isNull();
}
```

**Isolation entre comptes** — [`carnet-contact-backend/src/test/java/.../controller/ContactControllerTest.java`](../carnet-contact-backend/src/test/java/com/example/carnet_contact_backend/controller/ContactControllerTest.java)

```java
@Test
@DisplayName("Modifier le contact d'un autre renvoie 404")
void modifierLeContactDUnAutre_renvoie404() throws Exception {
    Contact contactDAlice = creerContact(alice, "Dupont", "Marie", "marie@exemple.fr");

    mockMvc.perform(put("/api/contacts/" + contactDAlice.getId())
                    .header("Authorization", "Bearer " + jetonBob)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"nom\":\"Pirate\",\"prenom\":\"X\",\"email\":\"x@exemple.fr\"}"))
            .andExpect(status().isNotFound());

    // Vérifier le statut ne suffit pas : un 404 renvoyé APRÈS avoir écrit
    // serait catastrophique.
    assertThat(contactRepository.findById(contactDAlice.getId()).orElseThrow().getNom())
            .isEqualTo("Dupont");
}
```

**Rotation du jeton** — [`carnet-contact-backend/src/test/java/.../controller/AuthControllerTest.java`](../carnet-contact-backend/src/test/java/com/example/carnet_contact_backend/controller/AuthControllerTest.java)

```java
@Test
@DisplayName("Un jeton de rafraîchissement déjà utilisé est refusé (401)")
void rafraichir_deuxFoisAvecLeMemeJeton_renvoie401() throws Exception {
    String jetonLong = lire(inscrire("alice@exemple.fr", "motdepasse"), "$.jetonRafraichissement");

    mockMvc.perform(post("/api/auth/rafraichir") /* ... */).andExpect(status().isOk());
    mockMvc.perform(post("/api/auth/rafraichir") /* ... */).andExpect(status().isUnauthorized());
}
```

**Annulation par `switchMap`** — [`carnet-contact_frontend/src/app/services/contact.spec.ts`](../carnet-contact_frontend/src/app/services/contact.spec.ts)

```typescript
it('annule la requête précédente quand une nouvelle recherche part', () => {
  service.rechercher('dup');
  service.rechercher('dupont');

  const requetes = backend.match(r => r.url === '/api/contacts');
  expect(requetes[0].cancelled).toBe(true);
  expect(requetes[1].cancelled).toBe(false);

  requetes[1].flush(page([contactExemple]));
  expect(service.contacts()).toEqual([contactExemple]);
});
```

**Un seul renouvellement pour plusieurs 401** — [`carnet-contact_frontend/src/app/interceptors/rafraichissement-interceptor.spec.ts`](../carnet-contact_frontend/src/app/interceptors/rafraichissement-interceptor.spec.ts)

```typescript
it('ne lance qu\'un seul renouvellement pour plusieurs 401 simultanés', () => {
  http.get('/api/contacts').subscribe({ error: () => {} });
  http.get('/api/messages/non-lus').subscribe({ error: () => {} });

  backend.expectOne('/api/contacts').flush('', { status: 401, statusText: 'Unauthorized' });
  backend.expectOne('/api/messages/non-lus').flush('', { status: 401, statusText: 'Unauthorized' });

  // UN seul appel de renouvellement, pas deux.
  const renouvellements = backend.match('/api/auth/rafraichir');
  expect(renouvellements.length).toBe(1);
  /* ... */
});
```

**Message d'erreur composé** — [`carnet-contact_frontend/src/app/interceptors/erreur-interceptor.spec.ts`](../carnet-contact_frontend/src/app/interceptors/erreur-interceptor.spec.ts)

```typescript
it('compose le libellé métier et la raison technique', () => {
  http.get('/api/contacts', {
    context: contexte({ libelle: 'Impossible de charger les contacts' })
  }).subscribe({ error: () => {} });

  backend.expectOne('/api/contacts').flush('', { status: 500, statusText: 'Server Error' });

  expect(etat.erreur())
    .toBe('Impossible de charger les contacts : le serveur a rencontré une erreur interne (500).');
});
```

### Fabriques de données de test

Un jour, l'interface `Utilisateur` gagne deux champs obligatoires (`role`, `actif`). Quatre fichiers de test cessent aussitôt de compiler — chacun contenait sa propre copie de l'objet :

```typescript
const compte = { id: 1, email: 'alice@exemple.fr', nomAffichage: 'Alice' };
```

Il faut alors corriger quatre fois la même chose, à l'identique. Le vrai problème n'est pas la correction, c'est qu'elle se **répétera** à chaque évolution du modèle.

La réponse est une **fabrique** : une fonction qui construit l'objet, avec des valeurs par défaut raisonnables.

```typescript
/**
 * Partial<T> rend toutes les proprietes facultatives. Un test qui ne
 * s'interesse qu'au role ecrit unUtilisateur({ role: 'ADMIN' }) et laisse le
 * reste aux valeurs par defaut.
 *
 * Le test ne montre alors QUE ce qui compte pour lui : le bruit disparait, et
 * l'intention saute aux yeux.
 */
export function unUtilisateur(modifications: Partial<Utilisateur> = {}): Utilisateur {
  return {
    id: 1,
    email: 'alice@exemple.fr',
    nomAffichage: 'Alice',
    role: 'UTILISATEUR',
    actif: true,
    // L'etalement en DERNIER : ce que l'appelant fournit ecrase le defaut.
    // Place en premier, il serait lui-meme ecrase — l'ordre fait tout.
    ...modifications
  };
}
```

```typescript
// Avant
const bob = { id: 2, email: 'bob@exemple.fr', nomAffichage: 'Bob', role: 'UTILISATEUR', actif: true };

// Apres — et un champ ajoute au modele ne se corrige plus QU'A UN SEUL ENDROIT.
const bob = unUtilisateur({ id: 2, email: 'bob@exemple.fr', nomAffichage: 'Bob' });
```

C'est exactement la même logique que les variables CSS de la section 21 : **une information écrite une seule fois se met à jour une seule fois.**

**Dans le projet** — [`donnees-test.ts`](../carnet-contact_frontend/src/app/donnees-test.ts)

### Attendre ce qui n'est pas encore parti

Deux pièges reviennent constamment dans les tests Angular, et tous deux ont la même cause : **quelque chose est différé, et le test regarde trop tôt**.

```typescript
// Piege 1 : un timer(0, …). Le zero veut dire « au prochain tour de boucle »,
// pas « tout de suite ». Sans cette promesse vide qui rend la main au moteur
// JavaScript, expectOne chercherait un appel pas encore emis.
const rendreLaMain = () => new Promise(resolve => setTimeout(resolve, 0));

service.suivreFil(2);
await rendreLaMain();
backend.expectOne('/api/messages/2').flush([...]);
```

```typescript
// Piege 2 : un effect(). Il est DIFFERE, pas synchrone. TestBed.tick() lui
// laisse le temps de s'executer.
service.basculer();
TestBed.tick();
expect(document.documentElement.getAttribute('data-theme')).toBe('sombre');
```

| Symptôme | Cause | Correction |
|---|---|---|
| `Expected one matching request … found none` | Un `timer` n'a pas encore émis | `await` une promesse `setTimeout(…, 0)` |
| Un `effect()` ne semble pas s'être exécuté | Les effets sont différés | `TestBed.tick()` |
| Le test avec minuteurs simulés n'émet pas de requête | La file des micro-tâches n'est pas vidée | `vi.advanceTimersByTimeAsync()` plutôt que la variante synchrone |


---

## 27. Saisie et validation d'un mot de passe

### La règle des deux côtés

Jusqu'ici, l'inscription se contentait de vérifier que le mot de passe faisait six caractères. Le durcir soulève une question qui revient dans tout formulaire un peu sérieux : **où placer la règle ?**

La réponse est : aux deux endroits, et ce n'est pas de la duplication inutile — les deux versions ne servent pas à la même chose.

| | Côté navigateur | Côté serveur |
|---|---|---|
| Rôle | **Renseigner** pendant la saisie | **Décider** |
| Retour | Immédiat, à chaque frappe | Après l'envoi |
| Contournable ? | Oui, trivialement | Non |
| Si on l'enlève | L'utilisateur découvre son erreur après coup | La règle n'existe plus du tout |

La validation du navigateur est un **confort d'interface**, jamais une sécurité : n'importe qui peut envoyer une requête directement à l'API sans ouvrir le formulaire. Inversement, ne valider que côté serveur donne une expérience pénible — trois allers-retours pour comprendre ce qu'on attend de vous.

Corollaire pratique : si les deux versions divergent, le formulaire acceptera une saisie que le serveur refusera. Le seul remède est de les garder chacune **isolée dans son propre fichier**, faciles à comparer côte à côte, et de les couvrir par des tests symétriques.

### Côté serveur : une classe à part

```java
/**
 * Constructeur privé : cette classe n'est qu'un porte-méthodes, on ne veut
 * pas qu'on en crée des instances.
 */
public final class PolitiqueMotDePasse {

    private PolitiqueMotDePasse() {}

    /**
     * On renvoie la LISTE de ce qui manque, pas un simple booléen. « Refusé »
     * sans dire pourquoi oblige l'utilisateur à deviner ; énumérer les
     * critères non satisfaits lui permet de corriger du premier coup.
     */
    public record Resultat(boolean valide, List<String> manquants) {
        public String message() {
            return "Mot de passe trop faible — il lui manque : "
                    + String.join(", ", manquants) + ".";
        }
    }

    public static Resultat verifier(String motDePasse) {
        List<String> manquants = new ArrayList<>();

        if (motDePasse == null || motDePasse.length() < LONGUEUR_MINIMALE) {
            manquants.add(LONGUEUR_MINIMALE + " caractères minimum");
        }
        if (motDePasse != null) {
            // chars() rend le flux des caractères ; noneMatch s'arrête au
            // premier qui convient, sans parcourir toute la chaîne.
            if (motDePasse.chars().noneMatch(Character::isUpperCase)) {
                manquants.add("une majuscule");
            }
            // « Ni lettre ni chiffre » plutôt qu'une liste de symboles admis :
            // une liste oublierait toujours un caractère.
            if (motDePasse.chars().noneMatch(c -> !Character.isLetterOrDigit(c))) {
                manquants.add("un caractère spécial");
            }
            /* … */
        }

        return new Resultat(manquants.isEmpty(), manquants);
    }
}
```

Pourquoi une classe plutôt qu'un `if` dans le contrôleur ? Pour trois raisons qui reviendront souvent : la règle est devenue une vraie règle métier avec plusieurs critères ; elle est testable sans démarrer Spring, puisque c'est du calcul pur ; et elle doit pouvoir être comparée à son homologue Angular.

### La limite des règles de composition

Un point important à comprendre plutôt qu'à subir : **« Motdepasse1! » coche les cinq critères de forme** — longueur, minuscule, majuscule, chiffre, caractère spécial — et reste l'un des tout premiers mots de passe qu'une attaque essaie.

Les règles de composition ne mesurent pas la solidité, elles mesurent la *forme*. D'où l'ajout d'une liste de mots de passe trop courants, volontairement minuscule ici : elle illustre le principe plus qu'elle ne protège. Un vrai projet y brancherait une liste de plusieurs millions d'entrées.

```java
private static final Set<String> TROP_COURANTS = Set.of(
        "motdepasse", "password", "azerty123",
        // Ceux-ci cochent pourtant les cinq critères de forme :
        // c'est exactement pour eux que la liste existe.
        "motdepasse1!", "password1!", "azerty123!");
```

### Un validateur personnalisé côté Angular

`Validators.required` et `Validators.email` (section 7) ne sont rien d'autre que des fonctions d'une forme précise. Rien n'empêche d'écrire les siennes.

```typescript
/**
 * Un validateur reçoit le contrôle et rend soit `null`, soit un objet
 * décrivant l'erreur.
 *
 * La convention est contre-intuitive au début : `null` signifie VALIDE. Elle
 * se comprend en lisant l'objet renvoyé comme « la liste des erreurs » — pas
 * d'erreur, donc rien à renvoyer.
 */
export const motDePasseSolide: ValidatorFn = (
  control: AbstractControl
): ValidationErrors | null => {
  const valeur: string = control.value ?? '';

  // Champ vide : c'est le rôle de Validators.required. Un validateur qui se
  // mêle des cas des autres produit deux messages pour une seule erreur.
  if (valeur === '') {
    return null;
  }

  const manquants = CRITERES
    .filter(critere => !critere.verifie(valeur))
    .map(critere => critere.cle);

  // Un validateur n'est pas obligé de se limiter à un drapeau : il peut
  // transporter de quoi construire le message.
  return manquants.length === 0 ? null : { motDePasseFaible: { manquants } };
};
```

### Une seule liste pour valider et pour afficher

Même principe que la constante `RESEAUX` (section 20) : les critères sont décrits une fois, et servent aux deux usages. Ajouter un sixième critère ne demande qu'une ligne, et la liste à cocher suit toute seule.

```typescript
export interface Critere {
  cle: string;
  libelle: string;
  verifie: (valeur: string) => boolean;
}

export const CRITERES: Critere[] = [
  { cle: 'longueur',  libelle: '10 caractères minimum', verifie: v => v.length >= 10 },
  { cle: 'majuscule', libelle: 'Une majuscule',         verifie: v => /[A-Z]/.test(v) },
  { cle: 'special',   libelle: 'Un caractère spécial',  verifie: v => /[^a-zA-Z0-9]/.test(v) }
];
```

```html
<ul class="criteres" aria-label="Conditions du mot de passe">
  @for (critere of criteres; track critere.cle) {
    <li [class.satisfait]="critereSatisfait(critere)">
      <!-- La marque (• → ✓) ET la couleur portent l'information : la couleur
           seule serait invisible pour un daltonien. aria-hidden cache le
           symbole décoratif aux lecteurs d'écran, qui liront le libellé. -->
      <span class="marque" aria-hidden="true">
        @if (critereSatisfait(critere)) { ✓ } @else { • }
      </span>
      {{ critere.libelle }}
    </li>
  }
</ul>
```

Le composant réutilise la fonction `verifie` du critère plutôt que de relire l'erreur du validateur : **même source pour la validation et pour l'affichage**, donc aucun risque que la coche verte et le bouton désactivé racontent deux histoires différentes.

### Changer les règles à l'exécution : `setValidators`

Un même formulaire peut servir deux usages aux exigences différentes. Ici : créer un compte impose la règle de solidité, s'y connecter non.

```typescript
basculer(): void {
  this.mode.update(m => (m === 'connexion' ? 'inscription' : 'connexion'));

  const champ = this.formulaire.controls.motDePasse;

  if (this.mode() === 'inscription') {
    champ.setValidators([Validators.required, motDePasseSolide]);
  } else {
    champ.setValidators([Validators.required]);
  }

  // setValidators() change la RÈGLE mais ne rejoue pas la validation : sans
  // cet appel, le champ garderait le verdict calculé avec l'ancienne règle
  // jusqu'à la prochaine frappe.
  champ.updateValueAndValidity();
}
```

**Pourquoi retirer la règle à la connexion ?** Parce qu'elle ne s'applique qu'aux mots de passe qu'on **crée**. Les comptes existants ont pu l'être sous une politique plus souple ; exiger la nouvelle règle pour entrer enfermerait dehors leurs propriétaires. Et on ne peut pas non plus revalider les anciens en base : on ne stocke que des hachés (section 18), donc on est incapable de relire le mot de passe d'origine.

| Méthode | Rôle |
|---|---|
| `setValidators([...])` | Remplace la liste des validateurs du contrôle |
| `addValidators(...)` / `removeValidators(...)` | Ajoute ou retire sans toucher aux autres |
| `updateValueAndValidity()` | Rejoue la validation **maintenant** — indispensable après les précédentes |
| `control.hasError('cle')` | Ce contrôle porte-t-il cette erreur ? |
| `control.getError('cle')` | Récupère l'objet d'erreur, avec ses détails |

### Afficher ce qu'on tape

Masquer la saisie protège d'un regard par-dessus l'épaule ; la montrer évite de se tromper trois fois de suite sans comprendre pourquoi. Laisser le **choix** est la seule réponse correcte : l'utilisateur seul sait s'il est seul devant son écran.

```typescript
motDePasseVisible = signal(false);

basculerVisibiliteMotDePasse(): void {
  this.motDePasseVisible.update(v => !v);
}
```

```html
<!-- [type] est un binding de propriété comme un autre (section 6) : basculer
     entre 'password' et 'text' suffit, sans toucher au FormControl ni à sa
     valeur. -->
<input
  [type]="motDePasseVisible() ? 'text' : 'password'"
  formControlName="motDePasse" />

<!-- type="button" est OBLIGATOIRE : dans un <form>, un bouton sans type vaut
     type="submit" — celui-ci enverrait le formulaire au lieu de dévoiler le
     mot de passe. Piège classique, et silencieux.
     [attr.aria-pressed] annonce l'ÉTAT : c'est un interrupteur, pas une
     action ponctuelle. -->
<button
  type="button"
  (click)="basculerVisibiliteMotDePasse()"
  [attr.aria-pressed]="motDePasseVisible()"
  [attr.aria-label]="motDePasseVisible() ? 'Masquer le mot de passe' : 'Afficher le mot de passe'">
  @if (motDePasseVisible()) { Masquer } @else { Afficher }
</button>
```

`[attr.x]` plutôt que `[x]` : le premier écrit un **attribut HTML**, le second une **propriété de l'objet DOM**. Pour les attributs `aria-*`, qui n'ont pas de propriété correspondante, seule la forme `[attr.]` fonctionne.

> **Note de mise à jour.** Cette bascule écrite à la main a été **délibérément conservée** lors de l'adoption de PrimeNG (section 30), alors que la bibliothèque propose un `p-password` avec `[toggleMask]`. La raison est celle exposée juste au-dessus : PrimeNG rend son interrupteur sous forme de `<i>`, qui n'est ni atteignable au clavier ni annoncé comme un interrupteur. Le remplacer aurait été une régression. Adopter une bibliothèque ne veut pas dire accepter chacun de ses choix.

### Dans le projet

**Politique côté serveur** — [`carnet-contact-backend/src/main/java/.../security/PolitiqueMotDePasse.java`](../carnet-contact-backend/src/main/java/com/example/carnet_contact_backend/security/PolitiqueMotDePasse.java)

**Application** — [`carnet-contact-backend/src/main/java/.../controller/AuthController.java`](../carnet-contact-backend/src/main/java/com/example/carnet_contact_backend/controller/AuthController.java)

```java
// On rejette une requête MAL FORMÉE (400) avant de consulter l'état du
// serveur (409 si l'email est déjà pris) : l'ordre suit le sens des codes.
PolitiqueMotDePasse.Resultat verification =
        PolitiqueMotDePasse.verifier(demande.motDePasse());

if (!verification.valide()) {
    return ResponseEntity.badRequest().body(verification.message());
}
```

Ce que répond l'API, vérifié au `curl` :

| Mot de passe envoyé | Réponse |
|---|---|
| `court` | `400` — il lui manque : 10 caractères minimum, une majuscule, un chiffre, un caractère spécial |
| `tellementlong` | `400` — il lui manque : une majuscule, un chiffre, un caractère spécial |
| `MotDeP4sse` | `400` — il lui manque : un caractère spécial |
| `Motdepasse1!` | `400` — il lui manque : un mot de passe moins courant |
| `MotDeP4sse2026!` | `200` |

**Validateur et critères** — [`carnet-contact_frontend/src/app/validateurs/mot-de-passe.ts`](../carnet-contact_frontend/src/app/validateurs/mot-de-passe.ts)

**Formulaire** — [`carnet-contact_frontend/src/app/pages/connexion/connexion.ts`](../carnet-contact_frontend/src/app/pages/connexion/connexion.ts), [`connexion.html`](../carnet-contact_frontend/src/app/pages/connexion/connexion.html)

```typescript
critereSatisfait(critere: CritereMotDePasse): boolean {
  const valeur: string = this.formulaire.controls.motDePasse.value ?? '';
  return valeur !== '' && critere.verifie(valeur);
}
```

**Tests symétriques** — [`PolitiqueMotDePasseTest.java`](../carnet-contact-backend/src/test/java/com/example/carnet_contact_backend/security/PolitiqueMotDePasseTest.java) et [`mot-de-passe.spec.ts`](../carnet-contact_frontend/src/app/validateurs/mot-de-passe.spec.ts)

Côté Java, `@ParameterizedTest` rejoue la même méthode pour chaque valeur — cinq méthodes quasi identiques évitées, et le rapport nomme quand même chaque cas :

```java
@ParameterizedTest
@ValueSource(strings = {
        "Court1!",          // trop court
        "motdep4sse!",      // pas de majuscule
        "MOTDEP4SSE!",      // pas de minuscule
        "MotDePasse!",      // pas de chiffre
        "MotDeP4sse"        // pas de caractère spécial
})
void motDePasseIncomplet_estRefuse(String candidat) {
    assertThat(PolitiqueMotDePasse.verifier(candidat).valide()).isFalse();
}
```

Côté Angular, un test veille explicitement sur l'accord entre la liste à cocher et le validateur :

```typescript
it('la liste à cocher et le validateur donnent le même verdict', () => {
  for (const candidat of ['MotDeP4sse!', 'court', 'tellementlong', 'Motdepasse1!']) {
    const tousLesCriteresPassent =
      CRITERES_MOT_DE_PASSE.every(critere => critere.verifie(candidat));

    expect(valider(candidat) === null).toBe(tousLesCriteresPassent);
  }
});
```

---

## 28. Notifications du navigateur

### Le problème : prévenir quelqu'un qui ne regarde pas

Le sondage de la section 25 met la messagerie à jour toutes les cinq secondes — à condition d'avoir la page sous les yeux. Quelqu'un qui travaille dans un autre onglet ne verra rien.

Deux canaux existent, et **aucun ne suffit seul** :

| | Notification système | Bandeau dans l'application |
|---|---|---|
| Visible onglet en arrière-plan | Oui | Non |
| Demande une permission | Oui, refusable définitivement | Non |
| Disponible partout | Non (vieux navigateurs, HTTP simple) | Toujours |

D'où la règle retenue : **notification système quand l'onglet est caché et la permission accordée, bandeau interne dans tous les autres cas**. Chacun couvre l'angle mort de l'autre.

### L'API `Notification`

```typescript
/**
 * « indisponible » n'est pas une valeur de l'API : c'est la nôtre, pour le cas
 * où `Notification` n'existe pas du tout — rendu côté serveur, navigateur
 * ancien, page en HTTP simple. Sans cette quatrième valeur, il faudrait tester
 * `typeof Notification` à chaque usage.
 */
private permissionSignal = signal<NotificationPermission | 'indisponible'>('indisponible');

constructor() {
  if (this.navigateur && 'Notification' in window) {
    this.permissionSignal.set(Notification.permission);
  }
}
```

| Valeur de `Notification.permission` | Sens |
|---|---|
| `'default'` | Jamais demandé — on peut demander |
| `'granted'` | Accordé |
| `'denied'` | Refusé — **on ne peut plus redemander** |

### La permission se demande depuis un clic

C'est la contrainte la plus importante, et la plus facile à oublier :

```typescript
demanderPermission(): void {
  if (!this.navigateur || !('Notification' in window)) {
    return;
  }
  Notification.requestPermission().then(reponse => this.permissionSignal.set(reponse));
}
```

Les navigateurs **ignorent** (ou refusent d'office) une demande qui ne fait pas suite à une action de l'utilisateur — précisément pour empêcher les sites de réclamer l'autorisation dès l'ouverture de la page. Il faut donc un bouton, pas un appel au démarrage.

Et le refus est **définitif du point de vue du site** : un `'denied'` ne peut plus être changé par le code, seulement par l'utilisateur dans les réglages du navigateur. D'où l'importance de ne demander qu'à un moment où la demande a du sens, et de prévoir un repli qui fonctionne sans.

### Choisir le canal

```typescript
notifier(titre: string, corps: string): void {
  if (!this.navigateur) return;

  // document.hidden : vrai quand l'onglet n'est pas au premier plan (autre
  // onglet actif, fenêtre réduite). C'est ce qui évite de déclencher une
  // notification système alors que l'utilisateur a la page sous les yeux —
  // elle serait redondante et agaçante.
  if (this.permissionSignal() === 'granted' && document.hidden) {
    this.notificationSysteme(titre, corps);
  } else {
    this.ajouterBandeau(titre, corps);
  }
}

private notificationSysteme(titre: string, corps: string): void {
  const notification = new Notification(titre, {
    body: corps,
    // tag : les notifications partageant un tag se REMPLACENT au lieu de
    // s'empiler. Sans lui, dix messages reçus pendant une absence
    // produiraient dix bulles superposées.
    tag: 'mon-app-message',
    icon: '/favicon.ico'
  });

  notification.onclick = () => {
    // Ramener la fenêtre au premier plan : cliquer sur une notification sans
    // que rien ne s'affiche serait déroutant.
    window.focus();
    this.router.navigate(['/messages']);
    notification.close();
  };
}
```

### Savoir ce qui est vraiment nouveau

Le piège de fond n'est pas l'affichage, c'est la **détection**. Le sondage renvoie à chaque tour la liste **complète** des non-lus. Notifier bêtement à chaque réponse produirait une alerte toutes les quinze secondes pour un message qu'on n'a pas encore ouvert.

```typescript
/** Les identifiants déjà vus, pour ne notifier qu'une fois par message. */
private dejaVus = new Set<number>();

/**
 * Le premier tour établit l'état de départ SANS notifier : sinon, ouvrir
 * l'application annoncerait d'un coup tous les messages en attente — or ils
 * ne sont pas « nouveaux », ils étaient déjà là.
 */
private premierTour = true;

private signalerLesNouveaux(messages: Message[]): void {
  if (!this.premierTour) {
    for (const message of messages) {
      if (!this.dejaVus.has(message.id)) {
        this.notifications.notifier(
          `Message de ${message.expediteur.nomAffichage}`, message.contenu);
      }
    }
  }

  // On REMPLACE l'ensemble plutôt que d'y ajouter : un message lu disparaît de
  // la réponse, et doit donc sortir de la mémoire. Sinon elle grossirait sans
  // fin au fil de la session.
  this.dejaVus = new Set(messages.map(m => m.id));
  this.premierTour = false;
}
```

Et la remise à zéro à l'arrêt du suivi, sinon se reconnecter annoncerait de nouveau tout l'arriéré :

```typescript
arreterSuiviNonLus(): void {
  /* … */
  this.dejaVus.clear();
  this.premierTour = true;
}
```

### Le repli : des bandeaux dans l'application

```typescript
private ajouterBandeau(titre: string, corps: string): void {
  const id = this.prochainId++;
  this.bandeauxSignal.update(liste => [...liste, { id, titre, corps }]);

  // Disparition automatique : un bandeau informe, il n'a pas à rester à
  // l'écran jusqu'à ce qu'on le ferme.
  setTimeout(() => this.fermerBandeau(id), DUREE_BANDEAU_MS);
}
```

```html
<!-- role="status" + aria-live="polite" : un lecteur d'écran annonce
     l'apparition du bandeau sans interrompre ce qu'il est en train de lire. -->
<div class="pile-bandeaux" role="status" aria-live="polite">
  @for (bandeau of notifications.bandeaux(); track bandeau.id) { … }
</div>
```

```css
/* position: fixed — les bandeaux sont ancrés à la FENÊTRE, pas au document :
   ils restent visibles même si la page est défilée. */
.pile-bandeaux {
  position: fixed;
  right: 1rem;
  bottom: 1rem;
  z-index: 50;
  width: min(22rem, calc(100vw - 2rem));
}
```

Ils vivent dans la **coquille** (`app.html`) et non dans la page Messages : une notification doit pouvoir apparaître quelle que soit la page affichée — même raisonnement que les bannières d'erreur et de chargement de la section 17.

### Dire à l'utilisateur où il en est

Les quatre états de la permission n'appellent ni le même texte ni la même action. `@switch` (section 20) les sépare proprement :

```html
@switch (permissionNotifications()) {
  @case ('granted')      { <p>Les notifications sont activées.</p> }
  @case ('denied')       { <p>Vous les avez refusées. Un site ne peut pas
                              redemander l'autorisation lui-même : il faut la
                              rétablir dans les réglages du navigateur.</p> }
  @case ('indisponible') { <p>Ce navigateur ne les propose pas.</p> }
  @default               { <button type="button" (click)="activer()">Activer</button> }
}
```

Le cas `'denied'` mérite une vraie explication plutôt qu'un bouton qui ne ferait rien : l'utilisateur doit comprendre que la balle est dans son camp, et que le repli en bandeau continue de fonctionner entre-temps.

### Dans le projet

**Service** — [`carnet-contact_frontend/src/app/services/notification.ts`](../carnet-contact_frontend/src/app/services/notification.ts)

**Détection des nouveaux messages** — [`carnet-contact_frontend/src/app/services/message.ts`](../carnet-contact_frontend/src/app/services/message.ts)

**Affichage des bandeaux** — [`carnet-contact_frontend/src/app/app.html`](../carnet-contact_frontend/src/app/app.html), [`app.css`](../carnet-contact_frontend/src/app/app.css)

**Activation** — [`carnet-contact_frontend/src/app/pages/profil/profil.ts`](../carnet-contact_frontend/src/app/pages/profil/profil.ts), [`profil.html`](../carnet-contact_frontend/src/app/pages/profil/profil.html)

**Tests** — [`services/notification.spec.ts`](../carnet-contact_frontend/src/app/services/notification.spec.ts), [`services/message.spec.ts`](../carnet-contact_frontend/src/app/services/message.spec.ts)

jsdom, l'environnement de test, n'implémente pas l'API `Notification` : c'est exactement le cas « navigateur sans notifications système », qui se teste donc sans rien simuler. Pour le sondage, les **minuteurs simulés** de vitest permettent d'avancer le temps à la demande :

```typescript
// La variante …Async est indispensable : elle vide aussi la file des
// micro-tâches, donc la requête HTTP a le temps de partir.
const avancerDe = (ms: number) => vi.advanceTimersByTimeAsync(ms);

it('annonce uniquement les messages arrivés depuis le tour précédent', async () => {
  const espion = vi.spyOn(notifications, 'notifier');

  service.demarrerSuiviNonLus();
  await avancerDe(0);
  backend.expectOne('/api/messages/non-lus').flush([message(1, 'Bonjour')]);

  await avancerDe(INTERVALLE_NON_LUS_MS);
  backend.expectOne('/api/messages/non-lus')
    .flush([message(1, 'Bonjour'), message(2, 'Toujours là ?')]);

  expect(espion).toHaveBeenCalledTimes(1);
  expect(espion).toHaveBeenCalledWith('Message de Bob', 'Toujours là ?');
});
```

| Outil de test | Rôle |
|---|---|
| `vi.useFakeTimers()` / `vi.useRealTimers()` | Remplace puis restaure les minuteurs du navigateur |
| `vi.advanceTimersByTimeAsync(ms)` | Avance le temps et vide la file des micro-tâches |
| `vi.spyOn(objet, 'methode')` | Observe les appels d'une méthode sans changer son comportement |

---

## 29. Rôles et autorisations

Jusqu'ici, l'application distinguait deux états : connecté ou non. C'est de l'**authentification** — répondre à « qui es-tu ? ». Dès qu'on ajoute un panneau d'administration, une seconde question apparaît : « as-tu le droit de faire ça ? ». C'est l'**autorisation**, et les deux ne se confondent pas : un compte parfaitement authentifié peut n'avoir le droit de rien.

La confusion entre les deux est à l'origine d'une bonne part des failles réelles. Un développeur pressé cache le bouton « Supprimer le compte » aux non-administrateurs et considère le travail fait. Mais cacher un bouton ne ferme pas la route qu'il appelait : n'importe qui sachant écrire une requête HTTP y accède encore. **L'autorisation se décide sur le serveur** ; ce que fait le client n'est que du confort.

### Représenter un rôle : une énumération, pas une chaîne

```java
// Un enum plutot qu'un String : le compilateur refuse alors toute valeur
// inventee. Avec un String, une faute de frappe ("ADMN") passerait la
// compilation et ne se verrait qu'en production.
public enum Role {
    UTILISATEUR, ADMIN;

    // Spring Security attend ses autorites prefixees par "ROLE_". On centralise
    // cette convention ICI plutot que de l'eparpiller : elle appartient au
    // framework, pas a notre metier.
    public String autorite() {
        return "ROLE_" + name();
    }
}
```

```java
@Entity
public class Utilisateur {
    // @Enumerated(STRING) enregistre "ADMIN" en base. Le defaut (ORDINAL)
    // enregistrerait 1 — l'INDICE de la valeur dans l'enum. Le jour ou l'on
    // insere une valeur au milieu de l'enum, tous les indices se decalent et
    // les donnees existantes changent silencieusement de sens.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.UTILISATEUR;
}
```

**Dans le projet** — [`model/Role.java`](../carnet-contact-backend/src/main/java/com/example/carnet_contact_backend/model/Role.java) et [`model/Utilisateur.java`](../carnet-contact-backend/src/main/java/com/example/carnet_contact_backend/model/Utilisateur.java)

### Faire voyager le rôle dans le jeton

Le rôle doit accompagner chaque requête. Deux possibilités : le relire en base à chaque appel, ou le placer dans le jeton. On a choisi le jeton — c'est la logique même du JWT (section 18) : il porte tout ce qu'il faut pour décider, et le serveur ne garde aucune session.

```java
public String genererJeton(String email, Role role) {
    return Jwts.builder()
            .subject(email)
            // Une « claim » : une information supplementaire placee dans le
            // jeton. Elle est SIGNEE avec le reste — donc lisible par tous,
            // mais impossible a modifier sans invalider la signature.
            .claim("role", role.name())
            .signWith(cle)
            .compact();
}

public Role roleDuJeton(String jeton) {
    String brut = charge(jeton).get("role", String.class);
    try {
        return Role.valueOf(brut);
    } catch (IllegalArgumentException | NullPointerException e) {
        // Un jeton emis AVANT l'ajout des roles n'a pas cette claim. Plutot
        // que d'echouer, on retombe sur le role le moins privilegie : en
        // securite, le defaut doit toujours etre le plus restrictif.
        return Role.UTILISATEUR;
    }
}
```

Le filtre pose ensuite ce rôle comme **autorité** sur l'authentification :

```java
var autorites = List.of(new SimpleGrantedAuthority(jwtService.roleDuJeton(jeton).autorite()));
var authentification = new UsernamePasswordAuthenticationToken(email, null, autorites);
SecurityContextHolder.getContext().setAuthentication(authentification);
```

Et la configuration réserve les routes :

```java
.authorizeHttpRequests(a -> a
    // hasRole("ADMIN") cherche l'autorite "ROLE_ADMIN" : le prefixe est
    // ajoute implicitement. C'est la source d'erreur classique — ecrire
    // hasRole("ROLE_ADMIN") fait chercher "ROLE_ROLE_ADMIN".
    .requestMatchers("/api/admin/**").hasRole("ADMIN")
    .anyRequest().authenticated())
```

| Méthode | Rôle |
|---|---|
| `hasRole("ADMIN")` | Exige l'autorité `ROLE_ADMIN` (préfixe `ROLE_` ajouté implicitement) |
| `hasAuthority("ROLE_ADMIN")` | Exige l'autorité exacte, sans préfixe ajouté |
| `hasAnyRole("ADMIN", "MODERATEUR")` | Accepte l'un ou l'autre |
| `authenticated()` | Exige seulement d'être connecté |
| `permitAll()` | Ouvre la route à tous |

Un refus d'autorisation se distingue d'un refus d'authentification par son code HTTP :

| Code | Signification | Cas typique |
|---|---|---|
| `401 Unauthorized` | « Je ne sais pas qui tu es » | Jeton absent, expiré ou invalide |
| `403 Forbidden` | « Je sais qui tu es, et c'est non » | Utilisateur ordinaire sur `/api/admin/**` |

**Dans le projet** — [`security/JwtService.java`](../carnet-contact-backend/src/main/java/com/example/carnet_contact_backend/security/JwtService.java), [`security/JwtAuthFilter.java`](../carnet-contact-backend/src/main/java/com/example/carnet_contact_backend/security/JwtAuthFilter.java) et [`security/SecurityConfig.java`](../carnet-contact-backend/src/main/java/com/example/carnet_contact_backend/security/SecurityConfig.java)

### Le prix du sans-état : la fenêtre du jeton périmé

Mettre le rôle dans le jeton a une conséquence qu'il faut assumer : **un rôle retiré ne prend effet qu'à l'expiration du jeton** (quinze minutes ici). Pendant ce délai, un compte rétrogradé continue de présenter un jeton qui dit « ADMIN », et le serveur le croit — c'est ce jeton qui fait foi.

Ce n'est pas un défaut d'implémentation, c'est le compromis du sans-état : relire le rôle en base à chaque requête supprimerait la fenêtre, mais ajouterait une requête SQL à *tous* les appels. On atténue plutôt les conséquences :

- désactiver un compte **révoque ses jetons de rafraîchissement** : il ne pourra plus en obtenir un nouveau, la fenêtre se referme donc d'elle-même ;
- les actions destructrices vérifient l'état **réel en base**, pas ce que dit le jeton.

C'est précisément ce que fait le garde-fou « dernier administrateur » :

```java
// Vrai s'il n'existe AUCUN autre administrateur actif que la cible.
private boolean dernierAdminActif(Utilisateur cible) {
    return utilisateurRepository.countByRoleAndActifTrueAndIdNot(Role.ADMIN, cible.getId()) == 0;
}
```

Ce contrôle paraît inutile — l'appelant étant lui-même administrateur actif, il « compte » toujours pour un, et le résultat ne devrait jamais valoir zéro. Sauf dans un cas, et c'est exactement celui-là : **un appelant rétrogradé qui utilise encore son ancien jeton**. En base il n'est plus administrateur, il ne compte donc plus ; si sa cible est le dernier administrateur restant, l'opération laisserait l'application sans personne pour l'administrer — un état dont on ne pourrait plus sortir par l'interface.

Ce scénario a été vérifié en conditions réelles : Carla (admin) rétrograde Alice, puis Alice, avec son jeton encore valide, tente de supprimer Carla. Le serveur répond `400` et non `204`.

### Les trois règles d'un panneau d'administration

```java
// 1. On ne s'applique jamais une action a soi-meme : se desactiver, se
//    retrograder ou se supprimer ferait perdre l'acces dans la seconde.
if (cible.getId().equals(moi.getId())) {
    throw new ResponseStatusException(BAD_REQUEST, "Vous ne pouvez pas ...");
}

// 2. On ne retire jamais le dernier administrateur actif.
if (cible.getRole() == Role.ADMIN && dernierAdminActif(cible)) { ... }

// 3. Supprimer suit l'ordre INVERSE des dependances : les feuilles d'abord,
//    la racine en dernier. Supprimer l'utilisateur en premier violerait les
//    cles etrangeres de tout ce qui le pointe encore.
reactionRepository.supprimerCellesDe(id);            // ses reactions
reactionRepository.supprimerCellesDesMessagesDe(id); // celles recues
messageRepository.supprimerCeuxDe(id);
contactRepository.supprimerCeuxDe(id);
jetonRepository.supprimerTousPour(id);
utilisateurRepository.delete(cible);                 // la racine, en dernier
```

Toute la méthode porte `@Transactional` : ces six suppressions forment **une seule opération**. Sans elle, une panne au milieu laisserait un compte sans messages mais toujours présent — un état incohérent que rien ne viendrait réparer.

**Dans le projet** — [`controller/AdminController.java`](../carnet-contact-backend/src/main/java/com/example/carnet_contact_backend/controller/AdminController.java)

### Le premier compte devient administrateur

Un panneau d'administration pose un problème d'amorçage : il faut un administrateur pour en nommer un, mais il n'y en a aucun au départ. La solution la plus simple est de décider que **le premier inscrit l'est**.

```java
// count() vaut 0 uniquement pour la toute premiere inscription.
utilisateur.setRole(utilisateurRepository.count() == 0 ? Role.ADMIN : Role.UTILISATEUR);
```

C'est acceptable pour un projet d'apprentissage à base H2 **en mémoire** : la base repart vide à chaque démarrage, donc le premier compte recréé est de nouveau administrateur. En production, on préférerait un compte créé par un script de migration, hors du parcours d'inscription — sinon le premier visiteur venu devient administrateur.

**Dans le projet** — [`controller/AuthController.java`](../carnet-contact-backend/src/main/java/com/example/carnet_contact_backend/controller/AuthController.java)

### Refuser la connexion d'un compte désactivé

```java
if (!utilisateur.isActif()) {
    // 403 et non 401 : le mot de passe etait BON. Repondre « identifiants
    // incorrects » enverrait la personne le retaper indefiniment. Le message
    // doit dire ce qui se passe reellement, et vers qui se tourner.
    throw new ResponseStatusException(
            HttpStatus.FORBIDDEN, "Ce compte a été désactivé. Contactez un administrateur.");
}
```

Il faut fermer la porte des **deux** côtés. Refuser la connexion ne suffit pas : quelqu'un déjà connecté au moment de la désactivation garderait un jeton de rafraîchissement valide et pourrait se renouveler indéfiniment. Le renouvellement vérifie donc aussi l'état du compte, et la désactivation révoque les jetons existants.

### Côté Angular : une garde de rôle

```typescript
export const adminGuard: CanActivateFn = () => {
  const session = inject(SessionService);
  const router = inject(Router);

  if (session.estAdmin()) {
    return true;
  }

  // Vers l'accueil, PAS vers la connexion : la personne est bien identifiee,
  // il lui manque un droit. L'envoyer vers un formulaire de connexion
  // laisserait croire a un probleme de mot de passe.
  router.navigate(['/']);
  return false;
};
```

```typescript
// Deux gardes qui s'enchainent, dans cet ordre : etre connecte, PUIS etre
// administrateur. Une seule qui dit non suffit a bloquer la route.
{ path: 'admin', canActivate: [authGuard, adminGuard], component: Admin }
```

Cette garde lit un champ du compte mémorisé dans le navigateur — que n'importe qui pourrait modifier à la main pour afficher la page. Il n'en tirerait rien : le serveur refuse toutes les routes `/api/admin/**` à qui n'a pas le rôle dans son jeton **signé**, et une signature ne se falsifie pas (section 18). La garde fait donc ce qu'elle sait faire : éviter d'afficher un écran vide rempli d'erreurs 403.

**Dans le projet** — [`admin-guard.ts`](../carnet-contact_frontend/src/app/admin-guard.ts), [`services/session.ts`](../carnet-contact_frontend/src/app/services/session.ts) (le `computed` `estAdmin`) et [`app.routes.ts`](../carnet-contact_frontend/src/app/app.routes.ts)

### Mettre à jour une ligne sans recharger la liste

Après une action d'administration, la tentation est de recharger tout le tableau. C'est simple, mais le tableau clignote, le tri en cours se perd, et on paie une requête complète pour une ligne modifiée.

```typescript
changerActif(id: number, actif: boolean): void {
  this.http.put<LigneCompte>(`${this.apiUrl}/${id}/actif`, { actif })
    .pipe(catchError(() => EMPTY))
    // Le serveur renvoie la ligne A JOUR : lui seul connait les consequences
    // reelles de l'action. On la substitue sur place.
    .subscribe(ligne => this.remplacer(ligne));
}

private remplacer(ligne: LigneCompte): void {
  // .map() construit un NOUVEAU tableau : le signal detecte le changement.
  // Modifier l'element en place ne declencherait aucun reaffichage, la
  // reference du tableau n'ayant pas bouge.
  this.comptesSignal.update(liste => liste.map(c => (c.id === ligne.id ? ligne : c)));
}
```

Noter aussi le `catchError(() => EMPTY)` : quand le serveur refuse (« c'est le dernier administrateur »), le flux se termine **sans émettre**, donc `subscribe` ne s'exécute pas et l'affichage reste tel quel. La bannière d'erreur, elle, est alimentée par l'intercepteur (section 17).

**Dans le projet** — [`services/admin.ts`](../carnet-contact_frontend/src/app/services/admin.ts) et [`pages/admin/admin.ts`](../carnet-contact_frontend/src/app/pages/admin/admin.ts)

### Filtrer côté client ou côté serveur ?

La page d'administration filtre les comptes **en mémoire**, alors que la liste de contacts interroge le serveur (section 22). Ce n'est pas une incohérence, c'est une question d'échelle :

| | Contacts | Comptes |
|---|---|---|
| Volume attendu | Des milliers | Quelques dizaines |
| Tout charger d'un coup | Impensable | Sans conséquence |
| Filtrage | Serveur, paginé | `computed()` sur la liste déjà là |
| Anti-rebond | 300 ms (épargner des requêtes) | 200 ms (épargner des recalculs) |

La même question n'a pas la même bonne réponse selon la taille des données. Chercher « la » solution universelle est ici une erreur de méthode.

---

## 30. PrimeNG, couches CSS et chargement différé

Jusqu'à la section 21, chaque élément visuel du carnet était écrit à la main : boutons, champs, cartes. C'est excellent pour apprendre — on comprend ce qu'on affiche. Mais certains composants demandent beaucoup de travail pour un résultat que tout le monde attend identique : un tableau triable, une boîte de confirmation, un paginateur. Aucun n'a le moindre rapport avec le métier « carnet de contacts », et chacun cache des dizaines de détails (navigation au clavier, annonces aux lecteurs d'écran, comportement sur petit écran).

C'est là qu'une **bibliothèque de composants** paie. PrimeNG en fournit une centaine, déjà accessibles et déjà thématisables.

La règle pour décider : **prendre la bibliothèque pour ce qui est générique et coûteux, garder son propre code pour ce qui est spécifique ou déjà résolu.** Dans ce projet, le tableau des comptes et le paginateur sont passés à PrimeNG ; la bascule « Afficher / masquer le mot de passe » ne l'est pas, parce que `p-password` rend son interrupteur sous forme de `<i>` — ni focalisable au clavier, ni annoncé comme un interrupteur. Adopter une bibliothèque n'oblige pas à accepter chacun de ses choix.

### Installation et thème

```bash
# Les versions majeures de PrimeNG suivent celles d'Angular : primeng@21 pour
# Angular 21. Installer primeng@22 sur Angular 21 echoue a l'installation.
npm install primeng@21 @primeuix/themes @angular/cdk primeicons
```

```typescript
// definePreset part d'un theme fourni (Aura) et n'en remplace que ce qu'on
// veut. On n'ecrit donc que sa propre couleur principale, le reste (contrastes,
// etats survole/desactive, variante sombre) etant deja calcule.
const themeProjet = definePreset(Aura, {
  semantic: {
    primary: {
      50: '#eff5ff', 500: '#2b7bff', 600: '#0b5fff', 700: '#0740b5', 950: '#091f4a'
    }
  }
});

providePrimeNG({
  theme: {
    preset: themeProjet,
    options: {
      // Le selecteur qui declenche la variante sombre (section 31).
      darkModeSelector: '[data-theme="sombre"]',
      cssLayer: { name: 'primeng', order: 'theme, base, primeng' }
    }
  }
})
```

PrimeNG génère ses couleurs sous forme de variables CSS (`--p-primary-600`, …). On peut donc brancher son propre système dessus, et les deux restent d'accord pour toujours :

```css
:root {
  --bleu: var(--p-primary-600);   /* une seule palette pour les deux mondes */
}
```

**Dans le projet** — [`app.config.ts`](../carnet-contact_frontend/src/app/app.config.ts) et [`styles.css`](../carnet-contact_frontend/src/styles.css)

### Les couches de cascade (`@layer`)

Voici le piège qui coûte le plus de temps quand on introduit une bibliothèque dans un projet déjà stylé.

PrimeNG habille ses composants avec des **classes** (`.p-button`). Nos styles globaux visent des **balises** (`button`). Intuitivement, la classe devrait gagner : elle est plus spécifique. **C'est faux dès qu'il y a des couches.**

Une couche CSS (`@layer`) est un groupe de règles auquel on assigne une priorité. Et la règle est brutale :

> Une déclaration **hors de toute couche** l'emporte sur **toute** déclaration placée dans une couche, quelle que soit sa spécificité.

PrimeNG range délibérément tout son thème dans la couche `primeng` — précisément pour qu'on puisse le retoucher sans surenchérir en sélecteurs. Conséquence non désirée : nos règles `button { background: var(--bleu) }`, sans couche, écrasaient l'habillage de **tous** les `p-button`. Ils sortaient tous en bleu uni, leur `severity` (danger, secondary…) ignorée.

La bonne réponse n'est pas de monter en spécificité — une course sans fin — mais de **déclarer un ordre** :

```css
/* L'ordre de cette ligne fait loi : la derniere nommee gagne. */
@layer theme, base, primeng;

@layer base {
  /* Nos styles de balises deviennent ce qu'ils auraient toujours du etre :
     des VALEURS PAR DEFAUT, qu'un composant habille peut remplacer. */
  button { background: var(--bleu); color: #fff; }
  input  { border: 1.5px solid var(--bordure); }
}

/* Hors couche : nos classes a nous. Elles dominent tout, y compris PrimeNG —
   c'est voulu, ce sont des decisions, pas des defauts. */
.carte { background: var(--carte); }
```

Résultat : un `<button>` ordinaire garde notre habillage, un `p-button` garde le sien, et `.carte` continue de tout dominer.

| Priorité | Origine | Exemple |
|---|---|---|
| 1 (la plus forte) | Hors couche | `.carte`, `.muet` |
| 2 | `@layer primeng` | `.p-button`, `.p-datatable` |
| 3 | `@layer base` | `button`, `input`, `label` |

> À retenir : la spécificité ne départage que des règles **de la même couche**. Entre couches, seul l'ordre compte.

**Dans le projet** — [`styles.css`](../carnet-contact_frontend/src/styles.css)

### Retoucher l'intérieur d'un composant : `::ng-deep`

Angular **encapsule** le CSS d'un composant : à la compilation, il ajoute un attribut unique (`_ngcontent-abc`) sur chaque élément du gabarit et le colle à chaque sélecteur. `.cellule { … }` devient en réalité `.cellule[_ngcontent-abc] { … }`. C'est ce qui évite qu'une classe banale comme `.actions`, définie dans deux pages, se marche dessus.

Mais les `<table>`, `<tr>` et `<th>` d'un `p-table` ne sont pas dans notre gabarit : c'est la bibliothèque qui les fabrique, avec son propre attribut. Nos sélecteurs ne les atteignent jamais.

```css
/*
  ::ng-deep leve l'encapsulation pour ce qui SUIT, ce qui redonne acces a
  l'interieur du composant enfant.

  Le prix : la regle redevient globale. On la prefixe donc systematiquement par
  :host, qui la limite a l'interieur de CE composant — sans quoi elle
  s'appliquerait a toutes les tables de l'application.
*/
:host ::ng-deep .mon-tableau .p-datatable-thead > tr > th {
  text-transform: uppercase;
}
```

> `::ng-deep` est marqué « déprécié » depuis longtemps, sans remplaçant. Il reste la méthode employée en pratique ; la discipline `:host ::ng-deep` suffit à le rendre sûr.

**Dans le projet** — [`pages/admin/admin.css`](../carnet-contact_frontend/src/app/pages/admin/admin.css) et [`components/contact-list/contact-list.css`](../carnet-contact_frontend/src/app/components/contact-list/contact-list.css)

### Un tableau de données : `p-table`

```html
<p-table [value]="lignes()" [loading]="chargement()" dataKey="id"
         sortField="date" [sortOrder]="1">

  <!-- pTemplate decrit CHAQUE partie du tableau : on garde la main complete
       sur le rendu de chaque cellule, la bibliothèque ne fournissant que la
       structure et les comportements. -->
  <ng-template pTemplate="header">
    <tr>
      <th pSortableColumn="nom">Nom <p-sortIcon field="nom" /></th>
    </tr>
  </ng-template>

  <!-- let-ligne : la variable de la ligne courante, comme le « of » d'un @for -->
  <ng-template pTemplate="body" let-ligne>
    <tr><td>{{ ligne.nom }}</td></tr>
  </ng-template>

  <ng-template pTemplate="emptymessage">
    <tr><td colspan="2">Aucun résultat.</td></tr>
  </ng-template>
</p-table>
```

| Composant | Ce qu'il apporte |
|---|---|
| `p-table` | Tri par colonne, état de chargement, message de liste vide |
| `p-paginator` | Numéros de page, saut au début/à la fin, clavier |
| `p-confirmDialog` + `ConfirmationService` | Une boîte de confirmation pour toute une page |
| `p-tag` | Pastille colorée par `severity` (`success`, `danger`, `warn`…) |
| `p-avatar` | Image ronde, avec repli sur une initiale |
| `p-iconfield` / `p-inputicon` | Icône **dans** un champ, sans positionnement manuel |
| `pTooltip` | Infobulle (nécessite `TooltipModule` dans les `imports`) |

Le `ConfirmationService` se fournit **au niveau du composant**, pas dans `app.config` :

```typescript
@Component({
  // Declare ici, il vit le temps de la page au lieu de toute la session.
  providers: [ConfirmationService]
})
```

Et une confirmation n'est utile que si elle **nomme** ce qu'elle va détruire. « Êtes-vous sûr ? » se répond oui par réflexe ; « Le compte Bob sera supprimé, ainsi que ses 3 contacts et 7 messages » donne de quoi vérifier qu'on a cliqué sur la bonne ligne.

**Dans le projet** — [`pages/admin/admin.html`](../carnet-contact_frontend/src/app/pages/admin/admin.html) et [`pages/admin/admin.ts`](../carnet-contact_frontend/src/app/pages/admin/admin.ts)

### Directive ou composant : `pButton` sur un `<a>`

```html
<!-- Ceci NAVIGUE : c'est un lien, pas un bouton. Il doit pouvoir s'ouvrir dans
     un nouvel onglet, se copier, s'indexer. La DIRECTIVE pButton ne donne que
     l'apparence ; l'element reste ce qu'il doit etre. -->
<a pButton icon="pi pi-pencil" label="Modifier" [routerLink]="['/contact', id]"></a>

<!-- Ceci AGIT : le composant <p-button> genere un vrai <button>. -->
<p-button icon="pi pi-trash" severity="danger" (onClick)="supprimer()" />
```

Le choix n'est pas cosmétique : il décide de ce que le navigateur et les technologies d'assistance comprennent de l'élément (section 9).

### Convertir entre deux vocabulaires

```typescript
// Le paginateur raisonne en INDEX D'ELEMENT (`first` = rang du premier
// element affiche), notre service en NUMERO DE PAGE. On traduit A LA
// FRONTIERE : chaque monde garde son vocabulaire, plutot que de contaminer le
// service avec les unites d'un composant d'affichage.
changerPage(evenement: PaginatorState): void {
  const premier = evenement.first ?? 0;
  this.contactService.allerPage(Math.floor(premier / this.taillePage));
}
```

```html
<p-paginator [first]="page() * taillePage" [rows]="taillePage"
             [totalRecords]="total()" (onPageChange)="changerPage($event)" />
```

**Dans le projet** — [`components/contact-list/contact-list.ts`](../carnet-contact_frontend/src/app/components/contact-list/contact-list.ts)

### Le chargement différé (`loadComponent`)

Une bibliothèque de composants a un poids. Ajouter `p-table` à l'application a fait bondir le paquet JavaScript initial de plusieurs centaines de kilo-octets — téléchargés par **tout le monde**, alors que le panneau d'administration ne concerne qu'une poignée de comptes.

C'est exactement le cas d'usage du **chargement différé** :

```typescript
// Import statique en haut du fichier : le composant part dans le paquet
// principal. Le bon choix pour une page que tout le monde visite.
{ path: '', component: Accueil }

// loadComponent : import() retourne une PROMESSE. Angular ne declenche le
// telechargement de ce morceau de code qu'au moment ou quelqu'un navigue
// vers /admin. Les autres ne le paient jamais.
{
  path: 'admin',
  canActivate: [authGuard, adminGuard],
  loadComponent: () => import('./pages/admin/admin').then(m => m.Admin)
}
```

La règle : **différer ce qui est lourd ET rare**. Différer une page visitée par tous n'ajouterait qu'une attente au moment du clic.

Le résultat se lit directement dans la sortie de `ng build` :

```
Initial chunk files   | Names  |  Raw size
main.js               | main   | 274.76 kB     <- tout le monde telecharge ca

Lazy chunk files      | Names  |  Raw size
chunk-EOKOOYU4.js     | admin  | 619.90 kB     <- seulement les administrateurs
```

### Les budgets de paquet

Angular surveille la taille du paquet et prévient quand elle dépasse un seuil, défini dans `angular.json` :

```json
"budgets": [
  { "type": "initial", "maximumWarning": "1MB", "maximumError": "1.5MB" },
  { "type": "anyComponentStyle", "maximumWarning": "8kB", "maximumError": "12kB" }
]
```

Ces valeurs n'ont rien d'absolu : ce sont des **alarmes que l'on règle soi-même**. Les relever parce qu'une bibliothèque a été ajoutée volontairement est légitime ; les relever à chaque avertissement sans se demander pourquoi le paquet grossit fait perdre tout l'intérêt du garde-fou.

**Dans le projet** — [`app.routes.ts`](../carnet-contact_frontend/src/app/app.routes.ts) et [`angular.json`](../carnet-contact_frontend/angular.json)

---

## 31. Mode sombre

Un mode sombre bien fait n'est **pas un second site**. C'est exactement le même CSS, avec d'autres *valeurs* derrière les mêmes noms. Tout le travail a été fait à la section 21, en déclarant chaque couleur une seule fois sous forme de variable : ajouter le thème sombre n'a demandé de modifier aucune règle existante.

Le prix à payer est la contrepartie exacte de cette facilité : **plus aucune couleur ne doit être écrite en dur**, sinon elle reste claire dans le thème sombre. Une seule `background: #fff` oubliée quelque part, et une carte reste blanche au milieu d'une page noire.

### Deux jeux de valeurs, un seul jeu de noms

```css
:root {
  --fond: #f4f6fb;
  --carte: #ffffff;
  --texte: #131c2b;
  --bordure: #dce2ed;

  --ombre: 0 1px 2px rgb(19 28 43 / 0.06);

  /* color-scheme previent le NAVIGATEUR du theme en cours. Il en tient compte
     pour ce qu'il dessine lui-meme et que le CSS n'atteint pas : barres de
     defilement, selecteurs de date, menus deroulants natifs, champs de
     formulaire par defaut. Sans cette ligne, une page sombre garde des
     ascenseurs blancs. */
  color-scheme: light;
}

[data-theme="sombre"] {
  /* Pas de noir pur : #000 derriere du texte blanc produit un contraste
     eblouissant et fatigant. Les interfaces sombres soignees s'arretent a un
     gris tres fonce. */
  --fond: #0e1420;
  --carte: #172033;
  --texte: #e6ebf5;
  --bordure: #2a3650;

  /* Une ombre noire ne se voit pas sur fond sombre. La profondeur s'y exprime
     par la LUMIERE — un fond plus clair que son entourage — plutot que par
     l'ombre portee. On garde donc des ombres tres discretes. */
  --ombre: 0 1px 2px rgb(0 0 0 / 0.3);

  color-scheme: dark;
}
```

Un point qui surprend au début : **une couleur n'a pas de valeur absolue, elle se lit toujours CONTRE un fond**. Le bleu qui ressort bien sur blanc devient trop saturé sur noir. On monte alors d'un cran dans les nuances claires :

```css
:root            { --bleu: var(--p-primary-600); }
[data-theme="sombre"] { --bleu: var(--p-primary-400); }
```

### Pourquoi un attribut plutôt qu'une classe

```css
/* [data-theme="sombre"] DIT ce qu'il est : un etat, pas un style.
   Une classe .sombre se confondrait avec du style ordinaire, et rien
   n'empecherait de l'ajouter par erreur a un element quelconque. */
```

C'est aussi le sélecteur qu'on donne à PrimeNG (`darkModeSelector`, section 30), pour que la bibliothèque bascule en même temps que nos variables.

### Le service : décider, pas peindre

```typescript
@Injectable({ providedIn: 'root' })
export class ThemeService {
  private navigateur = isPlatformBrowser(inject(PLATFORM_ID));

  private themeSignal = signal<Theme>('clair');
  readonly theme = this.themeSignal.asReadonly();
  readonly sombre = computed(() => this.themeSignal() === 'sombre');

  constructor() {
    if (this.navigateur) {
      this.themeSignal.set(this.themeInitial());

      // Un effect() plutot qu'un appel dans chaque methode : l'attribut SUIT
      // le signal, quelle qu'en soit la cause. Une bascule ecrite plus tard,
      // ou un theme restaure au demarrage, n'auront rien a penser.
      effect(() => this.appliquer(this.themeSignal()));
    }
  }

  basculer(): void {
    this.themeSignal.update(t => (t === 'clair' ? 'sombre' : 'clair'));
  }

  private appliquer(theme: Theme): void {
    // On vise <html> et non <body> : les variables sont declarees sur :root,
    // et certains elements (dialogues, infobulles) se placent hors du <body>
    // de l'application.
    document.documentElement.setAttribute('data-theme', theme);
    localStorage.setItem('projet.theme', theme);
  }
}
```

La répartition à retenir : **le CSS sait peindre, le TypeScript sait décider.** Faire basculer les couleurs depuis le code aurait demandé de connaître, dans le service, chaque couleur de chaque composant.

### Préférence système, mais choix explicite prioritaire

```typescript
private themeInitial(): Theme {
  const memorise = localStorage.getItem('projet.theme');

  // Un choix explicite l'emporte TOUJOURS : il est plus recent, et plus
  // precis, que le reglage global du systeme.
  if (memorise === 'clair' || memorise === 'sombre') {
    return memorise;
  }

  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'sombre' : 'clair';
}
```

`matchMedia` interroge une media-query depuis le JavaScript, exactement comme le ferait une `@media` en CSS. Respecter `prefers-color-scheme` est le comportement attendu aujourd'hui : quelqu'un qui a réglé tout son ordinateur en sombre ne s'attend pas à recevoir une page blanche.

### Détection de plateforme ≠ détection de fonctionnalité

```typescript
// isPlatformBrowser dit qu'on est « dans un navigateur ». Mais le DOM simule
// des tests en est un tres partiel, ou matchMedia n'existe pas — et le test
// echouait sur « window.matchMedia is not a function ».
//
// Verifier la PLATEFORME ne dit rien de la FONCTIONNALITE. D'ou ce second
// garde-fou, qui retombe simplement sur le theme clair.
if (typeof window.matchMedia !== 'function') {
  return 'clair';
}
```

C'est un réflexe qui ressert souvent : avant d'appeler une API récente ou optionnelle (`Notification`, `IntersectionObserver`, `structuredClone`), tester **la fonction elle-même**, pas l'environnement supposé la fournir.

**Dans le projet** — [`services/theme.ts`](../carnet-contact_frontend/src/app/services/theme.ts) et son test [`services/theme.spec.ts`](../carnet-contact_frontend/src/app/services/theme.spec.ts)

### Le scintillement au chargement (FOUC)

Il reste un problème que le service ne peut pas résoudre : entre l'affichage du HTML et le démarrage d'Angular, il s'écoule quelques dizaines de millisecondes. Pendant ce temps, aucun attribut `data-theme` n'est posé — la page s'affiche donc en clair, puis bascule. Ce flash blanc, particulièrement désagréable de nuit, porte un nom : **FOUC** (*Flash Of Unstyled Content*).

La seule parade est un script **synchrone**, dans le `<head>`, exécuté avant que le navigateur ne peigne quoi que ce soit :

```html
<head>
  <script>
    // Volontairement minuscule, sans dependance et sans module : il doit
    // s'executer AVANT le premier rendu. Tout ce qui retarderait son
    // execution (defer, async, un import) reintroduirait le scintillement.
    (function () {
      try {
        var memorise = localStorage.getItem('projet.theme');
        var theme = memorise || (window.matchMedia
          && window.matchMedia('(prefers-color-scheme: dark)').matches ? 'sombre' : 'clair');
        document.documentElement.setAttribute('data-theme', theme);
      } catch (e) {
        // localStorage peut lever (navigation privee, cookies bloques).
        // Un theme est un confort : il ne doit JAMAIS empecher la page de
        // s'afficher.
      }
    })();
  </script>
</head>
```

Ce script duplique volontairement la logique du service. C'est l'un des rares cas où la duplication est justifiée : les deux s'exécutent à des moments où l'autre n'existe pas.

**Dans le projet** — [`src/index.html`](../carnet-contact_frontend/src/index.html)

### Tester une bascule de thème

Un service de thème qui garderait la bonne valeur sans l'écrire sur `<html>` laisserait la page obstinément claire. Le test doit donc vérifier l'**effet visible**, pas seulement le signal :

```typescript
it('écrit l\'attribut sur <html> et mémorise le choix', () => {
  const service = TestBed.inject(ThemeService);
  TestBed.tick();

  service.basculer();
  // tick() laisse les effect() s'executer : ils sont DIFFERES, pas synchrones.
  // Sans lui, l'attribut ne serait pas encore pose.
  TestBed.tick();

  expect(document.documentElement.getAttribute('data-theme')).toBe('sombre');
  expect(localStorage.getItem('projet.theme')).toBe('sombre');
});
```

---

## 32. Réactions et accusés de lecture

Deux ajouts à la messagerie qui, sous leur air anodin, posent chacun une question de modélisation intéressante : où ranger une donnée qui appartient à une **paire** (ce message, cette personne) ? et comment envoyer au client une information qui **dépend de qui regarde** ?

### Une entité de liaison

Une réaction n'appartient ni au message seul, ni à l'utilisateur seul : elle appartient au **couple**. C'est le cas type d'une table de liaison — la même forme que « un étudiant inscrit à un cours », « un utilisateur qui aime une publication ».

```java
@Entity
@Table(
    name = "reaction",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_reaction_message_utilisateur",
        // LA regle metier « une seule reaction par personne et par message »,
        // exprimee la ou elle ne peut pas etre contournee : dans le schema.
        // Un contrôle en Java se contourne par un bug, par une autre route,
        // ou par deux requetes simultanees ; une contrainte d'unicite, non.
        columnNames = { "message_id", "utilisateur_id" }))
public class Reaction {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // LAZY : on ne charge le message que si on le demande vraiment. Le defaut
    // d'un @ManyToOne est EAGER, qui ramenerait le message ENTIER a chaque
    // lecture de reaction (section 19).
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Message message;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Utilisateur utilisateur;

    // length = 8, pas 1 ou 2 : un emoji n'est PAS un caractere. « ❤️ » compte
    // deux points de code (le coeur, plus un selecteur de variante), et
    // certains emojis composes en comptent davantage.
    @Column(nullable = false, length = 8)
    private String emoji;
}
```

> Le nom donné à la contrainte (`uk_reaction_message_utilisateur`) n'est pas décoratif : c'est lui qui apparaîtra dans le message d'erreur de la base le jour où elle sera violée. Une contrainte anonyme donne `UK_a3f9b21` — inexploitable.

**Dans le projet** — [`model/Reaction.java`](../carnet-contact-backend/src/main/java/com/example/carnet_contact_backend/model/Reaction.java)

### Valider côté serveur ce que le client propose

```java
// La liste des emojis proposes vit des DEUX cotes : celle-ci decide de ce que
// le serveur ACCEPTE, celle du frontend de ce qu'on AFFICHE. Meme partage des
// roles que pour la politique de mot de passe (section 27) — le client rend
// service, le serveur fait loi.
public static final List<String> EMOJIS_AUTORISES = List.of("👍", "❤️", "😂", "😮", "😢");

// Set.copyOf : la recherche « est-ce dans la liste ? » devient immediate,
// au lieu de parcourir la liste a chaque appel.
private static final Set<String> EMOJIS_VALIDES = Set.copyOf(EMOJIS_AUTORISES);
```

Sans cette validation, n'importe quel client pourrait enregistrer n'importe quelle chaîne de huit caractères comme « réaction ». Une route `GET /api/messages/emojis` expose la liste, pour qu'un client puisse la lire plutôt que de la deviner.

### Une seule route, trois gestes : l'idempotence

Poser une réaction, en changer, la retirer : trois gestes. La tentation est d'en faire trois routes (`POST`, `PUT`, `DELETE`) et de laisser le client choisir.

C'est une erreur, et voici pourquoi : le client trancherait à partir d'un affichage peut-être périmé de quelques secondes. Deux clics rapides, et les deux appels se contredisent — un `POST` arrive alors qu'une réaction existe déjà, un `DELETE` alors qu'il n'y a plus rien.

```java
// UNE route. Le client dit seulement « j'ai clique sur 👍 » ; le SERVEUR
// compare a ce qui existe deja et en deduit le geste.
@PutMapping("/{messageId}/reaction")
public MessageVu reagir(...) {
    var existante = reactionRepository.findByMessageIdAndUtilisateurId(messageId, moi.getId());

    if (existante.isPresent()) {
        if (existante.get().getEmoji().equals(demande.emoji())) {
            reactionRepository.delete(existante.get());   // meme emoji -> on retire
        } else {
            existante.get().setEmoji(demande.emoji());    // autre emoji -> on remplace
            reactionRepository.save(existante.get());
        }
    } else {
        reactionRepository.save(new Reaction(message, moi, demande.emoji()));
    }
    ...
}
```

L'opération devient **idempotente au sens utile** : le serveur est seul à connaître l'état, donc le résultat ne dépend jamais de ce que le client croyait savoir.

`PUT` plutôt que `POST` dit d'ailleurs exactement cela : « mets la réaction de cette personne sur ce message dans tel état », et non « crée une nouvelle réaction ».

### Le problème N+1

Afficher un fil de cinquante messages avec leurs réactions, naïvement :

```java
for (Message m : messages) {
    // UNE requete SQL par message. Cinquante messages = cinquante requetes,
    // plus celle qui a ramene les messages. C'est le probleme « N+1 » : le
    // nombre de requetes croit avec le nombre de resultats.
    var reactions = reactionRepository.findByMessageId(m.getId());
}
```

La correction tient en une requête :

```java
// On demande TOUTES les reactions des messages concernes d'un coup...
var ids = messages.stream().map(Message::getId).toList();
var toutes = reactionRepository.findByMessageIdIn(ids);

// ...puis on les regroupe EN MEMOIRE par message. Une requete, quel que soit
// le nombre de messages.
var parMessage = toutes.stream().collect(groupingBy(r -> r.getMessage().getId()));
```

C'est un réflexe à acquérir : **dès qu'une requête apparaît à l'intérieur d'une boucle, il faut la sortir.** Le symptôme est discret en développement (une base locale de dix lignes répond vite) et brutal en production.

| Méthode dérivée | Ce qu'elle génère |
|---|---|
| `findByMessageId(Long)` | Une requête par message — à éviter dans une boucle |
| `findByMessageIdIn(List<Long>)` | Une seule requête `WHERE message_id IN (…)` |

**Dans le projet** — [`repository/ReactionRepository.java`](../carnet-contact-backend/src/main/java/com/example/carnet_contact_backend/repository/ReactionRepository.java)

### Une réponse qui dépend de qui regarde

Le client a besoin de savoir « 👍 ×3, **dont la mienne** ». Or `parMoi` ne peut pas être une colonne : la réponse n'est pas la même selon le lecteur. C'est donc au serveur de la calculer, à l'envoi.

```java
// Un record dedie a l'AFFICHAGE, distinct de l'entite. Il porte des donnees
// que la base ne contient pas telles quelles : un total, et un booleen
// relatif au demandeur.
public record ReactionResume(String emoji, long nombre, boolean parMoi) {}

public record MessageVu(Long id, Utilisateur expediteur, Utilisateur destinataire,
                        String contenu, Instant dateEnvoi, boolean lu,
                        List<ReactionResume> reactions) {}
```

```java
// LinkedHashMap et non HashMap : elle conserve l'ordre d'INSERTION. Sans
// cela, l'ordre des emojis sous une bulle changerait d'un rafraichissement a
// l'autre — un scintillement inexplicable pour l'utilisateur.
var parEmoji = new LinkedHashMap<String, List<Reaction>>();
```

Envoyer la liste nominative de qui a réagi aurait été plus « brut », mais aurait obligé chaque client à recompter, et aurait divulgué plus d'informations que nécessaire. **Un DTO envoie ce que l'affichage demande, pas ce que la base contient.**

### L'accusé de lecture

```java
@Column(nullable = false)
private boolean lu = false;
```

Le point délicat n'est pas le champ, c'est **le moment** où il bascule. Ici, ouvrir une conversation marque comme lus les messages qu'on y reçoit — le `GET` du fil a donc un effet de bord, ce qui est inhabituel pour une lecture. C'est un choix assumé : il correspond exactement à ce que « lu » veut dire pour un humain.

Une conséquence à gérer côté client : le serveur vient de marquer ces messages comme lus, la pastille de non-lus doit donc suivre **immédiatement**, sans attendre le prochain tour du sondage (jusqu'à quinze secondes plus tard) :

```typescript
.subscribe(messages => {
  this.filSignal.set(messages);
  // Les messages de cette conversation ne sont plus « non lus ».
  this.nonLusSignal.update(liste => liste.filter(m => m.expediteur.id !== autreId));
});
```

### Côté affichage : ne montrer une information que là où elle veut dire quelque chose

```html
@if (estDeMoi(message.expediteur.id)) {
  <span class="accuse" [class.accuse-lu]="message.lu">
    <i class="pi" [class.pi-check]="!message.lu" [class.pi-check-circle]="message.lu"></i>
    {{ message.lu ? 'Lu' : 'Envoyé' }}
  </span>
}
```

L'accusé ne s'affiche que sur ses **propres** messages. Sur un message reçu il ne voudrait rien dire : on sait forcément qu'on l'a lu, puisqu'on le regarde.

Noter aussi que l'état se lit de **deux** manières — le mot et l'icône — et pas seulement par la couleur. Quelqu'un qui distingue mal les nuances doit pouvoir lire l'état quand même.

### Regrouper les messages par journée

```typescript
// Afficher la date complete sous chaque bulle serait illisible : dans une
// conversation, l'heure suffit, et la date ne change qu'une fois par jour.
// L'information rare doit apparaitre rarement.
filParJour = computed<GroupeJour[]>(() => {
  const groupes: GroupeJour[] = [];

  for (const message of this.fil()) {
    const date = new Date(message.dateEnvoi);
    // toDateString() rabote l'heure : deux messages du meme jour donnent la
    // meme cle, quelle que soit la minute.
    const cle = date.toDateString();

    const dernier = groupes.at(-1);
    if (dernier?.cle === cle) {
      dernier.messages.push(message);
    } else {
      groupes.push({ cle, libelle: this.libelleJour(date), messages: [message] });
    }
  }

  return groupes;
});
```

```typescript
// « Aujourd'hui » et « Hier » plutot qu'une date : « 11/09/2026 » demande un
// calcul mental pour savoir si c'etait ce matin.
private libelleJour(date: Date): string {
  if (date.toDateString() === new Date().toDateString()) return "Aujourd'hui";
  ...
  // toLocaleDateString laisse le NAVIGATEUR formater selon la langue de
  // l'utilisateur : « lundi 8 septembre », sans qu'on ait a ecrire le nom
  // des mois nulle part.
  return date.toLocaleDateString('fr-FR', { weekday: 'long', day: 'numeric', month: 'long' });
}
```

Le découpage est fait dans le composant, pas dans le gabarit : **un gabarit décrit ce qu'on voit, il ne calcule pas.**

**Dans le projet** — [`pages/messages/messages.ts`](../carnet-contact_frontend/src/app/pages/messages/messages.ts), [`pages/messages/messages.html`](../carnet-contact_frontend/src/app/pages/messages/messages.html) et [`controller/MessageController.java`](../carnet-contact-backend/src/main/java/com/example/carnet_contact_backend/controller/MessageController.java)

### Un seul état pour une seule palette ouverte

```typescript
// Un signal pour TOUTE la liste, et non un booleen par bulle : c'est ce qui
// garantit qu'UNE SEULE palette est ouverte a la fois. Avec un etat par
// message, il faudrait penser a refermer les autres a chaque ouverture — et
// l'oubli finit toujours par arriver.
paletteOuverte = signal<number | null>(null);

basculerPalette(messageId: number): void {
  this.paletteOuverte.update(ouvert => (ouvert === messageId ? null : messageId));
}
```

C'est une forme générale utile : quand une contrainte dit « un seul à la fois », l'exprimer par **un seul état partagé** plutôt que par N états qu'il faudrait synchroniser.

### Positionner sans casser le défilement

La palette d'emojis est placée **dans le flux**, et non en `position: absolute`. La raison est concrète : la zone du fil a `overflow-y: auto`, et tout ce qui déborde de ses bords y est rogné — une palette flottant au-dessus de la première bulle disparaîtrait à moitié.

```css
/* Trois niveaux, chacun avec un role, et aucun positionnement absolu :
   .bloc-bulle   : la colonne (bulle, palette, reactions) et le cote d'affichage
   .rangee-bulle : la bulle et son bouton de reaction, cote a cote
   .bulle        : le fond colore */

/* row-reverse renvoie le bouton de l'autre cote sans toucher au HTML : la
   bulle reste le premier element du document, donc le premier lu a voix haute
   par un lecteur d'ecran. L'ordre visuel et l'ordre logique n'ont pas a etre
   identiques. */
.bloc-bulle.de-moi .rangee-bulle { flex-direction: row-reverse; }
```

```css
/* Le bouton reste visible (et non cache jusqu'au survol) : sur un ecran
   tactile il n'y a pas de survol — un bouton qui n'apparait qu'au :hover
   n'existe tout simplement pas sur mobile. On le rend discret, pas absent. */
.ouvrir-palette { opacity: 0.4; }
.rangee-bulle:hover .ouvrir-palette,
.ouvrir-palette:focus-visible { opacity: 1; }
```

**Dans le projet** — [`pages/messages/messages.css`](../carnet-contact_frontend/src/app/pages/messages/messages.css)

---

## 33. Backend Spring Boot

Spring Boot organise traditionnellement une application autour de trois couches bien distinctes, chacune avec une responsabilité précise, ce qui reflète une architecture logicielle très répandue dans le développement backend en général (pas seulement en Java). Comprendre cette séparation aide à savoir instinctivement où placer un nouveau bout de code selon ce qu'il doit faire.

### Entité JPA

```java
package com.example.monapp.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class MonEntite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String champ1;
    private String champ2;

    // Getters et setters (générables via VS Code : clic droit > Source Action > Generate Getters and Setters)
}
```

| Annotation | Rôle |
|---|---|
| `@Entity` | Cette classe est mappée vers une table en base de données |
| `@Id` | Désigne la clé primaire |
| `@GeneratedValue(strategy = GenerationType.IDENTITY)` | L'id est généré automatiquement par la base (auto-incrémenté) |

Une entité est le point de jonction entre le monde orienté objet du code Java et le monde relationnel d'une base de données. Chaque instance de cette classe correspond concrètement à une ligne dans une table, et chaque propriété correspond à une colonne. C'est ce mapping, entièrement piloté par les annotations, qui dispense d'avoir à écrire soi-même des requêtes SQL de création de table ou d'insertion — le framework s'en charge automatiquement à partir de la description de la classe.

### Repository

```java
package com.example.monapp.repository;

import com.example.monapp.model.MonEntite;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MonEntiteRepository extends JpaRepository<MonEntite, Long> {
}
```

`JpaRepository<MonEntite, Long>` fournit automatiquement, sans écrire de code : `findAll()`, `findById()`, `save()`, `deleteById()`, et bien d'autres méthodes. `Long` désigne le type de la clé primaire.

Le repository est la couche responsable exclusivement de l'accès aux données — lire, écrire, mettre à jour, supprimer. Le fait qu'il s'agisse d'une simple interface, sans aucune implémentation écrite à la main, est la partie la plus surprenante au premier abord : en héritant de `JpaRepository`, la classe hérite automatiquement d'un ensemble déjà tout fait de méthodes CRUD standard, et Spring génère lui-même, au démarrage de l'application, une implémentation concrète capable de dialoguer avec la base de données. On peut aussi y ajouter ses propres méthodes de recherche personnalisées simplement en déclarant leur signature, en suivant une convention de nommage précise (par exemple `findByNom(String nom)`), sans avoir à écrire la logique — Spring l'interprète automatiquement à partir du nom de la méthode.

### Contrôleur REST

```java
package com.example.monapp.controller;

import com.example.monapp.model.MonEntite;
import com.example.monapp.repository.MonEntiteRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ressource")
@CrossOrigin(origins = "http://localhost:4200")
public class MonController {

    private final MonEntiteRepository repository;

    public MonController(MonEntiteRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<MonEntite> getAll() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public MonEntite getById(@PathVariable Long id) {
        return repository.findById(id).orElseThrow();
    }

    @PostMapping
    public MonEntite create(@RequestBody MonEntite item) {
        return repository.save(item);
    }

    @PutMapping("/{id}")
    public MonEntite update(@PathVariable Long id, @RequestBody MonEntite item) {
        item.setId(id);
        return repository.save(item);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        repository.deleteById(id);
    }
}
```

| Annotation | Rôle |
|---|---|
| `@RestController` | Classe qui gère des requêtes HTTP, renvoie du JSON |
| `@RequestMapping("/api/ressource")` | Préfixe commun de toutes les routes de la classe |
| `@CrossOrigin(origins = "...")` | Autorise les requêtes venant d'une autre origine (ex: Angular sur un autre port) — sans ça : erreur CORS |
| `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping` | Associent une méthode à un verbe HTTP et une route |
| `@RequestBody` | Convertit automatiquement le JSON reçu en objet Java |
| `@PathVariable` | Récupère une valeur dynamique dans l'URL (ex: l'`id` dans `/api/ressource/5`) |

Le contrôleur constitue la porte d'entrée de l'application côté réseau : c'est lui qui expose des URLs accessibles depuis l'extérieur (typiquement depuis ton frontend Angular), reçoit les requêtes HTTP, les traduit en appels vers le repository, et renvoie une réponse formatée en JSON. C'est un peu le miroir exact, côté serveur, de ce que fait un service Angular côté client : là où le service Angular *appelle* une URL, le contrôleur Spring Boot est celui qui *répond* à cette URL.

L'annotation `@CrossOrigin` mérite une attention particulière, car son absence est une source fréquente de confusion pour les débutants : par défaut, les navigateurs bloquent, pour des raisons de sécurité, les requêtes JavaScript émises depuis une origine (ton frontend, par exemple `localhost:4200`) vers une autre origine (ton backend, `localhost:8080`) — ce mécanisme de protection s'appelle CORS. Sans cette annotation, même si le backend fonctionne parfaitement bien tout seul, toute tentative d'appel depuis Angular échouera avec une erreur visible dans la console du navigateur.

### La requête preflight (`OPTIONS`)

Le contrôle CORS ne se limite pas à un simple en-tête ajouté à la réponse. Pour toutes les requêtes que le navigateur juge « non anodines » — c'est le cas de `DELETE`, de `PUT`, et de `POST` lorsqu'il transporte du JSON — le navigateur n'envoie pas directement la requête demandée. Il envoie d'abord une requête préalable de type `OPTIONS` vers la même URL, appelée *preflight*, dont le sens est : « une page servie depuis telle origine souhaite effectuer telle méthode chez toi, l'autorises-tu ? ». Ce n'est qu'après une réponse favorable du serveur que la vraie requête part.

Cette mécanique explique deux observations déroutantes au premier abord.

| Observation | Explication |
|---|---|
| Une seule action produit **deux lignes** dans l'onglet Réseau : une `preflight` puis la vraie requête | Comportement normal du navigateur, pas un bug ni une requête envoyée en double par erreur |
| En cas d'erreur CORS, un `System.out.println` placé dans la méthode du contrôleur n'affiche **jamais rien** | C'est le `OPTIONS` préalable qui a échoué : la méthode Java n'a jamais été appelée, le navigateur ayant bloqué avant |

La seconde ligne est particulièrement utile à connaître pour déboguer. Une erreur CORS ressemble à un problème de backend, mais elle se produit en amont de tout code métier : inutile d'aller chercher un bug dans la logique du contrôleur ou du repository, c'est la configuration d'autorisation d'origine qu'il faut examiner.

À noter également que l'onglet Réseau du navigateur affiche, pour chaque requête, le fichier et la ligne de code qui l'ont déclenchée (colonne *Initiator* dans Chrome). C'est un réflexe de débogage précieux : lorsqu'une requête part alors qu'elle ne devrait pas — par exemple un rechargement de liste devenu inutile — cette colonne indique immédiatement quelle ligne du code en est responsable.

### Injection de dépendances côté Java

```java
private final MonEntiteRepository repository;

public MonController(MonEntiteRepository repository) {
    this.repository = repository;
}
```

Le principe est exactement le même que l'injection de dépendances vue côté Angular avec `inject()` : on ne crée jamais soi-même une instance du repository avec `new MonEntiteRepository()` — on la reçoit, déjà prête à l'emploi, fournie automatiquement par le framework. La différence se situe uniquement dans la syntaxe employée pour formuler cette demande : côté Java/Spring, la façon la plus classique consiste à déclarer la dépendance comme paramètre du constructeur de la classe. Spring détecte alors automatiquement, au démarrage de l'application, que ce contrôleur a besoin d'un `MonEntiteRepository`, et lui en fournit une instance sans intervention supplémentaire de ta part.

---

## 34. Git et GitHub

Git est un outil de gestion de versions : il permet de garder un historique complet de toutes les modifications apportées à un projet au fil du temps, sous forme d'une succession d'instantanés (les "commits"). GitHub, de son côté, est un service d'hébergement en ligne pour des dépôts Git — il permet de sauvegarder ce même historique sur un serveur distant, accessible depuis n'importe quel ordinateur, et sert également de plateforme de collaboration si un projet est partagé entre plusieurs personnes.

### Commandes de base

| Commande | Rôle |
|---|---|
| `git init` | Transforme un dossier en dépôt Git |
| `git status` | Affiche l'état actuel (fichiers modifiés, en attente, etc.) |
| `git add .` | Prépare tous les fichiers modifiés pour le prochain commit |
| `git commit -m "message"` | Crée un instantané (commit) de l'état actuel, avec un message |
| `git push` | Envoie les commits locaux vers le dépôt distant (GitHub) |
| `git pull` | Récupère les derniers commits depuis le dépôt distant |

Le fonctionnement de Git repose sur une séquence en deux temps qu'il est utile de bien intérioriser : d'abord on "prépare" (`add`) les modifications qu'on souhaite inclure dans le prochain instantané, puis on "valide" (`commit`) cet instantané avec un message qui en décrit le contenu. Cette étape intermédiaire de préparation permet, dans des cas plus avancés, de choisir précisément quels fichiers modifiés on veut inclure dans tel ou tel commit, plutôt que de tout regrouper systématiquement.

### Connexion initiale à GitHub (une seule fois par projet)

```bash
git remote add origin <url-du-dépôt-github>
git branch -M main
git push -u origin main
```

Cette séquence n'est à exécuter qu'une seule fois, lors de la mise en place initiale du lien entre le dépôt local (sur ton ordinateur) et le dépôt distant (sur GitHub). `git remote add origin` enregistre l'adresse du dépôt distant sous un nom court, `origin`, qu'on pourra réutiliser ensuite sans avoir à retaper l'URL complète. `git branch -M main` renomme la branche principale en `main` (la convention actuelle sur GitHub). `git push -u origin main` envoie les commits pour la première fois, et l'option `-u` crée un lien permanent entre la branche locale et sa contrepartie distante — c'est grâce à ce lien qu'un simple `git push`, sans argument supplémentaire, suffira pour toutes les fois suivantes.

### `.gitignore`

```
# Angular
mon-projet-angular/node_modules/
mon-projet-angular/.angular/
mon-projet-angular/dist/

# Spring Boot
mon-projet-backend/target/

# IDE / OS
.vscode/
.DS_Store
```

Ce fichier a pour rôle d'indiquer à Git une liste de fichiers ou de dossiers à ne jamais suivre, quelle que soit la commande utilisée. La raison principale est pratique : certains dossiers, comme `node_modules` ou `target`, sont entièrement régénérables à partir d'autres fichiers du projet (`package.json` pour npm, `pom.xml` pour Maven) et peuvent peser plusieurs centaines de mégaoctets — les inclure dans l'historique Git alourdirait considérablement le dépôt sans réel bénéfice, puisque n'importe qui peut les régénérer en une commande (`npm install`, `./mvnw compile`).

### Workflow habituel de travail

```bash
git add .
git commit -m "Description claire de ce qui a été fait"
git push
```

Prendre l'habitude de répéter cette séquence après chaque fonctionnalité ou correction significative permet de garder un historique lisible du projet, avec des points de restauration réguliers auxquels revenir en cas de problème, et une trace claire de la progression au fil du temps.

---

## 35. Pense-bête de dépannage

| Symptôme | Cause probable | Solution |
|---|---|---|
| Page blanche ou comportement incohérent au rafraîchissement, sans erreur dans la console | Cache `.angular` corrompu | Arrêter `ng serve`, puis `Remove-Item -Recurse -Force .angular`, puis relancer `ng serve` |
| `warning: adding embedded git repository` | Un `.git` existe déjà dans un sous-dossier (généré par `ng new`) | Supprimer ce `.git` imbriqué, puis `git rm --cached -rf <dossier>`, puis refaire `git add .` |
| Erreur CORS dans la console du navigateur lors d'un appel HTTP vers le backend | `@CrossOrigin` manquant ou mal configuré côté Spring Boot | Vérifier `@CrossOrigin(origins = "http://localhost:4200")` sur le contrôleur |
| Deux lignes apparaissent dans l'onglet Réseau pour une seule action (une `preflight` puis la vraie requête) | Comportement normal : le navigateur demande d'abord l'autorisation via `OPTIONS` | Rien à corriger — voir la sous-section sur la requête preflight |
| Erreur CORS alors qu'un `System.out.println` dans le contrôleur n'affiche rien | Le `OPTIONS` préalable a échoué : la méthode Java n'est jamais appelée | Chercher du côté de `@CrossOrigin`, pas dans la logique du contrôleur |
| `LF will be replaced by CRLF` à chaque `git add` sous Windows | Git convertit les fins de ligne entre le dépôt (LF) et le disque (CRLF) | Avertissement sans conséquence, aucune action nécessaire |
| `NG04002` / `Cannot match any routes` dans la console | Aucune route ne correspond à l'URL demandée (faute de frappe, route non déclarée) | Vérifier le tableau `routes`, et écrire les `path` SANS barre oblique initiale (`''`, pas `'/'`) |
| Un clic sur un `routerLink` recharge toute la page | Un `href` a été utilisé à la place, ou `RouterLink` absent des `imports` du composant | Utiliser `routerLink` / `[routerLink]` et l'ajouter aux `imports` |
| `ng build` échoue en réclamant les paramètres d'une route `:id` à prérendre | Route paramétrée laissée en `RenderMode.Prerender` alors que les valeurs n'existent qu'à l'exécution | La passer en `RenderMode.Client` dans `app.routes.server.ts` |
| La page de détail est vide au rafraîchissement (F5) alors qu'elle s'affiche via un clic | Accès direct à l'URL : le signal partagé n'a été rempli par personne | Appeler la méthode de chargement du service dans le `ngOnInit()` de la page de détail |
| Les données ajoutées disparaissent après un redémarrage du backend | Base H2 configurée en mémoire (comportement normal avec la config par défaut) | Attendu pour l'instant ; pour la persistance réelle, configurer H2 en mode fichier ou changer de base de données |
| Erreur TypeScript qui persiste alors que le code semble correct | Service de langage TypeScript désynchronisé | Palette de commandes → `TypeScript: Restart TS Server` |
| `./mvnw : Le terme n'est pas reconnu...` sous PowerShell | Forme Unix de la commande, inadaptée à PowerShell | Utiliser `.\mvnw.cmd spring-boot:run` (antislash + extension `.cmd`) |
| `Web server failed to start. Port 8080 was already in use.` | Un backend tourne déjà dans un autre terminal | Retrouver le terminal et faire `Ctrl + C` ; sinon `netstat -ano \| findstr :8080` puis `taskkill /PID <pid> /F` |
| `Port 4200 is already in use` au lancement de `ng serve` | Un `ng serve` déjà actif ailleurs | Fermer l'autre terminal, ou lancer sur un autre port : `ng serve --port 4201` |
| Page affichée mais toutes les listes vides, erreur réseau dans la console (`F12`) | Le backend n'est pas démarré, ou pas encore prêt | Vérifier le terminal du backend (`Started ...Application`) et tester l'URL de l'API directement dans le navigateur |
| `cannot read properties of undefined` au démarrage d'un composant qui référence un signal de service | Une propriété utilise `this.monService` alors que la ligne `inject()` est déclarée en dessous | Remonter la ligne `inject()` au-dessus : les champs d'une classe sont initialisés dans leur ordre de déclaration |
| Une liste ne se met pas à jour après un ajout ou une suppression faits par un autre composant | Chaque composant possède sa propre copie de la donnée dans un signal local | Déplacer la donnée dans le service (signal partagé, voir section 12) plutôt que de recharger la page |
| Le formulaire d'édition reste vide alors que la fiche s'affiche bien | Formulaire pré-rempli à la construction, avant l'arrivée des données du signal partagé | Pré-remplir dans un `effect()` qui réagit au signal, pas dans le `constructor` directement (section 14) |
| Le formulaire d'édition efface la saisie en cours de temps en temps | Un `effect()` de pré-remplissage se réexécute à chaque changement du signal (ex : rechargement de la liste) | Ajouter un drapeau booléen : ne `patchValue()` qu'une seule fois |
| `PUT`/`DELETE` renvoie 403 ou une erreur CORS alors que `GET` fonctionne | Requête « non anodine » : le navigateur envoie d'abord un `OPTIONS` (preflight) que `@CrossOrigin` doit autoriser | Vérifier `@CrossOrigin` sur le contrôleur (section 33) ; regarder la ligne `preflight` dans l'onglet Réseau |
| Modification enregistrée côté serveur mais la fiche affiche encore l'ancienne valeur | Le signal partagé n'a pas été mis à jour après le `PUT` | Dans le service, `.update()` avec `.map()` pour remplacer l'élément modifié par la réponse du serveur |
| `NG0203` / `inject() must be called from an injection context` sur un `effect()` | `effect()` appelé hors constructeur / hors champ de classe | Le déplacer dans le `constructor` du composant |
| Backend éteint ou en erreur : liste vide, formulaire sans réaction, aucun message | `.subscribe()` n'a qu'un callback de succès, l'erreur du flux n'est traitée nulle part | `.pipe(catchError(...))` dans le service + un signal d'erreur affiché (section 15) |
| `catchError` provoque `Type 'void' is not assignable to type 'ObservableInput<...>'` | Le callback de `catchError` ne retourne pas d'Observable | Retourner `of(valeurDeRepli)`, `EMPTY`, ou `throwError(() => err)` |
| La bannière d'erreur d'un `POST`/`PUT` met plusieurs secondes à apparaître (serveur éteint) | Le navigateur attend l'expiration du preflight `OPTIONS` avant de conclure à l'échec | Normal — pas de correction ; le `GET` sans preflight échoue plus vite (section 33) |
| Une modification du code (nouveau signal, `delay()` ajouté...) reste sans effet dans le navigateur | Le rechargement à chaud de `ng serve` n'a pas pris (fréquent sous Windows / avec le SSR) | `Ctrl + C` sur `ng serve`, `npm start`, attendre `bundle generation complete`, puis `Ctrl + Shift + R` dans le navigateur |
| L'indicateur de chargement ne s'affiche jamais au rafraîchissement de la page | Le `GET` initial part côté serveur (SSR) : `chargement` passe à `true` puis `false` avant l'envoi du HTML | Normal ; l'indicateur n'apparaît que sur les requêtes déclenchées par un clic (ajout, modif, suppression), section 16 |
| L'indicateur de chargement reste allumé après une erreur réseau | `set(false)` placé seulement dans `.subscribe(next)`, qui ne s'exécute pas en cas d'erreur | Le mettre dans `finalize()` du `.pipe()`, qui s'exécute quelle que soit l'issue (section 16) |
| Un contact en double après un double-clic sur « Ajouter » | Le bouton reste actif pendant la requête, chaque clic renvoie un `POST` | `[disabled]="form.invalid \|\| chargement()"` sur le bouton, en lisant le signal `chargement` du service |
| Une requête ne part jamais : rien dans l'onglet Réseau, aucune erreur en console | Un intercepteur n'appelle pas `next(req)`, ou n'en retourne pas le résultat | Vérifier que chaque intercepteur fait bien `return next(...)` sur **tous** ses chemins, sorties anticipées comprises (section 17) |
| L'échec d'une requête n'atteint plus le `catchError` du service | Le `catchError` d'un intercepteur retourne `of(...)` ou `EMPTY` : il « avale » l'erreur | Terminer par `return throwError(() => erreur)` pour la relancer (section 17) |
| L'indicateur de chargement s'éteint alors qu'une requête tourne encore | Un booléen partagé ne suffit plus dès que deux requêtes se chevauchent | Compter les requêtes en vol et dériver le booléen : `computed(() => compteur() > 0)` (section 17) |
| `req.url = ...` ou `req.headers.set(...)` dans un intercepteur reste sans effet | Un `HttpRequest` est immuable par conception | Passer par `req.clone({ url: ..., setHeaders: ... })` et transmettre la **copie** à `next()` (section 17) |
| `404` sur toutes les requêtes après passage aux URL relatives | L'intercepteur de base URL ne reconnaît pas le préfixe, ou n'est pas placé en premier dans `withInterceptors` | Vérifier le test `req.url.startsWith('/api')` et l'ordre du tableau (section 17) |
| `NG0203` / `inject() must be called from an injection context` dans un intercepteur | `inject()` appelé à l'intérieur d'un callback (`catchError`, `finalize`) au lieu du corps de la fonction | Appeler `inject()` en tête de l'intercepteur et garder la référence dans une `const` |
| Au démarrage du backend : `Using generated security password: ...` | Comportement par défaut dès que `spring-boot-starter-security` est présent, sans `UserDetailsService` déclaré | Sans conséquence si l'authentification passe par un filtre JWT ; pour supprimer le message, exclure `UserDetailsServiceAutoConfiguration` (section 18) |
| Toutes les requêtes renvoient 401/403 alors que le jeton semble correct | `@CrossOrigin` laissé sur le contrôleur : le CORS doit être géré par Spring Security une fois celui-ci en place | Retirer les `@CrossOrigin` et déclarer un `CorsConfigurationSource` dans `SecurityConfig` (section 18) |
| L'en-tête `Authorization` n'arrive pas au backend | Configuration CORS qui n'autorise pas cet en-tête | `config.setAllowedHeaders(List.of("*"))` dans la configuration CORS |
| Une requête sans jeton renvoie 403 au lieu de 401 | Aucun `AuthenticationEntryPoint` : Spring Security utilise `Http403ForbiddenEntryPoint` par défaut | Déclarer un point d'entrée qui répond 401 dans `.exceptionHandling(...)` (section 18) |
| Un `ResponseStatusException(NOT_FOUND)` ressort en 403 | La requête est réacheminée vers `/error`, et `OncePerRequestFilter` ne rejoue pas le filtre JWT sur ce redispatch : l'identité est perdue | Ajouter `.requestMatchers("/error").permitAll()` (section 18) |
| Mot de passe erroné sur la page de connexion : redirection en boucle vers la connexion | L'intercepteur réagit au 401 sans vérifier qu'un jeton existait — or un échec de connexion renvoie aussi 401 | Conditionner la déconnexion automatique à `session.jetonActuel() !== null` (section 18) |
| `localStorage is not defined` au démarrage | Accès à `localStorage` pendant le rendu côté serveur (SSR), où il n'existe pas | Encadrer par `isPlatformBrowser(inject(PLATFORM_ID))` (section 18) |
| Les pages protégées affichent le formulaire de connexion même une fois connecté | Routes prérendues alors que la garde dépend de `localStorage`, absent au build | Passer ces routes en `RenderMode.Client` dans `app.routes.server.ts` (section 18) |
| `Table "USER" not found` ou erreur de syntaxe SQL sur une entité `User` | `user` est un mot réservé en SQL | Renommer la table : `@Table(name = "utilisateur")` (section 19) |
| Le mot de passe haché apparaît dans une réponse JSON | Le champ est sérialisé par Jackson comme n'importe quel autre | `@JsonIgnore` sur le champ (section 19) |
| Récursion infinie / `StackOverflowError` à la sérialisation JSON | Deux entités qui se référencent mutuellement, chacune sérialisant l'autre | `@JsonIgnore` sur la référence inverse, ou ne pas déclarer la collection inverse (section 19) |
| `Repeated column in mapping for entity` sur une entité à deux relations vers le même type | Les deux `@ManyToOne` visent la même colonne par défaut | Nommer chaque colonne : `@JoinColumn(name = "expediteur_id")` (section 19) |
| `null` s'affiche littéralement dans un champ de formulaire pré-rempli | Le backend renvoie `null` pour un champ optionnel, alors qu'un contrôle attend une chaîne | Convertir au `patchValue()` : `c.emailPro ?? ''` (section 19) |
| Les logos de réseaux sociaux restent noirs malgré la couleur CSS | Le tracé SVG a une couleur figée dans l'attribut `fill` | Utiliser `fill: currentColor` en CSS et ne pas fixer `fill` dans le SVG (section 20) |
| La liste affiche le résultat d'une recherche précédente, pas la dernière tapée | Réponses revenues dans le désordre : la lente écrase la récente | `switchMap` à la place d'un `.subscribe()` par appel — il annule la requête précédente (section 22) |
| Après la première erreur réseau, la recherche ne repart plus jamais | `catchError` placé **à l'extérieur** du `switchMap` : il termine le flux externe pour de bon | Le déplacer à l'intérieur, sur la requête elle-même (section 22) |
| Une requête part à chaque touche frappée | `valueChanges` branché directement sur l'appel HTTP | `debounceTime(300)` puis `distinctUntilChanged()` avant le `subscribe` (section 22) |
| Une recherche depuis la page 3 ne renvoie rien alors que des résultats existent | Le numéro de page n'a pas été remis à zéro : on demande les résultats 19 à 24 d'une liste qui en a trois | `pageSignal.set(0)` dans la méthode de recherche (section 22) |
| `?taille=1000000` fait tout charger d'un coup | La taille de page vient du client sans être bornée | `Math.clamp(taille, 1, 50)` dans le contrôleur (section 22) |
| La page de détail affiche « introuvable » pour un contact qui existe | Elle cherche dans le signal de liste, qui ne contient plus qu'une page depuis la pagination | Ajouter un `GET /api/x/{id}` et un signal dédié (section 22) |
| Après un ajout, le contact n'apparaît pas au bon endroit (ou pas du tout) | La liste paginée a été mise à jour à la main, alors que le découpage est calculé par le serveur | Recharger la page courante après l'écriture au lieu de modifier le signal (section 22) |
| Le message d'erreur est technique (« Erreur 500 ») et ne dit pas ce qui a échoué | L'intercepteur ne connaît que le statut, pas l'intention de l'appel | Attacher un libellé par `HttpContextToken` et le composer avec la raison technique (section 23) |
| Le message d'erreur ne mentionne plus l'action alors qu'un libellé a été passé | `new HttpContext().set(...)` rend une **copie** : la valeur de retour a été ignorée | Réaffecter : `ctx = ctx.set(JETON, valeur)` (section 23) |
| La bannière « Chargement… » clignote toutes les quelques secondes | Le sondage périodique passe par l'intercepteur de chargement comme une requête ordinaire | Marquer ces requêtes `discret` et sortir tôt dans l'intercepteur (sections 23 et 25) |
| Toutes les requêtes échouent d'un coup au bout de 15 minutes | Le jeton d'accès a expiré et rien ne le renouvelle | Intercepteur de rafraîchissement + jeton long stocké en base (section 24) |
| L'utilisateur est déconnecté alors que le rafraîchissement aurait dû marcher | Plusieurs 401 simultanés ont lancé plusieurs rotations ; les dernières présentent un jeton déjà révoqué | Mutualiser l'appel : un seul Observable partagé par `shareReplay(1)` (section 24) |
| Boucle infinie d'appels à `/auth/rafraichir` | L'intercepteur tente de renouveler l'appel de renouvellement lui-même | Sortie anticipée sur `req.url.includes('/api/auth/')` (section 24) |
| Le 401 déconnecte avant toute tentative de renouvellement | L'intercepteur de rafraîchissement est placé trop haut dans `withInterceptors` | Le mettre **en dernier** : au retour, le plus profond voit l'erreur en premier (section 24) |
| Après une reconnexion, le rafraîchissement échoue systématiquement | Seul le jeton d'accès a été mémorisé : la rotation a émis un nouveau jeton long, perdu | Enregistrer les **deux** jetons à chaque réponse d'authentification (section 24) |
| Le rendu SSR ne se termine jamais : la page ne s'affiche pas, la requête expire | Un `timer` / `interval` démarré côté serveur empêche l'application d'être « stable » | Garder le sondage derrière `isPlatformBrowser(inject(PLATFORM_ID))` (section 25) |
| Le nombre de requêtes de sondage double, puis quadruple | La méthode de démarrage a été appelée plusieurs fois, empilant les timers | Garde-fou `if (this.suivi) return;` avant de s'abonner (section 25) |
| Des 401 arrivent toutes les 15 secondes après la déconnexion | Le sondage n'a pas été arrêté : il continue avec un jeton devenu invalide | Arrêter l'abonnement dans l'`effect()` qui suit l'état connecté (section 25) |
| Le sondage continue après avoir quitté la page | Aucun désabonnement à la destruction du composant | `ngOnDestroy()` qui appelle la méthode d'arrêt du service (section 25) |
| `Expected one matching request…, found none` dans un test alors que la requête part bien | La requête est déclenchée par un `timer`, qui passe par la file des tâches — `whenStable()` ne l'attend pas | Rendre la main une fois : `await new Promise(r => setTimeout(r, 0))` (section 26) |
| `Vitest caught unhandled errors` : `NG04002 Cannot match any routes` | Un intercepteur navigue vers `/connexion`, absente du `provideRouter([])` du test | Déclarer la route dans le test : `provideRouter([{ path: 'connexion', children: [] }])` (section 26) |
| Un test passe seul mais échoue quand toute la suite tourne | Des données d'un test précédent traînent en base | `@Transactional` sur la classe de test (annulation automatique) et `create-drop` dans `src/test/resources` (section 26) |
| `No qualifying bean of type 'ObjectMapper'` dans un test Spring Boot 4 | Jackson 3 n'expose plus le même type de bean qu'en Boot 3 | Lire le JSON avec `JsonPath.read(corps, "$.champ")`, déjà disponible via les dépendances de test (section 26) |
| `An error was thrown in afterAll` / erreur non gérée sur un test d'intercepteur | L'intercepteur relance l'erreur et le `subscribe` n'a pas de callback `error` | `subscribe({ error: () => {} })` sur les appels censés échouer (section 26) |
| Cliquer sur « Afficher le mot de passe » soumet le formulaire | Un `<button>` sans `type` vaut `type="submit"` dans un `<form>` | Toujours écrire `type="button"` sur un bouton qui n'envoie pas le formulaire (section 27) |
| Le bouton reste désactivé alors que tous les critères sont cochés en vert | La liste affichée et le validateur ne s'appuient pas sur la même source | Faire lire la même fonction `verifie` aux deux, et le vérifier par un test (section 27) |
| Après avoir basculé entre connexion et inscription, le formulaire garde l'ancien verdict | `setValidators()` change la règle mais ne rejoue pas la validation | Enchaîner avec `champ.updateValueAndValidity()` (section 27) |
| Impossible de se connecter à un ancien compte depuis le durcissement du mot de passe | La règle de solidité a été appliquée aussi à la connexion | Ne l'appliquer qu'à la création : un haché ne peut pas être revalidé (section 27) |
| Un mot de passe accepté par le formulaire est refusé par l'API | Les deux versions de la politique ont divergé | Les garder chacune dans son fichier et les couvrir par des tests symétriques (section 27) |
| `aria-pressed` / `aria-label` dynamique reste vide dans le HTML | `[aria-pressed]` vise une propriété DOM qui n'existe pas | Utiliser la forme attribut : `[attr.aria-pressed]` (section 27) |
| `Notification.requestPermission()` n'ouvre aucune fenêtre | La demande ne fait pas suite à un geste de l'utilisateur ; les navigateurs l'ignorent | La déclencher depuis un `(click)`, jamais au démarrage (section 28) |
| Impossible de redemander la permission après un refus | `denied` est définitif du point de vue du site | Prévoir un repli qui marche sans, et expliquer le chemin dans les réglages du navigateur (section 28) |
| `Notification is not defined` au démarrage ou dans les tests | Rendu côté serveur, ou environnement sans cette API (jsdom) | Tester `isPlatformBrowser(...) && 'Notification' in window` avant tout usage (section 28) |
| Une notification pour chaque message non lu, toutes les quinze secondes | Le sondage renvoie la liste complète à chaque tour | Mémoriser les identifiants déjà vus, et remplacer l'ensemble à chaque réponse (section 28) |
| Ouvrir l'application annonce d'un coup tous les messages en attente | Le premier tour de sondage est traité comme les suivants | Faire du premier tour une simple prise d'état, sans notification (section 28) |
| Dix notifications système empilées après une absence | Chaque appel crée une bulle distincte | Leur donner le même `tag` : elles se remplacent au lieu de s'empiler (section 28) |
| Une notification système s'affiche alors qu'on a la page sous les yeux | Le canal est choisi sans regarder si l'onglet est actif | Conditionner à `document.hidden`, et se rabattre sur un bandeau sinon (section 28) |
| Un test à minuteurs simulés se bloque sur une requête HTTP qui ne part jamais | `advanceTimersByTime` n'attend pas la file des micro-tâches | Utiliser la variante `await vi.advanceTimersByTimeAsync(ms)` (section 28) |
| `Could not find stylesheet file './xxx.css'` | Le `styleUrl` d'un composant pointe vers un fichier qui n'existe pas encore | Créer le fichier, même vide — Angular refuse de compiler tant qu'il manque |
| `Could not resolve "@angular/animations/browser"` | `provideAnimationsAsync()` est déclaré, mais le paquet n'est pas installé | Vérifier si la bibliothèque en a réellement besoin (`grep -r "@angular/animations" node_modules/primeng/`) ; PrimeNG 21 ne l'utilise plus — retirer le provider plutôt qu'installer un paquet déprécié (section 30) |
| `npm install @angular/x` échoue sur `peer @angular/core@21.2.23` alors que 21.2.22 est installé | Les paquets Angular s'exigent mutuellement **à la version exacte**, pas en `^` | Installer la version identique à celle de `@angular/core` : `npm install @angular/animations@21.2.22` |
| `primeng@22` refuse de s'installer | Les majeures de PrimeNG suivent celles d'Angular | Installer la majeure correspondante (`primeng@21` pour Angular 21) (section 30) |
| Tous les `p-button` sortent en bleu uni, leur `severity` ignorée | Une règle globale sans couche (`button { … }`) écrase l'habillage de PrimeNG, rangé dans `@layer primeng` — le hors-couche bat toujours une couche | Déclarer `@layer theme, base, primeng;` et placer ses styles de balises dans `@layer base` (section 30) |
| Une règle CSS de composant ne touche pas l'intérieur d'un `p-table` / `p-paginator` | L'encapsulation Angular n'attache son attribut qu'aux éléments du gabarit, pas à ceux fabriqués par la bibliothèque | Préfixer par `:host ::ng-deep` (section 30) |
| `window.matchMedia is not a function` dans les tests | `isPlatformBrowser` est vrai, mais le DOM simulé ne fournit pas cette API | Tester la fonction elle-même : `typeof window.matchMedia !== 'function'` (section 31) |
| La page s'affiche en clair une fraction de seconde avant de passer en sombre | Le thème n'est posé qu'au démarrage d'Angular, après le premier rendu (FOUC) | Un script synchrone dans le `<head>` qui pose `data-theme` avant toute peinture (section 31) |
| La page est sombre mais les ascenseurs restent blancs | Le navigateur dessine lui-même certains éléments, hors de portée du CSS | Déclarer `color-scheme: dark` sur le thème sombre (section 31) |
| Un élément reste clair en mode sombre | Une couleur écrite en dur quelque part (`#fff`) au lieu d'une variable | Chercher les couleurs littérales dans les feuilles de composants (section 31) |
| `bundle initial exceeded maximum budget` après l'ajout d'une bibliothèque | Le budget d'`angular.json` est une alarme réglée à la main, pas une limite technique | Différer les pages lourdes et rares avec `loadComponent`, puis ajuster le budget en connaissance de cause (section 30) |
| Une palette ou une infobulle est coupée en bord de liste | Le conteneur a `overflow-y: auto`, qui rogne tout ce qui déborde | Placer l'élément dans le flux plutôt qu'en `position: absolute` (section 32) |
| `pTooltip` n'affiche rien | `TooltipModule` absent des `imports` du composant | L'ajouter ; une directive PrimeNG non importée est silencieusement ignorée |
| `hasRole("ROLE_ADMIN")` refuse un administrateur | `hasRole` ajoute déjà le préfixe : il cherche `ROLE_ROLE_ADMIN` | Écrire `hasRole("ADMIN")`, ou `hasAuthority("ROLE_ADMIN")` (section 29) |
| Un compte rétrogradé garde ses droits quelques minutes | Le rôle voyage dans le jeton, valable jusqu'à son expiration | Comportement attendu du sans-état ; révoquer les jetons de rafraîchissement et vérifier l'état réel en base pour les actions destructrices (section 29) |
| `curl -d '{"emoji":"👍"}'` renvoie 400 sous Git Bash alors que le serveur est correct | Le shell Windows altère les caractères non-ASCII de la ligne de commande | Écrire le corps dans un fichier et utiliser `--data-binary @fichier.json`, ou échapper en séquences JSON (`\uD83D\uDC4D`) |
| Un résultat contredit le code qu'on vient d'écrire | Ce n'est pas ce code qui tourne : ancienne instance encore démarrée sur le port | `netstat -ano \| grep :8080` avant de conclure ; redémarrer, ou utiliser un autre port |

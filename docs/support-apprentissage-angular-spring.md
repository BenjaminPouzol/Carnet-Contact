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
16. [Backend Spring Boot](#16-backend-spring-boot)
17. [Git et GitHub](#17-git-et-github)
18. [Pense-bête de dépannage](#18-pense-bête-de-dépannage)

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

Serveur éteint : la bannière du `GET` (au chargement) apparaît presque instantanément, celle d'un `POST` d'ajout met quelques secondes. Ce n'est pas un bug du code. Un `POST` qui transporte du JSON est une requête « non anodine » : le navigateur envoie d'abord une requête `OPTIONS` de vérification (le *preflight*, section 16). Quand le serveur ne répond pas, le navigateur laisse ce preflight expirer avant de conclure à l'échec. Le `GET`, requête « simple », part directement et échoue tout de suite.

## 16. Backend Spring Boot

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

## 17. Git et GitHub

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

## 18. Pense-bête de dépannage

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
| `PUT`/`DELETE` renvoie 403 ou une erreur CORS alors que `GET` fonctionne | Requête « non anodine » : le navigateur envoie d'abord un `OPTIONS` (preflight) que `@CrossOrigin` doit autoriser | Vérifier `@CrossOrigin` sur le contrôleur (section 16) ; regarder la ligne `preflight` dans l'onglet Réseau |
| Modification enregistrée côté serveur mais la fiche affiche encore l'ancienne valeur | Le signal partagé n'a pas été mis à jour après le `PUT` | Dans le service, `.update()` avec `.map()` pour remplacer l'élément modifié par la réponse du serveur |
| `NG0203` / `inject() must be called from an injection context` sur un `effect()` | `effect()` appelé hors constructeur / hors champ de classe | Le déplacer dans le `constructor` du composant |
| Backend éteint ou en erreur : liste vide, formulaire sans réaction, aucun message | `.subscribe()` n'a qu'un callback de succès, l'erreur du flux n'est traitée nulle part | `.pipe(catchError(...))` dans le service + un signal d'erreur affiché (section 15) |
| `catchError` provoque `Type 'void' is not assignable to type 'ObservableInput<...>'` | Le callback de `catchError` ne retourne pas d'Observable | Retourner `of(valeurDeRepli)`, `EMPTY`, ou `throwError(() => err)` |
| La bannière d'erreur d'un `POST`/`PUT` met plusieurs secondes à apparaître (serveur éteint) | Le navigateur attend l'expiration du preflight `OPTIONS` avant de conclure à l'échec | Normal — pas de correction ; le `GET` sans preflight échoue plus vite (section 16) |

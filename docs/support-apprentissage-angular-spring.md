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
22. [Backend Spring Boot](#22-backend-spring-boot)
23. [Git et GitHub](#23-git-et-github)
24. [Pense-bête de dépannage](#24-pense-bête-de-dépannage)

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

Serveur éteint : la bannière du `GET` (au chargement) apparaît presque instantanément, celle d'un `POST` d'ajout met quelques secondes. Ce n'est pas un bug du code. Un `POST` qui transporte du JSON est une requête « non anodine » : le navigateur envoie d'abord une requête `OPTIONS` de vérification (le *preflight*, section 22). Quand le serveur ne répond pas, le navigateur laisse ce preflight expirer avant de conclure à l'échec. Le `GET`, requête « simple », part directement et échoue tout de suite.

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

## 22. Backend Spring Boot

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

## 23. Git et GitHub

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

## 24. Pense-bête de dépannage

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
| `PUT`/`DELETE` renvoie 403 ou une erreur CORS alors que `GET` fonctionne | Requête « non anodine » : le navigateur envoie d'abord un `OPTIONS` (preflight) que `@CrossOrigin` doit autoriser | Vérifier `@CrossOrigin` sur le contrôleur (section 22) ; regarder la ligne `preflight` dans l'onglet Réseau |
| Modification enregistrée côté serveur mais la fiche affiche encore l'ancienne valeur | Le signal partagé n'a pas été mis à jour après le `PUT` | Dans le service, `.update()` avec `.map()` pour remplacer l'élément modifié par la réponse du serveur |
| `NG0203` / `inject() must be called from an injection context` sur un `effect()` | `effect()` appelé hors constructeur / hors champ de classe | Le déplacer dans le `constructor` du composant |
| Backend éteint ou en erreur : liste vide, formulaire sans réaction, aucun message | `.subscribe()` n'a qu'un callback de succès, l'erreur du flux n'est traitée nulle part | `.pipe(catchError(...))` dans le service + un signal d'erreur affiché (section 15) |
| `catchError` provoque `Type 'void' is not assignable to type 'ObservableInput<...>'` | Le callback de `catchError` ne retourne pas d'Observable | Retourner `of(valeurDeRepli)`, `EMPTY`, ou `throwError(() => err)` |
| La bannière d'erreur d'un `POST`/`PUT` met plusieurs secondes à apparaître (serveur éteint) | Le navigateur attend l'expiration du preflight `OPTIONS` avant de conclure à l'échec | Normal — pas de correction ; le `GET` sans preflight échoue plus vite (section 22) |
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

# Refonte du cours Angular — plan

**Goal:** Compléter `docs/cours-angular.md` (étapes 11 à 26 annoncées mais jamais rédigées, étape 27 sur le fil d'actualité) et ajouter des schémas Mermaid partout où un schéma éclaire mieux qu'un paragraphe, sans retirer les paragraphes explicatifs existants.

**Spec :** demande de l'utilisateur (« refais le cours Angular en rajoutant des schémas quand c'est possible, tout en gardant les paragraphes explicatifs ») ; format des schémas choisi : Mermaid.

## Contraintes

- Public inchangé : quelqu'un qui n'a jamais fait d'Angular. Chaque terme nouveau en **gras** et défini à sa première apparition.
- Forme de chaque étape, identique aux étapes 1 à 10 : « Le problème » → « La notion » → syntaxe générique commentée → « Dans le projet » avec marqueurs **[ÉTAPE]** / **[DÉFINITIF]** / **[REMPLACÉ]** (code extrait de Git, commit cité) → tableau « Ce que cette étape a introduit » → renvoi à la section du support.
- Schémas : blocs ` ```mermaid ` ; libellés entre guillemets ; `<br/>` pour les retours à la ligne ; `&lt;` / `&gt;` pour les chevrons. Un schéma accompagne un paragraphe, il ne le remplace jamais.
- Numéros du support après renumérotation : Backend 36, Git 37, Pense-bête 38 ; nouvelles sections 33 (Bean Validation), 34 (curseur), 35 (fil).
- Ne rien réécrire des étapes 1 à 10 en dehors des ajouts de schémas.

## Commits de référence

| Étape | Commit | Extraits [ÉTAPE] |
|---|---|---|
| 11 Routing | `a50da82` | `app.routes.ts`, `app.html`, `contact-detail.ts` (version `computed`) |
| 12 PUT + `effect()` | `3a39b53` | `contact-edit.ts` (version `computed` + drapeau) |
| 13 `catchError` | `41b9bcf` | `services/contact.ts` (signal `erreur`) |
| 14 `finalize` | `47eea88` | `services/contact.ts` (signal `chargement`) |
| 15 Intercepteurs | `fe1aaf3` | `services/contact.ts` allégé, `erreur-interceptor.ts` |
| 16 à 18 | `cf7f261` | `erreur-interceptor.ts` (401), session, gardes, `reseaux-sociaux`, `styles.css` |
| 19 à 23 | `3c06e4b` | pagination, contexte HTTP, rafraîchissement, sondage, tests, mot de passe, notifications |
| 24 à 26 | `19da6f8`, `3e2acc7` | administration, PrimeNG, couches, mode sombre, réactions |
| 27 | copie de travail | fil d'actualité |

## Schémas prévus

| Étape | Schéma |
|---|---|
| Vocabulaire | Navigateur / serveur / base (flowchart) ; ordre d'exécution asynchrone (séquence) |
| 1 | Chaîne de démarrage `index.html` → `main.ts` → `app.config.ts` → `App` |
| 2 | Arbre des composants |
| 3 | Interface : vérifiée à l'écriture, absente à l'exécution |
| 4 | Signal → lecteurs prévenus ; `signal` / `computed` / `effect` |
| 5 | `inject()` (un exemplaire) contre `new` (des copies) |
| 6 | Mise à jour d'une liste avec et sans `track` |
| 7 | Sens des trois bindings |
| 8 | `output()` : l'enfant annonce, le parent décide ; tableau des trois sens |
| 9 | Séquence `subscribe` → requête → réponse ; architecture « cassée » avec `reload()` |
| 10 | Avant / après : le service devient propriétaire |
| 11 | URL → routeur → composant dans `<router-outlet>` ; `href` contre `routerLink` |
| 12 | Séquence : chargement → signal vide → réponse → `effect` → `patchValue` une fois |
| 13 | Flux en erreur → `catchError` → `of([])` ou `EMPTY` |
| 14 | `finalize` sur les deux issues |
| 15 | La chaîne des intercepteurs, aller et retour |
| 16 | Séquence de connexion ; requête authentifiée ; garde de route |
| 17 | `input()` descend, `output()` remonte |
| 18 | Variables CSS : une définition, des usages |
| 19 | Frappe → `debounceTime` → `switchMap` qui annule |
| 20 | Trois 401 simultanés → un seul rafraîchissement partagé → rejeu |
| 21 | Cycle de vie du sondage (états) |
| 22 | Test : `TestBed` + faux serveur `HttpTestingController` |
| 23 | Choix du canal de notification (décision) |
| 24 | Gardes enchaînées ; chargement différé |
| 25 | Ordre des couches CSS ; bascule de thème |
| 26 | Une route, trois gestes (décision) |
| 27 | Curseur contre pages ; composants du fil ; droits 403 / 404 |

## Tâches

1. En-tête (carte du parcours, sommaire, Partie VI, note sur les schémas) et schémas des étapes 1 à 3.
2. Schémas des étapes 4 à 10.
3. Partie III : étapes 11 à 15, bilan de partie.
4. Partie IV : étapes 16 à 21, bilan.
5. Partie V : étapes 22 à 26, bilan.
6. Partie VI : étape 27, bilan final.
7. Vérifications : chaque ancre du sommaire correspond à un titre, chaque bloc Mermaid est fermé, chaque renvoi au support vise une section existante ; mention dans `progression-pedagogique.md`.

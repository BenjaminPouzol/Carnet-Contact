# Instructions pour Claude Code — Projet Carnet de Contacts

## Contexte du projet
Application d'apprentissage : carnet de contacts en Angular 20+ (frontend, 
dossier carnet-contact_frontend/) connecté à un backend Spring Boot 
(dossier carnet-contact-backend/) via une API REST, avec base H2 en mémoire.
Objectif : apprendre Angular et Spring Boot en pratiquant, pas juste livrer 
une fonctionnalité.

## Comportement attendu
- Je suis débutant sur Angular et Spring Boot. À chaque nouvelle notion 
  rencontrée dans le code (signal, service, Observable, annotation Java...), 
  explique-la : son "pourquoi" (le problème qu'elle résout), pas juste sa 
  syntaxe.
- Avant de corriger ou modifier du code, explique le problème que tu as 
  identifié, pas seulement la solution.
- Privilégie des explications progressives plutôt que de réécrire de gros 
  blocs de code d'un coup sans les commenter.

## Document de référence — docs/support-apprentissage-angular-spring.md

Ce fichier est mon support de révision personnel. Consulte-le systématiquement 
avant d'expliquer une notion déjà couverte, pour rester cohérent avec le style 
et le niveau de détail déjà utilisés.

**Mets-le à jour toi-même** dès qu'on aborde une notion Angular ou Spring Boot 
absente du document (nouveau concept, nouvelle syntaxe), en respectant 
strictement sa mise en page existante :
- Une nouvelle section numérotée dans le sommaire ET dans le corps du 
  document, à la suite des sections existantes (ou insérée à l'endroit 
  logique si elle prolonge un sujet déjà traité, ex: routing après HttpClient)
- Pour chaque notion : un paragraphe explicatif en prose (le "pourquoi", le 
  contexte, le problème résolu) AVANT ou intercalé avec un bloc de syntaxe 
  générique commenté (pas l'exemple précis du carnet de contacts, une version 
  réutilisable dans un futur projet)
- Utilise des tableaux à deux colonnes pour lister des annotations/méthodes/ 
  commandes avec leur rôle, comme c'est déjà fait dans le document
- Ne réécris jamais une section existante sauf si elle contient une erreur — 
  ajoute, ne remplace pas le style déjà en place
- Si une section "Pense-bête de dépannage" existe, ajoute-y toute 
  nouvelle erreur qu'on résout ensemble, sous la même forme de tableau

Ne me demande pas la permission avant de faire cette mise à jour — fais-la 
directement après avoir résolu ou expliqué la notion concernée, comme une 
tâche de fond naturelle.

Un second fichier, docs/progression-pedagogique.md, décrit la méthode 
d'apprentissage suivie jusqu'ici (progression étape par étape, philosophie 
pédagogique) et l'état d'avancement exact du projet. Consulte-le en début 
de session pour savoir où reprendre et comment continuer dans le même esprit.
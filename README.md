# Carnet de contacts

Application de gestion de contacts développée pour **apprendre Angular et Spring Boot en pratiquant**. Le dépôt regroupe le frontend, le backend et deux supports d'apprentissage (`docs/`) qui expliquent chaque notion rencontrée au fil du projet.

## Pile technique

| Côté | Technologies |
|---|---|
| Frontend | Angular 21 (composants standalone, signals, SSR), RxJS, TypeScript, PrimeNG 21 |
| Backend | Spring Boot 4.1, Spring Security, Spring Data JPA, Bean Validation, base H2 en mémoire |
| Authentification | JWT d'accès (15 min) + jeton de rafraîchissement stocké en base, mots de passe hachés en BCrypt |
| Tests | vitest côté Angular, JUnit 5 + MockMvc côté Spring Boot |

## Fonctionnalités

- **Comptes** : inscription avec politique de mot de passe, connexion, session renouvelée automatiquement
- **Carnet privé** : chaque compte ne voit que ses contacts ; recherche et pagination côté serveur ; fiche enrichie (email pro, photo, six réseaux sociaux avec leurs logos)
- **Messagerie** : échanges entre comptes, rafraîchissement périodique, accusés de lecture, réactions emoji, notifications du navigateur
- **Fil d'actualité** : chaque compte partage ses hobbies dans onze catégories (Sport, Culture, Jeu vidéo, Informatique, Actualité, Musique, Cuisine, Voyage, Nature & plein air, Créations, Autre), avec filtre par catégorie, bouton « Voir plus », réactions emoji, modification par l'auteur et modération par les administrateurs
- **Administration** : rôles, activation et suppression des comptes, avec garde-fous (jamais le dernier administrateur)
- **Interface** : thème clair et sombre, identité bleu et rouge

## Architecture

- API REST sécurisée : filtre JWT et règles d'accès centralisées côté serveur ; les comptes ne s'exposent entre eux que sous une forme publique (sans email ni rôle)
- Côté client, cinq intercepteurs HTTP : adresse du backend, jeton, indicateur de chargement, messages d'erreur, renouvellement du jeton
- État partagé par signals dans des services Angular ; gardes de route ; chargement différé de l'administration
- Pagination par numéro de page pour le carnet, par curseur pour le fil d'actualité

## Démarrage

```powershell
# Backend (port 8080)
cd carnet-contact-backend
.\mvnw.cmd spring-boot:run

# Frontend (port 4200)
cd carnet-contact_frontend
npm install
npm start
```

Ouvrir http://localhost:4200. Le **premier compte inscrit devient administrateur**.

Console H2 : http://localhost:8080/h2-console (JDBC URL : `jdbc:h2:mem:carnet`). La base est en mémoire : tout disparaît au redémarrage du backend.

## Tests

```powershell
# Backend
cd carnet-contact-backend
.\mvnw.cmd test

# Frontend
cd carnet-contact_frontend
npx ng test --watch=false
```

## Documentation

| Document | Usage |
|---|---|
| [`docs/cours-angular.md`](docs/cours-angular.md) | Le cours, à lire dans l'ordre : Angular pas à pas sur ce projet |
| [`docs/support-apprentissage-angular-spring.md`](docs/support-apprentissage-angular-spring.md) | Le support de référence, à consulter par notion |
| [`docs/progression-pedagogique.md`](docs/progression-pedagogique.md) | L'historique du projet et la méthode d'apprentissage suivie |

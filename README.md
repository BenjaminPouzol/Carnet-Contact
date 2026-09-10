Carnet de Contacts
Application de gestion de contacts développée pour apprendre Angular et Spring Boot en pratiquant. Le dépôt regroupe le frontend et le backend, ainsi qu'un support d'apprentissage détaillé (docs/) 
qui explique chaque notion rencontrée au fil du projet.

Stack technique

Côté	                              Technologies

Frontend	                          Angular 21 (standalone components, signals, SSR), RxJS, TypeScript
Backend	                            Spring Boot 4, Spring Security, Spring Data JPA, base H2 en mémoire
Authentification	                  JWT (JJWT), mots de passe hachés en BCrypt

Fonctionnalités

Comptes utilisateurs : inscription, connexion, session persistante via jeton JWT
Carnet privé : chaque utilisateur ne voit et ne modifie que ses propres contacts
Fiche contact enrichie : email personnel et professionnel, téléphone, photo de profil, liens vers six réseaux sociaux (Instagram, Twitter/X, Facebook, Twitch, YouTube, LinkedIn) avec leurs logos
Messagerie interne : échange de messages entre comptes de l'application, avec indicateur de messages non lus
Interface : thème clair, design system maison en CSS (bleu comme couleur principale, rouge réservé aux actions destructrices)

Architecture

API REST sécurisée : filtre JWT côté serveur, intercepteurs HTTP côté client (ajout du jeton, gestion centralisée des erreurs et du chargement)
État partagé par signals dans des services Angular
Routing avec garde d'authentification et rendu SSR adapté aux routes protégées

Démarrage

# Backend (port 8080)
cd carnet-contact-backend
./mvnw spring-boot:run

# Frontend (port 4200)
cd carnet-contact_frontend
npm install
npm start
Console H2 disponible sur http://localhost:8080/h2-console (JDBC URL : jdbc:h2:mem:carnet).

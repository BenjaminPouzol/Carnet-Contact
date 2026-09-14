package com.example.carnet_contact_backend.model;

/**
 * Les catégories d'une publication du fil d'actualité.
 *
 * Une énumération plutôt qu'une table : la liste est fixée par l'application,
 * pas par ses utilisateurs. Une table demanderait un écran de gestion, une clé
 * étrangère et une jointure à chaque lecture, pour une liste qui ne change
 * qu'avec une nouvelle version du code.
 *
 * Stockée par son NOM (@Enumerated(STRING) dans Publication), pour la même
 * raison que Role : insérer une valeur au milieu de la liste ne doit pas
 * changer la catégorie des publications déjà enregistrées.
 *
 * Les libellés affichés (« Jeu vidéo », « Nature & plein air ») ne sont pas ici :
 * ils vivent côté Angular, dans publication.model.ts. Le serveur décide de ce
 * qu'il ACCEPTE, l'interface de ce qu'elle AFFICHE.
 */
public enum Categorie {
    SPORT,
    CULTURE,
    JEU_VIDEO,
    INFORMATIQUE,
    ACTUALITE,
    MUSIQUE,
    CUISINE,
    VOYAGE,
    NATURE,
    CREATIONS,
    AUTRE
}

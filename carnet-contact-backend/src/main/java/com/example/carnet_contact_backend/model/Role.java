package com.example.carnet_contact_backend.model;

/**
 * Les deux rôles de l'application.
 *
 * Une énumération plutôt qu'un booléen `admin` : le jour où un troisième rôle
 * apparaît (modérateur, invité…), le booléen obligerait à tout réécrire, là où
 * l'enum ne demande qu'une valeur de plus. Et un `Role.ADMIN` se lit mieux
 * qu'un `true` dont il faut deviner ce qu'il signifie.
 *
 * Spring Security attend par convention des autorités préfixées par `ROLE_`
 * quand on utilise `hasRole("ADMIN")` — d'où la méthode ci-dessous, qui évite
 * de recopier ce préfixe un peu partout et d'oublier le souligné.
 */
public enum Role {
    UTILISATEUR,
    ADMIN;

    public String autorite() {
        return "ROLE_" + name();
    }
}

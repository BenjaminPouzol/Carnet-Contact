package com.example.carnet_contact_backend.security;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Les règles qu'un mot de passe doit respecter à l'inscription.
 *
 * Pourquoi une classe à part, alors que le contrôleur faisait déjà la
 * vérification en une ligne (`length() < 6`) ? Pour trois raisons qui
 * reviendront souvent :
 *
 * 1. La règle est devenue une VRAIE règle métier, avec cinq critères et un
 *    message par critère. Noyée dans le contrôleur, elle rendrait celui-ci
 *    illisible.
 * 2. Elle est testable sans démarrer Spring : c'est du calcul pur, sans base
 *    ni requête HTTP (voir PolitiqueMotDePasseTest).
 * 3. Elle doit exister à DEUX endroits — ici et dans le formulaire Angular.
 *    Les isoler chacun de son côté rend la comparaison possible ; mélangées au
 *    reste, les deux versions divergeraient sans qu'on s'en aperçoive.
 *
 * Point important : cette politique ne s'applique QU'À L'INSCRIPTION. Les
 * comptes déjà créés avec un mot de passe plus court continuent de fonctionner
 * — on ne peut pas les revalider, puisqu'on ne stocke que des hachés (section
 * 18) et qu'on est donc incapable de relire le mot de passe d'origine.
 */
public final class PolitiqueMotDePasse {

    public static final int LONGUEUR_MINIMALE = 10;

    /**
     * Une poignée de mots de passe parmi les plus utilisés au monde. Ils
     * passeraient certains critères de forme tout en étant les premiers essayés
     * par n'importe quelle attaque.
     *
     * C'est une liste volontairement minuscule : elle illustre la limite des
     * règles de composition plutôt qu'elle ne prétend protéger. « Motdepasse1! »
     * coche les cinq critères et reste un très mauvais mot de passe. Un vrai
     * projet brancherait ici une liste de plusieurs millions d'entrées.
     */
    private static final Set<String> TROP_COURANTS = Set.of(
            "motdepasse", "password", "azertyuiop", "qwertyuiop",
            "123456789", "1234567890", "motdepasse1", "password1",
            "azerty123", "qwerty123", "administrateur", "bonjour123",
            // Ceux-ci cochent pourtant les cinq critères de forme : c'est
            // exactement pour eux que la liste existe.
            "motdepasse1!", "password1!", "azerty123!", "qwerty123!",
            "bonjour123!", "motdepasse2026!");

    // Constructeur privé : cette classe n'est qu'un porte-méthodes, on ne
    // veut pas qu'on en crée des instances.
    private PolitiqueMotDePasse() {}

    /**
     * Le résultat de la vérification.
     *
     * On renvoie la LISTE de ce qui manque, pas un simple booléen. « Mot de
     * passe refusé » sans dire pourquoi oblige l'utilisateur à deviner ; lui
     * donner les critères non satisfaits lui permet de corriger du premier coup.
     */
    public record Resultat(boolean valide, List<String> manquants) {

        /** Le message à afficher, prêt à l'emploi. */
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
            // chars() rend le flux des caractères ; anyMatch s'arrête au
            // premier qui convient, sans parcourir toute la chaîne.
            if (motDePasse.chars().noneMatch(Character::isLowerCase)) {
                manquants.add("une minuscule");
            }
            if (motDePasse.chars().noneMatch(Character::isUpperCase)) {
                manquants.add("une majuscule");
            }
            if (motDePasse.chars().noneMatch(Character::isDigit)) {
                manquants.add("un chiffre");
            }
            // « Ni lettre ni chiffre » plutôt qu'une liste de symboles admis :
            // la liste oublierait toujours un caractère, et refuser un mot de
            // passe parce qu'il contient « £ » serait absurde.
            if (motDePasse.chars().noneMatch(c -> !Character.isLetterOrDigit(c))) {
                manquants.add("un caractère spécial");
            }
            if (TROP_COURANTS.contains(motDePasse.toLowerCase())) {
                manquants.add("un mot de passe moins courant");
            }
        }

        return new Resultat(manquants.isEmpty(), manquants);
    }
}

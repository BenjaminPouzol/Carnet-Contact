package com.example.carnet_contact_backend.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Encore un test unitaire pur : la politique est du calcul, sans base ni
 * requête. Elle s'y prête d'autant mieux qu'elle a beaucoup de cas et une seule
 * responsabilité.
 */
class PolitiqueMotDePasseTest {

    @Test
    @DisplayName("Un mot de passe qui coche tous les critères est accepté")
    void motDePasseSolide_estAccepte() {
        PolitiqueMotDePasse.Resultat resultat = PolitiqueMotDePasse.verifier("MotDeP4sse!");

        assertThat(resultat.valide()).isTrue();
        assertThat(resultat.manquants()).isEmpty();
    }

    /**
     * @ParameterizedTest rejoue la MÊME méthode pour chaque valeur de la liste.
     * Sans elle, il faudrait cinq méthodes quasi identiques — et le rapport
     * d'exécution nomme quand même chaque cas séparément, donc on sait lequel a
     * échoué.
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "Court1!",          // trop court
            "motdep4sse!",      // pas de majuscule
            "MOTDEP4SSE!",      // pas de minuscule
            "MotDePasse!",      // pas de chiffre
            "MotDeP4sse"        // pas de caractère spécial
    })
    @DisplayName("Un mot de passe auquel il manque un critère est refusé")
    void motDePasseIncomplet_estRefuse(String candidat) {
        assertThat(PolitiqueMotDePasse.verifier(candidat).valide()).isFalse();
    }

    @Test
    @DisplayName("Le refus dit précisément ce qui manque")
    void leRefus_enumereLesCriteresManquants() {
        // « tellementlong » : assez long, mais ni majuscule, ni chiffre, ni
        // caractère spécial.
        PolitiqueMotDePasse.Resultat resultat = PolitiqueMotDePasse.verifier("tellementlong");

        assertThat(resultat.manquants()).hasSize(3);
        // Un message qui dit seulement « refusé » oblige l'utilisateur à
        // deviner ; celui-ci lui permet de corriger du premier coup.
        assertThat(resultat.message())
                .contains("une majuscule", "un chiffre", "un caractère spécial");
    }

    /**
     * Le cas qui montre la LIMITE des règles de composition : « Motdepasse1! »
     * coche les cinq critères de forme et reste un très mauvais mot de passe.
     * D'où la liste des mots de passe trop courants — minuscule ici, mais qui
     * illustre le principe.
     */
    @Test
    @DisplayName("Un mot de passe trop courant est refusé malgré sa forme")
    void motDePasseCourant_estRefuse() {
        // « Motdepasse1! » : 12 caractères, minuscule, majuscule, chiffre,
        // caractère spécial. Les cinq critères de forme sont satisfaits, et
        // pourtant c'est l'un des tout premiers mots de passe essayés.
        PolitiqueMotDePasse.Resultat resultat = PolitiqueMotDePasse.verifier("Motdepasse1!");

        assertThat(resultat.valide()).isFalse();
        assertThat(resultat.manquants()).containsExactly("un mot de passe moins courant");
    }

    @Test
    @DisplayName("null est refusé sans provoquer d'erreur")
    void motDePasseNull_estRefuse() {
        // Un @RequestBody peut très bien arriver sans le champ : la politique
        // doit répondre « invalide », pas lever une NullPointerException.
        assertThat(PolitiqueMotDePasse.verifier(null).valide()).isFalse();
    }
}

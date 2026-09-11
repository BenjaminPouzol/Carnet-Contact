package com.example.carnet_contact_backend.security;

import com.example.carnet_contact_backend.model.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Le test le plus simple qui soit : aucune annotation Spring, aucun contexte,
 * aucune base. On construit la classe à la main et on vérifie ce qu'elle rend.
 *
 * C'est ce qu'on appelle un test UNITAIRE au sens strict — il examine une
 * unité de code isolée. Il démarre en quelques millisecondes, là où un test
 * qui charge le contexte Spring met plusieurs secondes. Quand une classe ne
 * dépend de rien (JwtService ne fait que des calculs), c'est la bonne forme :
 * inutile de démarrer une application entière pour vérifier une signature.
 *
 * Le constructeur est ici un avantage inattendu de l'injection par
 * constructeur : il permet de fabriquer l'objet avec les valeurs qu'on veut,
 * y compris une durée absurde impossible à obtenir autrement.
 */
class JwtServiceTest {

    private static final String SECRET = "cle-de-test-suffisamment-longue-pour-hs256-32c";

    @Test
    @DisplayName("Un jeton fraîchement émis rend l'email qu'on y a mis")
    void jetonValide_rendLEmail() {
        JwtService service = new JwtService(SECRET, 60_000);

        String jeton = service.genererJeton("alice@exemple.fr", Role.UTILISATEUR);

        assertThat(service.emailDuJeton(jeton)).isEqualTo("alice@exemple.fr");
    }

    @Test
    @DisplayName("Un jeton signé avec une autre clé est refusé")
    void jetonDUneAutreCle_rendNull() {
        JwtService emetteur = new JwtService("une-tout-autre-cle-de-32-caracteres-au-moins", 60_000);
        JwtService verificateur = new JwtService(SECRET, 60_000);

        String jetonEtranger = emetteur.genererJeton("mallory@exemple.fr", Role.UTILISATEUR);

        // C'est LE test qui justifie tout le mécanisme : n'importe qui peut
        // fabriquer un JWT, mais seule la bonne clé produit une signature que
        // notre serveur accepte.
        assertThat(verificateur.emailDuJeton(jetonEtranger)).isNull();
    }

    @Test
    @DisplayName("Un jeton dont le contenu a été modifié est refusé")
    void jetonModifie_rendNull() {
        JwtService service = new JwtService(SECRET, 60_000);
        String jeton = service.genererJeton("alice@exemple.fr", Role.UTILISATEUR);

        // On abîme un caractère de la charge utile (la partie du milieu). La
        // signature ne correspond plus au contenu.
        String[] parties = jeton.split("\\.");
        String chargeModifiee = parties[1].substring(0, parties[1].length() - 1)
                + (parties[1].endsWith("A") ? "B" : "A");
        String jetonTrafique = parties[0] + "." + chargeModifiee + "." + parties[2];

        assertThat(service.emailDuJeton(jetonTrafique)).isNull();
    }

    @Test
    @DisplayName("Un jeton expiré est refusé")
    void jetonExpire_rendNull() {
        // Durée négative : le jeton naît déjà périmé. Impossible à obtenir en
        // conditions réelles sans attendre quinze minutes — c'est exactement ce
        // qu'un test unitaire permet de faire.
        JwtService service = new JwtService(SECRET, -1_000);

        String jetonPerime = service.genererJeton("alice@exemple.fr", Role.UTILISATEUR);

        assertThat(service.emailDuJeton(jetonPerime)).isNull();
    }
}

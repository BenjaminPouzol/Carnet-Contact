package com.example.carnet_contact_backend.abonnement;

import com.example.carnet_contact_backend.model.StatutAbonnement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Les règles de visibilité entre comptes, une par test.
 *
 * Test unitaire PUR : VueRelations ne reçoit que des ensembles d'identifiants
 * déjà chargés. Pas de base, pas de contexte Spring — chaque règle se vérifie
 * en une ligne, et le tableau de la spécification se relit directement ici.
 */
class VueRelationsTest {

    private static final Long MOI = 1L;
    private static final Long ALICE = 2L;

    private static VueRelations vue(
            Map<Long, StatutAbonnement> sortants,
            Set<Long> abonnesAcceptes,
            Set<Long> mOntEcrit,
            Set<Long> bloques,
            boolean admin) {
        return new VueRelations(MOI, admin, sortants, abonnesAcceptes, mOntEcrit, bloques);
    }

    private static VueRelations sansRelation() {
        return vue(Map.of(), Set.of(), Set.of(), Set.of(), false);
    }

    /** Je suis Alice, avec ce statut. */
    private static VueRelations quiSuitAlice(StatutAbonnement statut) {
        return vue(Map.of(ALICE, statut), Set.of(), Set.of(), Set.of(), false);
    }

    // --- Statut -------------------------------------------------------------

    @Test
    @DisplayName("Sans abonnement, le statut est AUCUN")
    void statut_aucunParDefaut() {
        assertThat(sansRelation().statut(ALICE)).isEqualTo(StatutRelation.AUCUN);
        assertThat(sansRelation().suit(ALICE)).isFalse();
    }

    @Test
    @DisplayName("Une demande en attente n'est pas un abonnement")
    void statut_enAttente() {
        VueRelations vue = quiSuitAlice(StatutAbonnement.EN_ATTENTE);

        assertThat(vue.statut(ALICE)).isEqualTo(StatutRelation.EN_ATTENTE);
        assertThat(vue.suit(ALICE)).isFalse();
        assertThat(vue.suivisAcceptes()).isEmpty();
    }

    @Test
    @DisplayName("Un abonnement accepté est un abonnement")
    void statut_accepte() {
        VueRelations vue = quiSuitAlice(StatutAbonnement.ACCEPTE);

        assertThat(vue.statut(ALICE)).isEqualTo(StatutRelation.ACCEPTE);
        assertThat(vue.suit(ALICE)).isTrue();
        assertThat(vue.suivisAcceptes()).containsExactly(ALICE);
    }

    @Test
    @DisplayName("ilMeSuit lit les abonnés acceptés, pas mes propres abonnements")
    void ilMeSuit() {
        assertThat(vue(Map.of(), Set.of(ALICE), Set.of(), Set.of(), false).ilMeSuit(ALICE)).isTrue();
        assertThat(quiSuitAlice(StatutAbonnement.ACCEPTE).ilMeSuit(ALICE)).isFalse();
    }

    // --- Contenu (publications) ---------------------------------------------

    @Test
    @DisplayName("Le contenu d'un compte public est visible par tous")
    void contenuPublic_visibleParTous() {
        assertThat(sansRelation().voitContenu(ALICE, false)).isTrue();
    }

    @Test
    @DisplayName("Le contenu d'un compte privé est caché sans abonnement accepté")
    void contenuPrive_cacheSansAbonnementAccepte() {
        assertThat(sansRelation().voitContenu(ALICE, true)).isFalse();
        // Demander à suivre ne suffit pas : il faut que la demande soit acceptée.
        assertThat(quiSuitAlice(StatutAbonnement.EN_ATTENTE).voitContenu(ALICE, true)).isFalse();
    }

    @Test
    @DisplayName("Le contenu d'un compte privé est visible pour un abonné accepté")
    void contenuPrive_visiblePourAbonneAccepte() {
        assertThat(quiSuitAlice(StatutAbonnement.ACCEPTE).voitContenu(ALICE, true)).isTrue();
    }

    /** L'administrateur modère : il doit pouvoir voir ce qu'il aurait à retirer. */
    @Test
    @DisplayName("Le contenu d'un compte privé est visible pour un administrateur")
    void contenuPrive_visiblePourAdministrateur() {
        assertThat(vue(Map.of(), Set.of(), Set.of(), Set.of(), true).voitContenu(ALICE, true)).isTrue();
    }

    @Test
    @DisplayName("On voit toujours son propre contenu, même en compte privé")
    void sonPropreContenu_toujoursVisible() {
        assertThat(sansRelation().voitContenu(MOI, true)).isTrue();
    }

    // --- Coordonnées professionnelles ----------------------------------------

    /**
     * La différence avec le contenu : un compte PUBLIC ne suffit pas. Les
     * publications sont faites pour être lues, l'email pro et les réseaux ne
     * sont montrés qu'aux personnes qui ont choisi de suivre.
     */
    @Test
    @DisplayName("Les coordonnées pro exigent un abonnement accepté, même pour un compte public")
    void coordonnees_exigentUnAbonnementAccepte() {
        assertThat(sansRelation().voitCoordonnees(ALICE)).isFalse();
        assertThat(quiSuitAlice(StatutAbonnement.EN_ATTENTE).voitCoordonnees(ALICE)).isFalse();
        assertThat(quiSuitAlice(StatutAbonnement.ACCEPTE).voitCoordonnees(ALICE)).isTrue();
    }

    @Test
    @DisplayName("On voit ses propres coordonnées")
    void sesPropresCoordonnees_visibles() {
        assertThat(sansRelation().voitCoordonnees(MOI)).isTrue();
    }

    // --- Messagerie ---------------------------------------------------------

    @Test
    @DisplayName("Écrire exige de suivre la personne (abonnement accepté)")
    void ecrire_exigeUnAbonnementAccepte() {
        assertThat(sansRelation().peutEcrire(ALICE)).isFalse();
        assertThat(quiSuitAlice(StatutAbonnement.EN_ATTENTE).peutEcrire(ALICE)).isFalse();
        assertThat(quiSuitAlice(StatutAbonnement.ACCEPTE).peutEcrire(ALICE)).isTrue();
    }

    /** Sinon, Alice pourrait écrire à quelqu'un qui n'aurait aucun moyen de lui répondre. */
    @Test
    @DisplayName("On peut toujours répondre à quelqu'un qui nous a écrit")
    void ecrire_autoriseLaReponse() {
        assertThat(vue(Map.of(), Set.of(), Set.of(ALICE), Set.of(), false).peutEcrire(ALICE)).isTrue();
    }

    @Test
    @DisplayName("On ne s'écrit pas à soi-même")
    void ecrire_aSoiMeme_impossible() {
        assertThat(vue(Map.of(MOI, StatutAbonnement.ACCEPTE), Set.of(), Set.of(MOI), Set.of(), false)
                .peutEcrire(MOI)).isFalse();
    }

    // --- Blocage ------------------------------------------------------------

    /**
     * Toutes les autres conditions sont réunies — abonnement accepté, message
     * reçu, rôle administrateur — et pourtant plus rien n'est permis. Le blocage
     * l'emporte sur chacune d'elles.
     */
    @Test
    @DisplayName("Un blocage l'emporte sur tout le reste")
    void blocage_couteTout() {
        VueRelations vue = vue(
                Map.of(ALICE, StatutAbonnement.ACCEPTE), Set.of(ALICE), Set.of(ALICE), Set.of(ALICE), true);

        assertThat(vue.bloque(ALICE)).isTrue();
        assertThat(vue.voitContenu(ALICE, false)).isFalse();
        assertThat(vue.voitCoordonnees(ALICE)).isFalse();
        assertThat(vue.peutEcrire(ALICE)).isFalse();
    }
}

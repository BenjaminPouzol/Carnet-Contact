package com.example.carnet_contact_backend.abonnement;

import com.example.carnet_contact_backend.model.Abonnement;
import com.example.carnet_contact_backend.model.Blocage;
import com.example.carnet_contact_backend.model.Message;
import com.example.carnet_contact_backend.model.Role;
import com.example.carnet_contact_backend.model.StatutAbonnement;
import com.example.carnet_contact_backend.model.Utilisateur;
import com.example.carnet_contact_backend.repository.AbonnementRepository;
import com.example.carnet_contact_backend.repository.BlocageRepository;
import com.example.carnet_contact_backend.repository.MessageRepository;
import com.example.carnet_contact_backend.repository.UtilisateurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Le CHARGEMENT des relations depuis la base.
 *
 * Les règles elles-mêmes sont vérifiées dans VueRelationsTest, sans base. Ici,
 * on vérifie seulement que chaque ensemble est rempli à partir des bonnes
 * lignes — en particulier le blocage, qui doit être vu dans les deux sens.
 */
@SpringBootTest
@Transactional
class RelationsTest {

    @Autowired
    private Relations relations;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private AbonnementRepository abonnementRepository;

    @Autowired
    private BlocageRepository blocageRepository;

    @Autowired
    private MessageRepository messageRepository;

    private Utilisateur moi;
    private Utilisateur alice;
    private Utilisateur bob;
    private Utilisateur carol;
    private Utilisateur dave;

    @BeforeEach
    void preparer() {
        moi = creer("moi@exemple.fr", Role.UTILISATEUR);
        alice = creer("alice@exemple.fr", Role.UTILISATEUR);
        bob = creer("bob@exemple.fr", Role.UTILISATEUR);
        carol = creer("carol@exemple.fr", Role.UTILISATEUR);
        dave = creer("dave@exemple.fr", Role.UTILISATEUR);
    }

    private Utilisateur creer(String email, Role role) {
        Utilisateur u = new Utilisateur();
        u.setEmail(email);
        u.setMotDePasse("peu-importe");
        u.setNomAffichage(email);
        u.setRole(role);
        u.setActif(true);
        u.setDateInscription(Instant.now());
        return utilisateurRepository.save(u);
    }

    private void abonnement(Utilisateur abonne, Utilisateur suivi, StatutAbonnement statut) {
        Abonnement a = new Abonnement();
        a.setAbonne(abonne);
        a.setSuivi(suivi);
        a.setStatut(statut);
        a.setDateDemande(Instant.now());
        if (statut == StatutAbonnement.ACCEPTE) {
            a.setDateAcceptation(Instant.now());
        }
        abonnementRepository.save(a);
    }

    private void bloquer(Utilisateur bloqueur, Utilisateur bloque) {
        Blocage b = new Blocage();
        b.setBloqueur(bloqueur);
        b.setBloque(bloque);
        b.setDateBlocage(Instant.now());
        blocageRepository.save(b);
    }

    private void message(Utilisateur expediteur, Utilisateur destinataire) {
        Message m = new Message();
        m.setExpediteur(expediteur);
        m.setDestinataire(destinataire);
        m.setContenu("Bonjour");
        m.setDateEnvoi(Instant.now());
        messageRepository.save(m);
    }

    @Test
    @DisplayName("La vue d'un compte reflète ses abonnements, ses abonnés, ses messages reçus et ses blocages")
    void vuePour_chargeToutesLesRelations() {
        abonnement(moi, alice, StatutAbonnement.ACCEPTE);
        abonnement(moi, bob, StatutAbonnement.EN_ATTENTE);
        abonnement(carol, moi, StatutAbonnement.ACCEPTE);
        message(bob, moi);
        bloquer(dave, moi);

        VueRelations vue = relations.vuePour(moi);

        assertThat(vue.statut(alice.getId())).isEqualTo(StatutRelation.ACCEPTE);
        assertThat(vue.statut(bob.getId())).isEqualTo(StatutRelation.EN_ATTENTE);
        assertThat(vue.statut(carol.getId())).isEqualTo(StatutRelation.AUCUN);

        assertThat(vue.ilMeSuit(carol.getId())).isTrue();
        assertThat(vue.ilMeSuit(alice.getId())).isFalse();

        // Alice : je la suis. Bob : ma demande attend, mais il m'a écrit.
        assertThat(vue.peutEcrire(alice.getId())).isTrue();
        assertThat(vue.peutEcrire(bob.getId())).isTrue();
        assertThat(vue.peutEcrire(carol.getId())).isFalse();

        // Dave m'a bloqué : c'est LUI le bloqueur, et la vue le voit quand même.
        assertThat(vue.bloque(dave.getId())).isTrue();
    }

    @Test
    @DisplayName("Un blocage est visible depuis les deux comptes")
    void blocage_vuDesDeuxCotes() {
        bloquer(moi, alice);

        assertThat(relations.vuePour(moi).bloque(alice.getId())).isTrue();
        assertThat(relations.vuePour(alice).bloque(moi.getId())).isTrue();
    }

    @Test
    @DisplayName("Le rôle administrateur est pris sur le compte chargé")
    void administrateur_voitLesComptesPrives() {
        Utilisateur admin = creer("admin@exemple.fr", Role.ADMIN);

        assertThat(relations.vuePour(admin).voitContenu(alice.getId(), true)).isTrue();
        assertThat(relations.vuePour(moi).voitContenu(alice.getId(), true)).isFalse();
    }
}

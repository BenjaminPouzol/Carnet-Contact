package com.example.carnet_contact_backend.abonnement;

import com.example.carnet_contact_backend.model.StatutAbonnement;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Tout ce qu'un compte doit savoir de ses relations pour répondre à « qui peut
 * voir quoi » — et les règles elles-mêmes, écrites à UN SEUL endroit.
 *
 * Deux choix se lisent dans cette classe.
 *
 * 1. Les règles ne vivent pas dans les contrôleurs. Profil, messagerie, fil et
 *    notifications posent les mêmes questions (« est-il bloqué ? », « le
 *    suis-je ? ») : recopier la réponse dans chacun, c'est s'exposer à ce
 *    qu'une copie diverge. C'est la leçon des intercepteurs, appliquée côté
 *    serveur.
 *
 * 2. Les données sont chargées UNE FOIS, sous forme d'ensembles d'identifiants
 *    (voir Relations.vuePour). Afficher vingt comptes ne coûte donc pas vingt
 *    requêtes par règle : les réponses se lisent en mémoire. C'est la parade
 *    au problème « N+1 » relevé par la revue du projet.
 *
 * La classe ne dépend ni de Spring ni de la base : elle se teste en
 * construisant ses ensembles à la main (VueRelationsTest).
 */
public final class VueRelations {

    private final Long moiId;
    private final boolean admin;
    private final Map<Long, StatutAbonnement> sortants;
    private final Set<Long> abonnesAcceptes;
    private final Set<Long> mOntEcrit;
    private final Set<Long> bloques;

    /**
     * @param sortants        mes abonnements : identifiant du compte suivi → statut
     * @param abonnesAcceptes les comptes qui me suivent (abonnement accepté)
     * @param mOntEcrit       les comptes qui m'ont déjà envoyé un message
     * @param bloques         les comptes en relation de blocage avec moi, DANS UN SENS OU L'AUTRE
     */
    public VueRelations(
            Long moiId,
            boolean admin,
            Map<Long, StatutAbonnement> sortants,
            Set<Long> abonnesAcceptes,
            Set<Long> mOntEcrit,
            Set<Long> bloques) {
        this.moiId = moiId;
        this.admin = admin;
        // Copies immuables : une vue décrit un instant, personne ne doit pouvoir
        // la modifier après coup.
        this.sortants = Map.copyOf(sortants);
        this.abonnesAcceptes = Set.copyOf(abonnesAcceptes);
        this.mOntEcrit = Set.copyOf(mOntEcrit);
        this.bloques = Set.copyOf(bloques);
    }

    public Long moiId() {
        return moiId;
    }

    public boolean estMoi(Long autreId) {
        return moiId.equals(autreId);
    }

    /** Un blocage coupe tout, quel que soit celui qui a bloqué. */
    public boolean bloque(Long autreId) {
        return bloques.contains(autreId);
    }

    public StatutRelation statut(Long autreId) {
        StatutAbonnement statut = sortants.get(autreId);
        if (statut == null) {
            return StatutRelation.AUCUN;
        }
        return statut == StatutAbonnement.ACCEPTE ? StatutRelation.ACCEPTE : StatutRelation.EN_ATTENTE;
    }

    /** « Je suis ce compte » : une demande en attente ne compte pas. */
    public boolean suit(Long autreId) {
        return sortants.get(autreId) == StatutAbonnement.ACCEPTE;
    }

    public boolean ilMeSuit(Long autreId) {
        return abonnesAcceptes.contains(autreId);
    }

    /**
     * Les publications d'un auteur.
     *
     * Compte public : visibles par tous. Compte privé : par ses abonnés
     * acceptés, par lui-même, et par un administrateur — qui modère, et doit
     * pouvoir voir ce qu'il aurait à retirer.
     */
    public boolean voitContenu(Long auteurId, boolean auteurPrive) {
        if (bloque(auteurId)) {
            return false;
        }
        return estMoi(auteurId) || !auteurPrive || suit(auteurId) || admin;
    }

    /**
     * L'email professionnel et les réseaux sociaux.
     *
     * Plus strict que le contenu : un compte public ne suffit pas. Ces
     * informations ne sont montrées qu'aux personnes qui ont choisi de suivre.
     * L'email de CONNEXION, lui, n'est concerné par aucune règle : il ne figure
     * dans aucun objet montré aux autres.
     */
    public boolean voitCoordonnees(Long autreId) {
        return !bloque(autreId) && (estMoi(autreId) || suit(autreId));
    }

    /**
     * Écrire à quelqu'un : il faut le suivre — ou qu'il nous ait déjà écrit.
     *
     * La seconde condition évite une impasse : Alice suit Bob et lui écrit ;
     * sans elle, Bob ne pourrait pas répondre tant qu'il ne suit pas Alice.
     */
    public boolean peutEcrire(Long autreId) {
        return !estMoi(autreId)
                && !bloque(autreId)
                && (suit(autreId) || mOntEcrit.contains(autreId));
    }

    /** Les comptes que je suis vraiment (abonnement accepté). */
    public Set<Long> suivisAcceptes() {
        return sortants.entrySet().stream()
                .filter(e -> e.getValue() == StatutAbonnement.ACCEPTE)
                .map(Map.Entry::getKey)
                .collect(Collectors.toUnmodifiableSet());
    }

    /** Tous les comptes vers lesquels j'ai une relation sortante, demandes comprises. */
    public Set<Long> sortantsTous() {
        return sortants.keySet();
    }
}

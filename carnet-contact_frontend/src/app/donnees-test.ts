import { Message } from './message.model';
import { ReactionResume } from './reaction.model';
import { AuteurPublic, Role, Utilisateur } from './utilisateur.model';
import { Publication } from './publication.model';
import { CompteResume, NotificationCompte, ProfilPublic } from './abonnement.model';

/**
 * Fabriques d'objets pour les tests.
 *
 * Le problème qu'elles résolvent : chaque fichier de test écrivait son propre
 * `{ id: 1, email: …, nomAffichage: … }`. Le jour où `Utilisateur` a gagné
 * `role` et `actif`, les quatre copies ont cessé de compiler d'un coup — et il
 * aurait fallu les corriger une par une, à l'identique.
 *
 * Une fabrique centralise la forme de l'objet : un champ ajouté au modèle ne
 * se corrige plus qu'ICI. C'est la même logique que les variables CSS de
 * styles.css — une information écrite une seule fois se met à jour une seule
 * fois.
 *
 * `Partial<T>` en paramètre : toutes les propriétés deviennent facultatives.
 * Un test qui ne s'intéresse qu'au rôle écrit `unUtilisateur({ role: 'ADMIN' })`
 * et laisse le reste aux valeurs par défaut. Le test ne montre alors QUE ce qui
 * compte pour lui — le bruit disparaît, et l'intention saute aux yeux.
 */
export function unUtilisateur(modifications: Partial<Utilisateur> = {}): Utilisateur {
  return {
    id: 1,
    email: 'alice@exemple.fr',
    nomAffichage: 'Alice',
    role: 'UTILISATEUR' as Role,
    actif: true,
    // Ajouté avec les abonnements : la fabrique en a profité exactement comme
    // prévu — une ligne ici, aucun fichier de test à reprendre.
    comptePrive: false,
    ...modifications
  };
}

/** Un compte tel que les AUTRES le voient : sans email ni rôle. */
export function unAuteur(modifications: Partial<AuteurPublic> = {}): AuteurPublic {
  return { id: 2, nomAffichage: 'Bob', photoUrl: null, ...modifications };
}

export function unMessage(modifications: Partial<Message> = {}): Message {
  return {
    id: 1,
    expediteur: unAuteur({ id: 2, nomAffichage: 'Bob' }),
    destinataire: unAuteur({ id: 1, nomAffichage: 'Alice' }),
    contenu: 'Salut',
    dateEnvoi: '2026-09-11T10:00:00Z',
    lu: false,
    reactions: [],
    ...modifications
  };
}

export function uneReaction(modifications: Partial<ReactionResume> = {}): ReactionResume {
  return { emoji: '👍', nombre: 1, parMoi: false, ...modifications };
}

export function unePublication(modifications: Partial<Publication> = {}): Publication {
  return {
    id: 1,
    auteur: unAuteur(),
    categorie: 'SPORT',
    contenu: 'Sortie vélo dimanche',
    imageUrl: null,
    datePublication: '2026-09-14T10:00:00Z',
    dateModification: null,
    reactions: [],
    modifiable: false,
    supprimable: false,
    ...modifications
  };
}

/** Un compte dans une liste de recherche ou d'abonnements. */
export function unCompte(modifications: Partial<CompteResume> = {}): CompteResume {
  return {
    id: 2,
    nomAffichage: 'Bob',
    photoUrl: null,
    comptePrive: false,
    statut: 'AUCUN',
    ilMeSuit: false,
    ...modifications
  };
}

/** Le profil public d'une personne, par défaut non suivie. */
export function unProfil(modifications: Partial<ProfilPublic> = {}): ProfilPublic {
  return {
    id: 2,
    nomAffichage: 'Bob',
    photoUrl: null,
    comptePrive: false,
    statut: 'AUCUN',
    ilMeSuit: false,
    nombreAbonnes: 0,
    nombreAbonnements: 0,
    contenuVisible: true,
    peutEcrire: false,
    coordonnees: null,
    ...modifications
  };
}

export function uneNotification(modifications: Partial<NotificationCompte> = {}): NotificationCompte {
  return {
    id: 1,
    type: 'NOUVEL_ABONNE',
    acteur: unAuteur({ id: 3, nomAffichage: 'Alice' }),
    date: '2026-09-15T10:00:00Z',
    lue: false,
    ...modifications
  };
}

import { AuteurPublic } from './utilisateur.model';

/**
 * Ma relation vers un autre compte, écrite comme l'enum Java StatutRelation.
 * « AUCUN » existe pour que le bouton ait toujours un état à afficher, sans
 * `null` à tester.
 */
export type StatutRelation = 'AUCUN' | 'EN_ATTENTE' | 'ACCEPTE';

/** Un compte dans une liste (recherche, abonnements, abonnés, demandes). */
export interface CompteResume {
  id: number;
  nomAffichage: string;
  photoUrl?: string | null;
  comptePrive: boolean;
  // Ma relation vers lui, et la sienne vers moi : deux questions distinctes.
  statut: StatutRelation;
  ilMeSuit: boolean;
}

/** Ce qu'un compte montre à ceux qui le suivent. Pas d'email de connexion. */
export interface CoordonneesPro {
  emailPro: string | null;
  instagram: string | null;
  twitter: string | null;
  facebook: string | null;
  twitch: string | null;
  youtube: string | null;
  linkedin: string | null;
}

/**
 * La page d'une personne. `contenuVisible`, `peutEcrire` et `coordonnees` sont
 * calculés par le serveur pour celui qui regarde : le gabarit n'a qu'à suivre.
 */
export interface ProfilPublic {
  id: number;
  nomAffichage: string;
  photoUrl?: string | null;
  comptePrive: boolean;
  statut: StatutRelation;
  ilMeSuit: boolean;
  nombreAbonnes: number;
  nombreAbonnements: number;
  contenuVisible: boolean;
  peutEcrire: boolean;
  // null tant qu'on ne suit pas la personne : le serveur ne les envoie pas.
  coordonnees: CoordonneesPro | null;
}

/** `enCommun` : combien de mes abonnements suivent déjà ce compte. */
export interface Suggestion {
  compte: CompteResume;
  enCommun: number;
}

export type TypeNotification = 'NOUVEL_ABONNE' | 'DEMANDE_RECUE' | 'DEMANDE_ACCEPTEE';

export interface NotificationCompte {
  id: number;
  type: TypeNotification;
  acteur: AuteurPublic;
  date: string;
  lue: boolean;
}

/** Une personne de la colonne « Conversations » de la messagerie. */
export interface Interlocuteur {
  compte: AuteurPublic;
  peutEcrire: boolean;
}

/**
 * Une phrase par type de notification.
 *
 * `Record<TypeNotification, …>` plutôt qu'un switch : TypeScript EXIGE une
 * entrée pour chacun des trois types. Le jour où le serveur en ajoute un
 * quatrième (et qu'on l'ajoute au type ci-dessus), l'oubli de sa phrase est une
 * erreur de compilation, pas un texte vide à l'écran.
 */
const PHRASES: Record<TypeNotification, (nom: string) => string> = {
  NOUVEL_ABONNE: nom => `${nom} vous suit`,
  DEMANDE_RECUE: nom => `${nom} demande à vous suivre`,
  DEMANDE_ACCEPTEE: nom => `${nom} a accepté votre demande`
};

export function texteNotification(notification: NotificationCompte): string {
  return PHRASES[notification.type](notification.acteur.nomAffichage);
}

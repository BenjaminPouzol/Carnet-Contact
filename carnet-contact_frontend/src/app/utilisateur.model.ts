/**
 * Les deux rôles, écrits exactement comme l'enum Java les sérialise.
 *
 * Un type UNION plutôt qu'un simple `string` : TypeScript refusera une faute
 * de frappe comme `'ADMINISTRATEUR'` à la compilation, au lieu de laisser une
 * comparaison silencieusement fausse passer en production.
 */
export type Role = 'UTILISATEUR' | 'ADMIN';

export interface Utilisateur {
  id: number;
  email: string;
  nomAffichage: string;
  photoUrl?: string | null;
  role: Role;
  actif: boolean;
  dateInscription?: string | null;
}

/**
 * Ce qu'un compte laisse voir de lui aux AUTRES : ni email, ni rôle, ni état.
 *
 * C'est la forme que renvoient désormais la liste des interlocuteurs, les
 * messages et le fil d'actualité. Le compte complet (`Utilisateur`) ne sort
 * plus que pour soi-même (`/api/utilisateurs/moi`) et pour l'administration.
 */
export interface AuteurPublic {
  id: number;
  nomAffichage: string;
  photoUrl?: string | null;
}

/**
 * Une ligne du tableau d'administration : le compte, plus des compteurs qui
 * n'existent pas dans l'entité (ils se calculent dans d'autres tables).
 */
export interface LigneCompte {
  id: number;
  email: string;
  nomAffichage: string;
  photoUrl?: string | null;
  role: Role;
  actif: boolean;
  dateInscription?: string | null;
  nombreContacts: number;
  nombreMessages: number;
  nombrePublications: number;
  // Calculé par le serveur : évite au client de comparer des identifiants
  // pour savoir quelles actions griser sur sa propre ligne.
  estMoi: boolean;
}

// Ce que renvoient /api/auth/connexion, /api/auth/inscription et
// /api/auth/rafraichir : deux jetons, plus le compte.
export interface ReponseAuth {
  // Jeton d'accès, court (15 min), envoyé dans l'en-tête Authorization.
  jeton: string;
  // Jeton de rafraîchissement, long (7 j), qui ne sert qu'à obtenir le suivant.
  jetonRafraichissement: string;
  utilisateur: Utilisateur;
}

export interface Utilisateur {
  id: number;
  email: string;
  nomAffichage: string;
  photoUrl?: string | null;
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

export interface Utilisateur {
  id: number;
  email: string;
  nomAffichage: string;
  photoUrl?: string | null;
}

// Ce que renvoie /api/auth/connexion et /api/auth/inscription.
export interface ReponseAuth {
  jeton: string;
  utilisateur: Utilisateur;
}

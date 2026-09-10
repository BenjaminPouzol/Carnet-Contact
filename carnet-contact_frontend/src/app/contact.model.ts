export interface Contact {
  id: number;
  nom: string;
  prenom: string;
  email: string;
  telephone?: string;

  // Champs optionnels : le « ? » dit à TypeScript que la propriété peut être
  // absente. Le backend renvoie null quand elle n'est pas renseignée.
  emailPro?: string | null;
  photoUrl?: string | null;

  instagram?: string | null;
  twitter?: string | null;
  facebook?: string | null;
  twitch?: string | null;
  youtube?: string | null;
  linkedin?: string | null;
}

/**
 * Les six réseaux gérés, décrits une seule fois ici.
 *
 * Sans cette liste, il faudrait répéter six fois le même bloc de gabarit et
 * six fois le même champ de formulaire. En la parcourant avec @for, on écrit
 * l'affichage une fois — et ajouter un septième réseau devient une ligne à
 * ajouter dans ce tableau.
 *
 * `as const` : TypeScript traite le tableau comme figé, ce qui permet d'en
 * déduire le type exact des clés au lieu du vague « string ».
 */
export const RESEAUX = [
  { cle: 'instagram', nom: 'Instagram', couleur: '#E1306C' },
  { cle: 'twitter', nom: 'Twitter / X', couleur: '#1DA1F2' },
  { cle: 'facebook', nom: 'Facebook', couleur: '#1877F2' },
  { cle: 'twitch', nom: 'Twitch', couleur: '#9146FF' },
  { cle: 'youtube', nom: 'YouTube', couleur: '#FF0000' },
  { cle: 'linkedin', nom: 'LinkedIn', couleur: '#0A66C2' }
] as const;

// Le type « une des six clés » : 'instagram' | 'twitter' | ... Déduit du
// tableau ci-dessus, donc toujours à jour.
export type CleReseau = typeof RESEAUX[number]['cle'];

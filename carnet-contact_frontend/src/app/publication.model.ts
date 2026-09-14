import { AuteurPublic } from './utilisateur.model';
import { ReactionResume } from './reaction.model';

/**
 * Les catégories du fil, décrites une seule fois — même patron que RESEAUX.
 *
 * `cle` est écrite exactement comme l'enum Java `Categorie` la sérialise. Le
 * serveur décide des valeurs ACCEPTÉES ; ce tableau décide de leur AFFICHAGE :
 * libellé, emoji et couleur. Il sert à la fois aux pastilles de filtre, au menu
 * du formulaire et à l'étiquette de chaque carte — ajouter une catégorie ici
 * (et dans l'enum) suffit à la faire apparaître partout.
 */
export const CATEGORIES = [
  { cle: 'SPORT', libelle: 'Sport', emoji: '⚽', couleur: '#16a34a' },
  { cle: 'CULTURE', libelle: 'Culture', emoji: '🎭', couleur: '#9333ea' },
  { cle: 'JEU_VIDEO', libelle: 'Jeu vidéo', emoji: '🎮', couleur: '#4f46e5' },
  { cle: 'INFORMATIQUE', libelle: 'Informatique', emoji: '💻', couleur: '#0891b2' },
  { cle: 'ACTUALITE', libelle: 'Actualité', emoji: '📰', couleur: '#b45309' },
  { cle: 'MUSIQUE', libelle: 'Musique', emoji: '🎵', couleur: '#db2777' },
  { cle: 'CUISINE', libelle: 'Cuisine', emoji: '🍳', couleur: '#ea580c' },
  { cle: 'VOYAGE', libelle: 'Voyage', emoji: '✈️', couleur: '#0284c7' },
  { cle: 'NATURE', libelle: 'Nature & plein air', emoji: '🌿', couleur: '#65a30d' },
  { cle: 'CREATIONS', libelle: 'Créations', emoji: '🎨', couleur: '#c026d3' },
  { cle: 'AUTRE', libelle: 'Autre', emoji: '💬', couleur: '#64748b' }
] as const;

/** « Une des onze clés », déduit du tableau : les deux ne peuvent pas diverger. */
export type Categorie = typeof CATEGORIES[number]['cle'];

export interface Publication {
  id: number;
  auteur: AuteurPublic;
  categorie: Categorie;
  contenu: string;
  imageUrl: string | null;
  // Instant Java sérialisé en chaîne ISO 8601, comme dateEnvoi des messages.
  datePublication: string;
  dateModification: string | null;
  reactions: ReactionResume[];
  // Calculés par le serveur pour le compte qui regarde : le gabarit n'a ni à
  // comparer des identifiants, ni à connaître les règles de droits.
  modifiable: boolean;
  supprimable: boolean;
}

/**
 * Une tranche du fil.
 *
 * `curseurSuivant` : l'identifiant à renvoyer pour obtenir la suite, ou null
 * quand il n'y a plus rien de plus ancien. Pas de total ni de nombre de pages :
 * un fil par curseur n'en a pas besoin (section 34).
 */
export interface PageFil {
  publications: Publication[];
  curseurSuivant: number | null;
}

/** Ce que le formulaire envoie pour créer ou modifier une publication. */
export interface DemandePublication {
  categorie: Categorie;
  contenu: string;
  imageUrl: string;
}

/** La description complète d'une catégorie à partir de sa clé. */
export function categorieDe(cle: Categorie) {
  // Le `!` est sûr : le type Categorie garantit que la clé figure dans le tableau.
  return CATEGORIES.find(categorie => categorie.cle === cle)!;
}

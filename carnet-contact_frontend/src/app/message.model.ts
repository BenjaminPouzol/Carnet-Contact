import { Utilisateur } from './utilisateur.model';

/**
 * Les réactions d'un message, REGROUPÉES par emoji.
 *
 * Le serveur envoie « 👍 3, dont la mienne » plutôt que la liste nominative :
 * c'est tout ce que l'affichage demande, et `parMoi` dépend de qui regarde —
 * une information que seul le serveur peut calculer.
 */
export interface ReactionResume {
  emoji: string;
  nombre: number;
  parMoi: boolean;
}

export interface Message {
  id: number;
  expediteur: Utilisateur;
  destinataire: Utilisateur;
  contenu: string;
  // Le backend sérialise un Instant Java en chaîne ISO 8601
  // ("2026-09-10T14:25:45.990Z") : côté TypeScript c'est un string, qu'on
  // convertit en Date seulement au moment de l'afficher.
  dateEnvoi: string;
  // L'accusé de lecture : passe à true quand le destinataire ouvre le fil.
  lu: boolean;
  reactions: ReactionResume[];
}

/**
 * Les emojis proposés par la barre de réaction.
 *
 * La même liste existe côté serveur, qui refuse tout ce qui n'y figure pas :
 * celle-ci décide de ce qu'on AFFICHE, celle du serveur de ce qu'on ACCEPTE.
 * Même partage des rôles que pour la politique de mot de passe (section 27).
 */
export const EMOJIS_REACTION = ['👍', '❤️', '😂', '😮', '😢'] as const;

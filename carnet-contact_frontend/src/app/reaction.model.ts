/**
 * Les réactions emoji, communes aux messages et aux publications du fil.
 *
 * Elles vivaient dans message.model.ts tant que seuls les messages en avaient.
 * Dès qu'un second modèle en a besoin, les laisser là obligerait le fil à
 * importer « un morceau de la messagerie » — une dépendance qui ne veut rien
 * dire.
 */

/**
 * Les réactions d'un élément, REGROUPÉES par emoji.
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

/**
 * Les emojis proposés par la barre de réaction.
 *
 * La même liste existe côté serveur, qui refuse tout ce qui n'y figure pas :
 * celle-ci décide de ce qu'on AFFICHE, celle du serveur de ce qu'on ACCEPTE.
 * Même partage des rôles que pour la politique de mot de passe (section 27).
 */
export const EMOJIS_REACTION = ['👍', '❤️', '😂', '😮', '😢'] as const;

import { Message, ReactionResume } from './message.model';
import { Role, Utilisateur } from './utilisateur.model';

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
    ...modifications
  };
}

export function unMessage(modifications: Partial<Message> = {}): Message {
  return {
    id: 1,
    expediteur: unUtilisateur({ id: 2, email: 'bob@exemple.fr', nomAffichage: 'Bob' }),
    destinataire: unUtilisateur(),
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

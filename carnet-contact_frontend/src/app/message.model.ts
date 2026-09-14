import { AuteurPublic } from './utilisateur.model';
import { ReactionResume } from './reaction.model';

export interface Message {
  id: number;
  // AuteurPublic et non Utilisateur : le serveur n'envoie plus l'email ni le
  // rôle de l'autre personne, seulement de quoi afficher son nom et sa photo.
  expediteur: AuteurPublic;
  destinataire: AuteurPublic;
  contenu: string;
  // Le backend sérialise un Instant Java en chaîne ISO 8601
  // ("2026-09-10T14:25:45.990Z") : côté TypeScript c'est un string, qu'on
  // convertit en Date seulement au moment de l'afficher.
  dateEnvoi: string;
  // L'accusé de lecture : passe à true quand le destinataire ouvre le fil.
  lu: boolean;
  reactions: ReactionResume[];
}

import { Utilisateur } from './utilisateur.model';

export interface Message {
  id: number;
  expediteur: Utilisateur;
  destinataire: Utilisateur;
  contenu: string;
  // Le backend sérialise un Instant Java en chaîne ISO 8601
  // ("2026-09-10T14:25:45.990Z") : côté TypeScript c'est un string, qu'on
  // convertit en Date seulement au moment de l'afficher.
  dateEnvoi: string;
  lu: boolean;
}

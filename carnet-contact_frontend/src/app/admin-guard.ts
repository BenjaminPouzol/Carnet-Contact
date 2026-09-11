import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { SessionService } from './services/session';

/**
 * Réserve une route aux administrateurs.
 *
 * Même avertissement qu'`authGuard`, et il compte encore plus ici : une garde
 * n'est PAS une sécurité. Celle-ci se contente de lire un champ du compte
 * mémorisé dans le navigateur — quelqu'un pourrait le modifier à la main et
 * afficher la page. Il n'en tirerait rien : le serveur refuse toutes les
 * routes /api/admin/** à qui n'a pas le rôle dans son jeton SIGNÉ, et une
 * signature ne se falsifie pas (section 18).
 *
 * La garde sert donc à ce qu'elle sait faire : éviter d'afficher un écran vide
 * rempli d'erreurs 403 à quelqu'un qui n'avait rien à y faire.
 */
export const adminGuard: CanActivateFn = () => {
  const session = inject(SessionService);
  const router = inject(Router);

  if (session.estAdmin()) {
    return true;
  }

  // Vers l'accueil et non vers la connexion : la personne EST connectée, elle
  // n'a simplement pas ce droit. L'envoyer vers un formulaire de connexion
  // laisserait croire à un problème d'identité.
  router.navigate(['/']);
  return false;
};

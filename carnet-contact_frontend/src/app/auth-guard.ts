import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { SessionService } from './services/session';

/**
 * Une GARDE de route : une fonction qu'Angular appelle AVANT d'activer une
 * route, et qui répond true (on passe) ou false (on bloque).
 *
 * Le problème qu'elle résout : sans elle, taper /contact/3 dans la barre
 * d'adresse sans être connecté afficherait une page vide et une bannière
 * d'erreur 401 — techniquement correct, mais incompréhensible. La garde
 * redirige vers la connexion avant même de construire le composant.
 *
 * À retenir : une garde n'est PAS une sécurité. Elle améliore l'expérience,
 * mais n'importe qui peut appeler l'API directement. La seule protection
 * réelle est côté serveur (Spring Security).
 */
export const authGuard: CanActivateFn = () => {
  const session = inject(SessionService);
  const router = inject(Router);

  if (session.connecte()) {
    return true;
  }

  // Rediriger plutôt que de renvoyer false sèchement : l'utilisateur atterrit
  // sur un écran qui lui dit quoi faire.
  router.navigate(['/connexion']);
  return false;
};

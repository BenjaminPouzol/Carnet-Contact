import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { SessionService } from '../services/session';

/**
 * Ajoute le jeton d'authentification à chaque requête sortante.
 *
 * C'est exactement le cas d'usage décrit en section 17 comme « le plus connu
 * des intercepteurs » — cette fois pour de vrai. Sans lui, il faudrait penser
 * à passer l'en-tête à la main dans chacun des appels de chacun des services.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  // SessionService et non AuthService : ce dernier injecte HttpClient, ce qui
  // recréerait la boucle de dépendances évitée en section 17.
  const jeton = inject(SessionService).jetonActuel();

  // Pas de jeton (visiteur non connecté, ou appel à /api/auth) : on laisse
  // passer tel quel. Envoyer "Bearer null" ferait échouer la requête.
  if (!jeton) {
    return next(req);
  }

  const requeteAuthentifiee = req.clone({
    setHeaders: { Authorization: `Bearer ${jeton}` }
  });

  return next(requeteAuthentifiee);
};

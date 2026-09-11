import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { SessionService } from '../services/session';
import { RafraichissementService } from '../services/rafraichissement';

/**
 * Rattrape les 401 dus à un jeton d'accès expiré : demande un jeton neuf, puis
 * REJOUE la requête qui avait échoué.
 *
 * Le bénéfice se mesure du point de vue de l'utilisateur : il clique sur
 * « Enregistrer » quinze minutes après s'être connecté, et… ça marche. Sans
 * cet intercepteur, il serait éjecté vers la page de connexion en perdant sa
 * saisie.
 *
 * Sa place dans la chaîne est essentielle. Il est enregistré EN DERNIER, donc
 * c'est le maillon le plus profond : à l'aller la requête le traverse en
 * dernier, au retour l'erreur l'atteint en PREMIER — avant erreurInterceptor,
 * qui déconnecte sur 401. Inversé, la déconnexion se produirait avant que la
 * tentative de renouvellement ait eu lieu.
 */
export const rafraichissementInterceptor: HttpInterceptorFn = (req, next) => {
  const session = inject(SessionService);
  const rafraichissement = inject(RafraichissementService);

  // Les appels d'authentification ne se rejouent pas. /rafraichir surtout :
  // il se rappellerait lui-même à l'infini sur un 401.
  if (req.url.includes('/api/auth/')) {
    return next(req);
  }

  return next(req).pipe(
    catchError((erreur: HttpErrorResponse) => {
      const rejouable = erreur.status === 401
        && session.jetonRafraichissementActuel() !== null;

      if (!rejouable) {
        return throwError(() => erreur);
      }

      return rafraichissement.obtenirNouveauJeton().pipe(
        // switchMap : remplacer un flux par un autre. Ici « quand le nouveau
        // jeton arrive, abandonne ce flux-ci et continue avec celui de la
        // requête rejouée ». C'est ce qui permet à l'appelant d'origine de
        // recevoir la vraie réponse, sans savoir qu'un détour a eu lieu.
        switchMap(nouveauJeton => next(req.clone({
          // La requête qui arrive ici porte déjà l'ancien en-tête, posé par
          // authInterceptor plus haut dans la chaîne. setHeaders l'écrase.
          setHeaders: { Authorization: `Bearer ${nouveauJeton}` }
        }))),

        // Le rafraîchissement a échoué : on relance l'erreur 401 D'ORIGINE,
        // pas celle du rafraîchissement. C'est elle qui a du sens pour la
        // suite de la chaîne — erreurInterceptor y verra un 401 et fera son
        // travail de déconnexion.
        catchError(() => throwError(() => erreur))
      );
    })
  );
};

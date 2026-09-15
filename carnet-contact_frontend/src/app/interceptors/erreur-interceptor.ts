import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { EtatHttpService } from '../services/etat-http';
import { SessionService } from '../services/session';
import { DISCRET, LIBELLE_ACTION } from './http-contexte';

/**
 * La RAISON technique de l'échec, formulée comme un fragment de phrase (pas de
 * majuscule, pas de point) : elle est destinée à être recollée derrière le
 * libellé métier.
 */
function raisonTechnique(erreur: HttpErrorResponse): string {
  switch (erreur.status) {
    // status 0 : la réponse n'est jamais arrivée (serveur éteint, réseau
    // coupé, CORS refusé). Ce n'est pas un code renvoyé par le serveur,
    // c'est l'absence de serveur.
    case 0:
      return 'le serveur est injoignable';
    case 400:
      return 'la requête a été refusée (400)';
    case 401:
      return 'la session a expiré';
    case 403:
      return "l'accès est refusé (403)";
    case 404:
      return 'la ressource est introuvable (404)';
    case 409:
      return 'une donnée en conflit existe déjà (409)';
    // Les deux refus propres à l'envoi d'un fichier : trop gros, ou pas une
    // image acceptée (le serveur lit les premiers octets, pas le nom).
    case 413:
      return 'l\'image dépasse la taille autorisée (413)';
    case 415:
      return 'ce format de fichier n\'est pas accepté (415)';
    case 500:
      return 'le serveur a rencontré une erreur interne (500)';
    default:
      return `une erreur inattendue s'est produite (${erreur.status})`;
  }
}

/**
 * Assemble le message affiché.
 *
 * Avec libellé  : « Impossible d'ajouter le contact : le serveur est injoignable. »
 * Sans libellé  : « Le serveur est injoignable. »
 *
 * Le service fournit la moitié qu'il est seul à connaître (l'intention),
 * l'intercepteur fournit celle qu'il est seul à connaître (le statut). Aucun
 * des deux ne fait le travail de l'autre.
 */
function messagePour(erreur: HttpErrorResponse, libelle: string | null): string {
  const raison = raisonTechnique(erreur);

  if (libelle) {
    return `${libelle} : ${raison}.`;
  }

  return raison.charAt(0).toUpperCase() + raison.slice(1) + '.';
}

/**
 * Renseigne le message d'erreur transverse, puis RELANCE l'erreur.
 *
 * Depuis l'ajout de l'authentification, il a une seconde mission : réagir au
 * 401. Un jeton a une durée de vie ; quand il expire, TOUTES les requêtes se
 * mettent à échouer. Traiter ce cas dans chaque service serait la même
 * duplication que celle qui a motivé les intercepteurs.
 */
export const erreurInterceptor: HttpInterceptorFn = (req, next) => {
  const etatHttp = inject(EtatHttpService);
  const session = inject(SessionService);
  const router = inject(Router);

  // Les appels d'authentification affichent leur erreur DANS leur formulaire
  // (« mot de passe incorrect » à côté du champ concerné). Doubler ce message
  // d'une bannière en haut de page serait redondant.
  const estAppelAuth = req.url.includes('/api/auth/');

  // Requête de fond : elle ne doit ni effacer un message affiché, ni en poser.
  const estDiscret = req.context.get(DISCRET);

  // Nouvelle requête = on repart d'un état sain. Remplace le
  // `erreurSignal.set(null)` qui ouvrait chaque méthode du service.
  if (!estDiscret) {
    etatHttp.effacerErreur();
  }

  return next(req).pipe(
    catchError((erreur: HttpErrorResponse) => {
      // Un 401 qui arrive JUSQU'ICI veut dire que le rafraîchissement du jeton
      // a déjà échoué (l'intercepteur de rafraîchissement est placé plus bas
      // dans la chaîne, il voit donc l'erreur en premier). Il n'y a plus rien à
      // tenter : on vide la session et on renvoie vers la connexion.
      if (erreur.status === 401 && session.jetonActuel() !== null) {
        session.vider();
        router.navigate(['/connexion']);
      }

      if (!estAppelAuth && !estDiscret) {
        etatHttp.signalerErreur(messagePour(erreur, req.context.get(LIBELLE_ACTION)));
      }

      // Le point important : on ne rend PAS de valeur de repli ici.
      // throwError relance l'erreur telle quelle, pour que le catchError du
      // service appelant continue de faire son travail. L'intercepteur
      // décide du MESSAGE, le service décide de la VALEUR DE REPLI (of([])
      // pour une lecture, EMPTY pour une écriture) — lui seul sait laquelle a
      // du sens.
      return throwError(() => erreur);
    })
  );
};

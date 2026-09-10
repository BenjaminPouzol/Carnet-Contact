import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { EtatHttpService } from '../services/etat-http';

/**
 * Traduit une erreur HTTP en message lisible. Un intercepteur ne connaît pas
 * l'intention métier de la requête (« ajouter un contact »), mais il connaît
 * son code de statut — et c'est souvent l'information la plus utile.
 */
function messagePour(erreur: HttpErrorResponse): string {
  switch (erreur.status) {
    // status 0 : la réponse n'est jamais arrivée (serveur éteint, réseau
    // coupé, CORS refusé). Ce n'est pas un code renvoyé par le serveur,
    // c'est l'absence de serveur.
    case 0:
      return 'Serveur injoignable. Est-il bien démarré ?';
    case 400:
      return 'Requête invalide (400).';
    case 404:
      return 'Ressource introuvable (404).';
    case 500:
      return 'Erreur interne du serveur (500).';
    default:
      return `Erreur inattendue (${erreur.status}).`;
  }
}

/**
 * Renseigne le message d'erreur transverse, puis RELANCE l'erreur.
 */
export const erreurInterceptor: HttpInterceptorFn = (req, next) => {
  const etatHttp = inject(EtatHttpService);

  // Nouvelle requête = on repart d'un état sain. Remplace le
  // `erreurSignal.set(null)` qui ouvrait chaque méthode du service.
  etatHttp.effacerErreur();

  return next(req).pipe(
    catchError((erreur: HttpErrorResponse) => {
      etatHttp.signalerErreur(messagePour(erreur));

      // Le point important : on ne rend PAS de valeur de repli ici.
      // throwError relance l'erreur telle quelle, pour que le catchError du
      // service appelant continue de faire son travail. L'intercepteur
      // décide du MESSAGE (générique), le service décide de la VALEUR DE
      // REPLI (of([]) pour une lecture, EMPTY pour une écriture) — lui seul
      // sait laquelle a du sens.
      return throwError(() => erreur);
    })
  );
};

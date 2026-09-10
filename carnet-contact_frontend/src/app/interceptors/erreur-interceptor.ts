import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { EtatHttpService } from '../services/etat-http';
import { SessionService } from '../services/session';

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
    case 401:
      return 'Session expirée. Reconnectez-vous.';
    case 403:
      return 'Accès refusé (403).';
    case 404:
      return 'Ressource introuvable (404).';
    case 409:
      return 'Conflit avec une donnée existante (409).';
    case 500:
      return 'Erreur interne du serveur (500).';
    default:
      return `Erreur inattendue (${erreur.status}).`;
  }
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

  // Nouvelle requête = on repart d'un état sain. Remplace le
  // `erreurSignal.set(null)` qui ouvrait chaque méthode du service.
  etatHttp.effacerErreur();

  return next(req).pipe(
    catchError((erreur: HttpErrorResponse) => {
      // Un 401 alors qu'on AVAIT un jeton = ce jeton n'est plus valable.
      // On vide la session et on renvoie vers la connexion. La condition sur
      // le jeton est essentielle : sans elle, un simple mot de passe erroné
      // sur la page de connexion (401 aussi) déclencherait une redirection
      // vers... la page de connexion.
      if (erreur.status === 401 && session.jetonActuel() !== null) {
        session.vider();
        router.navigate(['/connexion']);
      }

      if (!estAppelAuth) {
        etatHttp.signalerErreur(messagePour(erreur));
      }

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

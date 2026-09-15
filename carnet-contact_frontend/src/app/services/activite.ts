import { Injectable, PLATFORM_ID, inject, signal } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { EMPTY, Observable, Subscription, catchError, switchMap, timer } from 'rxjs';
import { NotificationCompte, texteNotification } from '../abonnement.model';
import { contexte } from '../interceptors/http-contexte';
import { NotificationService } from './notification';

/** Même rythme que la pastille des messages : une information d'ambiance. */
const INTERVALLE_MS = 15000;

/**
 * L'activité autour de son compte : nouveaux abonnés, demandes reçues,
 * demandes acceptées.
 *
 * C'est le patron du sondage des messages non lus (MessageService), repris
 * presque à l'identique — et c'est voulu : les mêmes problèmes appellent les
 * mêmes solutions.
 * - rien ne démarre côté serveur (SSR), où un timer empêcherait le rendu de
 *   se terminer ;
 * - on ne démarre jamais deux sondages ;
 * - on arrête tout à la déconnexion, sinon un 401 toutes les 15 secondes ;
 * - le serveur renvoie à chaque tour TOUTES les non-lues : on garde en mémoire
 *   ce qui a déjà été annoncé, et le premier tour observe sans rien annoncer.
 */
@Injectable({
  providedIn: 'root'
})
export class ActiviteService {
  private http = inject(HttpClient);
  private notifications = inject(NotificationService);
  private navigateur = isPlatformBrowser(inject(PLATFORM_ID));
  private apiUrl = '/api/notifications';

  private nonLuesSignal = signal<NotificationCompte[]>([]);
  readonly nonLues = this.nonLuesSignal.asReadonly();

  private suivi?: Subscription;
  private dejaVues = new Set<number>();
  private premierTour = true;

  demarrerSuivi(): void {
    if (!this.navigateur || this.suivi) {
      return;
    }

    this.suivi = timer(0, INTERVALLE_MS).pipe(
      switchMap(() => this.http.get<NotificationCompte[]>(`${this.apiUrl}/non-lues`, {
        // Toujours discret : personne n'a demandé cette requête.
        context: contexte({ discret: true })
      }).pipe(
        // À l'intérieur du switchMap : une coupure réseau ne doit pas arrêter
        // le sondage pour le reste de la session.
        catchError(() => EMPTY)
      ))
    ).subscribe(liste => {
      this.annoncerLesNouvelles(liste);
      this.nonLuesSignal.set(liste);
    });
  }

  arreterSuivi(): void {
    this.suivi?.unsubscribe();
    this.suivi = undefined;
    this.nonLuesSignal.set([]);
    this.dejaVues.clear();
    this.premierTour = true;
  }

  /** L'historique (lues comprises), pour la page Abonnements. */
  recentes(): Observable<NotificationCompte[]> {
    return this.http.get<NotificationCompte[]>(this.apiUrl, {
      context: contexte({ libelle: 'Impossible de charger les notifications' })
    });
  }

  /**
   * Tout marquer comme lu, à l'ouverture de la page Abonnements.
   *
   * La pastille ne se vide qu'une fois le serveur prévenu : si l'appel échoue,
   * les notifications reviendraient au prochain tour de sondage — mieux vaut ne
   * pas faire croire qu'elles ont disparu.
   */
  marquerLues(): void {
    this.http.put<void>(`${this.apiUrl}/lues`, null, {
      context: contexte({ discret: true })
    }).pipe(
      catchError(() => EMPTY)
    ).subscribe(() => this.nonLuesSignal.set([]));
  }

  private annoncerLesNouvelles(liste: NotificationCompte[]): void {
    if (!this.premierTour) {
      for (const notification of liste) {
        if (!this.dejaVues.has(notification.id)) {
          this.notifications.notifier('Abonnements', texteNotification(notification), '/abonnements');
        }
      }
    }

    // Remplacé, et non complété : une notification lue sort de la réponse, elle
    // doit sortir de la mémoire aussi.
    this.dejaVues = new Set(liste.map(n => n.id));
    this.premierTour = false;
  }
}

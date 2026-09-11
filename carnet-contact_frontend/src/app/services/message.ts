import { Injectable, PLATFORM_ID, inject, signal } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { EMPTY, Subscription, catchError, switchMap, timer } from 'rxjs';
import { Message } from '../message.model';
import { contexte } from '../interceptors/http-contexte';
import { NotificationService } from './notification';

/**
 * Rythmes de rafraîchissement.
 *
 * Deux valeurs différentes, pour deux usages différents : on regarde un fil de
 * discussion en attendant une réponse (5 s), alors que la pastille de non-lus
 * n'est qu'une information d'ambiance (15 s). Sonder trop souvent coûte des
 * requêtes pour rien ; pas assez donne une application qui paraît figée.
 */
const INTERVALLE_FIL_MS = 5000;
const INTERVALLE_NON_LUS_MS = 15000;

@Injectable({
  providedIn: 'root'
})
export class MessageService {
  private http = inject(HttpClient);
  private notifications = inject(NotificationService);
  private apiUrl = '/api/messages';

  // Même précaution que dans SessionService (section 18) : rien de périodique
  // ne doit démarrer pendant le rendu côté serveur. Un timer y tournerait dans
  // le vide — pire, Angular attend que l'application soit « stable » pour
  // renvoyer le HTML, et un flux qui ne se termine jamais l'en empêcherait.
  private navigateur = isPlatformBrowser(inject(PLATFORM_ID));

  // Le fil actuellement ouvert.
  private filSignal = signal<Message[]>([]);
  readonly fil = this.filSignal.asReadonly();

  // Les messages reçus non lus, pour la pastille de la barre de navigation.
  private nonLusSignal = signal<Message[]>([]);
  readonly nonLus = this.nonLusSignal.asReadonly();

  /**
   * Les abonnements en cours. Il faut les garder sous la main pour pouvoir les
   * arrêter : un `timer` tourne indéfiniment tant que personne ne se
   * désabonne. Ce service vivant aussi longtemps que l'application, un
   * abonnement oublié ici ne s'arrêterait JAMAIS — il continuerait d'interroger
   * le serveur après la déconnexion, avec un jeton devenu invalide.
   */
  private suiviFil?: Subscription;
  private suiviNonLus?: Subscription;

  /**
   * Les identifiants de non-lus déjà vus, pour ne notifier qu'une fois par
   * message. Le sondage renvoie à chaque tour la liste COMPLÈTE des non-lus :
   * sans cette mémoire, un message non ouvert déclencherait une notification
   * toutes les quinze secondes, indéfiniment.
   */
  private nonLusDejaVus = new Set<number>();

  /**
   * Le premier tour de sondage sert à établir l'état de départ, sans notifier.
   * Sinon, ouvrir l'application annoncerait d'un coup tous les messages en
   * attente — or ils ne sont pas « nouveaux », ils étaient déjà là.
   */
  private premierTourNonLus = true;

  /**
   * Ouvre un fil et le maintient à jour.
   *
   * Le « temps réel » se fait ici par SONDAGE (polling) : on redemande
   * régulièrement. Ce n'est pas la technique la plus élégante — un WebSocket
   * laisserait le serveur pousser les nouveautés au lieu de les attendre —
   * mais elle ne demande aucune infrastructure nouvelle, réutilise
   * l'authentification déjà en place, et se résume à un opérateur RxJS.
   *
   * timer(0, N) émet immédiatement, puis toutes les N millisecondes. Le
   * premier zéro est important : sans lui, ouvrir un fil laisserait l'écran
   * vide pendant cinq secondes.
   */
  suivreFil(autreId: number): void {
    // Changer d'interlocuteur doit arrêter le suivi précédent, sinon deux
    // timers écriraient tour à tour dans le même signal.
    this.arreterSuiviFil();

    if (!this.navigateur) {
      return;
    }

    this.suiviFil = timer(0, INTERVALLE_FIL_MS).pipe(
      // timer émet un compteur : 0, 1, 2… On s'en sert pour distinguer le
      // premier chargement (déclenché par le clic, donc l'utilisateur attend
      // et mérite l'indicateur) des suivants, silencieux.
      switchMap(tour => this.http.get<Message[]>(`${this.apiUrl}/${autreId}`, {
        context: contexte({
          discret: tour > 0,
          libelle: 'Impossible de charger la conversation'
        })
      }).pipe(
        // Comme dans ContactService : le catchError est À L'INTÉRIEUR du
        // switchMap. Dehors, la première coupure réseau terminerait le flux du
        // timer et arrêterait le rafraîchissement pour de bon.
        catchError(() => EMPTY)
      ))
    ).subscribe(messages => {
      this.filSignal.set(messages);

      // Le serveur a marqué ces messages comme lus en répondant : la pastille
      // doit suivre tout de suite, sans attendre le prochain tour du second
      // timer (jusqu'à 15 s plus tard).
      this.nonLusSignal.update(liste => liste.filter(m => m.expediteur.id !== autreId));
    });
  }

  arreterSuiviFil(): void {
    this.suiviFil?.unsubscribe();
    this.suiviFil = undefined;
    this.filSignal.set([]);
  }

  /** Maintient à jour la pastille de non-lus, tant qu'on est connecté. */
  demarrerSuiviNonLus(): void {
    // Déjà démarré : ne pas empiler un second timer. Sans ce garde-fou, chaque
    // déclenchement de l'effect() dans App en ajouterait un.
    if (!this.navigateur || this.suiviNonLus) {
      return;
    }

    this.suiviNonLus = timer(0, INTERVALLE_NON_LUS_MS).pipe(
      switchMap(() => this.http.get<Message[]>(`${this.apiUrl}/non-lus`, {
        // Toujours discret : personne n'a demandé cette requête.
        context: contexte({ discret: true })
      }).pipe(
        catchError(() => EMPTY)
      ))
    ).subscribe(messages => {
      this.signalerLesNouveaux(messages);
      this.nonLusSignal.set(messages);
    });
  }

  /** Prévient pour les messages qu'on n'avait encore jamais vus passer. */
  private signalerLesNouveaux(messages: Message[]): void {
    if (!this.premierTourNonLus) {
      for (const message of messages) {
        if (!this.nonLusDejaVus.has(message.id)) {
          this.notifications.notifier(
            `Message de ${message.expediteur.nomAffichage}`,
            message.contenu
          );
        }
      }
    }

    // On remplace entièrement l'ensemble plutôt que d'y ajouter : un message
    // lu disparaît de la réponse, et doit donc sortir de la mémoire. Sinon
    // elle grossirait sans fin au fil de la session.
    this.nonLusDejaVus = new Set(messages.map(m => m.id));
    this.premierTourNonLus = false;
  }

  arreterSuiviNonLus(): void {
    this.suiviNonLus?.unsubscribe();
    this.suiviNonLus = undefined;
    this.nonLusSignal.set([]);

    // Remise à zéro : à la prochaine connexion, le premier tour doit de nouveau
    // se contenter d'observer. Sans cela, se reconnecter annoncerait d'un coup
    // tous les messages en attente.
    this.nonLusDejaVus.clear();
    this.premierTourNonLus = true;
  }

  envoyer(destinataireId: number, contenu: string): void {
    this.http.post<Message>(this.apiUrl, { destinataireId, contenu }, {
      context: contexte({ libelle: 'Impossible d\'envoyer le message' })
    }).pipe(
      catchError(() => EMPTY)
    ).subscribe(message => {
      // On ajoute la réponse du serveur au fil sans attendre le prochain tour
      // du timer : son propre message doit apparaître instantanément. Elle
      // seule porte l'id et la date d'envoi réels (même raisonnement que pour
      // addContact).
      this.filSignal.update(fil => [...fil, message]);
    });
  }
}

import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, finalize, map, shareReplay, tap, throwError } from 'rxjs';
import { ReponseAuth } from '../utilisateur.model';
import { SessionService } from './session';
import { contexte } from '../interceptors/http-contexte';

/**
 * Obtient un jeton d'accès neuf à partir du jeton de rafraîchissement.
 *
 * Séparé de AuthService pour la raison désormais habituelle (sections 17 et
 * 18) : l'intercepteur de rafraîchissement doit pouvoir l'injecter, et
 * AuthService traîne derrière lui la session, la navigation et le profil.
 *
 * Le vrai sujet de ce service n'est pas l'appel HTTP — trois lignes — mais la
 * CONCURRENCE. Quand un jeton expire, ce n'est presque jamais une requête qui
 * échoue : c'est la page entière. Trois appels partis ensemble reçoivent trois
 * 401 quasi simultanés. Sans précaution, chacun lancerait son propre
 * rafraîchissement : trois rotations en chaîne côté serveur, dont les deux
 * dernières présenteraient un jeton que la première vient de révoquer — et
 * l'utilisateur serait déconnecté alors que tout allait bien.
 */
@Injectable({
  providedIn: 'root'
})
export class RafraichissementService {
  private http = inject(HttpClient);
  private session = inject(SessionService);

  /**
   * L'appel en cours, s'il y en a un. C'est la mémoire qui permet aux appelants
   * suivants de se greffer sur le premier au lieu d'en lancer un second.
   */
  private enCours: Observable<string> | null = null;

  obtenirNouveauJeton(): Observable<string> {
    // Un rafraîchissement est déjà parti : on rend le MÊME Observable. Les
    // appelants suivants attendent son résultat, sans nouvelle requête.
    if (this.enCours) {
      return this.enCours;
    }

    const jetonRafraichissement = this.session.jetonRafraichissementActuel();

    if (!jetonRafraichissement) {
      return throwError(() => new Error('Aucun jeton de rafraîchissement.'));
    }

    this.enCours = this.http.post<ReponseAuth>(
      '/api/auth/rafraichir',
      { jetonRafraichissement },
      // discret : ce renouvellement se produit au milieu d'une action de
      // l'utilisateur, qui ne doit voir ni indicateur ni message le concernant.
      { context: contexte({ discret: true }) }
    ).pipe(
      // La rotation a émis un nouveau jeton de rafraîchissement : il faut
      // mémoriser les DEUX, sinon la prochaine expiration présenterait une
      // valeur déjà révoquée.
      tap(reponse => this.session.ouvrir(
        reponse.jeton, reponse.jetonRafraichissement, reponse.utilisateur)),

      map(reponse => reponse.jeton),

      catchError(erreur => {
        // Le jeton long est refusé à son tour : il n'y a plus de porte de
        // sortie automatique, la session est réellement finie.
        this.session.vider();
        return throwError(() => erreur);
      }),

      // finalize efface la mémoire quand l'appel est terminé, dans un sens ou
      // dans l'autre. Sans cela, `enCours` garderait pour toujours le résultat
      // du premier rafraîchissement, et la deuxième expiration renverrait un
      // jeton déjà périmé.
      finalize(() => { this.enCours = null; }),

      // shareReplay(1) : une seule requête réseau, partagée par tous les
      // abonnés — et sa valeur rejouée pour ceux qui arrivent juste après la
      // réponse. Sans lui, un Observable HttpClient étant « froid », chaque
      // abonnement relancerait un appel : exactement ce qu'on cherche à éviter.
      shareReplay(1)
    );

    return this.enCours;
  }
}

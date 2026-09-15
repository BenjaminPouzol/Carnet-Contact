import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { AuteurPublic } from '../utilisateur.model';
import { AbonnementService } from './abonnement';
import { contexte } from '../interceptors/http-contexte';

/**
 * Bloquer et débloquer des comptes.
 *
 * Un service à part plutôt que des méthodes de plus dans AbonnementService :
 * bloquer n'est pas une variante de « suivre », c'est une mesure de protection,
 * avec ses propres écrans (liste des comptes bloqués) et ses propres effets.
 */
@Injectable({
  providedIn: 'root'
})
export class BlocageService {
  private http = inject(HttpClient);
  private abonnements = inject(AbonnementService);
  private apiUrl = '/api/blocages';

  /**
   * Le serveur supprime au passage les abonnements entre les deux comptes. On
   * reflète cet effet localement, pour qu'aucun bouton « Abonné·e » ne reste
   * affiché sur un compte qu'on vient de bloquer.
   */
  bloquer(id: number): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/${id}`, null, {
      context: contexte({ libelle: 'Impossible de bloquer ce compte' })
    }).pipe(
      tap(() => this.abonnements.oublier(id))
    );
  }

  /** Débloquer ne restaure aucun abonnement : il faudra suivre à nouveau. */
  debloquer(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`, {
      context: contexte({ libelle: 'Impossible de débloquer ce compte' })
    });
  }

  liste(): Observable<AuteurPublic[]> {
    return this.http.get<AuteurPublic[]>(this.apiUrl, {
      context: contexte({ libelle: 'Impossible de charger les comptes bloqués' })
    });
  }
}

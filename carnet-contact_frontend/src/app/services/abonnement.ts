import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { CompteResume, ProfilPublic, StatutRelation, Suggestion } from '../abonnement.model';
import { contexte } from '../interceptors/http-contexte';

/**
 * Suivre des comptes, et savoir à tout moment qui l'on suit.
 *
 * Le rôle central de ce service est le signal des STATUTS. Plusieurs boutons
 * « Suivre » peuvent viser le même compte sur un même écran — une carte par
 * publication d'Alice dans le fil. S'abonner depuis l'une doit mettre les
 * autres à jour aussitôt : c'est le signal partagé de la section 12, appliqué à
 * une Map.
 */
@Injectable({
  providedIn: 'root'
})
export class AbonnementService {
  private http = inject(HttpClient);
  private apiUrl = '/api/abonnements';

  /**
   * Identifiant du compte → mon statut vers lui. Un compte absent vaut AUCUN.
   *
   * ReadonlyMap, et une NOUVELLE Map à chaque changement (voir `definir`) : un
   * signal ne prévient ses lecteurs que si sa valeur change de RÉFÉRENCE.
   * Modifier la Map sur place laisserait tous les boutons sur l'ancien état.
   */
  private statutsSignal = signal<ReadonlyMap<number, StatutRelation>>(new Map());

  statutDe(id: number): StatutRelation {
    return this.statutsSignal().get(id) ?? 'AUCUN';
  }

  /** Remplit les statuts depuis le serveur. À appeler en arrivant sur une page qui affiche des boutons. */
  charger(): void {
    this.abonnements().subscribe({ error: () => {} });
  }

  /** Mes abonnements, acceptés et en attente — et, au passage, les statuts à jour. */
  abonnements(): Observable<CompteResume[]> {
    return this.http.get<CompteResume[]>(this.apiUrl, {
      context: contexte({ libelle: 'Impossible de charger vos abonnements' })
    }).pipe(
      tap(comptes => this.statutsSignal.set(new Map(comptes.map(c => [c.id, c.statut]))))
    );
  }

  /**
   * Suivre un compte.
   *
   * Le statut enregistré est celui de la RÉPONSE, pas une supposition : un
   * compte privé répond EN_ATTENTE. Et en cas d'échec, tap ne s'exécute pas —
   * le bouton garde son état précédent.
   */
  suivre(id: number): Observable<CompteResume> {
    return this.http.put<CompteResume>(`${this.apiUrl}/${id}`, null, {
      context: contexte({ libelle: 'Impossible de suivre ce compte' })
    }).pipe(
      tap(compte => this.definir(id, compte.statut))
    );
  }

  /** Ne plus suivre, ou annuler une demande en attente. */
  nePlusSuivre(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`, {
      context: contexte({ libelle: 'Impossible de ne plus suivre ce compte' })
    }).pipe(
      tap(() => this.oublier(id))
    );
  }

  /** Oublier un statut sans appel serveur (après un blocage, par exemple). */
  oublier(id: number): void {
    this.definir(id, 'AUCUN');
  }

  rechercher(terme: string): Observable<CompteResume[]> {
    return this.http.get<CompteResume[]>('/api/utilisateurs', {
      params: { recherche: terme },
      context: contexte({ libelle: 'Impossible de rechercher des comptes' })
    });
  }

  suggestions(): Observable<Suggestion[]> {
    return this.http.get<Suggestion[]>('/api/utilisateurs/suggestions', {
      context: contexte({ libelle: 'Impossible de charger les suggestions' })
    });
  }

  profil(id: number): Observable<ProfilPublic> {
    return this.http.get<ProfilPublic>(`/api/utilisateurs/${id}`, {
      context: contexte({ libelle: 'Impossible de charger ce profil' })
    });
  }

  abonnes(): Observable<CompteResume[]> {
    return this.http.get<CompteResume[]>(`${this.apiUrl}/abonnes`, {
      context: contexte({ libelle: 'Impossible de charger vos abonnés' })
    });
  }

  demandes(): Observable<CompteResume[]> {
    return this.http.get<CompteResume[]>(`${this.apiUrl}/demandes`, {
      context: contexte({ libelle: 'Impossible de charger les demandes' })
    });
  }

  accepter(id: number): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/demandes/${id}`, null, {
      context: contexte({ libelle: 'Impossible d\'accepter la demande' })
    });
  }

  refuser(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/demandes/${id}`, {
      context: contexte({ libelle: 'Impossible de refuser la demande' })
    });
  }

  retirerAbonne(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/abonnes/${id}`, {
      context: contexte({ libelle: 'Impossible de retirer cet abonné' })
    });
  }

  private definir(id: number, statut: StatutRelation): void {
    this.statutsSignal.update(actuels => {
      const copie = new Map(actuels);
      if (statut === 'AUCUN') {
        copie.delete(id);
      } else {
        copie.set(id, statut);
      }
      return copie;
    });
  }
}

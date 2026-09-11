import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { EMPTY, catchError, of } from 'rxjs';
import { LigneCompte, Role } from '../utilisateur.model';
import { contexte } from '../interceptors/http-contexte';

@Injectable({
  providedIn: 'root'
})
export class AdminService {
  private http = inject(HttpClient);
  private apiUrl = '/api/admin/comptes';

  private comptesSignal = signal<LigneCompte[]>([]);
  readonly comptes = this.comptesSignal.asReadonly();

  charger(): void {
    this.http.get<LigneCompte[]>(this.apiUrl, {
      context: contexte({ libelle: 'Impossible de charger les comptes' })
    }).pipe(
      catchError(() => of([]))
    ).subscribe(comptes => this.comptesSignal.set(comptes));
  }

  /**
   * Les trois méthodes d'écriture suivent le même schéma : le serveur renvoie
   * la ligne mise à jour, et on la remplace sur place.
   *
   * Pourquoi ne pas simplement recharger toute la liste ? Parce que le serveur
   * est seul à connaître les conséquences d'une action (un rôle changé, des
   * jetons révoqués), mais que recharger ferait clignoter le tableau entier
   * pour une seule ligne modifiée. Remplacer l'élément suffit — et la réponse
   * du serveur fait autorité sur son contenu.
   */
  changerActif(id: number, actif: boolean): void {
    this.http.put<LigneCompte>(`${this.apiUrl}/${id}/actif`, { actif }, {
      context: contexte({
        libelle: actif ? 'Impossible de réactiver ce compte' : 'Impossible de désactiver ce compte'
      })
    }).pipe(
      catchError(() => EMPTY)
    ).subscribe(ligne => this.remplacer(ligne));
  }

  changerRole(id: number, role: Role): void {
    this.http.put<LigneCompte>(`${this.apiUrl}/${id}/role`, { role }, {
      context: contexte({ libelle: 'Impossible de changer le rôle' })
    }).pipe(
      catchError(() => EMPTY)
    ).subscribe(ligne => this.remplacer(ligne));
  }

  supprimer(id: number): void {
    this.http.delete<void>(`${this.apiUrl}/${id}`, {
      context: contexte({ libelle: 'Impossible de supprimer ce compte' })
    }).pipe(
      catchError(() => EMPTY)
    ).subscribe(() => {
      this.comptesSignal.update(liste => liste.filter(c => c.id !== id));
    });
  }

  private remplacer(ligne: LigneCompte): void {
    this.comptesSignal.update(liste =>
      liste.map(c => (c.id === ligne.id ? ligne : c))
    );
  }
}

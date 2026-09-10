import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { EMPTY, of, catchError } from 'rxjs';
import { Contact } from '../contact.model';

@Injectable({
  providedIn: 'root'
})
export class ContactService {
  private http = inject(HttpClient);
  private apiUrl = 'http://localhost:8080/api/contacts';

  // Source de vérité côté client : la liste des contacts.
  private contactsSignal = signal<Contact[]>([]);
  readonly contacts = this.contactsSignal.asReadonly();

  // Deuxième signal : le dernier message d'erreur, ou null si tout va bien.
  // Un composant (ici App) l'affichera. null = pas d'erreur à montrer.
  private erreurSignal = signal<string | null>(null);
  readonly erreur = this.erreurSignal.asReadonly();

  chargerContacts(): void {
    this.erreurSignal.set(null); // on repart d'un état sain avant chaque appel

    this.http.get<Contact[]>(this.apiUrl).pipe(
      // catchError intercepte une erreur du flux (backend éteint, 500...).
      // Il DOIT retourner un Observable : ici of([]), une liste vide de repli,
      // pour que le .subscribe() reçoive quand même une valeur exploitable.
      catchError(() => {
        this.erreurSignal.set('Impossible de charger les contacts. Le serveur est-il démarré ?');
        return of([]);
      })
    ).subscribe(data => this.contactsSignal.set(data));
  }

  addContact(contact: Contact): void {
    this.erreurSignal.set(null);

    this.http.post<Contact>(this.apiUrl, contact).pipe(
      catchError(() => {
        this.erreurSignal.set("Impossible d'ajouter le contact.");
        // EMPTY : le flux se termine SANS émettre — le .subscribe() ne
        // s'exécute pas, donc le signal des contacts n'est pas touché.
        return EMPTY;
      })
    ).subscribe(contactCree => {
      this.contactsSignal.update(liste => [...liste, contactCree]);
    });
  }

  modifierContact(contact: Contact): void {
    this.erreurSignal.set(null);

    this.http.put<Contact>(`${this.apiUrl}/${contact.id}`, contact).pipe(
      catchError(() => {
        this.erreurSignal.set('Impossible d\'enregistrer les modifications.');
        return EMPTY;
      })
    ).subscribe(contactMaj => {
      this.contactsSignal.update(liste =>
        liste.map(c => (c.id === contactMaj.id ? contactMaj : c))
      );
    });
  }

  deleteContact(id: number): void {
    this.erreurSignal.set(null);

    this.http.delete<void>(`${this.apiUrl}/${id}`).pipe(
      catchError(() => {
        this.erreurSignal.set('Impossible de supprimer le contact.');
        return EMPTY;
      })
    ).subscribe(() => {
      this.contactsSignal.update(liste => liste.filter(c => c.id !== id));
    });
  }
}

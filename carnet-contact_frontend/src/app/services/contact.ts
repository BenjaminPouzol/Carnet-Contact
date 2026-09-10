import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { EMPTY, of, catchError } from 'rxjs';
import { Contact } from '../contact.model';

@Injectable({
  providedIn: 'root'
})
export class ContactService {
  private http = inject(HttpClient);

  // URL RELATIVE : baseUrlInterceptor y ajoute l'adresse du backend au
  // passage. Le service ne connaît plus le nom du serveur.
  private apiUrl = '/api/contacts';

  // Source de vérité côté client : la liste des contacts.
  private contactsSignal = signal<Contact[]>([]);
  readonly contacts = this.contactsSignal.asReadonly();

  // Ce service ne porte plus ni l'indicateur de chargement ni le message
  // d'erreur : les intercepteurs s'en chargent pour TOUTES les requêtes,
  // écrites ou encore à écrire. Il ne reste ici que la seule décision qu'un
  // intercepteur ne peut pas prendre à sa place : par quoi remplacer un flux
  // en échec — une valeur de repli pour une lecture, rien pour une écriture.

  chargerContacts(): void {
    this.http.get<Contact[]>(this.apiUrl).pipe(
      // Lecture en échec : une liste vide reste une valeur exploitable.
      catchError(() => of([]))
    ).subscribe(data => this.contactsSignal.set(data));
  }

  addContact(contact: Contact): void {
    this.http.post<Contact>(this.apiUrl, contact).pipe(
      // Écriture en échec : EMPTY, pour ne surtout pas toucher l'état local.
      catchError(() => EMPTY)
    ).subscribe(contactCree => {
      this.contactsSignal.update(liste => [...liste, contactCree]);
    });
  }

  modifierContact(contact: Contact): void {
    this.http.put<Contact>(`${this.apiUrl}/${contact.id}`, contact).pipe(
      catchError(() => EMPTY)
    ).subscribe(contactMaj => {
      this.contactsSignal.update(liste =>
        liste.map(c => (c.id === contactMaj.id ? contactMaj : c))
      );
    });
  }

  deleteContact(id: number): void {
    this.http.delete<void>(`${this.apiUrl}/${id}`).pipe(
      catchError(() => EMPTY)
    ).subscribe(() => {
      this.contactsSignal.update(liste => liste.filter(c => c.id !== id));
    });
  }
}

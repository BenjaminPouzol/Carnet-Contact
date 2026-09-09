import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Contact } from '../contact.model';

@Injectable({
  providedIn: 'root'
})
export class ContactService {
  private http = inject(HttpClient);
  private apiUrl = 'http://localhost:8080/api/contacts';

  // La liste vit ICI, dans le service singleton : c'est la source de
  // vérité côté client. Privée : seul le service a le droit de l'écrire.
  private contactsSignal = signal<Contact[]>([]);

  // Version exposée aux composants : lisible, mais pas modifiable.
  readonly contacts = this.contactsSignal.asReadonly();

  chargerContacts(): void {
    this.http.get<Contact[]>(this.apiUrl).subscribe(data => {
      // .set() : on remplace toute la liste par celle du serveur.
      this.contactsSignal.set(data);
    });
  }

  addContact(contact: Contact): void {
    this.http.post<Contact>(this.apiUrl, contact).subscribe(contactCree => {
      // On ajoute la réponse du SERVEUR : elle porte l'id généré par la base.
      this.contactsSignal.update(liste => [...liste, contactCree]);
    });
  }

  deleteContact(id: number): void {
    this.http.delete<void>(`${this.apiUrl}/${id}`).subscribe(() => {
      // Mise à jour locale : inutile de redemander la liste au serveur,
      // on sait déjà à quoi elle doit ressembler.
      // .filter() renvoie un nouveau tableau (règle d'immutabilité).
      this.contactsSignal.update(liste => liste.filter(c => c.id !== id));
    });
  }
}

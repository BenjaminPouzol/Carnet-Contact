import { Component, inject, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ContactList } from './components/contact-list/contact-list';
import { ContactForm } from './components/contact-form/contact-form';
import { ContactService } from './services/contact';
import { Contact } from './contact.model';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, ContactList, ContactForm],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  protected readonly title = signal('carnet-contact');

  private contactService = inject(ContactService);

  ajouterContact(contact: Contact): void {
    // Plus de .subscribe(), plus de window.location.reload() :
    // le service met à jour le signal partagé, ContactList suit toute seule.
    this.contactService.addContact(contact);
  }
}

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
    this.contactService.addContact(contact).subscribe(() => {
      window.location.reload();
    });
  }
}


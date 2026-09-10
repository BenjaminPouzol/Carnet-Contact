import { Component, inject } from '@angular/core';
import { ContactList } from '../../components/contact-list/contact-list';
import { ContactForm } from '../../components/contact-form/contact-form';
import { ContactService } from '../../services/contact';
import { Contact } from '../../contact.model';

@Component({
  selector: 'app-accueil',
  imports: [ContactList, ContactForm],
  templateUrl: './accueil.html',
  styleUrl: './accueil.css'
})
export class Accueil {
  private contactService = inject(ContactService);

  ajouterContact(contact: Contact): void {
    this.contactService.addContact(contact);
  }
}

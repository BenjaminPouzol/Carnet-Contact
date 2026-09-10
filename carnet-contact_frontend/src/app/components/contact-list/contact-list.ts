import { Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ContactService } from '../../services/contact';

@Component({
  selector: 'app-contact-list',
  imports: [RouterLink],
  templateUrl: './contact-list.html',
  styleUrl: './contact-list.css'
})
export class ContactList implements OnInit {
  private contactService = inject(ContactService);

  // Référence vers le signal du service, pas une copie.
  contacts = this.contactService.contacts;

  ngOnInit(): void {
    this.contactService.chargerContacts();
  }

  supprimer(id: number): void {
    this.contactService.deleteContact(id);
  }
}

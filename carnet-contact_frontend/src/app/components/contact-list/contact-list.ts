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

  // Références vers les signaux du service, pas des copies.
  contacts = this.contactService.contacts;
  chargement = this.contactService.chargement;

  ngOnInit(): void {
    this.contactService.chargerContacts();
  }

  supprimer(id: number): void {
    this.contactService.deleteContact(id);
  }
}

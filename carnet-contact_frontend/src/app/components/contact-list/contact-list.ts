import { Component, OnInit, inject, signal } from '@angular/core';
import { Contact } from '../../contact.model';
import { ContactService } from '../../services/contact';

@Component({
  selector: 'app-contact-list',
  imports: [],
  templateUrl: './contact-list.html',
  styleUrl: './contact-list.css'
})
export class ContactList implements OnInit {
  private contactService = inject(ContactService);
  contacts = signal<Contact[]>([]);

  ngOnInit(): void {
    this.chargerContacts();
  }

  chargerContacts(): void {
    this.contactService.getContacts().subscribe(data => {
      this.contacts.set(data);
    });
  }

  supprimer(id: number): void {
    this.contactService.deleteContact(id).subscribe(() => {
      this.chargerContacts();
    });
  }
}
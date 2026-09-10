import { Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ContactService } from '../../services/contact';
import { EtatHttpService } from '../../services/etat-http';
import { ReseauxSociaux } from '../reseaux-sociaux/reseaux-sociaux';

@Component({
  selector: 'app-contact-list',
  imports: [RouterLink, ReseauxSociaux],
  templateUrl: './contact-list.html',
  styleUrl: './contact-list.css'
})
export class ContactList implements OnInit {
  private contactService = inject(ContactService);
  private etatHttp = inject(EtatHttpService);

  // Références vers les signaux des services, pas des copies. Les données
  // viennent du service métier, l'état de chargement du service transverse.
  contacts = this.contactService.contacts;
  chargement = this.etatHttp.chargement;

  ngOnInit(): void {
    this.contactService.chargerContacts();
  }

  supprimer(id: number): void {
    this.contactService.deleteContact(id);
  }
}

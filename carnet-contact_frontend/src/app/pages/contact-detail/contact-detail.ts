import { ButtonModule } from 'primeng/button';
import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ContactService } from '../../services/contact';
import { ReseauxSociaux } from '../../components/reseaux-sociaux/reseaux-sociaux';

@Component({
  selector: 'app-contact-detail',
  imports: [RouterLink, ReseauxSociaux, ButtonModule],
  templateUrl: './contact-detail.html',
  styleUrl: './contact-detail.css'
})
export class ContactDetail implements OnInit {
  private route = inject(ActivatedRoute);
  private contactService = inject(ContactService);

  // L'id capturé dans l'URL (ex: "5" dans /contact/5). paramMap.get()
  // renvoie toujours une chaîne (les URL n'ont pas de type), d'où le
  // Number() pour comparer avec le id numérique d'un Contact.
  // snapshot : on lit la valeur une seule fois, à la création du
  // composant — suffisant ici, car aucun lien ne mène directement
  // d'une fiche contact à une autre fiche contact.
  private contactId = Number(this.route.snapshot.paramMap.get('id'));

  // Cette page cherchait auparavant son contact dans la liste partagée, avec
  // un computed(). La pagination a rendu ce raccourci faux : la liste ne
  // contient plus que six contacts, et celui qu'on demande peut être ailleurs.
  // Le service expose donc un signal dédié, alimenté par un appel à la fiche
  // seule. Le principe n'a pas changé — le gabarit lit un signal et se
  // réaffiche tout seul quand la réponse arrive — seule la source diffère.
  contact = this.contactService.contactCourant;

  ngOnInit(): void {
    this.contactService.chargerContact(this.contactId);
  }
}

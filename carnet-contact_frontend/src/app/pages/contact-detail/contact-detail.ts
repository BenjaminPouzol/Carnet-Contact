import { Component, OnInit, computed, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ContactService } from '../../services/contact';

@Component({
  selector: 'app-contact-detail',
  imports: [RouterLink],
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

  // computed() calcule une valeur DÉRIVÉE d'un ou plusieurs signals, et
  // se recalcule automatiquement dès que l'un d'eux change. Ici, elle
  // dépend du signal partagé contacts() : si la liste est encore vide au
  // premier affichage (réponse du serveur pas encore arrivée), contact()
  // vaudra undefined, puis se mettra à jour tout seul dès que la réponse
  // arrivera et remplira le signal — sans qu'on écrive .subscribe() ici.
  contact = computed(() =>
    this.contactService.contacts().find(c => c.id === this.contactId)
  );

  ngOnInit(): void {
    // Utile en cas d'accès direct à cette URL (lien partagé, rechargement
    // de la page) : le signal partagé serait alors encore vide.
    this.contactService.chargerContacts();
  }
}

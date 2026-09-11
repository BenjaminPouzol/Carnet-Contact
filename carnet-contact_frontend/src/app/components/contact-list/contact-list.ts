import { Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { debounceTime, distinctUntilChanged } from 'rxjs';
import { ContactService } from '../../services/contact';
import { EtatHttpService } from '../../services/etat-http';
import { ReseauxSociaux } from '../reseaux-sociaux/reseaux-sociaux';

@Component({
  selector: 'app-contact-list',
  imports: [RouterLink, ReactiveFormsModule, ReseauxSociaux],
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

  page = this.contactService.page;
  total = this.contactService.total;
  totalPages = this.contactService.totalPages;
  premierePage = this.contactService.premierePage;
  dernierePage = this.contactService.dernierePage;
  recherche = this.contactService.recherche;

  /**
   * Un FormControl seul, sans FormGroup autour : pour un champ unique qui
   * n'est pas soumis, le groupe n'apporterait rien.
   *
   * Il est initialisé avec le terme que le service a en mémoire. Sans cela,
   * revenir sur la liste après avoir consulté une fiche afficherait un champ
   * vide au-dessus de résultats filtrés — un décalage déroutant.
   */
  champRecherche = new FormControl(this.contactService.recherche(), { nonNullable: true });

  constructor() {
    this.champRecherche.valueChanges.pipe(
      // debounceTime : n'émettre qu'après 300 ms de silence. Sans lui, taper
      // « dupont » lancerait six requêtes — une par lettre — dont cinq déjà
      // périmées à leur arrivée. Avec, une seule part, quand l'utilisateur
      // marque une pause.
      debounceTime(300),

      // distinctUntilChanged : ignorer une valeur identique à la précédente.
      // Le cas typique : taper une lettre puis l'effacer pendant le délai —
      // la valeur finale est la même qu'au départ, la requête est inutile.
      distinctUntilChanged(),

      // takeUntilDestroyed : se désabonner automatiquement à la destruction du
      // composant. Un abonnement oublié continue de tourner après la
      // navigation, garde le composant en mémoire et peut écrire dans un état
      // que plus personne n'affiche. Appelé sans argument, il faut être dans
      // un contexte d'injection — le constructeur en est un.
      takeUntilDestroyed()
    ).subscribe(terme => this.contactService.rechercher(terme));
  }

  ngOnInit(): void {
    this.contactService.chargerContacts();
  }

  supprimer(id: number): void {
    this.contactService.deleteContact(id);
  }

  pagePrecedente(): void {
    this.contactService.allerPage(this.page() - 1);
  }

  pageSuivante(): void {
    this.contactService.allerPage(this.page() + 1);
  }

  effacerRecherche(): void {
    // On passe par le champ plutôt que par le service : la valeur traverse
    // ainsi le même tuyau que la saisie, et l'affichage reste cohérent.
    this.champRecherche.setValue('');
  }
}

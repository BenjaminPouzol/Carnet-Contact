import { Component, OnInit, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { PublicationService } from '../../services/publication';
import { AbonnementService } from '../../services/abonnement';
import { EtatHttpService } from '../../services/etat-http';
import { CATEGORIES, Categorie, categorieDe } from '../../publication.model';
import { PublicationForm } from '../../components/publication-form/publication-form';
import { PublicationCarte } from '../../components/publication-carte/publication-carte';

/**
 * Le fil d'actualité : publier, filtrer, lire, remonter le temps.
 *
 * La page ne détient presque rien : les données et le filtre vivent dans
 * PublicationService, les actions sur une publication dans sa carte. Elle se
 * contente d'assembler — c'est le rôle d'un composant de `pages/`.
 */
@Component({
  selector: 'app-fil',
  imports: [RouterLink, ButtonModule, PublicationForm, PublicationCarte],
  templateUrl: './fil.html',
  styleUrl: './fil.css'
})
export class Fil implements OnInit {
  private publicationService = inject(PublicationService);
  private abonnementService = inject(AbonnementService);
  private etatHttp = inject(EtatHttpService);

  readonly categories = CATEGORIES;

  publications = this.publicationService.publications;
  categorie = this.publicationService.categorie;
  abonnements = this.publicationService.abonnements;
  aDesPlusAnciennes = this.publicationService.aDesPlusAnciennes;
  chargement = this.etatHttp.chargement;

  /** Le libellé du filtre actif, pour un message « vide » qui dit de quoi. */
  libelleFiltre = computed(() => {
    const cle = this.categorie();
    return cle ? categorieDe(cle).libelle : null;
  });

  /**
   * On recharge à chaque arrivée sur la page, en gardant le filtre mémorisé
   * par le service : revenir sur le fil doit montrer ce qui a été publié
   * entre-temps, pas la liste figée de la dernière visite.
   *
   * Les statuts d'abonnement sont chargés aussi : les boutons « Suivre » des
   * cartes les lisent, et doivent afficher « Abonné·e » dès l'arrivée.
   */
  ngOnInit(): void {
    this.abonnementService.charger();
    this.publicationService.charger(this.categorie(), { abonnements: this.abonnements() });
  }

  filtrer(categorie: Categorie | null): void {
    // Recliquer sur le filtre déjà actif ne relance rien.
    if (categorie !== this.categorie()) {
      this.publicationService.charger(categorie, { abonnements: this.abonnements() });
    }
  }

  /** « Tout le monde » ou « Abonnements » : se combine avec la catégorie choisie. */
  choisirPortee(abonnements: boolean): void {
    if (abonnements !== this.abonnements()) {
      this.publicationService.charger(this.categorie(), { abonnements });
    }
  }

  voirPlus(): void {
    this.publicationService.chargerPlus();
  }
}

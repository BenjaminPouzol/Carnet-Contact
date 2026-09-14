import { Component, computed, inject, input, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ButtonModule } from 'primeng/button';
import { PublicationService } from '../../services/publication';
import { EtatHttpService } from '../../services/etat-http';
import { Publication, categorieDe } from '../../publication.model';
import { EMOJIS_REACTION } from '../../reaction.model';
import { PublicationForm } from '../publication-form/publication-form';

/**
 * Une publication du fil : son auteur, sa catégorie, son contenu, ses
 * réactions — et, selon les droits calculés par le serveur, les actions
 * « Modifier » et « Supprimer ».
 */
@Component({
  selector: 'app-publication-carte',
  imports: [DatePipe, ButtonModule, PublicationForm],
  templateUrl: './publication-carte.html',
  styleUrl: './publication-carte.css'
})
export class PublicationCarte {
  private publicationService = inject(PublicationService);
  private etatHttp = inject(EtatHttpService);

  publication = input.required<Publication>();

  readonly emojis = EMOJIS_REACTION;
  chargement = this.etatHttp.chargement;

  /** Libellé, emoji et couleur, déduits de la clé : rien n'est stocké en double. */
  categorie = computed(() => categorieDe(this.publication().categorie));

  initiale = computed(() =>
    (this.publication().auteur.nomAffichage || '?').charAt(0).toUpperCase());

  // Trois états propres à CETTE carte : ils n'intéressent personne d'autre,
  // un signal de composant suffit — pas besoin de les ranger dans le service.
  enModification = signal(false);
  paletteOuverte = signal(false);

  /**
   * La suppression demande une confirmation, mais DANS la carte plutôt que
   * dans une boîte de dialogue p-confirmDialog. Deux raisons : le paquet
   * initial frôle déjà son budget d'alerte (la page d'administration, elle,
   * est chargée à part), et la question apparaît à l'endroit exact du clic,
   * sans déplacer le regard.
   */
  confirmationOuverte = signal(false);

  basculerPalette(): void {
    this.paletteOuverte.update(ouverte => !ouverte);
  }

  reagir(emoji: string): void {
    this.publicationService.reagir(this.publication().id, emoji);
    // On referme dès le clic : la palette a fait son travail.
    this.paletteOuverte.set(false);
  }

  modifier(): void {
    this.confirmationOuverte.set(false);
    this.enModification.set(true);
  }

  finModification(): void {
    this.enModification.set(false);
  }

  demanderSuppression(): void {
    this.confirmationOuverte.set(true);
  }

  annulerSuppression(): void {
    this.confirmationOuverte.set(false);
  }

  confirmerSuppression(): void {
    this.confirmationOuverte.set(false);
    this.publicationService.supprimer(this.publication().id);
  }
}

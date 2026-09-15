import { Component, inject, input, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { finalize } from 'rxjs';
import { FORMATS_IMAGE, ImageService, TAILLE_MAX_IMAGE } from '../../services/image';

/**
 * Un champ d'adresse d'image, doublé d'un bouton pour envoyer un fichier.
 *
 * Il reçoit en input() le FormControl du formulaire parent. Une image envoyée
 * devient une adresse, que le composant écrit dans ce contrôle : le parent ne
 * voit aucune différence avec une adresse collée à la main, et n'a pas une
 * ligne à changer dans sa logique d'enregistrement.
 *
 * L'alternative « à la Angular » serait d'implémenter ControlValueAccessor,
 * l'interface qui permet d'écrire `formControlName="photoUrl"` sur un
 * composant maison. Même résultat ici, pour nettement plus de code.
 */
@Component({
  selector: 'app-champ-image',
  imports: [ReactiveFormsModule, ButtonModule, InputTextModule],
  templateUrl: './champ-image.html',
  styleUrl: './champ-image.css'
})
export class ChampImage {
  private images = inject(ImageService);

  controle = input.required<FormControl<string | null>>();
  identifiant = input.required<string>();
  libelle = input.required<string>();
  placeholder = input('');

  // Pour l'attribut accept : le sélecteur de fichiers du système ne propose
  // alors que des images. Simple aide au tri, le serveur revérifie.
  readonly formatsAcceptes = FORMATS_IMAGE.join(',');

  // Des signaux, et non de simples booléens : l'application tourne sans
  // zone.js, et c'est un signal modifié qui déclenche le rafraîchissement de
  // l'affichage quand la réponse HTTP arrive.
  envoiEnCours = signal(false);
  erreurLocale = signal<string | null>(null);

  fichierChoisi(evenement: Event): void {
    const selecteur = evenement.target as HTMLInputElement;
    const fichier = selecteur.files?.[0];

    // Vidé tout de suite : sinon, choisir à nouveau LE MÊME fichier (après un
    // échec, par exemple) ne déclencherait aucun événement « change ».
    selecteur.value = '';

    if (fichier) {
      this.envoyer(fichier);
    }
  }

  /**
   * Vérification locale avant l'envoi : un confort (réponse immédiate, pas de
   * 5 Mo transférés pour rien), pas une sécurité — le serveur lit les octets et
   * refuse lui-même ce qui ne convient pas.
   */
  envoyer(fichier: File): void {
    this.erreurLocale.set(null);

    if (!FORMATS_IMAGE.includes(fichier.type)) {
      this.erreurLocale.set('Formats acceptés : JPEG, PNG, WebP ou GIF.');
      return;
    }
    if (fichier.size > TAILLE_MAX_IMAGE) {
      this.erreurLocale.set('L\'image dépasse 5 Mo.');
      return;
    }

    this.envoiEnCours.set(true);

    this.images.envoyer(fichier).pipe(
      // finalize (section 16) : remis à false en cas de succès COMME d'échec.
      finalize(() => this.envoiEnCours.set(false))
    ).subscribe({
      next: url => {
        this.controle().setValue(url);
        // Pour le parent, c'est une vraie modification de l'utilisateur.
        this.controle().markAsDirty();
      },
      // La bannière est déjà affichée par erreurInterceptor. On ne touche pas
      // au contrôle : l'adresse précédente reste en place.
      error: () => {}
    });
  }
}

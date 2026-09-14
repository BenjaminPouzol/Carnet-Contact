import { Component, OnInit, inject, input, output } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { PublicationService } from '../../services/publication';
import { EtatHttpService } from '../../services/etat-http';
import { CATEGORIES, Categorie, DemandePublication, Publication } from '../../publication.model';

/** La même limite que la colonne et le @Size du serveur. */
const LONGUEUR_MAX_CONTENU = 2000;

/**
 * Le miroir du @Pattern serveur. Un champ vide est accepté d'office :
 * Validators.pattern ne juge pas une valeur vide (c'est le rôle de required).
 */
const MOTIF_IMAGE = /^https?:\/\/\S+$/;

/**
 * Pour que chaque formulaire de la page ait ses propres `id`. Le formulaire de
 * publication et ceux ouverts en modification dans les cartes coexistent :
 * deux `id="contenu"` casseraient les `<label for>` — un clic sur un libellé
 * enverrait le curseur dans le mauvais formulaire.
 */
let prochainNumero = 1;

@Component({
  selector: 'app-publication-form',
  imports: [ReactiveFormsModule, ButtonModule],
  templateUrl: './publication-form.html',
  styleUrl: './publication-form.css'
})
export class PublicationForm implements OnInit {
  private fb = inject(FormBuilder);
  private publicationService = inject(PublicationService);
  private etatHttp = inject(EtatHttpService);

  /** La publication à modifier ; absente, le formulaire en crée une nouvelle. */
  publication = input<Publication | null>(null);

  /** Émis après un enregistrement RÉUSSI, avec la réponse du serveur. */
  termine = output<Publication>();

  annuler = output<void>();

  readonly categories = CATEGORIES;
  readonly longueurMax = LONGUEUR_MAX_CONTENU;
  readonly numero = prochainNumero++;
  chargement = this.etatHttp.chargement;

  formulaire = this.fb.group({
    // fb.control<Categorie | null> : sans le type explicite, TypeScript
    // déduirait `null` tout court, et refuserait ensuite 'SPORT'.
    categorie: this.fb.control<Categorie | null>(null, Validators.required),
    contenu: ['', [
      Validators.required,
      // Au moins un caractère qui ne soit pas un espace : required seul
      // laisserait passer « ␣␣␣ », que le serveur refuserait (@NotBlank).
      Validators.pattern(/\S/),
      Validators.maxLength(LONGUEUR_MAX_CONTENU)
    ]],
    imageUrl: ['', [Validators.maxLength(500), Validators.pattern(MOTIF_IMAGE)]]
  });

  /**
   * ngOnInit et non le constructeur : les input() ne sont pas encore reçus au
   * moment de la construction. Pas besoin d'effect() non plus (section 14) —
   * la publication est déjà là, elle n'« arrive » pas plus tard du serveur.
   */
  ngOnInit(): void {
    const existante = this.publication();
    if (existante) {
      this.formulaire.setValue({
        categorie: existante.categorie,
        contenu: existante.contenu,
        imageUrl: existante.imageUrl ?? ''
      });
    }
  }

  longueurContenu(): number {
    return this.formulaire.controls.contenu.value?.length ?? 0;
  }

  /** L'aperçu n'est tenté que sur une adresse valide, pas à chaque lettre. */
  apercuImage(): string | null {
    const champ = this.formulaire.controls.imageUrl;
    return champ.valid && champ.value ? champ.value : null;
  }

  onSubmit(): void {
    if (this.formulaire.invalid) {
      return;
    }

    const valeurs = this.formulaire.getRawValue();
    const demande: DemandePublication = {
      categorie: valeurs.categorie!,
      contenu: valeurs.contenu!,
      imageUrl: valeurs.imageUrl ?? ''
    };

    const existante = this.publication();
    const appel = existante
      ? this.publicationService.modifier(existante.id, demande)
      : this.publicationService.publier(demande);

    appel.subscribe({
      next: publication => {
        if (!existante) {
          // On garde la catégorie : publier deux fois de suite dans « Sport »
          // ne doit pas obliger à la rechoisir.
          this.formulaire.reset({ categorie: demande.categorie, contenu: '', imageUrl: '' });
        }
        this.termine.emit(publication);
      },
      // La bannière est déjà affichée par erreurInterceptor. Ce callback vide
      // n'est pas un oubli : sans lui, l'erreur remonterait « non gérée » dans
      // la console. Et on ne touche pas au formulaire — la saisie reste prête à
      // être renvoyée.
      error: () => {}
    });
  }
}

import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { Component, effect, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../services/auth';
import { EtatHttpService } from '../../services/etat-http';
import { NotificationService } from '../../services/notification';
import { ChampImage } from '../../components/champ-image/champ-image';
import { RESEAUX } from '../../contact.model';
import { DemandeProfil } from '../../utilisateur.model';

@Component({
  selector: 'app-profil',
  imports: [ReactiveFormsModule, ButtonModule, InputTextModule, ChampImage],
  templateUrl: './profil.html',
  styleUrl: './profil.css'
})
export class Profil {
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);
  private etatHttp = inject(EtatHttpService);
  private notifications = inject(NotificationService);

  chargement = this.etatHttp.chargement;
  utilisateur = this.auth.utilisateur;
  permissionNotifications = this.notifications.permission;

  // La même liste que pour les contacts : le gabarit la parcourt avec @for,
  // au lieu d'écrire six fois le même champ.
  readonly reseaux = RESEAUX;

  enregistre = signal(false);

  formulaire = this.fb.group({
    nomAffichage: ['', Validators.required],
    photoUrl: [''],
    // Validators.email sans required : facultatif, mais s'il est rempli il doit
    // ressembler à une adresse — la même règle que le @Email du serveur.
    emailPro: ['', Validators.email],
    instagram: [''],
    twitter: [''],
    facebook: [''],
    twitch: [''],
    youtube: [''],
    linkedin: [''],
    comptePrive: [false]
  });

  private formulaireRempli = false;

  constructor() {
    // Même schéma qu'en section 14 : le profil vient d'un signal, qui peut
    // être encore vide au premier rendu (restauration depuis localStorage,
    // ou réponse du serveur en route). On pré-remplit dès qu'il arrive, une
    // seule fois, pour ne pas écraser une saisie en cours.
    effect(() => {
      const u = this.utilisateur();
      if (u && !this.formulaireRempli) {
        // `?? ''` : un champ absent côté serveur vaut null, et un champ de
        // formulaire afficherait littéralement « null » dans la case.
        this.formulaire.patchValue({
          nomAffichage: u.nomAffichage,
          photoUrl: u.photoUrl ?? '',
          emailPro: u.emailPro ?? '',
          instagram: u.instagram ?? '',
          twitter: u.twitter ?? '',
          facebook: u.facebook ?? '',
          twitch: u.twitch ?? '',
          youtube: u.youtube ?? '',
          linkedin: u.linkedin ?? '',
          // `?? false` : une session enregistrée avant les abonnements n'a pas
          // encore ce champ dans localStorage.
          comptePrive: u.comptePrive ?? false
        });
        this.formulaireRempli = true;
      }
    });
  }

  onSubmit(): void {
    if (this.formulaire.invalid) {
      return;
    }

    const valeurs = this.formulaire.getRawValue();
    const demande: DemandeProfil = {
      nomAffichage: valeurs.nomAffichage!,
      photoUrl: valeurs.photoUrl ?? '',
      emailPro: valeurs.emailPro ?? '',
      instagram: valeurs.instagram ?? '',
      twitter: valeurs.twitter ?? '',
      facebook: valeurs.facebook ?? '',
      twitch: valeurs.twitch ?? '',
      youtube: valeurs.youtube ?? '',
      linkedin: valeurs.linkedin ?? '',
      comptePrive: valeurs.comptePrive ?? false
    };

    this.auth.modifierProfil(demande).subscribe({
      next: () => {
        this.enregistre.set(true);
        // Le message de confirmation disparaît seul : il informe, il n'a pas
        // à rester à l'écran indéfiniment.
        setTimeout(() => this.enregistre.set(false), 2500);
      },
      // La bannière vient de erreurInterceptor ; la saisie reste en place.
      error: () => {}
    });
  }

  /** Aperçu live de l'URL saisie, sans attendre l'enregistrement. */
  apercu(): string {
    return this.formulaire.value.photoUrl?.trim() || '';
  }

  /**
   * La demande de permission part d'un CLIC, et c'est obligatoire : les
   * navigateurs ignorent une demande qui ne fait pas suite à une action de
   * l'utilisateur, pour empêcher les sites de la réclamer dès l'ouverture.
   */
  activerNotifications(): void {
    this.notifications.demanderPermission();
  }
}

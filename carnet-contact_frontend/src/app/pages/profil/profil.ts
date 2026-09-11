import { Component, effect, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../services/auth';
import { EtatHttpService } from '../../services/etat-http';
import { NotificationService } from '../../services/notification';

@Component({
  selector: 'app-profil',
  imports: [ReactiveFormsModule],
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

  enregistre = signal(false);

  formulaire = this.fb.group({
    nomAffichage: ['', Validators.required],
    photoUrl: ['']
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
        this.formulaire.patchValue({
          nomAffichage: u.nomAffichage,
          photoUrl: u.photoUrl ?? ''
        });
        this.formulaireRempli = true;
      }
    });
  }

  onSubmit(): void {
    if (this.formulaire.invalid) {
      return;
    }

    const { nomAffichage, photoUrl } = this.formulaire.value;

    this.auth.modifierProfil(nomAffichage!, photoUrl || '').subscribe({
      next: () => {
        this.enregistre.set(true);
        // Le message de confirmation disparaît seul : il informe, il n'a pas
        // à rester à l'écran indéfiniment.
        setTimeout(() => this.enregistre.set(false), 2500);
      }
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

import { Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../services/auth';
import { EtatHttpService } from '../../services/etat-http';
import { CRITERES_MOT_DE_PASSE, CritereMotDePasse, motDePasseSolide } from '../../validateurs/mot-de-passe';

@Component({
  selector: 'app-connexion',
  imports: [ReactiveFormsModule],
  templateUrl: './connexion.html',
  styleUrl: './connexion.css'
})
export class Connexion {
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);
  private router = inject(Router);
  private etatHttp = inject(EtatHttpService);

  chargement = this.etatHttp.chargement;

  // Un seul composant pour les deux usages : on bascule entre « se
  // connecter » et « créer un compte ». Deux pages presque identiques
  // n'auraient rien apporté.
  mode = signal<'connexion' | 'inscription'>('connexion');

  // Message d'erreur LOCAL au formulaire. Distinct de la bannière transverse
  // du service : « mot de passe incorrect » concerne ce formulaire précis, il
  // doit s'afficher à côté de lui, pas en haut de l'application.
  messageErreur = signal<string | null>(null);

  // Affichage en clair du mot de passe, pour relire ce qu'on a tapé. Masquer
  // la saisie protège d'un regard par-dessus l'épaule ; la montrer évite de
  // se tromper trois fois de suite sans comprendre pourquoi. Laisser le choix
  // à l'utilisateur est la seule réponse correcte — lui seul sait s'il est
  // seul devant son écran.
  motDePasseVisible = signal(false);

  // La liste des critères, pour l'afficher à cocher pendant la saisie.
  criteres = CRITERES_MOT_DE_PASSE;

  formulaire = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    motDePasse: ['', [Validators.required]],
    nomAffichage: ['']
  });

  basculer(): void {
    this.mode.update(m => (m === 'connexion' ? 'inscription' : 'connexion'));
    this.messageErreur.set(null);

    const champ = this.formulaire.controls.motDePasse;

    if (this.mode() === 'inscription') {
      champ.setValidators([Validators.required, motDePasseSolide]);
    } else {
      // Pourquoi RETIRER la règle à la connexion ? Parce qu'elle ne s'applique
      // qu'aux mots de passe qu'on CRÉE. Les comptes existants ont pu être
      // créés sous une politique plus souple ; exiger la nouvelle règle pour
      // se connecter empêcherait purement et simplement leurs propriétaires
      // d'entrer. Le serveur applique d'ailleurs la même distinction.
      champ.setValidators([Validators.required]);
    }

    // setValidators() change la règle mais ne REJOUE pas la validation :
    // sans cet appel, le champ garderait l'état (valide / invalide) calculé
    // avec l'ancienne règle jusqu'à la prochaine frappe.
    champ.updateValueAndValidity();
  }

  basculerVisibiliteMotDePasse(): void {
    this.motDePasseVisible.update(v => !v);
  }

  /**
   * true si ce critère est satisfait par la saisie en cours.
   *
   * On réutilise la fonction `verifie` du critère lui-même, plutôt que de
   * relire l'erreur du validateur : c'est la MÊME source pour la validation et
   * pour l'affichage, donc aucun risque que la coche verte et le bouton
   * désactivé racontent deux histoires différentes.
   */
  critereSatisfait(critere: CritereMotDePasse): boolean {
    const valeur: string = this.formulaire.controls.motDePasse.value ?? '';
    return valeur !== '' && critere.verifie(valeur);
  }

  onSubmit(): void {
    if (this.formulaire.invalid) {
      return;
    }

    this.messageErreur.set(null);
    const { email, motDePasse, nomAffichage } = this.formulaire.value;

    // Ici on utilise la forme OBJET de subscribe (section 15) : on a besoin
    // du callback error, parce que la suite dépend du résultat — naviguer en
    // cas de succès, afficher un message sinon.
    const appel = this.mode() === 'connexion'
      ? this.auth.connexion(email!, motDePasse!)
      : this.auth.inscription(email!, motDePasse!, nomAffichage || '');

    appel.subscribe({
      next: () => this.router.navigate(['/']),
      error: (erreur: HttpErrorResponse) => {
        this.messageErreur.set(this.messagePour(erreur));
      }
    });
  }

  private messagePour(erreur: HttpErrorResponse): string {
    // Le backend renvoie son explication dans le corps de la réponse : on
    // l'affiche telle quelle quand elle existe, elle est plus précise que
    // tout ce qu'on pourrait deviner depuis le code de statut.
    if (typeof erreur.error === 'string' && erreur.error.trim() !== '') {
      return erreur.error;
    }

    if (erreur.status === 401) {
      return 'Email ou mot de passe incorrect.';
    }
    if (erreur.status === 409) {
      return 'Un compte existe déjà avec cet email.';
    }
    if (erreur.status === 0) {
      return 'Serveur injoignable. Est-il bien démarré ?';
    }
    return 'La demande a échoué. Réessayez.';
  }
}

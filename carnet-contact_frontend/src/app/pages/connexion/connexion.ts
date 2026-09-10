import { Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../services/auth';
import { EtatHttpService } from '../../services/etat-http';

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

  formulaire = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    motDePasse: ['', [Validators.required, Validators.minLength(6)]],
    nomAffichage: ['']
  });

  basculer(): void {
    this.mode.update(m => (m === 'connexion' ? 'inscription' : 'connexion'));
    this.messageErreur.set(null);
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

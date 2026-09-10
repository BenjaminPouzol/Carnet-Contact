import { Component, effect, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { EtatHttpService } from './services/etat-http';
import { AuthService } from './services/auth';
import { MessageService } from './services/message';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  private router = inject(Router);

  protected etatHttp = inject(EtatHttpService);
  protected auth = inject(AuthService);
  protected messages = inject(MessageService);

  constructor() {
    // La pastille de messages non lus doit être remplie dès qu'on est
    // connecté, pas seulement en visitant la page Messages. Un effect() sur
    // connecte() couvre les deux entrées possibles dans cet état : la
    // connexion via le formulaire, et la restauration de session au
    // rechargement de la page.
    effect(() => {
      if (this.auth.connecte()) {
        this.messages.chargerNonLus();
      }
    });
  }

  protected deconnexion(): void {
    this.auth.deconnexion();
    this.router.navigate(['/connexion']);
  }
}

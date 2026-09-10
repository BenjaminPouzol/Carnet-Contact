import { Component, inject, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { EtatHttpService } from './services/etat-http';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  protected readonly title = signal('carnet-contact');

  // La coquille n'a plus besoin du service MÉTIER : ses deux bannières sont
  // alimentées par les intercepteurs, via ce service transverse. Un affichage
  // transverse dépend désormais d'un état transverse — plus de ContactService
  // injecté ici juste pour lire deux signaux qui ne parlaient pas de contacts.
  protected etatHttp = inject(EtatHttpService);
}

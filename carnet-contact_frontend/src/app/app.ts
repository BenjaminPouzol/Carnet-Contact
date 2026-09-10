import { Component, inject, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ContactService } from './services/contact';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  protected readonly title = signal('carnet-contact');

  // La coquille est le bon endroit pour un affichage transverse comme
  // une bannière d'erreur : elle est visible quelle que soit la page.
  protected contactService = inject(ContactService);
}

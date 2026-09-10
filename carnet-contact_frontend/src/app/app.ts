import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  // App ne contient plus que ce qui est commun à TOUTES les pages.
  protected readonly title = signal('carnet-contact');
}

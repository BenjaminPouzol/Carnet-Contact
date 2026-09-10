import { Routes } from '@angular/router';
import { Accueil } from './pages/accueil/accueil';
import { ContactDetail } from './pages/contact-detail/contact-detail';

export const routes: Routes = [
  // pathMatch: 'full' = ne correspondre QUE si l'URL est entièrement vide.
  { path: '', component: Accueil, pathMatch: 'full' },

  // ':id' est un segment variable : il capture n'importe quelle valeur
  // et la range sous le nom "id" (contact/5, contact/12...).
  { path: 'contact/:id', component: ContactDetail }
];

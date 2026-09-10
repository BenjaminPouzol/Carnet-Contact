import { Routes } from '@angular/router';
import { Accueil } from './pages/accueil/accueil';
import { ContactDetail } from './pages/contact-detail/contact-detail';
import { ContactEdit } from './pages/contact-edit/contact-edit';

export const routes: Routes = [
  { path: '', component: Accueil, pathMatch: 'full' },
  { path: 'contact/:id', component: ContactDetail },
  { path: 'contact/:id/modifier', component: ContactEdit }
];

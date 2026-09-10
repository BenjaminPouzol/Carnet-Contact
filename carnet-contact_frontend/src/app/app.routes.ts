import { Routes } from '@angular/router';
import { Accueil } from './pages/accueil/accueil';
import { ContactDetail } from './pages/contact-detail/contact-detail';
import { ContactEdit } from './pages/contact-edit/contact-edit';
import { Connexion } from './pages/connexion/connexion';
import { Messages } from './pages/messages/messages';
import { Profil } from './pages/profil/profil';
import { authGuard } from './auth-guard';

export const routes: Routes = [
  // Seule route publique.
  { path: 'connexion', component: Connexion },

  // canActivate : la garde s'exécute avant d'activer la route. Un tableau,
  // car on peut en enchaîner plusieurs (toutes doivent dire oui).
  { path: '', component: Accueil, pathMatch: 'full', canActivate: [authGuard] },
  { path: 'contact/:id', component: ContactDetail, canActivate: [authGuard] },
  { path: 'contact/:id/modifier', component: ContactEdit, canActivate: [authGuard] },
  { path: 'messages', component: Messages, canActivate: [authGuard] },
  { path: 'profil', component: Profil, canActivate: [authGuard] }
];

import { Routes } from '@angular/router';
import { Accueil } from './pages/accueil/accueil';
import { ContactDetail } from './pages/contact-detail/contact-detail';
import { ContactEdit } from './pages/contact-edit/contact-edit';
import { Connexion } from './pages/connexion/connexion';
import { Messages } from './pages/messages/messages';
import { Profil } from './pages/profil/profil';
import { authGuard } from './auth-guard';
import { adminGuard } from './admin-guard';

export const routes: Routes = [
  // Seule route publique.
  { path: 'connexion', component: Connexion },

  // canActivate : la garde s'exécute avant d'activer la route. Un tableau,
  // car on peut en enchaîner plusieurs (toutes doivent dire oui).
  { path: '', component: Accueil, pathMatch: 'full', canActivate: [authGuard] },
  { path: 'contact/:id', component: ContactDetail, canActivate: [authGuard] },
  { path: 'contact/:id/modifier', component: ContactEdit, canActivate: [authGuard] },
  { path: 'messages', component: Messages, canActivate: [authGuard] },
  { path: 'profil', component: Profil, canActivate: [authGuard] },

  /**
   * Deux gardes qui s'enchaînent, dans cet ordre : être connecté, PUIS être
   * administrateur. L'ordre compte — un visiteur anonyme doit atterrir sur la
   * page de connexion, pas sur l'accueil.
   *
   * loadComponent, au lieu de `component` : c'est du CHARGEMENT DIFFÉRÉ.
   *
   * Toutes les autres pages sont importées en haut du fichier, donc empaquetées
   * dans le fichier JavaScript principal : tout le monde les télécharge au
   * premier affichage. C'est le bon choix pour des pages que tout le monde
   * visite.
   *
   * Ici, non. Le panel d'administration traîne derrière lui tout p-table, et
   * il ne concerne qu'une poignée de comptes. `import()` retourne une promesse :
   * Angular ne déclenche le téléchargement de ce morceau de code qu'au moment
   * où quelqu'un navigue vers /admin. Les autres ne le paient jamais.
   *
   * La règle générale : différer ce qui est lourd ET rare. Différer une page
   * visitée par tous ne ferait qu'ajouter une attente au moment du clic.
   */
  {
    path: 'admin',
    canActivate: [authGuard, adminGuard],
    loadComponent: () => import('./pages/admin/admin').then(m => m.Admin)
  }
];

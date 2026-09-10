import { RenderMode, ServerRoute } from '@angular/ssr';

export const serverRoutes: ServerRoute[] = [
  // La page de connexion ne dépend d'aucune donnée : elle peut être
  // pré-générée au build, et s'affiche donc instantanément.
  { path: 'connexion', renderMode: RenderMode.Prerender },

  // Toutes les autres routes sont derrière la garde d'authentification, qui
  // s'appuie sur localStorage — inexistant côté serveur. Les prérendre
  // produirait le HTML de la page de connexion pour chacune, servi ensuite à
  // des utilisateurs déjà connectés. RenderMode.Client laisse le navigateur
  // les construire, quand la session est connue.
  { path: '**', renderMode: RenderMode.Client }
];

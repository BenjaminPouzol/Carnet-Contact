import { RenderMode, ServerRoute } from '@angular/ssr';

export const serverRoutes: ServerRoute[] = [
  // Routes paramétrées : les id n'existent qu'à l'exécution, dans la base.
  // Impossible de les prérendre au build — le navigateur les construit.
  { path: 'contact/:id', renderMode: RenderMode.Client },
  { path: 'contact/:id/modifier', renderMode: RenderMode.Client },

  // Toutes les autres routes sont pré-générées au build.
  { path: '**', renderMode: RenderMode.Prerender }
];

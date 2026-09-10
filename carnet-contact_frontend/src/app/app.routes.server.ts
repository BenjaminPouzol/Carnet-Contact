import { RenderMode, ServerRoute } from '@angular/ssr';

export const serverRoutes: ServerRoute[] = [
  {
    // Route paramétrée : les id n'existent qu'à l'exécution, dans la base.
    // Impossible de pré-générer ces pages au moment du build — on laisse
    // donc le navigateur les construire.
    path: 'contact/:id',
    renderMode: RenderMode.Client
  },
  {
    // Toutes les autres routes sont pré-générées au build.
    path: '**',
    renderMode: RenderMode.Prerender
  }
];

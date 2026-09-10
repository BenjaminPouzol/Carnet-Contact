import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';
import { provideClientHydration, withEventReplay } from '@angular/platform-browser';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { baseUrlInterceptor } from './interceptors/base-url-interceptor';
import { authInterceptor } from './interceptors/auth-interceptor';
import { chargementInterceptor } from './interceptors/chargement-interceptor';
import { erreurInterceptor } from './interceptors/erreur-interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideClientHydration(withEventReplay()),
    // L'ORDRE compte : à l'aller, la requête traverse le tableau de haut en
    // bas ; au retour, la réponse le remonte de bas en haut.
    // base-url réécrit donc l'URL avant que les autres ne la voient, et le
    // catchError de erreur s'exécute avant le finalize de chargement.
    provideHttpClient(
      withInterceptors([
        baseUrlInterceptor,
        authInterceptor,
        chargementInterceptor,
        erreurInterceptor
      ])
    )
  ]
};

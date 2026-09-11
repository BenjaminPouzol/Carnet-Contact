import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';
import { providePrimeNG } from 'primeng/config';
import Aura from '@primeuix/themes/aura';
import { definePreset } from '@primeuix/themes';

import { routes } from './app.routes';
import { provideClientHydration, withEventReplay } from '@angular/platform-browser';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { baseUrlInterceptor } from './interceptors/base-url-interceptor';
import { authInterceptor } from './interceptors/auth-interceptor';
import { chargementInterceptor } from './interceptors/chargement-interceptor';
import { erreurInterceptor } from './interceptors/erreur-interceptor';
import { rafraichissementInterceptor } from './interceptors/rafraichissement-interceptor';

/**
 * Le thème PrimeNG, ajusté aux couleurs du carnet.
 *
 * `definePreset` part d'un thème existant (Aura) et n'en redéfinit que ce qu'on
 * veut changer — ici la palette « primary », pour retrouver le bleu de
 * l'application. Repartir de zéro obligerait à décrire les centaines de jetons
 * dont Aura a déjà des valeurs raisonnables.
 *
 * Les 50 à 950 sont des NUANCES : PrimeNG s'en sert pour dériver tout seul les
 * états (survol, focus, désactivé) et les deux modes clair et sombre. C'est
 * exactement ce que faisait `--bleu-pale` / `--bleu` / `--bleu-fonce` à la main
 * (section 21), en beaucoup plus complet.
 */
const themeCarnet = definePreset(Aura, {
  semantic: {
    primary: {
      50: '#eff5ff',
      100: '#dbe8ff',
      200: '#bed7ff',
      300: '#91beff',
      400: '#5d9bff',
      500: '#3d82ff',
      600: '#0b5fff',
      700: '#0740b5',
      800: '#0a379a',
      900: '#0d3179',
      950: '#091f4a'
    }
  }
});

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideClientHydration(withEventReplay()),

    providePrimeNG({
      theme: {
        preset: themeCarnet,
        options: {
          // LA ligne qui fait le mode sombre. PrimeNG génère les deux jeux de
          // couleurs, et bascule dès que ce sélecteur correspond. On vise un
          // attribut plutôt qu'une classe : `data-theme` DIT ce qu'il est,
          // là où une classe `.sombre` se confondrait avec du style ordinaire.
          darkModeSelector: '[data-theme="sombre"]',

          // Les styles de PrimeNG sont placés dans une couche CSS nommée, et
          // `order` fixe l'ordre des couches : la dernière nommée l'emporte.
          //
          // `primeng` est donc placée APRÈS `base`, la couche où styles.css
          // range ses styles de balises (button, input). Un p-button garde
          // ainsi l'habillage de la bibliothèque, tandis qu'un <button>
          // ordinaire garde le nôtre.
          //
          // Ce qui, dans styles.css, reste HORS de toute couche (nos classes
          // .carte, .muet…) continue de dominer PrimeNG : on peut donc encore
          // retoucher un composant sans surenchérir en sélecteurs.
          cssLayer: {
            name: 'primeng',
            order: 'theme, base, primeng'
          }
        }
      }
    }),
    // L'ORDRE compte : à l'aller, la requête traverse le tableau de haut en
    // bas ; au retour, la réponse le remonte de bas en haut.
    // base-url réécrit donc l'URL avant que les autres ne la voient, et le
    // catchError de erreur s'exécute avant le finalize de chargement.
    //
    // rafraichissement est placé EN DERNIER, donc au plus profond : c'est lui
    // qui voit l'erreur en premier au retour. Il peut ainsi rattraper un 401
    // avant que erreurInterceptor ne déconnecte l'utilisateur.
    provideHttpClient(
      withInterceptors([
        baseUrlInterceptor,
        authInterceptor,
        chargementInterceptor,
        erreurInterceptor,
        rafraichissementInterceptor
      ])
    )
  ]
};

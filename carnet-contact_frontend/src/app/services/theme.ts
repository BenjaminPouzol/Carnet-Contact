import { Injectable, PLATFORM_ID, computed, effect, inject, signal } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';

export type Theme = 'clair' | 'sombre';

const CLE_THEME = 'carnet.theme';

/**
 * Le thème clair ou sombre.
 *
 * Tout le travail visuel est fait par le CSS : un attribut `data-theme` sur la
 * balise <html> suffit à faire basculer les deux jeux de variables (styles.css)
 * ET le thème PrimeNG (déclaré par `darkModeSelector` dans app.config.ts). Ce
 * service ne fait donc qu'une chose : décider de la valeur de cet attribut, et
 * s'en souvenir.
 *
 * C'est la bonne répartition : le CSS sait peindre, le TypeScript sait décider.
 * Faire basculer les couleurs depuis le code aurait demandé de connaître, ici,
 * chaque couleur de chaque composant.
 */
@Injectable({
  providedIn: 'root'
})
export class ThemeService {
  private navigateur = isPlatformBrowser(inject(PLATFORM_ID));

  private themeSignal = signal<Theme>('clair');
  readonly theme = this.themeSignal.asReadonly();
  readonly sombre = computed(() => this.themeSignal() === 'sombre');

  constructor() {
    if (this.navigateur) {
      this.themeSignal.set(this.themeInitial());

      // Un effect() plutôt qu'un appel dans chaque méthode : l'attribut suit
      // le signal, quelle qu'en soit la cause. Une bascule écrite plus tard,
      // ou un thème restauré au démarrage, n'auront rien à penser.
      effect(() => this.appliquer(this.themeSignal()));
    }
  }

  /**
   * Le thème de départ : celui qu'on a choisi la dernière fois, sinon celui du
   * système d'exploitation.
   *
   * Respecter la préférence système est le comportement attendu aujourd'hui :
   * quelqu'un qui a réglé tout son ordinateur en sombre ne s'attend pas à
   * recevoir une page blanche. Mais un choix explicite l'emporte toujours —
   * il est plus récent, et plus précis, que le réglage global.
   */
  private themeInitial(): Theme {
    const memorise = localStorage.getItem(CLE_THEME);

    if (memorise === 'clair' || memorise === 'sombre') {
      return memorise;
    }

    // matchMedia interroge une media-query depuis le JavaScript, exactement
    // comme le ferait une @media en CSS.
    //
    // Le `typeof` n'est pas de la superstition : isPlatformBrowser dit qu'on
    // est « dans un navigateur », mais le DOM simulé des tests en est un très
    // partiel, où matchMedia n'existe pas. Vérifier la PLATEFORME ne dit rien
    // de la FONCTIONNALITÉ — d'où ce second garde-fou, qui retombe simplement
    // sur le thème clair quand la préférence système est hors de portée.
    if (typeof window.matchMedia !== 'function') {
      return 'clair';
    }

    return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'sombre' : 'clair';
  }

  private appliquer(theme: Theme): void {
    // On vise <html> et non <body> : les variables sont déclarées sur :root,
    // et certains éléments (les dialogues PrimeNG, par exemple) se placent
    // hors du <body> de l'application.
    document.documentElement.setAttribute('data-theme', theme);
    localStorage.setItem(CLE_THEME, theme);
  }

  basculer(): void {
    this.themeSignal.update(t => (t === 'clair' ? 'sombre' : 'clair'));
  }
}

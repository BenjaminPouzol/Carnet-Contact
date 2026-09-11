import { TestBed } from '@angular/core/testing';
import { ThemeService } from './theme';

describe('ThemeService', () => {
  beforeEach(() => {
    localStorage.clear();
    document.documentElement.removeAttribute('data-theme');
    TestBed.configureTestingModule({});
  });

  it('démarre en clair quand rien n\'a été choisi', () => {
    // Le DOM simulé des tests n'a pas matchMedia : le service doit malgré
    // tout se décider, sans lever d'erreur. C'est précisément ce que vérifie
    // ce test — la robustesse au manque, pas la préférence système.
    expect(TestBed.inject(ThemeService).theme()).toBe('clair');
  });

  it('restaure le thème mémorisé au démarrage', () => {
    localStorage.setItem('carnet.theme', 'sombre');

    const service = TestBed.inject(ThemeService);

    expect(service.theme()).toBe('sombre');
    expect(service.sombre()).toBe(true);
  });

  /**
   * LE test qui compte : la bascule doit produire un EFFET VISIBLE, pas
   * seulement changer un signal. Tout le mode sombre repose sur cet attribut —
   * un service qui garderait la bonne valeur sans l'écrire sur <html> laisserait
   * la page obstinément claire.
   */
  it('écrit l\'attribut sur <html> et mémorise le choix', () => {
    const service = TestBed.inject(ThemeService);
    TestBed.tick();

    service.basculer();
    // tick() laisse les effect() s'exécuter : ils sont différés, pas
    // synchrones. Sans lui, l'attribut ne serait pas encore posé.
    TestBed.tick();

    expect(service.theme()).toBe('sombre');
    expect(document.documentElement.getAttribute('data-theme')).toBe('sombre');
    expect(localStorage.getItem('carnet.theme')).toBe('sombre');

    service.basculer();
    TestBed.tick();

    expect(document.documentElement.getAttribute('data-theme')).toBe('clair');
    expect(localStorage.getItem('carnet.theme')).toBe('clair');
  });
});

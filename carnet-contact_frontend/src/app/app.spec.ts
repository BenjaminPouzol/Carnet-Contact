import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { App } from './app';
import { SessionService } from './services/session';
import { MessageService } from './services/message';
import { ActiviteService } from './services/activite';
import { EtatHttpService } from './services/etat-http';
import { unUtilisateur } from './donnees-test';

/**
 * La coquille de l'application : en-tête, navigation, bannières transverses.
 *
 * Tester un composant, c'est vérifier ce que l'utilisateur VOIT à partir d'un
 * état donné — pas relire ses variables internes. On règle donc l'état par les
 * services, puis on interroge le HTML produit.
 */
describe('App', () => {
  let backend: HttpTestingController;

  /**
   * Le suivi des non-lus démarre sur un `timer(0, …)`, dont la première
   * émission passe par la file des tâches du navigateur — même avec un délai
   * de zéro. `whenStable()` ne l'attend pas : il faut rendre la main une fois
   * pour que la requête parte.
   */
  const rendreLaMain = () => new Promise(resolve => setTimeout(resolve, 0));

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [App],
      providers: [
        // La coquille contient <router-outlet /> et des [routerLink] : sans
        // routeur configuré, ces directives ne trouvent pas leurs dépendances.
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });

    backend = TestBed.inject(HttpTestingController);
    localStorage.clear();
  });

  afterEach(() => {
    // Les deux suivis périodiques sont des timers : sans arrêt explicite, ils
    // continueraient de tourner d'un test à l'autre.
    TestBed.inject(MessageService).arreterSuiviNonLus();
    TestBed.inject(ActiviteService).arreterSuivi();
  });

  it('masque la navigation tant que personne n\'est connecté', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();

    const html = fixture.nativeElement as HTMLElement;
    expect(html.querySelector('nav')).toBeNull();
    expect(html.textContent).not.toContain('Déconnexion');
  });

  it('affiche la navigation et le compte une fois connecté', async () => {
    TestBed.inject(SessionService).ouvrir('jeton', 'rafraichissement', unUtilisateur());

    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();

    // Se connecter démarre les deux suivis : messages non lus et notifications
    // d'abonnement. Les deux requêtes partent, on répond à chacune.
    await rendreLaMain();
    backend.expectOne('/api/messages/non-lus').flush([]);
    backend.expectOne('/api/notifications/non-lues').flush([]);
    await fixture.whenStable();

    const html = fixture.nativeElement as HTMLElement;
    expect(html.querySelector('nav')).not.toBeNull();
    expect(html.textContent).toContain('Alice');
    expect(html.textContent).toContain('Abonnements');
  });

  it('affiche la bannière d\'erreur alimentée par l\'intercepteur', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();

    TestBed.inject(EtatHttpService).signalerErreur('Le serveur est injoignable.');
    await fixture.whenStable();

    const banniere = (fixture.nativeElement as HTMLElement).querySelector('.erreur');
    expect(banniere?.textContent).toContain('Le serveur est injoignable.');
  });
});

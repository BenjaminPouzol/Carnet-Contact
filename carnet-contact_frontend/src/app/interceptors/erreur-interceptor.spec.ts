import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { erreurInterceptor } from './erreur-interceptor';
import { chargementInterceptor } from './chargement-interceptor';
import { contexte } from './http-contexte';
import { EtatHttpService } from '../services/etat-http';

/**
 * Un intercepteur se teste comme un service HTTP : on l'enregistre dans
 * `provideHttpClient(withInterceptors([...]))`, on lance une requête, et on
 * observe ce qu'il a produit — ici le contenu du signal d'erreur.
 *
 * `subscribe({ error: () => {} })` est nécessaire sur chaque appel : ces
 * intercepteurs RELANCENT l'erreur, et une erreur sans gestionnaire remonterait
 * comme un échec du test.
 */
describe('erreurInterceptor', () => {
  let http: HttpClient;
  let backend: HttpTestingController;
  let etat: EtatHttpService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([chargementInterceptor, erreurInterceptor])),
        provideHttpClientTesting()
      ]
    });

    http = TestBed.inject(HttpClient);
    backend = TestBed.inject(HttpTestingController);
    etat = TestBed.inject(EtatHttpService);
  });

  afterEach(() => backend.verify());

  /**
   * Le cœur du chantier « nuance des messages ». Avant le contexte, ce message
   * se réduisait à « Erreur interne du serveur (500) » : exact, mais muet sur
   * ce que l'utilisateur venait de tenter.
   */
  it('compose le libellé métier et la raison technique', () => {
    http.get('/api/contacts', {
      context: contexte({ libelle: 'Impossible de charger les contacts' })
    }).subscribe({ error: () => {} });

    backend.expectOne('/api/contacts').flush('', { status: 500, statusText: 'Server Error' });

    expect(etat.erreur())
      .toBe('Impossible de charger les contacts : le serveur a rencontré une erreur interne (500).');
  });

  it('explique un fichier trop volumineux (413) et un format refusé (415)', () => {
    http.post('/api/images', {}).subscribe({ error: () => {} });
    backend.expectOne('/api/images').flush('', { status: 413, statusText: 'Payload Too Large' });
    expect(etat.erreur()).toBe('L\'image dépasse la taille autorisée (413).');

    http.post('/api/images', {}).subscribe({ error: () => {} });
    backend.expectOne('/api/images').flush('', { status: 415, statusText: 'Unsupported Media Type' });
    expect(etat.erreur()).toBe('Ce format de fichier n\'est pas accepté (415).');
  });

  it('se rabat sur la seule raison technique quand aucun libellé n\'est fourni', () => {
    http.get('/api/quelquechose').subscribe({ error: () => {} });

    backend.expectOne('/api/quelquechose').flush('', { status: 0, statusText: 'Unknown Error' });

    expect(etat.erreur()).toBe('Le serveur est injoignable.');
  });

  it('efface le message précédent au départ d\'une nouvelle requête', () => {
    etat.signalerErreur('Un vieux message.');

    http.get('/api/contacts').subscribe({ error: () => {} });
    // Le message est effacé dès le DÉPART de la requête, avant toute réponse.
    expect(etat.erreur()).toBeNull();

    backend.expectOne('/api/contacts').flush([]);
  });

  /**
   * Le drapeau « discret », qui rend le sondage de la messagerie supportable :
   * sans lui, une coupure réseau passagère ferait apparaître une bannière
   * d'erreur toutes les cinq secondes, pour une requête que personne n'a
   * demandée.
   */
  it('reste muet sur une requête de fond', () => {
    http.get('/api/messages/non-lus', { context: contexte({ discret: true }) })
      .subscribe({ error: () => {} });

    backend.expectOne('/api/messages/non-lus')
      .flush('', { status: 500, statusText: 'Server Error' });

    expect(etat.erreur()).toBeNull();
  });

  it('n\'allume pas l\'indicateur de chargement sur une requête de fond', () => {
    http.get('/api/messages/non-lus', { context: contexte({ discret: true }) }).subscribe();
    expect(etat.chargement()).toBe(false);

    backend.expectOne('/api/messages/non-lus').flush([]);
  });

  it('allume puis éteint l\'indicateur sur une requête ordinaire', () => {
    http.get('/api/contacts').subscribe();
    expect(etat.chargement()).toBe(true);

    backend.expectOne('/api/contacts').flush([]);
    expect(etat.chargement()).toBe(false);
  });

  it('laisse les appels d\'authentification afficher leur propre message', () => {
    http.post('/api/auth/connexion', {}).subscribe({ error: () => {} });

    backend.expectOne('/api/auth/connexion')
      .flush('', { status: 401, statusText: 'Unauthorized' });

    // Le formulaire de connexion affiche « mot de passe incorrect » à côté du
    // champ : une bannière en haut de page ferait doublon.
    expect(etat.erreur()).toBeNull();
  });
});

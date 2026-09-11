import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { authInterceptor } from './auth-interceptor';
import { erreurInterceptor } from './erreur-interceptor';
import { rafraichissementInterceptor } from './rafraichissement-interceptor';
import { SessionService } from '../services/session';

/**
 * Le scénario le plus difficile à vérifier à la main : il faudrait attendre
 * quinze minutes qu'un jeton expire. Un test le provoque en une ligne, en
 * répondant 401.
 *
 * On enregistre les trois intercepteurs dans le MÊME ORDRE que dans
 * app.config.ts : leur ordre fait partie du comportement testé — c'est lui qui
 * garantit que le rafraîchissement voit le 401 avant la déconnexion.
 */
describe('rafraichissementInterceptor', () => {
  let http: HttpClient;
  let backend: HttpTestingController;
  let session: SessionService;

  const compte = { id: 1, email: 'alice@exemple.fr', nomAffichage: 'Alice' };

  beforeEach(() => {
    localStorage.clear();

    TestBed.configureTestingModule({
      providers: [
        // La route existe pour de vrai : erreurInterceptor renvoie vers
        // /connexion quand le renouvellement échoue, et un routeur vide
        // rejetterait cette navigation.
        provideRouter([{ path: 'connexion', children: [] }]),
        provideHttpClient(withInterceptors([
          authInterceptor,
          erreurInterceptor,
          rafraichissementInterceptor
        ])),
        provideHttpClientTesting()
      ]
    });

    http = TestBed.inject(HttpClient);
    backend = TestBed.inject(HttpTestingController);
    session = TestBed.inject(SessionService);

    session.ouvrir('jeton-perime', 'rafraichissement-1', compte);
  });

  afterEach(() => backend.verify());

  it('renouvelle le jeton puis rejoue la requête qui avait échoué', () => {
    let recu: unknown;
    http.get('/api/contacts').subscribe(reponse => { recu = reponse; });

    // 1. La requête part avec le jeton périmé et se fait refuser.
    const premiereTentative = backend.expectOne('/api/contacts');
    expect(premiereTentative.request.headers.get('Authorization')).toBe('Bearer jeton-perime');
    premiereTentative.flush('', { status: 401, statusText: 'Unauthorized' });

    // 2. L'intercepteur demande une paire neuve, sans que l'appelant le sache.
    const renouvellement = backend.expectOne('/api/auth/rafraichir');
    expect(renouvellement.request.body).toEqual({ jetonRafraichissement: 'rafraichissement-1' });
    renouvellement.flush({
      jeton: 'jeton-neuf',
      jetonRafraichissement: 'rafraichissement-2',
      utilisateur: compte
    });

    // 3. La requête d'origine repart, avec le nouveau jeton.
    const secondeTentative = backend.expectOne('/api/contacts');
    expect(secondeTentative.request.headers.get('Authorization')).toBe('Bearer jeton-neuf');
    secondeTentative.flush([{ id: 1 }]);

    // 4. L'appelant reçoit la vraie réponse : de son point de vue, rien ne
    //    s'est passé. C'est tout l'intérêt — il n'a pas perdu sa saisie.
    expect(recu).toEqual([{ id: 1 }]);
    // Et la rotation a bien été mémorisée, sinon la prochaine expiration
    // présenterait un jeton déjà révoqué.
    expect(session.jetonRafraichissementActuel()).toBe('rafraichissement-2');
  });

  /**
   * Le test qui justifie le `shareReplay` de RafraichissementService.
   *
   * Quand un jeton expire, ce n'est pas une requête qui échoue mais toutes
   * celles en vol. Sans mise en commun, chacune lancerait son propre
   * renouvellement — et comme le serveur révoque l'ancien jeton à chaque
   * rotation, les suivants échoueraient, déconnectant l'utilisateur alors que
   * tout allait bien.
   */
  it('ne lance qu\'un seul renouvellement pour plusieurs 401 simultanés', () => {
    http.get('/api/contacts').subscribe({ error: () => {} });
    http.get('/api/messages/non-lus').subscribe({ error: () => {} });

    backend.expectOne('/api/contacts').flush('', { status: 401, statusText: 'Unauthorized' });
    backend.expectOne('/api/messages/non-lus')
      .flush('', { status: 401, statusText: 'Unauthorized' });

    // UN seul appel de renouvellement, pas deux.
    const renouvellements = backend.match('/api/auth/rafraichir');
    expect(renouvellements.length).toBe(1);

    renouvellements[0].flush({
      jeton: 'jeton-neuf',
      jetonRafraichissement: 'rafraichissement-2',
      utilisateur: compte
    });

    // Les deux requêtes repartent, chacune avec le nouveau jeton.
    backend.expectOne('/api/contacts').flush([]);
    backend.expectOne('/api/messages/non-lus').flush([]);
  });

  it('vide la session quand le renouvellement échoue à son tour', () => {
    http.get('/api/contacts').subscribe({ error: () => {} });

    backend.expectOne('/api/contacts').flush('', { status: 401, statusText: 'Unauthorized' });
    backend.expectOne('/api/auth/rafraichir')
      .flush('', { status: 401, statusText: 'Unauthorized' });

    // Plus de porte de sortie : la session est réellement terminée.
    expect(session.jetonActuel()).toBeNull();
    expect(session.jetonRafraichissementActuel()).toBeNull();
  });

  it('ne tente rien sur une erreur qui n\'est pas un 401', () => {
    http.get('/api/contacts').subscribe({ error: () => {} });

    backend.expectOne('/api/contacts').flush('', { status: 500, statusText: 'Server Error' });

    // Un 500 ne se règle pas en changeant de jeton : le rejouer n'aurait aucune
    // chance de mieux marcher, et masquerait la vraie panne.
    backend.expectNone('/api/auth/rafraichir');
    expect(session.jetonActuel()).toBe('jeton-perime');
  });

  it('ne tente pas de renouveler un appel d\'authentification', () => {
    http.post('/api/auth/connexion', {}).subscribe({ error: () => {} });

    backend.expectOne('/api/auth/connexion')
      .flush('', { status: 401, statusText: 'Unauthorized' });

    // Sans cette exclusion, /rafraichir se rappellerait lui-même à l'infini.
    backend.expectNone('/api/auth/rafraichir');
  });
});

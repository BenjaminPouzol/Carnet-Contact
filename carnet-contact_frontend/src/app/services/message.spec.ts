import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { MessageService } from './message';
import { NotificationService } from './notification';
import { Message } from '../message.model';
import { unMessage, uneReaction, unUtilisateur } from '../donnees-test';

/** L'intervalle du sondage des non-lus, tel que défini dans le service. */
const INTERVALLE_NON_LUS_MS = 15000;

describe('MessageService — notification des nouveaux messages', () => {
  let service: MessageService;
  let backend: HttpTestingController;
  let notifications: NotificationService;

  const bob = unUtilisateur({ id: 2, email: 'bob@exemple.fr', nomAffichage: 'Bob' });
  const moi = unUtilisateur();

  function message(id: number, contenu: string): Message {
    return unMessage({ id, expediteur: bob, destinataire: moi, contenu });
  }

  /**
   * Le sondage repose sur un `timer`. Avec les minuteurs simulés de vitest, on
   * « avance le temps » à la demande : un tour de sondage se déclenche
   * instantanément au lieu d'attendre quinze vraies secondes.
   *
   * La variante `…Async` est indispensable ici : elle vide aussi la file des
   * micro-tâches, donc la requête HTTP a bien le temps de partir avant qu'on
   * n'aille la chercher.
   */
  const avancerDe = (ms: number) => vi.advanceTimersByTimeAsync(ms);

  beforeEach(() => {
    vi.useFakeTimers();

    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()]
    });

    service = TestBed.inject(MessageService);
    backend = TestBed.inject(HttpTestingController);
    notifications = TestBed.inject(NotificationService);
  });

  afterEach(() => {
    service.arreterSuiviNonLus();
    backend.verify();
    // Toujours rendre les vrais minuteurs, sinon les tests suivants
    // hériteraient d'un temps figé.
    vi.useRealTimers();
  });

  /**
   * LE test qui compte. Le sondage renvoie à chaque tour la liste COMPLÈTE des
   * non-lus : sans mémoire du tour précédent, ouvrir l'application annoncerait
   * d'un coup tous les messages en attente, puis recommencerait toutes les
   * quinze secondes, indéfiniment.
   */
  it('n\'annonce pas les messages déjà en attente à l\'ouverture', async () => {
    const espion = vi.spyOn(notifications, 'notifier');

    service.demarrerSuiviNonLus();
    await avancerDe(0);

    backend.expectOne('/api/messages/non-lus')
      .flush([message(1, 'Bonjour'), message(2, 'Tu es là ?')]);

    expect(espion).not.toHaveBeenCalled();
    // La pastille, elle, doit bien afficher les deux.
    expect(service.nonLus().length).toBe(2);
  });

  it('annonce uniquement les messages arrivés depuis le tour précédent', async () => {
    const espion = vi.spyOn(notifications, 'notifier');

    service.demarrerSuiviNonLus();
    await avancerDe(0);
    backend.expectOne('/api/messages/non-lus').flush([message(1, 'Bonjour')]);

    // Tour suivant : le message 1 était déjà connu, le 2 est nouveau.
    await avancerDe(INTERVALLE_NON_LUS_MS);
    backend.expectOne('/api/messages/non-lus')
      .flush([message(1, 'Bonjour'), message(2, 'Toujours là ?')]);

    expect(espion).toHaveBeenCalledTimes(1);
    expect(espion).toHaveBeenCalledWith('Message de Bob', 'Toujours là ?');
  });

  it('n\'annonce pas deux fois le même message non lu', async () => {
    const espion = vi.spyOn(notifications, 'notifier');

    service.demarrerSuiviNonLus();
    await avancerDe(0);
    backend.expectOne('/api/messages/non-lus').flush([]);

    await avancerDe(INTERVALLE_NON_LUS_MS);
    backend.expectOne('/api/messages/non-lus').flush([message(1, 'Bonjour')]);
    expect(espion).toHaveBeenCalledTimes(1);

    // Le message reste non lu : il revient dans la réponse, mais il a déjà
    // été annoncé.
    await avancerDe(INTERVALLE_NON_LUS_MS);
    backend.expectOne('/api/messages/non-lus').flush([message(1, 'Bonjour')]);
    expect(espion).toHaveBeenCalledTimes(1);
  });

  it('repart d\'un état neuf après une déconnexion', async () => {
    const espion = vi.spyOn(notifications, 'notifier');

    service.demarrerSuiviNonLus();
    await avancerDe(0);
    backend.expectOne('/api/messages/non-lus').flush([message(1, 'Bonjour')]);

    service.arreterSuiviNonLus();

    // Reconnexion : le message 1 est toujours non lu, mais il n'est pas
    // « nouveau » — le premier tour observe sans annoncer.
    service.demarrerSuiviNonLus();
    await avancerDe(0);
    backend.expectOne('/api/messages/non-lus').flush([message(1, 'Bonjour')]);

    expect(espion).not.toHaveBeenCalled();
  });

  it('ne démarre pas deux sondages si on appelle deux fois', async () => {
    service.demarrerSuiviNonLus();
    service.demarrerSuiviNonLus();
    await avancerDe(0);

    // expectOne échouerait s'il y avait deux requêtes en attente : c'est la
    // preuve que le garde-fou empêche bien d'empiler un second timer.
    backend.expectOne('/api/messages/non-lus').flush([]);
  });
});

/**
 * Second describe dans le même fichier : les réactions touchent le même
 * service, mais n'ont besoin ni de minuteurs simulés ni du service de
 * notification. Les séparer garde chaque mise en place minimale — un beforeEach
 * qui prépare des choses inutiles au test rend celui-ci plus dur à lire.
 */
describe('MessageService — réactions', () => {
  let service: MessageService;
  let backend: HttpTestingController;

  const bob = unUtilisateur({ id: 2, email: 'bob@exemple.fr', nomAffichage: 'Bob' });

  /**
   * suivreFil s'appuie sur timer(0, …) : le 0 veut dire « au prochain tour de
   * boucle », pas « tout de suite ». Cette promesse vide rend la main au
   * moteur JavaScript le temps que la requête parte réellement — sans elle,
   * expectOne chercherait un appel pas encore émis.
   */
  const rendreLaMain = () => new Promise(resolve => setTimeout(resolve, 0));

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()]
    });

    service = TestBed.inject(MessageService);
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    service.arreterSuiviFil();
    backend.verify();
  });

  /**
   * LE test qui compte : la réponse du serveur remplace le message DANS le fil,
   * et lui seul.
   *
   * Sans ce remplacement, sa propre réaction n'apparaîtrait qu'au prochain tour
   * de sondage — jusqu'à cinq secondes après le clic, ce qui donne une
   * interface qui semble ignorer les clics.
   */
  it('remplace le message réagi sans toucher aux autres', async () => {
    service.suivreFil(2);
    await rendreLaMain();
    backend.expectOne('/api/messages/2').flush([
      unMessage({ id: 10, expediteur: bob, contenu: 'Salut' }),
      unMessage({ id: 11, expediteur: bob, contenu: 'Ça va ?' })
    ]);

    service.reagir(10, '👍');

    const requete = backend.expectOne('/api/messages/10/reaction');
    expect(requete.request.method).toBe('PUT');
    expect(requete.request.body).toEqual({ emoji: '👍' });

    requete.flush(unMessage({
      id: 10, expediteur: bob, contenu: 'Salut',
      reactions: [uneReaction({ nombre: 1, parMoi: true })]
    }));

    const fil = service.fil();
    expect(fil.length).toBe(2);
    expect(fil[0].reactions).toEqual([{ emoji: '👍', nombre: 1, parMoi: true }]);
    // Le voisin est intact.
    expect(fil[1].reactions).toEqual([]);
  });

  it('laisse le fil inchangé si le serveur refuse l\'emoji', async () => {
    service.suivreFil(2);
    await rendreLaMain();
    backend.expectOne('/api/messages/2')
      .flush([unMessage({ id: 10, expediteur: bob })]);

    service.reagir(10, '🍕');
    backend.expectOne('/api/messages/10/reaction')
      .flush({ message: 'Emoji non autorisé' }, { status: 400, statusText: 'Bad Request' });

    expect(service.fil()[0].reactions).toEqual([]);
  });
});

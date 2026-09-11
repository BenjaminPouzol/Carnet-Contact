import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { MessageService } from './message';
import { NotificationService } from './notification';
import { Message } from '../message.model';

/** L'intervalle du sondage des non-lus, tel que défini dans le service. */
const INTERVALLE_NON_LUS_MS = 15000;

describe('MessageService — notification des nouveaux messages', () => {
  let service: MessageService;
  let backend: HttpTestingController;
  let notifications: NotificationService;

  const bob = { id: 2, email: 'bob@exemple.fr', nomAffichage: 'Bob' };
  const moi = { id: 1, email: 'alice@exemple.fr', nomAffichage: 'Alice' };

  function message(id: number, contenu: string): Message {
    return {
      id, expediteur: bob, destinataire: moi, contenu,
      dateEnvoi: '2026-09-11T10:00:00Z', lu: false
    };
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

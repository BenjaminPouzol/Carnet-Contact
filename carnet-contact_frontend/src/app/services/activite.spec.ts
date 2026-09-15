import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ActiviteService } from './activite';
import { NotificationService } from './notification';
import { uneNotification } from '../donnees-test';

/** L'intervalle du sondage, tel que défini dans le service. */
const INTERVALLE_MS = 15000;

/**
 * Même mécanique que la notification des messages (message.spec.ts) : le
 * sondage renvoie à chaque tour TOUTES les non-lues, il faut donc se souvenir
 * de ce qui a déjà été annoncé.
 */
describe('ActiviteService — annonce des notifications d\'abonnement', () => {
  let service: ActiviteService;
  let backend: HttpTestingController;
  let notifications: NotificationService;

  const avancerDe = (ms: number) => vi.advanceTimersByTimeAsync(ms);

  beforeEach(() => {
    vi.useFakeTimers();

    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()]
    });

    service = TestBed.inject(ActiviteService);
    backend = TestBed.inject(HttpTestingController);
    notifications = TestBed.inject(NotificationService);
  });

  afterEach(() => {
    service.arreterSuivi();
    backend.verify();
    vi.useRealTimers();
  });

  it('n\'annonce pas ce qui attendait déjà à l\'ouverture', async () => {
    const espion = vi.spyOn(notifications, 'notifier');

    service.demarrerSuivi();
    await avancerDe(0);
    backend.expectOne('/api/notifications/non-lues').flush([uneNotification({ id: 1 })]);

    expect(espion).not.toHaveBeenCalled();
    // La pastille, elle, la compte bien.
    expect(service.nonLues().length).toBe(1);
  });

  it('annonce une nouvelle notification, avec sa phrase et le lien vers les abonnements', async () => {
    const espion = vi.spyOn(notifications, 'notifier');

    service.demarrerSuivi();
    await avancerDe(0);
    backend.expectOne('/api/notifications/non-lues').flush([]);

    await avancerDe(INTERVALLE_MS);
    backend.expectOne('/api/notifications/non-lues')
      .flush([uneNotification({ id: 7, type: 'DEMANDE_RECUE' })]);

    expect(espion).toHaveBeenCalledTimes(1);
    expect(espion).toHaveBeenCalledWith('Abonnements', 'Alice demande à vous suivre', '/abonnements');
  });

  it('n\'annonce pas deux fois la même notification', async () => {
    const espion = vi.spyOn(notifications, 'notifier');

    service.demarrerSuivi();
    await avancerDe(0);
    backend.expectOne('/api/notifications/non-lues').flush([]);

    await avancerDe(INTERVALLE_MS);
    backend.expectOne('/api/notifications/non-lues').flush([uneNotification({ id: 7 })]);
    await avancerDe(INTERVALLE_MS);
    backend.expectOne('/api/notifications/non-lues').flush([uneNotification({ id: 7 })]);

    expect(espion).toHaveBeenCalledTimes(1);
  });

  it('repart d\'un état neuf après l\'arrêt', async () => {
    const espion = vi.spyOn(notifications, 'notifier');

    service.demarrerSuivi();
    await avancerDe(0);
    backend.expectOne('/api/notifications/non-lues').flush([uneNotification({ id: 1 })]);

    service.arreterSuivi();
    expect(service.nonLues()).toEqual([]);

    service.demarrerSuivi();
    await avancerDe(0);
    backend.expectOne('/api/notifications/non-lues').flush([uneNotification({ id: 1 })]);

    expect(espion).not.toHaveBeenCalled();
  });

  it('ne démarre pas deux sondages', async () => {
    service.demarrerSuivi();
    service.demarrerSuivi();
    await avancerDe(0);

    backend.expectOne('/api/notifications/non-lues').flush([]);
  });

  it('vide les non-lues une fois le serveur prévenu', async () => {
    service.demarrerSuivi();
    await avancerDe(0);
    backend.expectOne('/api/notifications/non-lues').flush([uneNotification({ id: 1 })]);

    service.marquerLues();
    const requete = backend.expectOne('/api/notifications/lues');
    expect(requete.request.method).toBe('PUT');
    requete.flush(null, { status: 204, statusText: 'No Content' });

    expect(service.nonLues()).toEqual([]);
  });
});

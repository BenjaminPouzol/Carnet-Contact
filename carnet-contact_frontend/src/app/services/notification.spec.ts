import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { NotificationService } from './notification';

describe('NotificationService', () => {
  let service: NotificationService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideRouter([])]
    });
    service = TestBed.inject(NotificationService);
  });

  /**
   * jsdom, l'environnement de test, n'implémente pas l'API Notification. C'est
   * exactement le cas « navigateur sans notifications système » qu'on a prévu —
   * et il se teste donc sans rien simuler.
   */
  it('signale l\'API comme indisponible quand le navigateur ne la propose pas', () => {
    expect(service.permission()).toBe('indisponible');
  });

  it('se rabat sur un bandeau interne quand la notification système est impossible', () => {
    service.notifier('Message de Bob', 'Salut !');

    const bandeaux = service.bandeaux();
    expect(bandeaux.length).toBe(1);
    expect(bandeaux[0].titre).toBe('Message de Bob');
    expect(bandeaux[0].corps).toBe('Salut !');
  });

  it('empile plusieurs bandeaux et les ferme individuellement', () => {
    service.notifier('Premier', 'a');
    service.notifier('Second', 'b');
    expect(service.bandeaux().length).toBe(2);

    service.fermerBandeau(service.bandeaux()[0].id);

    // Chaque bandeau a son propre identifiant : fermer l'un ne doit pas
    // emporter l'autre.
    expect(service.bandeaux().length).toBe(1);
    expect(service.bandeaux()[0].titre).toBe('Second');
  });

  /**
   * `vi.useFakeTimers()` remplace les minuteurs du navigateur par des minuteurs
   * pilotés depuis le test. On peut alors « avancer le temps » instantanément,
   * au lieu d'attendre six vraies secondes.
   */
  it('fait disparaître le bandeau tout seul au bout du délai', () => {
    vi.useFakeTimers();

    try {
      service.notifier('Message de Bob', 'Salut !');
      expect(service.bandeaux().length).toBe(1);

      vi.advanceTimersByTime(6000);

      expect(service.bandeaux().length).toBe(0);
    } finally {
      // Toujours rendre les vrais minuteurs, sinon les tests suivants
      // hériteraient d'un temps figé.
      vi.useRealTimers();
    }
  });

  it('ne fait rien de dangereux si on demande la permission sans API disponible', () => {
    // Sortie anticipée : pas d'exception, et l'état ne change pas.
    expect(() => service.demanderPermission()).not.toThrow();
    expect(service.permission()).toBe('indisponible');
  });
});

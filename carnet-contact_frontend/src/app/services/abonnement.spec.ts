import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AbonnementService } from './abonnement';
import { unCompte } from '../donnees-test';

describe('AbonnementService', () => {
  let service: AbonnementService;
  let backend: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(AbonnementService);
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => backend.verify());

  /**
   * LE test qui compte. Le client ne décide pas du résultat d'un clic sur
   * « Suivre » : un compte privé répond EN_ATTENTE, pas ACCEPTE. Deviner
   * « ACCEPTE » côté navigateur afficherait « Abonné·e » à tort.
   */
  it('garde le statut RENVOYÉ par le serveur après un abonnement', () => {
    service.suivre(3).subscribe();

    const requete = backend.expectOne('/api/abonnements/3');
    expect(requete.request.method).toBe('PUT');
    requete.flush(unCompte({ id: 3, comptePrive: true, statut: 'EN_ATTENTE' }));

    expect(service.statutDe(3)).toBe('EN_ATTENTE');
  });

  it('remet AUCUN après avoir cessé de suivre', () => {
    service.suivre(3).subscribe();
    backend.expectOne('/api/abonnements/3').flush(unCompte({ id: 3, statut: 'ACCEPTE' }));

    service.nePlusSuivre(3).subscribe();
    backend.expectOne(r => r.method === 'DELETE' && r.url === '/api/abonnements/3')
      .flush(null, { status: 204, statusText: 'No Content' });

    expect(service.statutDe(3)).toBe('AUCUN');
  });

  it('ne change rien si l\'abonnement échoue', () => {
    service.suivre(3).subscribe({ error: () => {} });
    backend.expectOne('/api/abonnements/3').flush('', { status: 404, statusText: 'Not Found' });

    expect(service.statutDe(3)).toBe('AUCUN');
  });

  it('charge les statuts de tous ses abonnements', () => {
    service.charger();
    backend.expectOne('/api/abonnements').flush([
      unCompte({ id: 2, statut: 'ACCEPTE' }),
      unCompte({ id: 5, statut: 'EN_ATTENTE' })
    ]);

    expect(service.statutDe(2)).toBe('ACCEPTE');
    expect(service.statutDe(5)).toBe('EN_ATTENTE');
    // Un compte absent de la liste n'est pas suivi.
    expect(service.statutDe(9)).toBe('AUCUN');
  });

  it('envoie le terme de recherche en paramètre', () => {
    service.rechercher('bo').subscribe();

    const requete = backend.expectOne(r => r.url === '/api/utilisateurs');
    expect(requete.request.params.get('recherche')).toBe('bo');
    requete.flush([]);
  });

  it('accepte une demande par PUT sur la ressource de la demande', () => {
    service.accepter(4).subscribe();

    const requete = backend.expectOne('/api/abonnements/demandes/4');
    expect(requete.request.method).toBe('PUT');
    requete.flush(null, { status: 204, statusText: 'No Content' });
  });
});

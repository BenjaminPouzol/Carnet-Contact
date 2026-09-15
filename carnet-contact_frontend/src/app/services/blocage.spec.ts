import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { BlocageService } from './blocage';
import { AbonnementService } from './abonnement';
import { unCompte } from '../donnees-test';

describe('BlocageService', () => {
  let service: BlocageService;
  let abonnements: AbonnementService;
  let backend: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(BlocageService);
    abonnements = TestBed.inject(AbonnementService);
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => backend.verify());

  /**
   * Côté serveur, bloquer supprime les abonnements entre les deux comptes. Le
   * signal des statuts doit suivre tout de suite : sinon un bouton « Abonné·e »
   * resterait affiché pour un compte qu'on vient de bloquer.
   */
  it('oublie le statut d\'abonnement du compte bloqué', () => {
    abonnements.suivre(3).subscribe();
    backend.expectOne('/api/abonnements/3').flush(unCompte({ id: 3, statut: 'ACCEPTE' }));

    service.bloquer(3).subscribe();
    const requete = backend.expectOne('/api/blocages/3');
    expect(requete.request.method).toBe('PUT');
    requete.flush(null, { status: 204, statusText: 'No Content' });

    expect(abonnements.statutDe(3)).toBe('AUCUN');
  });

  it('débloque par DELETE', () => {
    service.debloquer(3).subscribe();

    const requete = backend.expectOne('/api/blocages/3');
    expect(requete.request.method).toBe('DELETE');
    requete.flush(null, { status: 204, statusText: 'No Content' });
  });
});

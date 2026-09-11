import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AdminService } from './admin';
import { LigneCompte } from '../utilisateur.model';

describe('AdminService', () => {
  let service: AdminService;
  let backend: HttpTestingController;

  function ligne(modifications: Partial<LigneCompte> = {}): LigneCompte {
    return {
      id: 2,
      email: 'bob@exemple.fr',
      nomAffichage: 'Bob',
      role: 'UTILISATEUR',
      actif: true,
      dateInscription: '2026-09-01T09:00:00Z',
      nombreContacts: 3,
      nombreMessages: 7,
      estMoi: false,
      ...modifications
    };
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });

    service = TestBed.inject(AdminService);
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => backend.verify());

  it('charge la liste des comptes', () => {
    service.charger();
    backend.expectOne('/api/admin/comptes').flush([ligne(), ligne({ id: 3 })]);

    expect(service.comptes().length).toBe(2);
  });

  /**
   * LE test qui compte : après une action, la ligne modifiée doit être
   * remplacée SUR PLACE, sans recharger la liste entière.
   *
   * Ce qui est vérifié ici, ce n'est pas l'appel HTTP — c'est qu'aucune
   * seconde requête ne part (backend.verify() y veillerait), et que les
   * autres lignes sont rigoureusement intactes. Un rechargement ferait
   * clignoter le tableau et perdrait le tri en cours.
   */
  it('remplace la ligne modifiée sans recharger le reste', () => {
    service.charger();
    backend.expectOne('/api/admin/comptes').flush([ligne(), ligne({ id: 3, nomAffichage: 'Carla' })]);

    service.changerActif(2, false);

    const requete = backend.expectOne('/api/admin/comptes/2/actif');
    expect(requete.request.method).toBe('PUT');
    expect(requete.request.body).toEqual({ actif: false });
    requete.flush(ligne({ actif: false }));

    expect(service.comptes().length).toBe(2);
    expect(service.comptes()[0].actif).toBe(false);
    // La ligne voisine n'a pas bougé.
    expect(service.comptes()[1].nomAffichage).toBe('Carla');
  });

  it('retire la ligne supprimée de la liste', () => {
    service.charger();
    backend.expectOne('/api/admin/comptes').flush([ligne(), ligne({ id: 3 })]);

    service.supprimer(2);
    backend.expectOne('/api/admin/comptes/2').flush(null);

    expect(service.comptes().map(c => c.id)).toEqual([3]);
  });

  /**
   * Une erreur serveur (par exemple : « c'est le dernier administrateur »)
   * ne doit RIEN changer localement. Sans le catchError du service, l'échec
   * remonterait jusqu'à la console sous forme d'erreur non gérée.
   */
  it('laisse la liste intacte quand le serveur refuse', () => {
    service.charger();
    backend.expectOne('/api/admin/comptes').flush([ligne({ role: 'ADMIN' })]);

    service.changerRole(2, 'UTILISATEUR');
    backend.expectOne('/api/admin/comptes/2/role')
      .flush({ message: 'Dernier administrateur' }, { status: 409, statusText: 'Conflict' });

    expect(service.comptes()[0].role).toBe('ADMIN');
  });
});

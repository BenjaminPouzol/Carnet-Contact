import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { BoutonSuivre } from './bouton-suivre';
import { AbonnementService } from '../../services/abonnement';
import { SessionService } from '../../services/session';
import { StatutRelation } from '../../abonnement.model';
import { unCompte, unUtilisateur } from '../../donnees-test';

/**
 * Le bouton lit le statut dans AbonnementService et ne garde aucun état à lui :
 * c'est ce qui permet à deux boutons visant le même compte de rester d'accord.
 * On règle donc l'état par le service, puis on regarde ce que le bouton affiche.
 */
describe('BoutonSuivre', () => {
  let fixture: ComponentFixture<BoutonSuivre>;
  let backend: HttpTestingController;
  let abonnements: AbonnementService;

  async function creer(compteId: number, comptePrive = false): Promise<void> {
    fixture = TestBed.createComponent(BoutonSuivre);
    fixture.componentRef.setInput('compteId', compteId);
    fixture.componentRef.setInput('comptePrive', comptePrive);
    await fixture.whenStable();
  }

  function bouton(): HTMLButtonElement | null {
    return (fixture.nativeElement as HTMLElement).querySelector('button');
  }

  /** Pose un statut connu, comme si le serveur venait de le renvoyer. */
  function statutInitial(id: number, statut: StatutRelation): void {
    abonnements.suivre(id).subscribe();
    backend.expectOne(`/api/abonnements/${id}`).flush(unCompte({ id, statut }));
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [BoutonSuivre],
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    backend = TestBed.inject(HttpTestingController);
    abonnements = TestBed.inject(AbonnementService);
    // Le compte connecté a l'identifiant 1.
    TestBed.inject(SessionService).ouvrir('jeton', 'rafraichissement', unUtilisateur({ id: 1 }));
  });

  afterEach(() => {
    backend.verify();
    localStorage.clear();
  });

  it('propose « Suivre » pour un compte public non suivi', async () => {
    await creer(2);
    expect(bouton()?.textContent).toContain('Suivre');
    expect(bouton()?.textContent).not.toContain('Demander');
  });

  it('propose « Demander à suivre » pour un compte privé', async () => {
    await creer(2, true);
    expect(bouton()?.textContent).toContain('Demander à suivre');
  });

  it('affiche « Demande envoyée » tant que la demande attend', async () => {
    statutInitial(2, 'EN_ATTENTE');
    await creer(2, true);
    expect(bouton()?.textContent).toContain('Demande envoyée');
  });

  it('affiche « Abonné·e » une fois l\'abonnement accepté', async () => {
    statutInitial(2, 'ACCEPTE');
    await creer(2);
    expect(bouton()?.textContent).toContain('Abonné·e');
  });

  it('suit au clic et prévient le parent du nouveau statut', async () => {
    await creer(2);
    const recus: StatutRelation[] = [];
    fixture.componentInstance.statutChange.subscribe(statut => recus.push(statut));

    bouton()!.click();
    backend.expectOne(r => r.method === 'PUT' && r.url === '/api/abonnements/2')
      .flush(unCompte({ id: 2, statut: 'ACCEPTE' }));
    await fixture.whenStable();

    expect(recus).toEqual(['ACCEPTE']);
    expect(bouton()?.textContent).toContain('Abonné·e');
  });

  it('cesse de suivre au clic sur « Abonné·e »', async () => {
    statutInitial(2, 'ACCEPTE');
    await creer(2);

    bouton()!.click();
    backend.expectOne(r => r.method === 'DELETE' && r.url === '/api/abonnements/2')
      .flush(null, { status: 204, statusText: 'No Content' });
    await fixture.whenStable();

    expect(bouton()?.textContent).toContain('Suivre');
  });

  /** On ne se suit pas soi-même : le bouton n'a rien à proposer. */
  it('ne s\'affiche pas sur son propre compte', async () => {
    await creer(1);
    expect(bouton()).toBeNull();
  });
});

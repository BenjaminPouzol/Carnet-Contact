import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { of } from 'rxjs';
import { Personne } from './personne';
import { SessionService } from '../../services/session';
import { ProfilPublic } from '../../abonnement.model';
import { unProfil, unUtilisateur } from '../../donnees-test';

/**
 * La page d'une personne. Le serveur décide de ce qui est visible
 * (`coordonnees`, `contenuVisible`) : la page doit suivre ces décisions à la
 * lettre, sans en prendre d'autres.
 */
describe('Personne', () => {
  let backend: HttpTestingController;

  const aucuneCoordonnee = {
    emailPro: null, instagram: null, twitter: null, facebook: null,
    twitch: null, youtube: null, linkedin: null
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [Personne],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        // paramMap est un Observable : la page doit réagir quand l'identifiant
        // change sans que le composant soit recréé (d'une personne à l'autre).
        { provide: ActivatedRoute, useValue: { paramMap: of(convertToParamMap({ id: '2' })) } }
      ]
    });

    backend = TestBed.inject(HttpTestingController);
    TestBed.inject(SessionService).ouvrir('jeton', 'rafraichissement', unUtilisateur({ id: 1 }));
  });

  afterEach(() => {
    backend.verify();
    localStorage.clear();
  });

  /** Crée la page, répond aux statuts d'abonnement et au profil, rend le HTML. */
  async function afficher(profil: ProfilPublic): Promise<HTMLElement> {
    const fixture = TestBed.createComponent(Personne);
    await fixture.whenStable();

    backend.expectOne('/api/abonnements').flush([]);
    backend.expectOne('/api/utilisateurs/2').flush(profil);
    await fixture.whenStable();

    return fixture.nativeElement as HTMLElement;
  }

  it('affiche l\'email pro et les réseaux quand le serveur les envoie', async () => {
    const html = await afficher(unProfil({
      statut: 'ACCEPTE',
      peutEcrire: true,
      coordonnees: { ...aucuneCoordonnee, emailPro: 'bob@entreprise.fr', instagram: 'bob.insta' }
    }));

    backend.expectOne(r => r.url === '/api/publications').flush({ publications: [], curseurSuivant: null });

    expect(html.textContent).toContain('bob@entreprise.fr');
    expect(html.querySelector('a[href="https://instagram.com/bob.insta"]')).not.toBeNull();
    expect(html.textContent).not.toContain('Suivez Bob');
  });

  it('explique comment voir les coordonnées quand on ne suit pas la personne', async () => {
    const html = await afficher(unProfil({ coordonnees: null }));

    backend.expectOne(r => r.url === '/api/publications').flush({ publications: [], curseurSuivant: null });

    expect(html.textContent).toContain('Suivez Bob pour voir ses coordonnées professionnelles');
  });

  /** Inutile de demander au serveur ce qu'il refuserait de montrer. */
  it('ne demande pas les publications d\'un compte privé non suivi', async () => {
    const html = await afficher(unProfil({ comptePrive: true, contenuVisible: false }));

    backend.expectNone(r => r.url === '/api/publications');
    expect(html.textContent).toContain('Ce compte est privé');
  });

  it('annonce un compte introuvable sur un 404', async () => {
    const fixture = TestBed.createComponent(Personne);
    await fixture.whenStable();

    backend.expectOne('/api/abonnements').flush([]);
    backend.expectOne('/api/utilisateurs/2').flush('', { status: 404, statusText: 'Not Found' });
    await fixture.whenStable();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Compte introuvable');
  });
});

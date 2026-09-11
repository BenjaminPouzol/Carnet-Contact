import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ContactDetail } from './contact-detail';

/**
 * Cette page lit son identifiant dans l'URL. En test, il n'y a pas de
 * navigation : on remplace ActivatedRoute par un objet qui rend exactement ce
 * que le composant en lit — ici `snapshot.paramMap`.
 *
 * C'est un « bouchon » (stub) : un faux minimal, suffisant pour la seule
 * question posée. Inutile de simuler un routeur complet pour fournir un « 7 ».
 */
describe('ContactDetail', () => {
  let backend: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ContactDetail],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: convertToParamMap({ id: '7' }) } }
        }
      ]
    });

    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => backend.verify());

  it('demande le contact de l\'URL et affiche son nom', async () => {
    const fixture = TestBed.createComponent(ContactDetail);
    await fixture.whenStable();

    // La preuve que la page ne se contente plus de fouiller la liste chargée :
    // elle réclame sa propre fiche, ce qui fonctionne même si le contact 7 se
    // trouve sur une autre page de la liste.
    backend.expectOne('/api/contacts/7').flush({
      id: 7, nom: 'Dupont', prenom: 'Marie', email: 'marie@exemple.fr'
    });
    await fixture.whenStable();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Marie Dupont');
  });

  it('affiche un message quand le contact est introuvable', async () => {
    const fixture = TestBed.createComponent(ContactDetail);
    await fixture.whenStable();

    backend.expectOne('/api/contacts/7')
      .flush('', { status: 404, statusText: 'Not Found' });
    await fixture.whenStable();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('introuvable');
  });
});

import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ContactEdit } from './contact-edit';

describe('ContactEdit', () => {
  let backend: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ContactEdit],
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

  /**
   * Le comportement à protéger : le formulaire se remplit tout seul quand la
   * réponse arrive (l'effect() de la section 14). C'est du code qu'il est
   * facile de casser sans s'en apercevoir — le formulaire s'afficherait
   * simplement vide, sans erreur ni message.
   */
  it('pré-remplit le formulaire dès que le contact arrive', async () => {
    const fixture = TestBed.createComponent(ContactEdit);
    await fixture.whenStable();

    backend.expectOne('/api/contacts/7').flush({
      id: 7, nom: 'Dupont', prenom: 'Marie', email: 'marie@exemple.fr', telephone: null
    });
    await fixture.whenStable();

    const valeurs = fixture.componentInstance.contactForm.value;
    expect(valeurs.nom).toBe('Dupont');
    expect(valeurs.prenom).toBe('Marie');
    // Un champ null côté serveur devient une chaîne vide, sinon le gabarit
    // afficherait littéralement « null » dans la case.
    expect(valeurs.telephone).toBe('');
  });
});

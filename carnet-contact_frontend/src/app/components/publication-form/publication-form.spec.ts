import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { PublicationForm } from './publication-form';
import { Categorie, Publication } from '../../publication.model';
import { unePublication } from '../../donnees-test';

describe('PublicationForm', () => {
  let fixture: ComponentFixture<PublicationForm>;
  let composant: PublicationForm;
  let backend: HttpTestingController;

  /**
   * Le composant est créé DANS chaque test, et non dans le beforeEach : en mode
   * modification, la publication doit être passée avant le premier affichage,
   * puisque c'est ngOnInit qui pré-remplit le formulaire.
   */
  async function creer(publication: Publication | null = null): Promise<void> {
    fixture = TestBed.createComponent(PublicationForm);
    composant = fixture.componentInstance;
    fixture.componentRef.setInput('publication', publication);
    await fixture.whenStable();
  }

  function remplir(valeurs: Partial<{ categorie: Categorie; contenu: string; imageUrl: string }> = {}) {
    composant.formulaire.setValue({ categorie: 'SPORT', contenu: 'Vélo', imageUrl: '', ...valeurs });
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [PublicationForm],
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => backend.verify());

  it('exige une catégorie et un contenu', async () => {
    await creer();
    expect(composant.formulaire.valid).toBe(false);

    remplir();
    expect(composant.formulaire.valid).toBe(true);
  });

  /** Même règle que @NotBlank côté serveur : des espaces ne sont pas un contenu. */
  it('refuse un contenu fait seulement d\'espaces', async () => {
    await creer();
    remplir({ contenu: '    ' });
    expect(composant.formulaire.valid).toBe(false);
  });

  it('refuse un contenu de plus de 2000 caractères', async () => {
    await creer();
    remplir({ contenu: 'a'.repeat(2001) });
    expect(composant.formulaire.valid).toBe(false);
  });

  it('accepte une image vide ou en http(s), refuse le reste', async () => {
    await creer();

    remplir({ imageUrl: 'javascript:alert(1)' });
    expect(composant.formulaire.valid).toBe(false);

    remplir({ imageUrl: 'https://exemple.fr/photo.jpg' });
    expect(composant.formulaire.valid).toBe(true);

    remplir({ imageUrl: '' });
    expect(composant.formulaire.valid).toBe(true);
  });

  /**
   * LE test qui compte, et le défaut relevé par la revue sur le formulaire de
   * contact : vider la saisie AVANT la réponse du serveur, c'est la perdre au
   * premier échec.
   */
  it('garde la saisie quand la publication échoue', async () => {
    await creer();
    remplir({ contenu: 'Ma sortie vélo' });

    composant.onSubmit();
    backend.expectOne(r => r.method === 'POST')
      .flush('', { status: 500, statusText: 'Server Error' });

    expect(composant.formulaire.controls.contenu.value).toBe('Ma sortie vélo');
  });

  it('vide le texte, garde la catégorie et prévient le parent après un succès', async () => {
    await creer();
    const recues: Publication[] = [];
    composant.termine.subscribe(publication => recues.push(publication));
    remplir({ contenu: 'Ma sortie vélo' });

    composant.onSubmit();
    backend.expectOne(r => r.method === 'POST').flush(unePublication({ id: 7 }));

    expect(composant.formulaire.controls.contenu.value).toBe('');
    expect(composant.formulaire.controls.categorie.value).toBe('SPORT');
    expect(recues.map(p => p.id)).toEqual([7]);
  });

  it('pré-remplit le formulaire et envoie un PUT en mode modification', async () => {
    await creer(unePublication({
      id: 4, categorie: 'CUISINE', contenu: 'Tarte aux pommes', imageUrl: 'https://exemple.fr/tarte.jpg'
    }));

    expect(composant.formulaire.getRawValue()).toEqual({
      categorie: 'CUISINE', contenu: 'Tarte aux pommes', imageUrl: 'https://exemple.fr/tarte.jpg'
    });

    composant.onSubmit();
    backend.expectOne(r => r.method === 'PUT' && r.url === '/api/publications/4')
      .flush(unePublication({ id: 4, categorie: 'CUISINE' }));
  });
});

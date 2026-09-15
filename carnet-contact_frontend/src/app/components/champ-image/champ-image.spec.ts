import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormControl } from '@angular/forms';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ChampImage } from './champ-image';

describe('ChampImage', () => {
  let fixture: ComponentFixture<ChampImage>;
  let composant: ChampImage;
  let backend: HttpTestingController;
  let controle: FormControl<string | null>;

  /** Un faux fichier : seuls sa taille et son type annoncé comptent côté client. */
  const image = (taille = 10, type = 'image/png') =>
    new File([new Uint8Array(taille)], 'photo.png', { type });

  beforeEach(async () => {
    TestBed.configureTestingModule({
      imports: [ChampImage],
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    backend = TestBed.inject(HttpTestingController);

    // Le contrôle vient normalement du formulaire parent : on en fabrique un,
    // déjà rempli, pour pouvoir vérifier qu'un échec ne l'écrase pas.
    controle = new FormControl<string | null>('https://exemple.fr/ancienne.jpg');

    fixture = TestBed.createComponent(ChampImage);
    fixture.componentRef.setInput('controle', controle);
    fixture.componentRef.setInput('identifiant', 'photo');
    fixture.componentRef.setInput('libelle', 'Photo');
    composant = fixture.componentInstance;
    await fixture.whenStable();
  });

  afterEach(() => backend.verify());

  it('refuse un format hors liste sans rien envoyer', () => {
    composant.envoyer(image(10, 'image/svg+xml'));

    expect(composant.erreurLocale()).toContain('JPEG');
    backend.expectNone('/api/images');
  });

  it('refuse une image de plus de 5 Mo sans rien envoyer', () => {
    composant.envoyer(image(5 * 1024 * 1024 + 1));

    expect(composant.erreurLocale()).toContain('5 Mo');
    backend.expectNone('/api/images');
  });

  it('place l\'URL reçue dans le contrôle du formulaire parent', () => {
    composant.envoyer(image());
    expect(composant.envoiEnCours()).toBe(true);

    backend.expectOne('/api/images').flush({ url: 'http://localhost:8080/api/images/abc' });

    expect(controle.value).toBe('http://localhost:8080/api/images/abc');
    // dirty : pour le formulaire parent, c'est une vraie modification de l'utilisateur.
    expect(controle.dirty).toBe(true);
    expect(composant.envoiEnCours()).toBe(false);
  });

  it('garde l\'ancienne adresse si l\'envoi échoue', () => {
    composant.envoyer(image());

    backend.expectOne('/api/images').flush('', { status: 415, statusText: 'Unsupported Media Type' });

    expect(controle.value).toBe('https://exemple.fr/ancienne.jpg');
    expect(composant.envoiEnCours()).toBe(false);
  });
});

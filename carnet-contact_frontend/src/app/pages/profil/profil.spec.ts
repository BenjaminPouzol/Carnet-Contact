import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Profil } from './profil';
import { SessionService } from '../../services/session';
import { unUtilisateur } from '../../donnees-test';

describe('Profil', () => {
  let backend: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [Profil],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()]
    });
    backend = TestBed.inject(HttpTestingController);

    TestBed.inject(SessionService).ouvrir('jeton', 'rafraichissement', unUtilisateur({
      nomAffichage: 'Alice',
      emailPro: 'alice@entreprise.fr',
      instagram: 'alice.insta',
      comptePrive: true
    }));
  });

  afterEach(() => {
    backend.verify();
    localStorage.clear();
  });

  async function creer() {
    const fixture = TestBed.createComponent(Profil);
    await fixture.whenStable();
    return fixture.componentInstance;
  }

  it('pré-remplit l\'email pro, les réseaux et la confidentialité depuis le compte', async () => {
    const composant = await creer();

    const valeurs = composant.formulaire.getRawValue();
    expect(valeurs.emailPro).toBe('alice@entreprise.fr');
    expect(valeurs.instagram).toBe('alice.insta');
    // Un réseau non renseigné devient une chaîne vide, pas « null » dans la case.
    expect(valeurs.linkedin).toBe('');
    expect(valeurs.comptePrive).toBe(true);
  });

  it('envoie l\'email pro, les réseaux et la confidentialité', async () => {
    const composant = await creer();
    composant.formulaire.patchValue({ emailPro: 'nouveau@entreprise.fr', linkedin: 'alice-l', comptePrive: false });

    composant.onSubmit();

    const requete = backend.expectOne('/api/utilisateurs/moi');
    expect(requete.request.method).toBe('PUT');
    expect(requete.request.body).toEqual(expect.objectContaining({
      nomAffichage: 'Alice',
      emailPro: 'nouveau@entreprise.fr',
      instagram: 'alice.insta',
      linkedin: 'alice-l',
      comptePrive: false
    }));
    requete.flush(unUtilisateur({ nomAffichage: 'Alice', emailPro: 'nouveau@entreprise.fr', comptePrive: false }));
  });

  /** Même règle que le @Email du serveur : le refus arrive avant l'envoi. */
  it('refuse un email pro mal formé', async () => {
    const composant = await creer();
    composant.formulaire.patchValue({ emailPro: 'pas-un-email' });

    expect(composant.formulaire.invalid).toBe(true);
  });
});

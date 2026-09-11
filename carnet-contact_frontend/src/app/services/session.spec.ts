import { TestBed } from '@angular/core/testing';
import { SessionService } from './session';

describe('SessionService', () => {
  const compte = { id: 1, email: 'alice@exemple.fr', nomAffichage: 'Alice' };

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({});
  });

  it('mémorise les deux jetons et le compte', () => {
    const session = TestBed.inject(SessionService);
    session.ouvrir('acces', 'rafraichissement', compte);

    expect(session.connecte()).toBe(true);
    expect(session.jetonActuel()).toBe('acces');
    expect(session.jetonRafraichissementActuel()).toBe('rafraichissement');
  });

  /**
   * Le comportement qui rend un F5 indolore. Il est facile à casser sans rien
   * remarquer : l'application marcherait parfaitement jusqu'au premier
   * rechargement de page.
   *
   * On simule le rechargement en réinitialisant le TestBed : le service est
   * reconstruit, et son constructeur relit ce qui traîne dans localStorage.
   */
  it('restaure la session au rechargement de la page', () => {
    TestBed.inject(SessionService).ouvrir('acces', 'rafraichissement', compte);

    TestBed.resetTestingModule();
    TestBed.configureTestingModule({});
    const apresRechargement = TestBed.inject(SessionService);

    expect(apresRechargement.connecte()).toBe(true);
    // Le jeton de rafraîchissement doit survivre lui aussi : sans lui, la
    // session repartirait pour 15 minutes seulement, sans renouvellement
    // possible.
    expect(apresRechargement.jetonRafraichissementActuel()).toBe('rafraichissement');
    expect(apresRechargement.utilisateur()?.nomAffichage).toBe('Alice');
  });

  it('efface tout à la déconnexion', () => {
    const session = TestBed.inject(SessionService);
    session.ouvrir('acces', 'rafraichissement', compte);

    session.vider();

    expect(session.connecte()).toBe(false);
    expect(session.jetonRafraichissementActuel()).toBeNull();
    // Et rien ne reste sur le disque : un rechargement ne ressusciterait pas
    // la session.
    expect(localStorage.length).toBe(0);
  });
});

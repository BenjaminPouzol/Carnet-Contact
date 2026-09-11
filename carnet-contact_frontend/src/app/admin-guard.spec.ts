import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { adminGuard } from './admin-guard';
import { SessionService } from './services/session';
import { unUtilisateur } from './donnees-test';

describe('adminGuard', () => {
  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideRouter([{ path: '', children: [] }])]
    });
  });

  /**
   * runInInjectionContext : une garde est une simple fonction, mais elle
   * appelle inject() — ce qui n'est permis que pendant la construction d'un
   * service ou d'un composant. Cette aide crée artificiellement ce contexte,
   * et permet donc d'appeler la garde comme une fonction ordinaire.
   */
  const executer = () => TestBed.runInInjectionContext(
    () => adminGuard(null!, null!)
  );

  it('laisse passer un administrateur', () => {
    TestBed.inject(SessionService).ouvrir('jeton', 'rafraichissement',
      unUtilisateur({ role: 'ADMIN' }));

    expect(executer()).toBe(true);
  });

  /**
   * LE test qui compte : la redirection va vers l'ACCUEIL, pas vers la page de
   * connexion. La personne est bien identifiée — l'envoyer se reconnecter
   * laisserait croire à un problème de mot de passe, alors qu'il s'agit d'un
   * droit manquant.
   */
  it('renvoie un utilisateur ordinaire vers l\'accueil', () => {
    TestBed.inject(SessionService).ouvrir('jeton', 'rafraichissement', unUtilisateur());
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);

    expect(executer()).toBe(false);
    expect(navigate).toHaveBeenCalledWith(['/']);
  });

  it('refuse aussi quand personne n\'est connecté', () => {
    vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);

    expect(executer()).toBe(false);
  });
});

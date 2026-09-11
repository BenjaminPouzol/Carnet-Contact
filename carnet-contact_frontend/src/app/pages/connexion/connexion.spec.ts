import { TestBed, ComponentFixture } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Connexion } from './connexion';

describe('Connexion', () => {
  let fixture: ComponentFixture<Connexion>;

  /** Le champ mot de passe, tel que le voit l'utilisateur. */
  const champMotDePasse = () =>
    (fixture.nativeElement as HTMLElement).querySelector<HTMLInputElement>('#mdp')!;

  const boutonOeil = () =>
    (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>('.bouton-oeil')!;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      imports: [Connexion],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()]
    });

    fixture = TestBed.createComponent(Connexion);
    await fixture.whenStable();
  });

  // --- Afficher / masquer le mot de passe -----------------------------------

  it('masque le mot de passe par défaut', () => {
    expect(champMotDePasse().type).toBe('password');
  });

  it('affiche puis remasque le mot de passe au clic', async () => {
    boutonOeil().click();
    await fixture.whenStable();
    expect(champMotDePasse().type).toBe('text');

    boutonOeil().click();
    await fixture.whenStable();
    expect(champMotDePasse().type).toBe('password');
  });

  /**
   * Un bouton sans `type` vaut `type="submit"` dans un `<form>`. Sans
   * `type="button"`, cliquer sur « Afficher » enverrait le formulaire au lieu
   * de dévoiler le mot de passe — un bug silencieux et déroutant.
   */
  it('le bouton d\'affichage ne soumet pas le formulaire', () => {
    expect(boutonOeil().type).toBe('button');
  });

  it('annonce son état aux lecteurs d\'écran', async () => {
    expect(boutonOeil().getAttribute('aria-pressed')).toBe('false');

    boutonOeil().click();
    await fixture.whenStable();

    expect(boutonOeil().getAttribute('aria-pressed')).toBe('true');
  });

  // --- Critères du mot de passe ---------------------------------------------

  it('n\'affiche pas la liste des critères en mode connexion', () => {
    expect((fixture.nativeElement as HTMLElement).querySelector('.criteres')).toBeNull();
  });

  it('affiche la liste des critères en mode inscription', async () => {
    fixture.componentInstance.basculer();
    await fixture.whenStable();

    const criteres = (fixture.nativeElement as HTMLElement).querySelectorAll('.criteres li');
    expect(criteres.length).toBe(fixture.componentInstance.criteres.length);
  });

  it('coche les critères au fur et à mesure de la saisie', async () => {
    fixture.componentInstance.basculer();
    fixture.componentInstance.formulaire.controls.motDePasse.setValue('motdepasse');
    await fixture.whenStable();

    const satisfaits = (fixture.nativeElement as HTMLElement)
      .querySelectorAll('.criteres li.satisfait');

    // « motdepasse » : longueur et minuscule d'accord, le reste non.
    expect(satisfaits.length).toBe(2);
  });

  /**
   * La règle de solidité s'applique à la CRÉATION d'un mot de passe, pas à son
   * usage. Un compte créé sous une politique plus souple doit continuer de
   * pouvoir se connecter — sinon on enfermerait dehors ses propriétaires.
   */
  it('n\'exige pas un mot de passe solide pour se connecter', () => {
    const formulaire = fixture.componentInstance.formulaire;
    formulaire.controls.email.setValue('alice@exemple.fr');
    formulaire.controls.motDePasse.setValue('vieux');

    expect(formulaire.valid).toBe(true);
  });

  it('exige un mot de passe solide pour créer un compte', () => {
    fixture.componentInstance.basculer();

    const formulaire = fixture.componentInstance.formulaire;
    formulaire.controls.email.setValue('alice@exemple.fr');
    formulaire.controls.motDePasse.setValue('vieux');
    expect(formulaire.valid).toBe(false);

    formulaire.controls.motDePasse.setValue('MotDeP4sse!');
    expect(formulaire.valid).toBe(true);
  });

  /**
   * `setValidators()` change la règle mais ne rejoue pas la validation : sans
   * l'`updateValueAndValidity()` de `basculer()`, le champ garderait le verdict
   * calculé avec l'ancienne règle.
   */
  it('réévalue le champ immédiatement quand on change de mode', () => {
    const formulaire = fixture.componentInstance.formulaire;
    formulaire.controls.email.setValue('alice@exemple.fr');
    formulaire.controls.motDePasse.setValue('vieux');
    expect(formulaire.valid).toBe(true);

    // Sans réévaluation, le formulaire resterait valide après la bascule.
    fixture.componentInstance.basculer();
    expect(formulaire.valid).toBe(false);
  });
});

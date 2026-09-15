import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, ParamMap, convertToParamMap, provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Messages } from './messages';
import { MessageService } from '../../services/message';
import { SessionService } from '../../services/session';
import { Interlocuteur } from '../../abonnement.model';
import { Message } from '../../message.model';
import { unAuteur, unCompte, unMessage, unUtilisateur } from '../../donnees-test';

/**
 * La messagerie, depuis les abonnements. La liste des conversations et le droit
 * d'écrire à chacune viennent du serveur : la page doit s'y tenir.
 */
describe('Messages', () => {
  let backend: HttpTestingController;

  // Les paramètres d'adresse (?avec=…) du test en cours. La fabrique déclarée
  // plus bas ne les lit qu'à la création du composant : chaque test peut donc
  // les choisir juste avant d'afficher la page.
  let parametres: ParamMap;

  const bob: Interlocuteur = { compte: unAuteur({ id: 2, nomAffichage: 'Bob' }), peutEcrire: true };
  const chloe: Interlocuteur = { compte: unAuteur({ id: 3, nomAffichage: 'Chloé' }), peutEcrire: false };

  beforeEach(() => {
    parametres = convertToParamMap({});

    TestBed.configureTestingModule({
      imports: [Messages],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        // useFactory et non useValue : l'objet est fabriqué au moment où le
        // composant le demande, donc APRÈS que le test a choisi ses paramètres.
        { provide: ActivatedRoute, useFactory: () => ({ snapshot: { queryParamMap: parametres } }) }
      ]
    });

    backend = TestBed.inject(HttpTestingController);
    TestBed.inject(SessionService).ouvrir('jeton', 'rafraichissement', unUtilisateur({ id: 1 }));
  });

  afterEach(() => {
    // Un fil ouvert se rafraîchit toutes les cinq secondes : on l'arrête avant
    // de vérifier qu'aucune requête n'est restée sans réponse.
    TestBed.inject(MessageService).arreterSuiviFil();
    backend.verify();
    localStorage.clear();
  });

  /** Crée la page, éventuellement avec ?avec=, et répond aux deux chargements de départ. */
  async function afficher(interlocuteurs: Interlocuteur[], avec?: number) {
    if (avec !== undefined) {
      parametres = convertToParamMap({ avec: String(avec) });
    }

    const fixture = TestBed.createComponent(Messages);
    await fixture.whenStable();

    backend.expectOne('/api/abonnements').flush([]);
    backend.expectOne('/api/messages/interlocuteurs').flush(interlocuteurs);
    await fixture.whenStable();

    return { fixture, composant: fixture.componentInstance, html: fixture.nativeElement as HTMLElement };
  }

  /**
   * Répond au premier chargement d'un fil ouvert. suivreFil repose sur
   * timer(0, …), qui part au prochain tour de boucle et non tout de suite
   * (voir message.spec.ts) : on rend d'abord la main.
   */
  async function repondreAuFil(fixture: ComponentFixture<Messages>, autreId: number, messages: Message[] = []) {
    await new Promise(resolve => setTimeout(resolve, 0));
    backend.expectOne(`/api/messages/${autreId}`).flush(messages);
    await fixture.whenStable();
  }

  it('liste les conversations fournies par le serveur', async () => {
    const { html } = await afficher([bob, chloe]);

    const noms = [...html.querySelectorAll('.ligne-compte .nom')].map(e => e.textContent?.trim());
    expect(noms).toEqual(['Bob', 'Chloé']);
  });

  it('ouvre la conversation demandée par ?avec=', async () => {
    const { fixture, composant, html } = await afficher([bob, chloe], 3);
    await repondreAuFil(fixture, 3);

    expect(composant.selection()?.compte.id).toBe(3);
    expect(html.querySelector('.colonne-droite h3')?.textContent).toContain('Chloé');
  });

  /** Aucune requête vers /api/messages/9 : backend.verify() le vérifie en fin de test. */
  it('ignore ?avec= quand la personne n\'est pas dans la liste', async () => {
    const { composant } = await afficher([bob], 9);
    await new Promise(resolve => setTimeout(resolve, 0));

    expect(composant.selection()).toBeNull();
  });

  it('sans droit d\'écrire : pas de zone de saisie, mais la raison et le bouton Suivre', async () => {
    const { fixture, html } = await afficher([bob, chloe], 3);
    await repondreAuFil(fixture, 3, [
      unMessage({ expediteur: unAuteur({ id: 1, nomAffichage: 'Alice' }), destinataire: chloe.compte })
    ]);

    expect(html.querySelector('form.saisie')).toBeNull();
    expect(html.querySelector('.lecture-seule')?.textContent).toContain('Suivez ce compte pour lui');
    expect(html.querySelector('.lecture-seule app-bouton-suivre')).not.toBeNull();
  });

  it('suivre depuis la conversation fait apparaître la zone de saisie', async () => {
    const { fixture, html } = await afficher([chloe], 3);
    await repondreAuFil(fixture, 3);

    html.querySelector<HTMLButtonElement>('.lecture-seule app-bouton-suivre button')!.click();
    backend.expectOne(req => req.method === 'PUT' && req.url === '/api/abonnements/3')
      .flush(unCompte({ id: 3, nomAffichage: 'Chloé', statut: 'ACCEPTE' }));
    await fixture.whenStable();

    expect(html.querySelector('form.saisie')).not.toBeNull();
    expect(html.querySelector('.lecture-seule')).toBeNull();
  });

  /** LE test qui compte : avant, le champ était vidé sans attendre la réponse. */
  it('un refus à l\'envoi laisse le texte dans le champ', async () => {
    const { fixture, composant } = await afficher([bob], 2);
    await repondreAuFil(fixture, 2);

    composant.formulaire.setValue({ contenu: 'Coucou' });
    composant.envoyer();

    backend.expectOne(req => req.method === 'POST' && req.url === '/api/messages')
      .flush({ message: 'Suivez cette personne pour lui écrire.' }, { status: 403, statusText: 'Forbidden' });

    expect(composant.formulaire.value.contenu).toBe('Coucou');
    expect(composant.fil()).toEqual([]);
  });

  it('un envoi accepté vide le champ et ajoute le message au fil', async () => {
    const { fixture, composant } = await afficher([bob], 2);
    await repondreAuFil(fixture, 2);

    composant.formulaire.setValue({ contenu: 'Coucou' });
    composant.envoyer();

    backend.expectOne(req => req.method === 'POST' && req.url === '/api/messages')
      .flush(unMessage({
        id: 50, expediteur: unAuteur({ id: 1, nomAffichage: 'Alice' }),
        destinataire: bob.compte, contenu: 'Coucou'
      }));

    expect(composant.formulaire.value.contenu).toBeFalsy();
    expect(composant.fil().map(m => m.contenu)).toEqual(['Coucou']);
  });
});

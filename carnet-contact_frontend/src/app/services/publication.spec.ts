import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { PublicationService } from './publication';
import { PageFil, Publication } from '../publication.model';
import { unePublication, uneReaction } from '../donnees-test';

describe('PublicationService', () => {
  let service: PublicationService;
  let backend: HttpTestingController;

  const tranche = (publications: Publication[], curseurSuivant: number | null = null): PageFil =>
    ({ publications, curseurSuivant });

  const requeteFil = () =>
    backend.expectOne(r => r.method === 'GET' && r.url === '/api/publications');

  const ids = () => service.publications().map(p => p.id);

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });

    service = TestBed.inject(PublicationService);
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => backend.verify());

  it('charge la première tranche, sans catégorie ni curseur', () => {
    service.charger(null);

    const requete = requeteFil();
    expect(requete.request.params.get('taille')).toBe('10');
    expect(requete.request.params.has('categorie')).toBe(false);
    expect(requete.request.params.has('avant')).toBe(false);

    requete.flush(tranche([unePublication({ id: 3 })], 3));

    expect(ids()).toEqual([3]);
    expect(service.aDesPlusAnciennes()).toBe(true);
  });

  it('envoie la catégorie choisie', () => {
    service.charger('SPORT');

    const requete = requeteFil();
    expect(requete.request.params.get('categorie')).toBe('SPORT');
    requete.flush(tranche([]));

    expect(service.categorie()).toBe('SPORT');
  });

  it('« Voir plus » repart du curseur et ajoute à la fin', () => {
    service.charger(null);
    requeteFil().flush(tranche([unePublication({ id: 12 }), unePublication({ id: 11 })], 11));

    service.chargerPlus();

    const requete = requeteFil();
    expect(requete.request.params.get('avant')).toBe('11');
    requete.flush(tranche([unePublication({ id: 10 })], null));

    expect(ids()).toEqual([12, 11, 10]);
    expect(service.aDesPlusAnciennes()).toBe(false);
  });

  it('« Voir plus » ne demande rien quand tout est affiché', () => {
    service.charger(null);
    requeteFil().flush(tranche([unePublication()], null));

    service.chargerPlus();

    // verify() dans afterEach échouerait si une requête était partie.
  });

  /**
   * Le test qui justifie de faire passer « Voir plus » par le même switchMap
   * que le changement de filtre. Sans annulation, la suite du fil « Tout »
   * arriverait après le changement et s'ajouterait sous les publications
   * « Cuisine ».
   */
  it('changer de catégorie annule un « Voir plus » encore en route', () => {
    service.charger(null);
    requeteFil().flush(tranche([unePublication({ id: 12 })], 12));

    service.chargerPlus();
    service.charger('CUISINE');

    const requetes = backend.match(r => r.url === '/api/publications');
    expect(requetes.length).toBe(2);
    expect(requetes[0].cancelled).toBe(true);

    requetes[1].flush(tranche([unePublication({ id: 5, categorie: 'CUISINE' })]));
    expect(ids()).toEqual([5]);
  });

  it('reste utilisable après une erreur serveur', () => {
    service.charger(null);
    requeteFil().flush('', { status: 500, statusText: 'Server Error' });

    service.charger(null);
    requeteFil().flush(tranche([unePublication({ id: 1 })]));

    expect(ids()).toEqual([1]);
  });

  it('une publication de la catégorie filtrée apparaît en tête', () => {
    service.charger('SPORT');
    requeteFil().flush(tranche([unePublication({ id: 1, categorie: 'SPORT' })]));

    service.publier({ categorie: 'SPORT', contenu: 'Piscine', imageUrl: '' }).subscribe();
    backend.expectOne(r => r.method === 'POST')
      .flush(unePublication({ id: 2, categorie: 'SPORT' }));

    expect(ids()).toEqual([2, 1]);
  });

  it('une publication d\'une autre catégorie n\'entre pas dans le filtre', () => {
    service.charger('SPORT');
    requeteFil().flush(tranche([unePublication({ id: 1, categorie: 'SPORT' })]));

    service.publier({ categorie: 'CUISINE', contenu: 'Tarte', imageUrl: '' }).subscribe();
    backend.expectOne(r => r.method === 'POST')
      .flush(unePublication({ id: 2, categorie: 'CUISINE' }));

    expect(ids()).toEqual([1]);
  });

  it('une publication modifiée qui quitte la catégorie filtrée disparaît', () => {
    service.charger('SPORT');
    requeteFil().flush(tranche([unePublication({ id: 1, categorie: 'SPORT' })]));

    service.modifier(1, { categorie: 'NATURE', contenu: 'Randonnée', imageUrl: '' }).subscribe();
    backend.expectOne(r => r.method === 'PUT' && r.url === '/api/publications/1')
      .flush(unePublication({ id: 1, categorie: 'NATURE' }));

    expect(ids()).toEqual([]);
  });

  /** Le curseur rend la mise à jour locale sûre : rien ne se décale derrière. */
  it('supprime localement, sans recharger le fil', () => {
    service.charger(null);
    requeteFil().flush(tranche([unePublication({ id: 2 }), unePublication({ id: 1 })]));

    service.supprimer(2);
    backend.expectOne(r => r.method === 'DELETE' && r.url === '/api/publications/2').flush(null);

    expect(ids()).toEqual([1]);
  });

  it('remplace la publication réagie par la réponse du serveur', () => {
    service.charger(null);
    requeteFil().flush(tranche([unePublication({ id: 1 }), unePublication({ id: 2 })]));

    service.reagir(1, '👍');

    const requete = backend.expectOne('/api/publications/1/reaction');
    expect(requete.request.method).toBe('PUT');
    expect(requete.request.body).toEqual({ emoji: '👍' });
    requete.flush(unePublication({ id: 1, reactions: [uneReaction({ parMoi: true })] }));

    expect(service.publications()[0].reactions[0].parMoi).toBe(true);
    expect(service.publications()[1].reactions).toEqual([]);
  });

  // --- Filtres « abonnements » et « auteur » -------------------------------

  it('envoie le filtre « abonnements », puis le filtre « auteur », sans les mélanger', () => {
    service.charger(null, { abonnements: true });

    const abonnements = requeteFil();
    expect(abonnements.request.params.get('abonnements')).toBe('true');
    expect(abonnements.request.params.has('auteur')).toBe(false);
    abonnements.flush(tranche([]));

    // Charger la page d'une personne repart d'un filtre neuf : « abonnements »
    // ne doit pas traîner dans la requête.
    service.charger(null, { auteurId: 5 });

    const auteur = requeteFil();
    expect(auteur.request.params.get('auteur')).toBe('5');
    expect(auteur.request.params.has('abonnements')).toBe(false);
    auteur.flush(tranche([]));
  });

  it('« Voir plus » garde le filtre « abonnements »', () => {
    service.charger(null, { abonnements: true });
    requeteFil().flush(tranche([unePublication({ id: 12 })], 12));

    service.chargerPlus();

    const requete = requeteFil();
    expect(requete.request.params.get('abonnements')).toBe('true');
    expect(requete.request.params.get('avant')).toBe('12');
    requete.flush(tranche([]));
  });

  /** On ne se suit pas soi-même : sa propre publication n'a rien à faire sous « Abonnements ». */
  it('sa propre publication n\'apparaît pas sous « Abonnements »', () => {
    service.charger(null, { abonnements: true });
    requeteFil().flush(tranche([unePublication({ id: 1 })]));

    service.publier({ categorie: 'SPORT', contenu: 'Piscine', imageUrl: '' }).subscribe();
    backend.expectOne(r => r.method === 'POST').flush(unePublication({ id: 9 }));

    expect(ids()).toEqual([1]);
  });

  it('une publication n\'apparaît pas sur la page d\'une autre personne', () => {
    service.charger(null, { auteurId: 5 });
    requeteFil().flush(tranche([]));

    service.publier({ categorie: 'SPORT', contenu: 'Piscine', imageUrl: '' }).subscribe();
    backend.expectOne(r => r.method === 'POST')
      .flush(unePublication({ id: 9, auteur: { id: 1, nomAffichage: 'Alice', photoUrl: null } }));

    expect(ids()).toEqual([]);
  });
});

import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ContactService } from './contact';
import { Contact, PageContacts } from '../contact.model';

/**
 * Tester un service qui fait des requêtes HTTP.
 *
 * Le problème : un test ne doit dépendre ni du réseau ni d'un backend démarré.
 * S'il échoue, on veut savoir que c'est le code qui est faux — pas que le
 * serveur était éteint. Et il doit pouvoir simuler des situations qu'on ne sait
 * pas provoquer à volonté (une erreur 500, une réponse qui arrive en retard).
 *
 * La solution d'Angular : `provideHttpClientTesting()` remplace la couche qui
 * parle au réseau. Le service continue d'utiliser HttpClient exactement comme en
 * vrai — il n'a aucune idée qu'il est testé — mais les requêtes atterrissent
 * dans un « faux serveur » qu'on pilote depuis le test, le
 * `HttpTestingController`.
 *
 * | Méthode | Rôle |
 * |---|---|
 * | `expectOne(url)` | Affirme qu'une requête et une seule attend sur cette URL, et la rend |
 * | `match(critère)` | Rend toutes les requêtes en attente qui correspondent |
 * | `req.flush(corps)` | Répond avec succès |
 * | `req.flush(corps, { status })` | Répond avec une erreur |
 * | `verify()` | Échoue s'il reste une requête à laquelle personne n'a répondu |
 */
describe('ContactService', () => {
  let service: ContactService;
  let backend: HttpTestingController;

  const contactExemple: Contact = {
    id: 1, nom: 'Dupont', prenom: 'Marie', email: 'marie@exemple.fr'
  };

  function page(contenu: Contact[], extras: Partial<PageContacts> = {}): PageContacts {
    return {
      contenu,
      page: 0,
      taille: 6,
      total: contenu.length,
      totalPages: contenu.length > 0 ? 1 : 0,
      ...extras
    };
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });

    service = TestBed.inject(ContactService);
    backend = TestBed.inject(HttpTestingController);
  });

  // verify() dans un afterEach : c'est le garde-fou qui attrape les requêtes
  // parties sans qu'on s'y attende — souvent le signe d'un appel en trop.
  afterEach(() => backend.verify());

  it('remplit les signaux à partir de la réponse paginée', () => {
    service.chargerContacts();

    const requete = backend.expectOne(r => r.url === '/api/contacts');
    requete.flush(page([contactExemple], { total: 13, totalPages: 3 }));

    expect(service.contacts()).toEqual([contactExemple]);
    expect(service.total()).toBe(13);
    expect(service.totalPages()).toBe(3);
  });

  it('envoie les critères de recherche et de page dans la query string', () => {
    service.allerPage(2);

    const requete = backend.expectOne(r => r.url === '/api/contacts');
    expect(requete.request.params.get('page')).toBe('2');
    expect(requete.request.params.get('taille')).toBe('6');
    expect(requete.request.params.get('recherche')).toBe('');

    requete.flush(page([]));
  });

  it('revient à la première page quand la recherche change', () => {
    service.allerPage(3);
    backend.expectOne(r => r.url === '/api/contacts').flush(page([], { page: 3 }));
    expect(service.page()).toBe(3);

    service.rechercher('dupont');

    const requete = backend.expectOne(r => r.url === '/api/contacts');
    // Sans la remise à zéro, on demanderait les résultats 19 à 24 d'une
    // recherche qui n'en rend que trois : une page vide, à tort.
    expect(requete.request.params.get('page')).toBe('0');
    expect(requete.request.params.get('recherche')).toBe('dupont');

    requete.flush(page([contactExemple]));
  });

  /**
   * Le test qui justifie `switchMap`.
   *
   * Deux frappes rapprochées lancent deux requêtes. Si la première revenait
   * après la seconde, elle écraserait le résultat récent par un résultat
   * périmé. switchMap l'empêche en annulant la précédente — ce que le faux
   * serveur nous laisse constater directement.
   */
  it('annule la requête précédente quand une nouvelle recherche part', () => {
    service.rechercher('dup');
    service.rechercher('dupont');

    const requetes = backend.match(r => r.url === '/api/contacts');
    expect(requetes.length).toBe(2);
    expect(requetes[0].cancelled).toBe(true);
    expect(requetes[1].cancelled).toBe(false);

    requetes[1].flush(page([contactExemple]));
    expect(service.contacts()).toEqual([contactExemple]);
  });

  /**
   * Le test qui justifie la POSITION de `catchError` (à l'intérieur du
   * switchMap, pas à l'extérieur).
   *
   * Placé dehors, il attraperait l'erreur du flux externe, qui se terminerait
   * alors définitivement : plus aucune recherche ne repartirait de la session.
   * Ce test échouerait sur la seconde moitié — la requête n'existerait même pas.
   */
  it('reste utilisable après une erreur serveur', () => {
    service.chargerContacts();
    backend.expectOne(r => r.url === '/api/contacts')
      .flush('', { status: 500, statusText: 'Server Error' });

    service.chargerContacts();
    backend.expectOne(r => r.url === '/api/contacts').flush(page([contactExemple]));

    expect(service.contacts()).toEqual([contactExemple]);
  });

  it('recharge la page courante après un ajout', () => {
    service.addContact(contactExemple);

    backend.expectOne(r => r.method === 'POST' && r.url === '/api/contacts')
      .flush(contactExemple);

    // Le nouveau contact n'est pas ajouté au signal à la main : le découpage
    // en pages appartient au serveur, seul un rechargement peut le refléter.
    backend.expectOne(r => r.method === 'GET' && r.url === '/api/contacts')
      .flush(page([contactExemple]));

    expect(service.contacts()).toEqual([contactExemple]);
  });

  it('laisse la liste intacte quand une suppression échoue', () => {
    service.chargerContacts();
    backend.expectOne(r => r.method === 'GET').flush(page([contactExemple]));

    service.deleteContact(1);
    backend.expectOne(r => r.method === 'DELETE')
      .flush('', { status: 500, statusText: 'Server Error' });

    // EMPTY plutôt qu'une valeur de repli : sur une écriture en échec, l'état
    // local ne doit surtout pas bouger, sinon l'écran mentirait sur ce que la
    // base contient vraiment.
    expect(service.contacts()).toEqual([contactExemple]);
  });

  it('charge un contact seul pour la page de détail', () => {
    service.chargerContact(1);

    backend.expectOne('/api/contacts/1').flush(contactExemple);

    expect(service.contactCourant()).toEqual(contactExemple);
  });
});

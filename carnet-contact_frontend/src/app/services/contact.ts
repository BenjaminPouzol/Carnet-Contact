import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { EMPTY, Subject, catchError, switchMap } from 'rxjs';
import { Contact, PageContacts } from '../contact.model';
import { contexte } from '../interceptors/http-contexte';

/** Nombre de contacts affichés par page. */
const TAILLE_PAGE = 6;

@Injectable({
  providedIn: 'root'
})
export class ContactService {
  private http = inject(HttpClient);

  // URL RELATIVE : baseUrlInterceptor y ajoute l'adresse du backend au
  // passage. Le service ne connaît plus le nom du serveur.
  private apiUrl = '/api/contacts';

  // Source de vérité côté client : LA PAGE de contacts actuellement affichée.
  // Ce n'est plus « tous les contacts » — nuance qui a des conséquences, voir
  // contactCourant plus bas.
  private contactsSignal = signal<Contact[]>([]);
  readonly contacts = this.contactsSignal.asReadonly();

  // Les critères de la page affichée. Ils vivent dans le service et non dans
  // le composant : après un ajout, c'est le service qui doit savoir quelle
  // page recharger.
  private rechercheSignal = signal('');
  readonly recherche = this.rechercheSignal.asReadonly();

  private pageSignal = signal(0);
  readonly page = this.pageSignal.asReadonly();

  private totalSignal = signal(0);
  readonly total = this.totalSignal.asReadonly();

  private totalPagesSignal = signal(0);
  readonly totalPages = this.totalPagesSignal.asReadonly();

  // Valeurs dérivées : les templates n'ont ainsi aucun calcul à faire, et les
  // deux boutons ne peuvent pas se désynchroniser de l'état réel.
  readonly premierePage = computed(() => this.pageSignal() === 0);
  readonly dernierePage = computed(() => this.pageSignal() >= this.totalPagesSignal() - 1);

  /**
   * Le contact affiché par la page de détail ou d'édition.
   *
   * Pourquoi un signal séparé, alors qu'on cherchait jusqu'ici dans la liste
   * avec un computed() ? Parce que la pagination a invalidé l'hypothèse sur
   * laquelle cette astuce reposait : le signal `contacts` ne contient plus
   * TOUS les contacts, seulement six. Ouvrir /contact/42 après une recherche
   * qui ne le ramène pas donnait une page « contact introuvable » alors que
   * le contact existe. Découper une collection oblige à prévoir un accès
   * unitaire.
   */
  private contactCourantSignal = signal<Contact | null>(null);
  readonly contactCourant = this.contactCourantSignal.asReadonly();

  /**
   * Le tuyau des demandes de liste.
   *
   * Un Subject est un Observable qu'on peut alimenter à la main : `next()`
   * pousse une valeur dedans, et les abonnés la reçoivent. Il sert ici de
   * point de rendez-vous entre les appels ponctuels (`chargerContacts()`,
   * `rechercher()`) et un unique flux durable, monté une fois dans le
   * constructeur.
   *
   * Pourquoi cette complication ? Pour `switchMap`. En tapant « dupont »,
   * l'utilisateur déclenche plusieurs requêtes rapprochées ; rien ne garantit
   * qu'elles reviennent dans l'ordre où elles sont parties. Avec un
   * `.subscribe()` par appel, une réponse lente à « dup » pourrait arriver
   * APRÈS celle de « dupont » et réécrire la liste avec un résultat périmé.
   * switchMap supprime le problème à la racine : chaque nouvelle demande
   * ANNULE la précédente, réponse comprise.
   */
  private demandes = new Subject<void>();

  constructor() {
    this.demandes.pipe(
      switchMap(() => this.http.get<PageContacts>(this.apiUrl, {
        // Angular assemble la query string : ?recherche=dupont&page=0&taille=6
        // Il échappe au passage les caractères spéciaux, ce qu'une
        // concaténation à la main oublierait.
        params: {
          recherche: this.rechercheSignal(),
          page: this.pageSignal(),
          taille: TAILLE_PAGE
        },
        context: contexte({ libelle: 'Impossible de charger les contacts' })
      }).pipe(
        // catchError est placé À L'INTÉRIEUR du switchMap, et c'est capital.
        // À l'extérieur, il attraperait l'erreur du flux EXTERNE — celui des
        // demandes — qui se terminerait alors définitivement : la première
        // panne réseau condamnerait la recherche pour le reste de la session.
        // Ici, seule la requête fautive est neutralisée ; le tuyau reste ouvert.
        catchError(() => EMPTY)
      ))
    ).subscribe(page => {
      this.contactsSignal.set(page.contenu);
      this.pageSignal.set(page.page);
      this.totalSignal.set(page.total);
      this.totalPagesSignal.set(page.totalPages);

      // Supprimer le dernier contact d'une page laisse une page vide, alors
      // que des résultats existent avant. On recule d'un cran plutôt que
      // d'afficher « aucun contact » à quelqu'un qui en a trente.
      if (page.contenu.length === 0 && page.page > 0 && page.total > 0) {
        this.allerPage(page.page - 1);
      }
    });
  }

  /** (Re)charge la page courante, avec les critères actuels. */
  chargerContacts(): void {
    this.demandes.next();
  }

  /** Nouvelle recherche : on repart forcément de la première page. */
  rechercher(terme: string): void {
    this.rechercheSignal.set(terme);
    // Sans cette remise à zéro, chercher « zoé » depuis la page 3 afficherait
    // une page vide : trois résultats existent, mais on en demande les 19e à 24e.
    this.pageSignal.set(0);
    this.chargerContacts();
  }

  allerPage(page: number): void {
    this.pageSignal.set(Math.max(0, page));
    this.chargerContacts();
  }

  /** Charge un contact seul, pour la page de détail ou d'édition. */
  chargerContact(id: number): void {
    this.contactCourantSignal.set(null);

    this.http.get<Contact>(`${this.apiUrl}/${id}`, {
      context: contexte({ libelle: 'Impossible de charger ce contact' })
    }).pipe(
      catchError(() => EMPTY)
    ).subscribe(contact => this.contactCourantSignal.set(contact));
  }

  addContact(contact: Contact): void {
    this.http.post<Contact>(this.apiUrl, contact, {
      context: contexte({ libelle: 'Impossible d\'ajouter le contact' })
    }).pipe(
      // Écriture en échec : EMPTY, pour ne surtout pas toucher l'état local.
      catchError(() => EMPTY)
    ).subscribe(() => {
      // On ne peut plus se contenter d'ajouter la réponse au signal : le
      // découpage en pages est calculé par le SERVEUR. Selon son nom, le
      // nouveau contact appartient peut-être à une autre page, et le total a
      // changé de toute façon. Recharger la page courante est la seule
      // manière de rester d'accord avec lui.
      this.chargerContacts();
    });
  }

  modifierContact(contact: Contact): void {
    this.http.put<Contact>(`${this.apiUrl}/${contact.id}`, contact, {
      context: contexte({ libelle: 'Impossible d\'enregistrer les modifications' })
    }).pipe(
      catchError(() => EMPTY)
    ).subscribe(contactMaj => {
      // Mise à jour immédiate de la fiche affichée : la page de détail montre
      // le nouveau nom sans attendre le rechargement de la liste.
      this.contactCourantSignal.set(contactMaj);
      // Le nom a pu changer, donc le tri, donc la page : on resynchronise.
      this.chargerContacts();
    });
  }

  deleteContact(id: number): void {
    this.http.delete<void>(`${this.apiUrl}/${id}`, {
      context: contexte({ libelle: 'Impossible de supprimer le contact' })
    }).pipe(
      catchError(() => EMPTY)
    ).subscribe(() => {
      // Le .filter() local ne suffit plus : supprimer une ligne fait remonter
      // un contact de la page suivante, que seul le serveur connaît.
      this.chargerContacts();
    });
  }
}

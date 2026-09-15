import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { EMPTY, Observable, Subject, catchError, map, switchMap, tap } from 'rxjs';
import { Categorie, DemandePublication, PageFil, Publication } from '../publication.model';
import { contexte } from '../interceptors/http-contexte';

/** Nombre de publications demandées à chaque chargement. */
const TAILLE_FIL = 10;

/**
 * Ce que le fil affiche : une catégorie (ou toutes), les seuls comptes suivis
 * (ou tout le monde), et éventuellement un seul auteur (la page d'une personne).
 *
 * Un objet plutôt que trois signaux séparés : les trois changent ENSEMBLE, à
 * chaque chargement. Trois signaux mis à jour l'un après l'autre laisseraient,
 * pendant un instant, un filtre à moitié changé.
 */
interface FiltreFil {
  categorie: Categorie | null;
  abonnements: boolean;
  auteurId: number | null;
}

/**
 * Une demande de tranche du fil.
 *
 * Le filtre et le curseur voyagent DANS la demande, figés au moment du clic,
 * au lieu d'être relus dans les signaux quand la requête part : une demande
 * décrit exactement ce qu'on a voulu, même si l'état a bougé depuis.
 */
interface DemandeTranche {
  suite: boolean;
  filtre: FiltreFil;
  avant: number | null;
}

@Injectable({
  providedIn: 'root'
})
export class PublicationService {
  private http = inject(HttpClient);
  private apiUrl = '/api/publications';

  private publicationsSignal = signal<Publication[]>([]);
  readonly publications = this.publicationsSignal.asReadonly();

  // Le filtre actif. Il vit dans le service, comme la recherche des contacts :
  // après une publication, c'est le service qui doit savoir si elle entre dans
  // la liste affichée.
  private filtreSignal = signal<FiltreFil>({ categorie: null, abonnements: false, auteurId: null });

  // Des lectures dérivées, pour les pages : elles n'ont pas à connaître la forme
  // de l'objet filtre.
  readonly categorie = computed(() => this.filtreSignal().categorie);
  readonly abonnements = computed(() => this.filtreSignal().abonnements);

  /**
   * Le CURSEUR : l'id de la plus ancienne publication affichée, à renvoyer en
   * `avant` pour obtenir la suite. null quand le serveur a tout donné.
   *
   * Pas de numéro de page ici. Un fil grandit par le haut pendant qu'on le lit :
   * « page 2 » ne désignerait plus les mêmes publications d'une minute à
   * l'autre, alors que « plus anciennes que 42 » désigne toujours les mêmes.
   */
  private curseurSignal = signal<number | null>(null);
  readonly aDesPlusAnciennes = computed(() => this.curseurSignal() !== null);

  /**
   * Un seul tuyau pour les deux sortes de demandes (nouveau filtre, suite du
   * fil), et un switchMap au bout : la dernière demande annule celle qui était
   * en route. C'est ce qui empêche un « Voir plus » lent de venir ajouter des
   * publications d'une catégorie qu'on vient de quitter.
   */
  private demandes = new Subject<DemandeTranche>();

  constructor() {
    this.demandes.pipe(
      switchMap(demande => this.http.get<PageFil>(this.apiUrl, {
        params: this.parametres(demande),
        context: contexte({ libelle: 'Impossible de charger le fil' })
      }).pipe(
        // On garde `suite` à côté de la réponse : à l'arrivée, il faut savoir
        // s'il s'agit de REMPLACER la liste ou de la PROLONGER.
        map(tranche => ({ tranche, suite: demande.suite })),
        // À l'intérieur du switchMap, comme dans ContactService : une panne ne
        // doit pas fermer le tuyau pour le reste de la session.
        catchError(() => EMPTY)
      ))
    ).subscribe(({ tranche, suite }) => {
      this.publicationsSignal.update(liste =>
        suite ? [...liste, ...tranche.publications] : tranche.publications);
      this.curseurSignal.set(tranche.curseurSuivant);
    });
  }

  /**
   * (Re)part du début du fil.
   *
   * Les options ont des valeurs par défaut : `charger('SPORT')`, écrit avant les
   * abonnements, garde exactement son sens — et un filtre qu'on ne mentionne
   * pas est remis à zéro, pas hérité du chargement précédent.
   */
  charger(categorie: Categorie | null, options: { abonnements?: boolean; auteurId?: number | null } = {}): void {
    const filtre: FiltreFil = {
      categorie,
      abonnements: options.abonnements ?? false,
      auteurId: options.auteurId ?? null
    };

    this.filtreSignal.set(filtre);
    // On vide tout de suite : laisser les publications « Sport » affichées
    // sous le filtre « Cuisine » le temps de la réponse serait trompeur.
    this.publicationsSignal.set([]);
    this.curseurSignal.set(null);
    this.demandes.next({ suite: false, filtre, avant: null });
  }

  /** La tranche suivante, plus ancienne, avec le même filtre. Sans effet quand tout est affiché. */
  chargerPlus(): void {
    const avant = this.curseurSignal();
    if (avant === null) {
      return;
    }
    this.demandes.next({ suite: true, filtre: this.filtreSignal(), avant });
  }

  /**
   * Publier RENVOIE l'Observable, comme AuthService.connexion : l'appelant a
   * besoin de savoir que ça a réussi pour vider son formulaire — et surtout de
   * savoir que ça a échoué pour NE PAS le vider.
   *
   * Pas de rechargement après coup, contrairement aux contacts : avec un
   * curseur, insérer en tête ne décale rien de ce qui est déjà chargé.
   */
  publier(demande: DemandePublication): Observable<Publication> {
    return this.http.post<Publication>(this.apiUrl, demande, {
      context: contexte({ libelle: 'Impossible de publier' })
    }).pipe(
      tap(publication => {
        if (this.correspondAuFiltre(publication)) {
          this.publicationsSignal.update(liste => [publication, ...liste]);
        }
      })
    );
  }

  modifier(id: number, demande: DemandePublication): Observable<Publication> {
    return this.http.put<Publication>(`${this.apiUrl}/${id}`, demande, {
      context: contexte({ libelle: 'Impossible d\'enregistrer la modification' })
    }).pipe(
      tap(publication => {
        // Changer de catégorie peut faire sortir la publication du filtre
        // affiché : on la retire plutôt que de laisser une carte « Nature »
        // dans la liste « Sport ».
        this.publicationsSignal.update(liste => this.correspondAuFiltre(publication)
          ? liste.map(p => (p.id === id ? publication : p))
          : liste.filter(p => p.id !== id));
      })
    );
  }

  supprimer(id: number): void {
    this.http.delete<void>(`${this.apiUrl}/${id}`, {
      context: contexte({ libelle: 'Impossible de supprimer la publication' })
    }).pipe(
      // Écriture en échec : EMPTY, l'état local ne bouge pas.
      catchError(() => EMPTY)
    ).subscribe(() => {
      this.publicationsSignal.update(liste => liste.filter(p => p.id !== id));
    });
  }

  /**
   * Même contrat que MessageService.reagir : on dit « j'ai cliqué sur 👍 », le
   * serveur décide s'il pose, remplace ou retire, et renvoie la publication à
   * jour. Discret : une réaction ne mérite pas la bannière « Chargement… ».
   */
  reagir(id: number, emoji: string): void {
    this.http.put<Publication>(`${this.apiUrl}/${id}/reaction`, { emoji }, {
      context: contexte({ discret: true, libelle: 'Impossible d\'enregistrer la réaction' })
    }).pipe(
      catchError(() => EMPTY)
    ).subscribe(publication => {
      this.publicationsSignal.update(liste => liste.map(p => (p.id === id ? publication : p)));
    });
  }

  /**
   * Une publication que JE viens d'écrire ou de modifier a-t-elle sa place
   * dans la liste affichée ?
   *
   * - pas si la catégorie filtrée est une autre ;
   * - jamais sous « Abonnements » : on ne se suit pas soi-même ;
   * - pas sur la page d'une autre personne.
   */
  private correspondAuFiltre(publication: Publication): boolean {
    const filtre = this.filtreSignal();

    if (filtre.categorie !== null && filtre.categorie !== publication.categorie) {
      return false;
    }
    if (filtre.abonnements) {
      return false;
    }
    if (filtre.auteurId !== null && filtre.auteurId !== publication.auteur.id) {
      return false;
    }
    return true;
  }

  /** N'envoie que les paramètres utiles : pas de `categorie=null` dans l'URL. */
  private parametres(demande: DemandeTranche): Record<string, string | number> {
    const params: Record<string, string | number> = { taille: TAILLE_FIL };
    const filtre = demande.filtre;

    if (filtre.categorie !== null) {
      params['categorie'] = filtre.categorie;
    }
    if (filtre.abonnements) {
      params['abonnements'] = 'true';
    }
    if (filtre.auteurId !== null) {
      params['auteur'] = filtre.auteurId;
    }
    if (demande.avant !== null) {
      params['avant'] = demande.avant;
    }

    return params;
  }
}

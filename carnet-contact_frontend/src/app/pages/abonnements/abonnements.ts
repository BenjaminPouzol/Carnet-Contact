import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, debounceTime, distinctUntilChanged, map, of, switchMap } from 'rxjs';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { AbonnementService } from '../../services/abonnement';
import { BlocageService } from '../../services/blocage';
import { ActiviteService } from '../../services/activite';
import { CompteResume, NotificationCompte, Suggestion, texteNotification } from '../../abonnement.model';
import { AuteurPublic } from '../../utilisateur.model';
import { CarteCompte } from '../../components/carte-compte/carte-compte';

/**
 * Tout ce qui concerne les personnes qu'on suit — et qui nous suivent.
 *
 * Une page distincte des contacts, et c'est la demande de départ : un contact
 * est une fiche de SON carnet privé ; un abonnement relie deux COMPTES de
 * l'application. Les deux listes n'ont ni la même source ni les mêmes règles.
 *
 * Chaque liste est un signal local, rechargé après l'action qui la concerne.
 * Pas de signal partagé ici (contrairement aux statuts, dans le service) :
 * ces listes ne servent qu'à cette page.
 */
@Component({
  selector: 'app-abonnements',
  imports: [DatePipe, RouterLink, ReactiveFormsModule, ButtonModule, InputTextModule, CarteCompte],
  templateUrl: './abonnements.html',
  styleUrl: './abonnements.css'
})
export class Abonnements implements OnInit {
  private abonnements = inject(AbonnementService);
  private blocages = inject(BlocageService);
  private activite = inject(ActiviteService);

  notifications = signal<NotificationCompte[]>([]);
  demandes = signal<CompteResume[]>([]);

  // null : aucune recherche en cours (on montre alors les suggestions) ;
  // tableau vide : une recherche qui n'a rien trouvé. Deux situations, deux
  // messages différents — d'où deux valeurs distinctes.
  resultats = signal<CompteResume[] | null>(null);

  suggestions = signal<Suggestion[]>([]);
  mesAbonnements = signal<CompteResume[]>([]);
  abonnes = signal<CompteResume[]>([]);
  bloques = signal<AuteurPublic[]>([]);

  // Une seule requête (mes abonnements) alimente deux sections : les comptes
  // que je suis vraiment, et les demandes que j'ai envoyées.
  suivis = computed(() => this.mesAbonnements().filter(compte => compte.statut === 'ACCEPTE'));
  envoyees = computed(() => this.mesAbonnements().filter(compte => compte.statut === 'EN_ATTENTE'));

  champRecherche = new FormControl('', { nonNullable: true });

  // Exposée au gabarit, qui n'a pas accès aux imports du fichier.
  readonly texteNotification = texteNotification;

  constructor() {
    // Le patron de la recherche de contacts (section 22), avec une différence :
    // le switchMap est ICI, dans la page, parce que ces résultats ne vivent que
    // sur cette page. debounceTime évite une requête par lettre, switchMap
    // annule la recherche précédente si la réponse tarde, et catchError reste
    // À L'INTÉRIEUR du switchMap pour qu'une erreur n'arrête pas la recherche.
    this.champRecherche.valueChanges.pipe(
      map(valeur => valeur.trim()),
      debounceTime(300),
      distinctUntilChanged(),
      switchMap(terme => terme === ''
        ? of(null)
        : this.abonnements.rechercher(terme).pipe(catchError(() => of([])))),
      takeUntilDestroyed()
    ).subscribe(resultats => this.resultats.set(resultats));
  }

  ngOnInit(): void {
    // L'historique d'abord, PUIS le marquage « lu » : la page affiche encore en
    // surbrillance ce qui était nouveau, pendant que la pastille du menu se vide.
    this.activite.recentes().subscribe({
      next: notifications => {
        this.notifications.set(notifications);
        this.activite.marquerLues();
      },
      error: () => {}
    });

    this.chargerDemandes();
    this.chargerRelations();
    this.chargerAbonnes();
    this.chargerBloques();
  }

  accepter(compte: CompteResume): void {
    this.abonnements.accepter(compte.id).subscribe({
      // Accepté : il quitte les demandes et rejoint les abonnés.
      next: () => {
        this.chargerDemandes();
        this.chargerAbonnes();
      },
      error: () => {}
    });
  }

  refuser(compte: CompteResume): void {
    this.abonnements.refuser(compte.id).subscribe({
      next: () => this.chargerDemandes(),
      error: () => {}
    });
  }

  retirer(compte: CompteResume): void {
    this.abonnements.retirerAbonne(compte.id).subscribe({
      next: () => this.chargerAbonnes(),
      error: () => {}
    });
  }

  debloquer(compte: AuteurPublic): void {
    this.blocages.debloquer(compte.id).subscribe({
      // Débloqué, le compte peut reparaître dans les suggestions.
      next: () => {
        this.chargerBloques();
        this.chargerRelations();
      },
      error: () => {}
    });
  }

  /**
   * Un bouton « Suivre » a changé un statut, quelle que soit la section :
   * « Je suis », « Demandes envoyées » et les suggestions doivent suivre.
   */
  relationModifiee(): void {
    this.chargerRelations();
  }

  private chargerDemandes(): void {
    this.abonnements.demandes().subscribe({
      next: liste => this.demandes.set(liste),
      error: () => {}
    });
  }

  /** Mes abonnements (qui remettent aussi à jour les statuts des boutons) et les suggestions. */
  private chargerRelations(): void {
    this.abonnements.abonnements().subscribe({
      next: liste => this.mesAbonnements.set(liste),
      error: () => {}
    });
    this.abonnements.suggestions().subscribe({
      next: liste => this.suggestions.set(liste),
      error: () => {}
    });
  }

  private chargerAbonnes(): void {
    this.abonnements.abonnes().subscribe({
      next: liste => this.abonnes.set(liste),
      error: () => {}
    });
  }

  private chargerBloques(): void {
    this.blocages.liste().subscribe({
      next: liste => this.bloques.set(liste),
      error: () => {}
    });
  }
}

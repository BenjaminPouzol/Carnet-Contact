import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { map } from 'rxjs';
import { ButtonModule } from 'primeng/button';
import { AbonnementService } from '../../services/abonnement';
import { BlocageService } from '../../services/blocage';
import { PublicationService } from '../../services/publication';
import { EtatHttpService } from '../../services/etat-http';
import { AuthService } from '../../services/auth';
import { ProfilPublic } from '../../abonnement.model';
import { RESEAUX } from '../../contact.model';
import { BoutonSuivre } from '../../components/bouton-suivre/bouton-suivre';
import { ReseauxSociaux } from '../../components/reseaux-sociaux/reseaux-sociaux';
import { PublicationCarte } from '../../components/publication-carte/publication-carte';

/**
 * La page d'une personne : qui elle est, ce qu'elle publie, et ce qu'on peut
 * faire avec elle (suivre, écrire, retirer de ses abonnés, bloquer).
 *
 * La page ne décide RIEN de ce qui est visible. Le serveur envoie
 * `coordonnees` (ou null), `contenuVisible`, `peutEcrire` : elle affiche en
 * conséquence. Les règles restent ainsi à un seul endroit, côté serveur.
 */
@Component({
  selector: 'app-personne',
  imports: [RouterLink, ButtonModule, BoutonSuivre, ReseauxSociaux, PublicationCarte],
  templateUrl: './personne.html',
  styleUrl: './personne.css'
})
export class Personne {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private abonnements = inject(AbonnementService);
  private blocages = inject(BlocageService);
  private publicationService = inject(PublicationService);
  private etatHttp = inject(EtatHttpService);
  private auth = inject(AuthService);

  profil = signal<ProfilPublic | null>(null);
  introuvable = signal(false);

  // Même principe que la suppression d'une publication : la confirmation
  // s'affiche à l'endroit du clic, sans boîte de dialogue.
  confirmationBlocage = signal(false);

  publications = this.publicationService.publications;
  aDesPlusAnciennes = this.publicationService.aDesPlusAnciennes;
  chargement = this.etatHttp.chargement;

  estMoi = computed(() => this.profil()?.id === this.auth.utilisateur()?.id);

  initiale = computed(() => (this.profil()?.nomAffichage || '?').charAt(0).toUpperCase());

  /** Des coordonnées reçues, mais toutes vides : la personne n'a rien renseigné. */
  coordonneesVides = computed(() => {
    const coordonnees = this.profil()?.coordonnees;
    return !!coordonnees && !coordonnees.emailPro && RESEAUX.every(reseau => !coordonnees[reseau.cle]);
  });

  constructor() {
    /**
     * paramMap (un Observable) et non snapshot : passer de /personne/2 à
     * /personne/3 — en cliquant sur un nom dans une publication — RÉUTILISE le
     * même composant. Le snapshot, lu une seule fois, garderait l'ancienne
     * personne à l'écran ; l'Observable émet à chaque nouvel identifiant.
     *
     * takeUntilDestroyed : l'abonnement s'arrête quand on quitte la page.
     */
    this.route.paramMap.pipe(
      map(parametres => Number(parametres.get('id'))),
      takeUntilDestroyed()
    ).subscribe(id => this.charger(id));
  }

  /** Après un changement de relation : le serveur recalcule ce qui est visible. */
  recharger(): void {
    const profil = this.profil();
    if (profil) {
      this.charger(profil.id);
    }
  }

  retirerDeMesAbonnes(): void {
    const profil = this.profil();
    if (!profil) {
      return;
    }
    this.abonnements.retirerAbonne(profil.id).subscribe({
      next: () => this.recharger(),
      error: () => {}
    });
  }

  demanderBlocage(): void {
    this.confirmationBlocage.set(true);
  }

  annulerBlocage(): void {
    this.confirmationBlocage.set(false);
  }

  /** Après un blocage, la page n'a plus rien à montrer : retour aux abonnements. */
  confirmerBlocage(): void {
    const profil = this.profil();
    if (!profil) {
      return;
    }
    this.blocages.bloquer(profil.id).subscribe({
      next: () => this.router.navigate(['/abonnements']),
      error: () => this.confirmationBlocage.set(false)
    });
  }

  voirPlus(): void {
    this.publicationService.chargerPlus();
  }

  private charger(id: number): void {
    this.profil.set(null);
    this.introuvable.set(false);
    this.confirmationBlocage.set(false);

    // Les statuts d'abonnement alimentent le bouton « Suivre ».
    this.abonnements.charger();

    this.abonnements.profil(id).subscribe({
      next: profil => {
        this.profil.set(profil);
        // On ne demande pas au serveur ce qu'il refuserait de montrer.
        if (profil.contenuVisible) {
          this.publicationService.charger(null, { auteurId: id });
        }
      },
      // Inconnu, désactivé ou bloqué : le serveur répond 404 dans les trois cas,
      // et la page n'a pas à savoir lequel.
      error: () => this.introuvable.set(true)
    });
  }
}

import { Component, computed, inject, input, output, signal } from '@angular/core';
import { ButtonModule } from 'primeng/button';
import { Observable, finalize, map } from 'rxjs';
import { AbonnementService } from '../../services/abonnement';
import { AuthService } from '../../services/auth';
import { StatutRelation } from '../../abonnement.model';

/**
 * « Suivre » / « Demande envoyée » / « Abonné·e », selon ma relation au compte.
 *
 * Le bouton ne garde AUCUN état à lui : il lit le statut dans AbonnementService.
 * C'est ce qui permet à deux boutons visant le même compte — deux publications
 * d'Alice dans le fil — de rester d'accord : l'un change le signal du service,
 * l'autre se met à jour tout seul.
 */
@Component({
  selector: 'app-bouton-suivre',
  imports: [ButtonModule],
  templateUrl: './bouton-suivre.html',
  styleUrl: './bouton-suivre.css'
})
export class BoutonSuivre {
  private abonnements = inject(AbonnementService);
  private auth = inject(AuthService);

  compteId = input.required<number>();
  comptePrive = input(false);

  /**
   * Prévient le parent du nouveau statut, une fois le serveur d'accord.
   *
   * Le bouton lui-même n'en a pas besoin (il lit le service), mais une page peut
   * avoir à réagir : recharger un profil pour faire apparaître les coordonnées,
   * ou retirer une ligne d'une liste de suggestions.
   */
  statutChange = output<StatutRelation>();

  // Pendant l'appel : le bouton est désactivé, un double clic ne part pas deux fois.
  enCours = signal(false);

  estMoi = computed(() => this.auth.utilisateur()?.id === this.compteId());

  /** computed() lit un signal du service à travers statutDe : il se recalcule avec lui. */
  statut = computed(() => this.abonnements.statutDe(this.compteId()));

  libelle = computed(() => {
    switch (this.statut()) {
      case 'ACCEPTE':
        return 'Abonné·e';
      case 'EN_ATTENTE':
        return 'Demande envoyée';
      default:
        // Un compte privé ne se « suit » pas d'un clic : le libellé l'annonce.
        return this.comptePrive() ? 'Demander à suivre' : 'Suivre';
    }
  });

  icone = computed(() => {
    switch (this.statut()) {
      case 'ACCEPTE':
        return 'pi pi-check';
      case 'EN_ATTENTE':
        return 'pi pi-clock';
      default:
        return this.comptePrive() ? 'pi pi-lock' : 'pi pi-user-plus';
    }
  });

  /** L'infobulle dit ce qu'un clic va FAIRE, pas l'état actuel — déjà écrit sur le bouton. */
  infobulle = computed(() => {
    switch (this.statut()) {
      case 'ACCEPTE':
        return 'Ne plus suivre';
      case 'EN_ATTENTE':
        return 'Annuler la demande';
      default:
        return null;
    }
  });

  basculer(): void {
    const id = this.compteId();

    // Les deux branches rendent un Observable du nouveau statut : la suite du
    // traitement est la même quel que soit le sens du clic.
    const appel: Observable<StatutRelation> = this.statut() === 'AUCUN'
      ? this.abonnements.suivre(id).pipe(map(compte => compte.statut))
      : this.abonnements.nePlusSuivre(id).pipe(map((): StatutRelation => 'AUCUN'));

    this.enCours.set(true);

    appel.pipe(
      finalize(() => this.enCours.set(false))
    ).subscribe({
      next: statut => this.statutChange.emit(statut),
      // La bannière vient de l'intercepteur ; le statut du service n'a pas bougé.
      error: () => {}
    });
  }
}

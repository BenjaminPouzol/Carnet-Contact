import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { debounceTime, distinctUntilChanged } from 'rxjs';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { AvatarModule } from 'primeng/avatar';
import { InputTextModule } from 'primeng/inputtext';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { TooltipModule } from 'primeng/tooltip';
import { ConfirmationService } from 'primeng/api';
import { AdminService } from '../../services/admin';
import { EtatHttpService } from '../../services/etat-http';
import { LigneCompte } from '../../utilisateur.model';

@Component({
  selector: 'app-admin',
  imports: [
    DatePipe, ReactiveFormsModule, TableModule, ButtonModule, TagModule,
    AvatarModule, InputTextModule, ConfirmDialogModule, TooltipModule
  ],
  // ConfirmationService est fourni ICI plutôt que dans app.config : il ne sert
  // qu'à cette page. Le déclarer au niveau du composant le fait vivre le temps
  // de la page, au lieu de toute la session.
  providers: [ConfirmationService],
  templateUrl: './admin.html',
  styleUrl: './admin.css'
})
export class Admin implements OnInit {
  private adminService = inject(AdminService);
  private etatHttp = inject(EtatHttpService);
  private confirmation = inject(ConfirmationService);

  comptes = this.adminService.comptes;
  chargement = this.etatHttp.chargement;

  champRecherche = new FormControl('', { nonNullable: true });

  /** Le terme réellement appliqué, une fois la frappe retombée. */
  private terme = signal('');

  /**
   * Le filtrage se fait ICI, en mémoire, et non côté serveur comme pour les
   * contacts (section 22).
   *
   * La différence tient au volume : les comptes d'une application se comptent
   * en dizaines, les contacts en milliers. Filtrer une liste déjà chargée est
   * instantané et ne coûte aucune requête ; pagination et recherche serveur
   * seraient ici de la complexité pour rien. La même solution n'est pas la
   * bonne selon l'échelle.
   */
  comptesFiltres = computed(() => {
    const recherche = this.terme().trim().toLowerCase();

    if (recherche === '') {
      return this.comptes();
    }

    return this.comptes().filter(c =>
      c.email.toLowerCase().includes(recherche)
      || c.nomAffichage.toLowerCase().includes(recherche)
    );
  });

  // Compteurs d'en-tête, dérivés de la liste : aucune donnée en double, donc
  // aucun risque de désynchronisation après une action.
  readonly nombreAdmins = computed(() => this.comptes().filter(c => c.role === 'ADMIN').length);
  readonly nombreDesactives = computed(() => this.comptes().filter(c => !c.actif).length);

  constructor() {
    this.champRecherche.valueChanges.pipe(
      // Le filtrage est local, donc instantané : 200 ms suffisent, là où une
      // recherche réseau demandait 300 ms (section 22). L'anti-rebond sert
      // ici à éviter de recalculer la liste à chaque touche, pas à épargner
      // des requêtes.
      debounceTime(200),
      distinctUntilChanged(),
      takeUntilDestroyed()
    ).subscribe(valeur => this.terme.set(valeur));
  }

  ngOnInit(): void {
    this.adminService.charger();
  }

  basculerActif(compte: LigneCompte): void {
    this.adminService.changerActif(compte.id, !compte.actif);
  }

  basculerRole(compte: LigneCompte): void {
    this.adminService.changerRole(compte.id, compte.role === 'ADMIN' ? 'UTILISATEUR' : 'ADMIN');
  }

  /**
   * La suppression est irréversible et emporte les contacts et les messages :
   * elle passe par une confirmation explicite, qui NOMME le compte visé et
   * annonce les conséquences chiffrées.
   *
   * Une boîte de dialogue qui dirait seulement « Êtes-vous sûr ? » ne servirait
   * à rien : on répond oui par réflexe. Celle-ci donne de quoi vérifier qu'on
   * a cliqué sur la bonne ligne.
   */
  demanderSuppression(compte: LigneCompte): void {
    this.confirmation.confirm({
      header: 'Supprimer ce compte ?',
      message: `Le compte « ${compte.nomAffichage} » sera supprimé, ainsi que ses `
        + `${compte.nombreContacts} contact(s), ${compte.nombreMessages} message(s) `
        + `et ${compte.nombrePublications} publication(s). Cette action est irréversible.`,
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Supprimer',
      rejectLabel: 'Annuler',
      acceptButtonStyleClass: 'p-button-danger',
      rejectButtonStyleClass: 'p-button-text',
      accept: () => this.adminService.supprimer(compte.id)
    });
  }
}

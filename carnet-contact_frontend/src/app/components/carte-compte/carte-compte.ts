import { Component, computed, input, output } from '@angular/core';
import { RouterLink } from '@angular/router';
import { BoutonSuivre } from '../bouton-suivre/bouton-suivre';
import { StatutRelation } from '../../abonnement.model';

/**
 * Ce qu'il faut pour afficher une ligne de compte.
 *
 * Un type à part plutôt que CompteResume : la carte sert aussi pour la liste
 * des comptes bloqués, qui ne reçoit que des AuteurPublic. Les deux types
 * possèdent ces champs — TypeScript compare les FORMES, pas les noms de types,
 * donc l'un comme l'autre convient sans conversion.
 */
export interface CompteAffichable {
  id: number;
  nomAffichage: string;
  photoUrl?: string | null;
  comptePrive?: boolean;
}

/**
 * Une ligne de compte : avatar, nom cliquable vers sa page, bouton « Suivre ».
 *
 * `<ng-content />` réserve une place pour des actions propres à chaque liste
 * (« Accepter » / « Refuser » pour une demande, « Débloquer » pour un blocage) :
 * c'est la PROJECTION de contenu. Le parent écrit ses boutons entre les balises
 * `<app-carte-compte>`, et la carte les affiche à l'endroit prévu.
 */
@Component({
  selector: 'app-carte-compte',
  imports: [RouterLink, BoutonSuivre],
  templateUrl: './carte-compte.html',
  styleUrl: './carte-compte.css'
})
export class CarteCompte {
  compte = input.required<CompteAffichable>();
  avecBouton = input(true);

  // Relayé depuis le bouton : le parent peut recharger la liste concernée.
  statutChange = output<StatutRelation>();

  initiale = computed(() => (this.compte().nomAffichage || '?').charAt(0).toUpperCase());
}

import { Injectable, computed, signal } from '@angular/core';

/**
 * État TRANSVERSE des requêtes HTTP : combien sont en cours, et quel est le
 * dernier message d'erreur. Ce service ne connaît aucun métier — il ne parle
 * ni de contacts ni d'autre chose. Il est alimenté par les intercepteurs et
 * lu par la coquille de l'application.
 *
 * Pourquoi ne pas laisser ces deux signaux dans ContactService ? Parce que la
 * règle « allumer un indicateur pendant une requête » n'est pas du métier
 * contact : elle vaut pour toute l'application. Et parce qu'un intercepteur
 * qui injecterait ContactService, lequel injecte HttpClient, lequel appelle
 * l'intercepteur, formerait une boucle de dépendances difficile à suivre.
 */
@Injectable({
  providedIn: 'root'
})
export class EtatHttpService {
  // Un COMPTEUR, pas un booléen : maintenant que l'intercepteur voit toutes
  // les requêtes, deux d'entre elles peuvent être en vol en même temps. Avec
  // un booléen, la première qui se termine éteindrait l'indicateur alors que
  // la seconde tourne encore.
  private requetesEnCours = signal(0);

  // Le booléen attendu par les templates est DÉRIVÉ du compteur : aucun
  // risque que les deux se désynchronisent.
  readonly chargement = computed(() => this.requetesEnCours() > 0);

  private erreurSignal = signal<string | null>(null);
  readonly erreur = this.erreurSignal.asReadonly();

  debutRequete(): void {
    this.requetesEnCours.update(n => n + 1);
  }

  finRequete(): void {
    // Math.max par prudence : le compteur ne doit jamais passer sous zéro,
    // sinon chargement() resterait faux même avec une requête en vol.
    this.requetesEnCours.update(n => Math.max(0, n - 1));
  }

  signalerErreur(message: string): void {
    this.erreurSignal.set(message);
  }

  effacerErreur(): void {
    this.erreurSignal.set(null);
  }
}

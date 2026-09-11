import { Injectable, PLATFORM_ID, computed, inject, signal } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { Utilisateur } from '../utilisateur.model';

const CLE_JETON = 'carnet.jeton';
const CLE_RAFRAICHISSEMENT = 'carnet.rafraichissement';
const CLE_UTILISATEUR = 'carnet.utilisateur';

/**
 * Porte l'état de session : le jeton et le compte connecté.
 *
 * Pourquoi séparé de AuthService ? Pour la même raison que EtatHttpService
 * (section 17) : l'intercepteur d'authentification a besoin de LIRE le jeton.
 * S'il injectait AuthService, qui injecte HttpClient, qui appelle
 * l'intercepteur, on retomberait sur la boucle de dépendances évitée la
 * dernière fois. Ce service-ci ne dépend de rien : il peut être injecté
 * partout sans risque.
 */
@Injectable({
  providedIn: 'root'
})
export class SessionService {
  // PLATFORM_ID : un jeton d'injection fourni par Angular qui dit sur quelle
  // plateforme le code tourne. Indispensable avec le SSR (section 13) :
  // localStorage n'existe QUE dans le navigateur, y toucher côté serveur
  // ferait planter le rendu.
  private plateforme = inject(PLATFORM_ID);
  private navigateur = isPlatformBrowser(this.plateforme);

  private jetonSignal = signal<string | null>(null);

  // Le second jeton, celui qui sert à renouveler le premier. Il ne part JAMAIS
  // dans l'en-tête Authorization : il ne sert qu'à l'appel /api/auth/rafraichir.
  private rafraichissementSignal = signal<string | null>(null);

  private utilisateurSignal = signal<Utilisateur | null>(null);

  readonly utilisateur = this.utilisateurSignal.asReadonly();

  // État dérivé : « connecté » n'est pas une donnée à stocker, c'est une
  // conséquence de la présence d'un jeton.
  readonly connecte = computed(() => this.jetonSignal() !== null);

  constructor() {
    // Au démarrage, on restaure la session laissée par la visite précédente.
    // Sans cela, un simple F5 déconnecterait l'utilisateur.
    if (this.navigateur) {
      const jeton = localStorage.getItem(CLE_JETON);
      const utilisateurBrut = localStorage.getItem(CLE_UTILISATEUR);

      if (jeton && utilisateurBrut) {
        this.jetonSignal.set(jeton);
        this.rafraichissementSignal.set(localStorage.getItem(CLE_RAFRAICHISSEMENT));
        try {
          this.utilisateurSignal.set(JSON.parse(utilisateurBrut));
        } catch {
          // Donnée corrompue : on repart d'une session vide plutôt que de
          // laisser l'application dans un état incohérent.
          this.vider();
        }
      }
    }
  }

  /**
   * Lecture synchrone du jeton, pour l'intercepteur. On expose une méthode
   * plutôt que le signal lui-même : l'intercepteur n'a pas à réagir aux
   * changements, il a juste besoin de la valeur au moment de l'envoi.
   */
  jetonActuel(): string | null {
    return this.jetonSignal();
  }

  jetonRafraichissementActuel(): string | null {
    return this.rafraichissementSignal();
  }

  ouvrir(jeton: string, jetonRafraichissement: string, utilisateur: Utilisateur): void {
    this.jetonSignal.set(jeton);
    this.rafraichissementSignal.set(jetonRafraichissement);
    this.utilisateurSignal.set(utilisateur);

    if (this.navigateur) {
      localStorage.setItem(CLE_JETON, jeton);
      localStorage.setItem(CLE_RAFRAICHISSEMENT, jetonRafraichissement);
      localStorage.setItem(CLE_UTILISATEUR, JSON.stringify(utilisateur));
    }
  }

  /** Met à jour le profil affiché sans toucher au jeton. */
  majUtilisateur(utilisateur: Utilisateur): void {
    this.utilisateurSignal.set(utilisateur);
    if (this.navigateur) {
      localStorage.setItem(CLE_UTILISATEUR, JSON.stringify(utilisateur));
    }
  }

  vider(): void {
    this.jetonSignal.set(null);
    this.rafraichissementSignal.set(null);
    this.utilisateurSignal.set(null);

    if (this.navigateur) {
      localStorage.removeItem(CLE_JETON);
      localStorage.removeItem(CLE_RAFRAICHISSEMENT);
      localStorage.removeItem(CLE_UTILISATEUR);
    }
  }
}

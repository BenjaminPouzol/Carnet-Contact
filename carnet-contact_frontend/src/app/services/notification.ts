import { Injectable, PLATFORM_ID, inject, signal } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { Router } from '@angular/router';

/** Un bandeau affiché dans l'application elle-même. */
export interface Bandeau {
  id: number;
  titre: string;
  corps: string;
}

/** Durée d'affichage d'un bandeau avant disparition automatique. */
const DUREE_BANDEAU_MS = 6000;

/**
 * Prévient l'utilisateur qu'il se passe quelque chose — ici, l'arrivée d'un
 * message.
 *
 * Deux canaux, et pas un seul, parce qu'aucun ne suffit :
 *
 * - La **notification du système** (celle qui apparaît dans un coin de l'écran,
 *   même quand le navigateur est réduit) est la seule qui atteigne quelqu'un
 *   qui ne regarde pas l'onglet. Mais elle exige une permission, que
 *   l'utilisateur peut refuser — et qu'on ne peut alors plus redemander.
 * - Le **bandeau interne** marche toujours, sans rien demander. Mais il est
 *   invisible si l'onglet est en arrière-plan.
 *
 * D'où la règle appliquée plus bas : notification système quand l'onglet est
 * caché ET la permission accordée, bandeau interne dans tous les autres cas.
 */
@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  private router = inject(Router);
  private navigateur = isPlatformBrowser(inject(PLATFORM_ID));

  /**
   * L'état de la permission.
   *
   * « indisponible » n'est pas une valeur de l'API : c'est la nôtre, pour le
   * cas où `Notification` n'existe pas du tout — rendu côté serveur, navigateur
   * ancien, ou page servie en HTTP simple sur certains navigateurs. Sans cette
   * quatrième valeur, il faudrait tester `typeof Notification` à chaque usage.
   */
  private permissionSignal = signal<NotificationPermission | 'indisponible'>('indisponible');
  readonly permission = this.permissionSignal.asReadonly();

  private bandeauxSignal = signal<Bandeau[]>([]);
  readonly bandeaux = this.bandeauxSignal.asReadonly();

  private prochainId = 1;

  constructor() {
    if (this.navigateur && 'Notification' in window) {
      this.permissionSignal.set(Notification.permission);
    }
  }

  /**
   * Demande l'autorisation d'afficher des notifications système.
   *
   * À appeler depuis un CLIC. Les navigateurs refusent (ou ignorent
   * silencieusement) une demande de permission qui ne fait pas suite à une
   * action de l'utilisateur — précisément pour empêcher les sites de réclamer
   * l'autorisation dès l'ouverture de la page. C'est pourquoi il y a un bouton
   * dans la page Profil, et pas un appel automatique au démarrage.
   */
  demanderPermission(): void {
    if (!this.navigateur || !('Notification' in window)) {
      return;
    }

    Notification.requestPermission().then(reponse => this.permissionSignal.set(reponse));
  }

  /**
   * Prévient l'utilisateur, par le canal qui a une chance d'être vu.
   *
   * `document.hidden` est vrai quand l'onglet n'est pas au premier plan (autre
   * onglet actif, fenêtre réduite). C'est ce qui permet de ne PAS déclencher
   * une notification système quand l'utilisateur a justement la page sous les
   * yeux — elle serait redondante et agaçante.
   */
  notifier(titre: string, corps: string): void {
    if (!this.navigateur) {
      return;
    }

    if (this.permissionSignal() === 'granted' && document.hidden) {
      this.notificationSysteme(titre, corps);
    } else {
      this.ajouterBandeau(titre, corps);
    }
  }

  private notificationSysteme(titre: string, corps: string): void {
    const notification = new Notification(titre, {
      body: corps,
      // tag : les notifications partageant un tag se REMPLACENT au lieu de
      // s'empiler. Sans lui, dix messages reçus pendant une absence
      // produiraient dix bulles superposées.
      tag: 'carnet-message',
      icon: '/favicon.ico'
    });

    notification.onclick = () => {
      // Ramener la fenêtre au premier plan : cliquer sur une notification sans
      // que rien ne s'affiche serait déroutant.
      window.focus();
      this.router.navigate(['/messages']);
      notification.close();
    };
  }

  private ajouterBandeau(titre: string, corps: string): void {
    const id = this.prochainId++;

    this.bandeauxSignal.update(liste => [...liste, { id, titre, corps }]);

    // Disparition automatique : un bandeau informe, il n'a pas à rester à
    // l'écran jusqu'à ce qu'on le ferme.
    setTimeout(() => this.fermerBandeau(id), DUREE_BANDEAU_MS);
  }

  fermerBandeau(id: number): void {
    this.bandeauxSignal.update(liste => liste.filter(b => b.id !== id));
  }
}

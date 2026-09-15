import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth';
import { MessageService } from '../../services/message';
import { AbonnementService } from '../../services/abonnement';
import { EtatHttpService } from '../../services/etat-http';
import { Message } from '../../message.model';
import { Interlocuteur, StatutRelation } from '../../abonnement.model';
import { EMOJIS_REACTION } from '../../reaction.model';
import { BoutonSuivre } from '../../components/bouton-suivre/bouton-suivre';

/** Un jour de conversation, avec les messages qu'il contient. */
interface GroupeJour {
  cle: string;
  libelle: string;
  messages: Message[];
}

@Component({
  selector: 'app-messages',
  imports: [ReactiveFormsModule, DatePipe, RouterLink, BoutonSuivre],
  templateUrl: './messages.html',
  styleUrl: './messages.css'
})
export class Messages implements OnInit, OnDestroy {
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);
  private messageService = inject(MessageService);
  private abonnements = inject(AbonnementService);
  private etatHttp = inject(EtatHttpService);
  private route = inject(ActivatedRoute);

  chargement = this.etatHttp.chargement;
  fil = this.messageService.fil;
  moi = this.auth.utilisateur;

  // readonly + as const côté modèle : la liste est figée, le gabarit ne peut
  // que la parcourir.
  readonly emojis = EMOJIS_REACTION;

  // Les conversations possibles, chacune avec son droit d'écrire. Chargées par
  // ce composant, pas par un signal partagé : elles ne servent qu'ici.
  interlocuteurs = signal<Interlocuteur[]>([]);

  /**
   * L'IDENTIFIANT de la conversation ouverte, et non une copie de l'objet.
   *
   * Garder l'objet ferait deux sources de vérité : la liste, et la sélection.
   * Le jour où la liste change — on vient de suivre la personne, on peut
   * désormais lui écrire —, la copie resterait sur l'ancien `peutEcrire` et la
   * zone de saisie n'apparaîtrait pas. Avec un identifiant, `selection` est
   * RELUE dans la liste à chaque changement de l'une ou de l'autre.
   */
  private selectionId = signal<number | null>(null);

  selection = computed(() =>
    this.interlocuteurs().find(i => i.compte.id === this.selectionId()) ?? null
  );

  /**
   * L'id du message dont la palette de réaction est ouverte, ou null.
   *
   * Un seul signal pour toute la liste, et non un booléen par bulle : c'est ce
   * qui garantit qu'UNE SEULE palette est ouverte à la fois. Avec un état par
   * message, il faudrait penser à refermer les autres à chaque ouverture — et
   * l'oubli finit toujours par arriver.
   */
  paletteOuverte = signal<number | null>(null);

  // Nombre de non-lus par expéditeur, dérivé du signal du service : quand un
  // fil est marqué lu, ces pastilles se mettent à jour toutes seules.
  nonLusParExpediteur = computed(() => {
    const compte = new Map<number, number>();
    for (const message of this.messageService.nonLus()) {
      const id = message.expediteur.id;
      compte.set(id, (compte.get(id) ?? 0) + 1);
    }
    return compte;
  });

  /**
   * Le fil découpé en journées.
   *
   * Afficher la date complète sous chaque bulle serait illisible : dans une
   * conversation, l'heure suffit, et la date ne change qu'une fois par jour.
   * On regroupe donc par journée et on n'écrit la date qu'une fois, en
   * séparateur — c'est la convention de toutes les messageries, et elle tient
   * à une raison simple : l'information rare doit apparaître rarement.
   *
   * Un computed : le découpage se refait tout seul à chaque tour de sondage,
   * mais SEULEMENT si le fil a changé.
   */
  filParJour = computed<GroupeJour[]>(() => {
    const groupes: GroupeJour[] = [];

    for (const message of this.fil()) {
      const date = new Date(message.dateEnvoi);
      // toDateString() rabote l'heure : deux messages du même jour donnent la
      // même clé, quelle que soit la minute.
      const cle = date.toDateString();

      const dernier = groupes.at(-1);
      if (dernier?.cle === cle) {
        dernier.messages.push(message);
      } else {
        groupes.push({ cle, libelle: this.libelleJour(date), messages: [message] });
      }
    }

    return groupes;
  });

  formulaire = this.fb.group({
    contenu: ['', [Validators.required, Validators.maxLength(2000)]]
  });

  ngOnInit(): void {
    // Les statuts d'abonnement, pour le bouton « Suivre » affiché quand on ne
    // peut plus écrire à quelqu'un.
    this.abonnements.charger();

    this.messageService.interlocuteurs().subscribe({
      next: liste => {
        this.interlocuteurs.set(liste);

        /*
         * `?avec=` est lu dans le SNAPSHOT de la route, et non dans l'Observable
         * paramMap qu'utilise la page Personne.
         *
         * Le snapshot est une photo prise à l'arrivée sur la page ; l'Observable
         * prévient de chaque changement ultérieur. La page Personne en a besoin :
         * un lien peut mener d'une personne à une autre sans recréer le
         * composant. Ici, rien dans la messagerie ne modifie `?avec=` : une
         * seule lecture suffit. Elle attend la liste, car on n'ouvre que la
         * conversation d'une personne qui y figure.
         */
        const demande = Number(this.route.snapshot.queryParamMap.get('avec'));
        if (liste.some(i => i.compte.id === demande)) {
          this.ouvrir(demande);
        }
      },
      // La bannière vient de erreurInterceptor ; la liste reste vide.
      error: () => {}
    });
  }

  /**
   * ngOnDestroy : le pendant de ngOnInit, appelé quand Angular retire le
   * composant de l'écran. C'est l'endroit où rendre ce qu'on a emprunté —
   * ici, arrêter le sondage du fil. Sans lui, quitter la page Messages
   * laisserait une requête partir toutes les cinq secondes pour alimenter un
   * affichage que plus personne ne regarde.
   */
  ngOnDestroy(): void {
    this.messageService.arreterSuiviFil();
  }

  ouvrir(compteId: number): void {
    this.selectionId.set(compteId);
    this.paletteOuverte.set(null);
    this.messageService.suivreFil(compteId);
  }

  envoyer(): void {
    const conversation = this.selection();
    if (this.formulaire.invalid || !conversation?.peutEcrire) {
      return;
    }

    this.messageService.envoyer(conversation.compte.id, this.formulaire.value.contenu!).subscribe({
      // Le champ n'est vidé qu'une fois le message accepté : en cas de refus,
      // le texte reste là, prêt à être renvoyé ou copié ailleurs.
      next: () => this.formulaire.reset(),
      // La bannière vient de erreurInterceptor.
      error: () => {}
    });
  }

  /**
   * Après un clic sur « Suivre » depuis une conversation en lecture seule.
   *
   * Seul un abonnement ACCEPTÉ rend le droit d'écrire : une demande en attente
   * (compte privé) ne change rien tant qu'elle n'est pas acceptée. La liste est
   * mise à jour sur place plutôt que rechargée — `selection`, calculée depuis
   * elle, suit d'elle-même.
   */
  apresSuivi(compteId: number, statut: StatutRelation): void {
    if (statut !== 'ACCEPTE') {
      return;
    }

    this.interlocuteurs.update(liste =>
      liste.map(i => (i.compte.id === compteId ? { ...i, peutEcrire: true } : i))
    );
  }

  /** true si le message a été écrit par le compte connecté. */
  estDeMoi(expediteurId: number): boolean {
    return this.moi()?.id === expediteurId;
  }

  nonLusDe(id: number): number {
    return this.nonLusParExpediteur().get(id) ?? 0;
  }

  basculerPalette(messageId: number): void {
    this.paletteOuverte.update(ouvert => (ouvert === messageId ? null : messageId));
  }

  reagir(message: Message, emoji: string): void {
    this.messageService.reagir(message.id, emoji);
    // On referme dès le clic, sans attendre la réponse : la palette a fait son
    // travail, la laisser ouverte masquerait la réaction qu'elle vient de
    // poser.
    this.paletteOuverte.set(null);
  }

  /**
   * « Aujourd'hui » et « Hier » plutôt qu'une date, quand c'est possible.
   *
   * Une date n'est utile que pour se repérer dans le temps ; or « 11/09/2026 »
   * demande un calcul mental pour savoir si c'était ce matin. Les deux
   * journées les plus fréquentes méritent donc leur mot.
   */
  private libelleJour(date: Date): string {
    const aujourdhui = new Date();
    const hier = new Date(aujourdhui);
    hier.setDate(hier.getDate() - 1);

    if (date.toDateString() === aujourdhui.toDateString()) {
      return "Aujourd'hui";
    }
    if (date.toDateString() === hier.toDateString()) {
      return 'Hier';
    }

    // toLocaleDateString laisse le NAVIGATEUR formater selon la langue de
    // l'utilisateur : « lundi 8 septembre » en français, sans qu'on ait à
    // écrire le nom des mois nulle part.
    return date.toLocaleDateString('fr-FR', {
      weekday: 'long', day: 'numeric', month: 'long',
      // L'année seulement si ce n'est pas celle en cours : la répéter partout
      // n'apprend rien.
      year: date.getFullYear() === aujourdhui.getFullYear() ? undefined : 'numeric'
    });
  }
}

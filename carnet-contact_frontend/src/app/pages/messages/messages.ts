import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { AuthService } from '../../services/auth';
import { MessageService } from '../../services/message';
import { EtatHttpService } from '../../services/etat-http';
import { AuteurPublic } from '../../utilisateur.model';
import { Message } from '../../message.model';
import { EMOJIS_REACTION } from '../../reaction.model';

/** Un jour de conversation, avec les messages qu'il contient. */
interface GroupeJour {
  cle: string;
  libelle: string;
  messages: Message[];
}

@Component({
  selector: 'app-messages',
  imports: [ReactiveFormsModule, DatePipe],
  templateUrl: './messages.html',
  styleUrl: './messages.css'
})
export class Messages implements OnInit, OnDestroy {
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);
  private messageService = inject(MessageService);
  private etatHttp = inject(EtatHttpService);

  chargement = this.etatHttp.chargement;
  fil = this.messageService.fil;
  moi = this.auth.utilisateur;

  // readonly + as const côté modèle : la liste est figée, le gabarit ne peut
  // que la parcourir.
  readonly emojis = EMOJIS_REACTION;

  // La liste des autres comptes. Chargée par ce composant, pas par un signal
  // partagé : elle ne sert qu'ici.
  interlocuteurs = signal<AuteurPublic[]>([]);

  // Le destinataire sélectionné. null = aucun fil ouvert.
  selection = signal<AuteurPublic | null>(null);

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
    this.auth.autresUtilisateurs().subscribe(liste => this.interlocuteurs.set(liste));
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

  ouvrir(utilisateur: AuteurPublic): void {
    this.selection.set(utilisateur);
    this.paletteOuverte.set(null);
    this.messageService.suivreFil(utilisateur.id);
  }

  envoyer(): void {
    const destinataire = this.selection();
    if (this.formulaire.invalid || !destinataire) {
      return;
    }

    this.messageService.envoyer(destinataire.id, this.formulaire.value.contenu!);
    this.formulaire.reset();
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

import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { AuthService } from '../../services/auth';
import { MessageService } from '../../services/message';
import { EtatHttpService } from '../../services/etat-http';
import { Utilisateur } from '../../utilisateur.model';

@Component({
  selector: 'app-messages',
  imports: [ReactiveFormsModule, DatePipe],
  templateUrl: './messages.html',
  styleUrl: './messages.css'
})
export class Messages implements OnInit {
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);
  private messageService = inject(MessageService);
  private etatHttp = inject(EtatHttpService);

  chargement = this.etatHttp.chargement;
  fil = this.messageService.fil;
  moi = this.auth.utilisateur;

  // La liste des autres comptes. Chargée par ce composant, pas par un signal
  // partagé : elle ne sert qu'ici.
  interlocuteurs = signal<Utilisateur[]>([]);

  // Le destinataire sélectionné. null = aucun fil ouvert.
  selection = signal<Utilisateur | null>(null);

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

  formulaire = this.fb.group({
    contenu: ['', [Validators.required, Validators.maxLength(2000)]]
  });

  ngOnInit(): void {
    this.auth.autresUtilisateurs().subscribe(liste => this.interlocuteurs.set(liste));
    this.messageService.chargerNonLus();
  }

  ouvrir(utilisateur: Utilisateur): void {
    this.selection.set(utilisateur);
    this.messageService.chargerFil(utilisateur.id);
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
}

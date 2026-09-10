import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { EMPTY, catchError, of, tap } from 'rxjs';
import { Message } from '../message.model';

@Injectable({
  providedIn: 'root'
})
export class MessageService {
  private http = inject(HttpClient);
  private apiUrl = '/api/messages';

  // Le fil actuellement ouvert.
  private filSignal = signal<Message[]>([]);
  readonly fil = this.filSignal.asReadonly();

  // Les messages recus non lus, pour la pastille de la barre de navigation.
  private nonLusSignal = signal<Message[]>([]);
  readonly nonLus = this.nonLusSignal.asReadonly();

  /** Ouvre le fil avec un interlocuteur (et le marque lu cote serveur). */
  chargerFil(autreId: number): void {
    this.http.get<Message[]>(`${this.apiUrl}/${autreId}`).pipe(
      catchError(() => of([]))
    ).subscribe(messages => {
      this.filSignal.set(messages);
      // Le serveur vient de marquer ces messages comme lus : on rafraichit
      // la pastille pour qu'elle ne mente pas.
      this.chargerNonLus();
    });
  }

  envoyer(destinataireId: number, contenu: string): void {
    this.http.post<Message>(this.apiUrl, { destinataireId, contenu }).pipe(
      catchError(() => EMPTY)
    ).subscribe(message => {
      // On ajoute la reponse du serveur au fil : elle seule porte l'id et la
      // date d'envoi reels (meme raisonnement que pour addContact).
      this.filSignal.update(fil => [...fil, message]);
    });
  }

  chargerNonLus(): void {
    this.http.get<Message[]>(`${this.apiUrl}/non-lus`).pipe(
      catchError(() => of([]))
    ).subscribe(messages => this.nonLusSignal.set(messages));
  }

  viderFil(): void {
    this.filSignal.set([]);
  }
}

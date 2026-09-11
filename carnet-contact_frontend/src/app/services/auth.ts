import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { EMPTY, Observable, catchError, tap } from 'rxjs';
import { ReponseAuth, Utilisateur } from '../utilisateur.model';
import { SessionService } from './session';
import { contexte } from '../interceptors/http-contexte';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private http = inject(HttpClient);
  private session = inject(SessionService);

  private apiUrl = '/api/auth';

  // On expose les signaux de la session : les composants n'ont ainsi qu'un
  // seul service à injecter pour lire l'état ET agir dessus.
  readonly utilisateur = this.session.utilisateur;
  readonly connecte = this.session.connecte;
  readonly estAdmin = this.session.estAdmin;

  /**
   * Ces deux méthodes RETOURNENT l'Observable au lieu de s'y abonner
   * elles-mêmes, contrairement à ContactService. La différence : ici
   * l'appelant a besoin de savoir quand ça a réussi, pour naviguer vers
   * l'accueil ou afficher une erreur de formulaire.
   *
   * tap() : un opérateur qui observe le flux sans le modifier — parfait pour
   * un effet de bord (mémoriser la session) tout en laissant la réponse
   * continuer son chemin vers l'appelant.
   */
  inscription(email: string, motDePasse: string, nomAffichage: string): Observable<ReponseAuth> {
    return this.http.post<ReponseAuth>(`${this.apiUrl}/inscription`, {
      email, motDePasse, nomAffichage
    }).pipe(
      tap(reponse => this.session.ouvrir(
        reponse.jeton, reponse.jetonRafraichissement, reponse.utilisateur))
    );
  }

  connexion(email: string, motDePasse: string): Observable<ReponseAuth> {
    return this.http.post<ReponseAuth>(`${this.apiUrl}/connexion`, {
      email, motDePasse
    }).pipe(
      tap(reponse => this.session.ouvrir(
        reponse.jeton, reponse.jetonRafraichissement, reponse.utilisateur))
    );
  }

  /**
   * Se déconnecter demande désormais quelque chose au serveur : révoquer le
   * jeton de rafraîchissement, pour qu'il ne rouvre plus rien.
   *
   * L'ordre compte. On vide la session LOCALEMENT tout de suite, sans attendre
   * la réponse : l'interface doit réagir au clic, et une panne réseau ne doit
   * pas laisser l'utilisateur connecté malgré lui. L'appel serveur part en
   * parallèle, et son échec éventuel est ignoré — le pire cas est un jeton
   * révocable qui reste valide jusqu'à son expiration.
   */
  deconnexion(): void {
    const jetonRafraichissement = this.session.jetonRafraichissementActuel();
    this.session.vider();

    if (jetonRafraichissement) {
      this.http.post<void>(`${this.apiUrl}/deconnexion`, { jetonRafraichissement }).pipe(
        catchError(() => EMPTY)
      ).subscribe();
    }
  }

  /** Met à jour son propre profil (nom affiché, photo). */
  modifierProfil(nomAffichage: string, photoUrl: string): Observable<Utilisateur> {
    return this.http.put<Utilisateur>('/api/utilisateurs/moi',
      { nomAffichage, photoUrl },
      { context: contexte({ libelle: 'Impossible d\'enregistrer le profil' }) }
    ).pipe(
      tap(utilisateur => this.session.majUtilisateur(utilisateur))
    );
  }

  /** Les autres comptes, pour choisir un destinataire. */
  autresUtilisateurs(): Observable<Utilisateur[]> {
    return this.http.get<Utilisateur[]>('/api/utilisateurs', {
      context: contexte({ libelle: 'Impossible de charger la liste des comptes' })
    });
  }
}

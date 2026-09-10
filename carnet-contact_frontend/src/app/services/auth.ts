import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { ReponseAuth, Utilisateur } from '../utilisateur.model';
import { SessionService } from './session';

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
      tap(reponse => this.session.ouvrir(reponse.jeton, reponse.utilisateur))
    );
  }

  connexion(email: string, motDePasse: string): Observable<ReponseAuth> {
    return this.http.post<ReponseAuth>(`${this.apiUrl}/connexion`, {
      email, motDePasse
    }).pipe(
      tap(reponse => this.session.ouvrir(reponse.jeton, reponse.utilisateur))
    );
  }

  deconnexion(): void {
    // Rien à demander au serveur : il ne garde aucune session (stateless).
    // Se déconnecter, c'est simplement jeter le jeton.
    this.session.vider();
  }

  /** Met à jour son propre profil (nom affiché, photo). */
  modifierProfil(nomAffichage: string, photoUrl: string): Observable<Utilisateur> {
    return this.http.put<Utilisateur>('/api/utilisateurs/moi', { nomAffichage, photoUrl }).pipe(
      tap(utilisateur => this.session.majUtilisateur(utilisateur))
    );
  }

  /** Les autres comptes, pour choisir un destinataire. */
  autresUtilisateurs(): Observable<Utilisateur[]> {
    return this.http.get<Utilisateur[]>('/api/utilisateurs');
  }
}

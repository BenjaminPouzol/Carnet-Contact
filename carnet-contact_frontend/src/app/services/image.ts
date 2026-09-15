import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { contexte } from '../interceptors/http-contexte';

/**
 * Les mêmes limites que le serveur (spring.servlet.multipart.max-file-size et
 * FormatImage). Les vérifier ici est un CONFORT — réponse immédiate, pas de
 * 5 Mo envoyés pour rien —, pas une sécurité : le serveur revérifie tout.
 */
export const TAILLE_MAX_IMAGE = 5 * 1024 * 1024;
export const FORMATS_IMAGE: readonly string[] = ['image/jpeg', 'image/png', 'image/webp', 'image/gif'];

@Injectable({
  providedIn: 'root'
})
export class ImageService {
  private http = inject(HttpClient);

  /**
   * Envoie un fichier et rend l'adresse à laquelle le serveur l'expose.
   *
   * FormData : l'objet que le navigateur fournit pour construire un corps
   * « multipart », celui qu'enverrait un formulaire HTML contenant un fichier.
   * Il transporte des octets bruts, là où le JSON ne transporte que du texte.
   *
   * Aucun Content-Type n'est posé à la main, et c'est volontaire : le
   * navigateur doit l'écrire lui-même, car il y ajoute la « frontière »
   * (boundary) qui sépare les parties du corps. Sans elle, Spring ne saurait
   * pas où commence le fichier.
   */
  envoyer(fichier: File): Observable<string> {
    const donnees = new FormData();
    donnees.append('fichier', fichier);

    return this.http.post<{ url: string }>('/api/images', donnees, {
      context: contexte({ libelle: 'Impossible d\'envoyer l\'image' })
    }).pipe(
      // Le reste de l'application ne veut qu'une adresse, pas l'enveloppe JSON.
      map(reponse => reponse.url)
    );
  }
}

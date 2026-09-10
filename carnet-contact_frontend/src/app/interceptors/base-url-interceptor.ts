import { HttpInterceptorFn } from '@angular/common/http';

// Un seul endroit dans toute l'application connaît l'adresse du backend.
const BASE_URL = 'http://localhost:8080';

/**
 * Préfixe les URL relatives commençant par /api par l'adresse du backend.
 *
 * Les deux intercepteurs précédents observaient la requête sans y toucher.
 * Celui-ci fait ce pour quoi les intercepteurs sont surtout connus : MODIFIER
 * la requête au passage. C'est le mécanisme derrière l'ajout automatique d'un
 * jeton d'authentification sur chaque appel.
 */
export const baseUrlInterceptor: HttpInterceptorFn = (req, next) => {
  // Toute requête ne va pas forcément vers notre API (fichier local, service
  // externe, URL déjà absolue) : on ne réécrit que ce qu'on reconnaît.
  if (!req.url.startsWith('/api')) {
    return next(req);
  }

  // Un HttpRequest est IMMUABLE : `req.url = ...` est interdit. clone() rend
  // une copie avec les champs remplacés, et on transmet la copie. Cette
  // immuabilité est volontaire : elle garantit qu'un intercepteur ne peut pas
  // modifier une requête déjà partie ailleurs dans la chaîne.
  const requeteModifiee = req.clone({ url: BASE_URL + req.url });

  return next(requeteModifiee);
};

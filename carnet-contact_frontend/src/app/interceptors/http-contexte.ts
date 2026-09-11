import { HttpContext, HttpContextToken } from '@angular/common/http';

/**
 * Le CONTEXTE d'une requête HTTP : des informations que l'appelant attache à
 * sa requête, et que les intercepteurs peuvent lire.
 *
 * Le problème qu'il résout. En section 17, sortir la gestion d'erreur des
 * services a coûté quelque chose : l'intercepteur ne connaît que le code de
 * statut, pas l'intention. « Erreur interne du serveur (500) » a remplacé
 * « Impossible d'ajouter le contact ». On avait gagné en duplication, perdu en
 * clarté.
 *
 * La tentation serait de remettre un catchError métier dans chaque service —
 * et de recréer exactement la duplication qu'on venait de supprimer. Le
 * contexte offre la troisième voie : le service déclare UNE donnée (ce qu'il
 * était en train de faire), l'intercepteur garde toute la logique.
 *
 * Pourquoi pas un simple en-tête HTTP ? Parce qu'un en-tête part sur le
 * réseau : on enverrait au serveur du texte français qui ne le regarde pas. Le
 * contexte, lui, ne quitte jamais le navigateur — c'est un canal de
 * communication entre le code appelant et les intercepteurs, rien de plus.
 */

/**
 * Ce que la requête était en train de faire, du point de vue de l'utilisateur.
 *
 * La fonction passée à HttpContextToken donne la valeur PAR DÉFAUT, utilisée
 * quand personne n'a rien attaché. Le contexte n'est donc jamais « absent » :
 * il y a toujours une valeur à lire, ce qui évite les tests de nullité partout.
 */
export const LIBELLE_ACTION = new HttpContextToken<string | null>(() => null);

/**
 * Requête de fond : l'utilisateur ne l'a pas déclenchée et n'attend rien.
 *
 * Sans ce drapeau, le rafraîchissement automatique de la messagerie ferait
 * clignoter la bannière « Chargement… » toutes les dix secondes, et
 * afficherait « Serveur injoignable » à la moindre coupure passagère —
 * pour une requête que personne n'a demandée.
 */
export const DISCRET = new HttpContextToken<boolean>(() => false);

/**
 * Raccourci pour construire le contexte à passer à HttpClient.
 *
 * Sans lui, chaque appel s'écrirait
 * `{ context: new HttpContext().set(LIBELLE_ACTION, '…') }` — exact, mais
 * assez verbeux pour décourager de s'en servir.
 */
export function contexte(options: { libelle?: string; discret?: boolean } = {}): HttpContext {
  let resultat = new HttpContext();

  if (options.libelle !== undefined) {
    resultat = resultat.set(LIBELLE_ACTION, options.libelle);
  }
  if (options.discret !== undefined) {
    resultat = resultat.set(DISCRET, options.discret);
  }

  return resultat;
}

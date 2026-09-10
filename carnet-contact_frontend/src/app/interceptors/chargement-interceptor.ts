import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { finalize } from 'rxjs';
import { EtatHttpService } from '../services/etat-http';

/**
 * Compte les requêtes en vol, pour l'indicateur de chargement.
 *
 * Remplace le couple `chargementSignal.set(true)` / `finalize(...)` qui était
 * recopié dans chacune des quatre méthodes de ContactService. Ici il est
 * écrit UNE fois et s'applique à toute requête, y compris celles qui n'ont
 * pas encore été écrites.
 */
export const chargementInterceptor: HttpInterceptorFn = (req, next) => {
  // Un intercepteur s'exécute dans un contexte d'injection : inject() y
  // fonctionne exactement comme dans un composant ou un service.
  const etatHttp = inject(EtatHttpService);

  etatHttp.debutRequete();

  // next(req) transmet la requête au maillon suivant de la chaîne (autre
  // intercepteur, ou le réseau) et renvoie l'Observable de la réponse.
  // On peut donc le .pipe() comme n'importe quel flux.
  return next(req).pipe(
    // Même raisonnement que dans le service auparavant : finalize s'exécute
    // que la requête réussisse ou échoue, donc le compteur redescend
    // toujours. Il couvre en plus un cas de plus : le désabonnement
    // (requête annulée parce que le composant a été détruit).
    finalize(() => etatHttp.finRequete())
  );
};

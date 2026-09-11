import { FormControl } from '@angular/forms';
import { CRITERES_MOT_DE_PASSE, motDePasseSolide } from './mot-de-passe';

/**
 * Aucun TestBed ici : un validateur est une fonction, on l'appelle directement.
 * C'est le pendant côté Angular de `PolitiqueMotDePasseTest` en Java — et les
 * deux doivent rester d'accord, sinon le formulaire accepterait une saisie que
 * le serveur refuserait ensuite.
 */
describe('motDePasseSolide', () => {
  const valider = (valeur: string) => motDePasseSolide(new FormControl(valeur));

  it('accepte un mot de passe qui coche tous les critères', () => {
    // null = valide. La convention est contre-intuitive : l'objet renvoyé est
    // « la liste des erreurs », donc pas d'erreur signifie rien à renvoyer.
    expect(valider('MotDeP4sse!')).toBeNull();
  });

  it('laisse le champ vide à Validators.required', () => {
    // Un validateur qui se mêle des cas des autres produirait deux messages
    // pour une seule erreur.
    expect(valider('')).toBeNull();
  });

  it('nomme précisément les critères manquants', () => {
    const erreurs = valider('tellementlong');

    expect(erreurs?.['motDePasseFaible'].manquants)
      .toEqual(['majuscule', 'chiffre', 'special']);
  });

  it('refuse un mot de passe trop courant malgré sa forme correcte', () => {
    // 12 caractères, minuscule, majuscule, chiffre, caractère spécial : les
    // cinq critères de forme passent. C'est la limite que la liste comble.
    expect(valider('Motdepasse1!')).not.toBeNull();
  });

  it('refuse dès qu\'un seul critère manque', () => {
    expect(valider('Court1!')).not.toBeNull();       // trop court
    expect(valider('motdep4sse!')).not.toBeNull();   // pas de majuscule
    expect(valider('MOTDEP4SSE!')).not.toBeNull();   // pas de minuscule
    expect(valider('MotDePasse!')).not.toBeNull();   // pas de chiffre
    expect(valider('MotDeP4sse')).not.toBeNull();    // pas de caractère spécial
  });

  /**
   * Le gabarit affiche la liste à cocher en appelant `verifie` de chaque
   * critère, et le bouton est désactivé par le validateur. Si les deux
   * pouvaient diverger, on afficherait six coches vertes au-dessus d'un bouton
   * grisé — sans aucune explication pour l'utilisateur.
   */
  it('la liste à cocher et le validateur donnent le même verdict', () => {
    const candidats = ['MotDeP4sse!', 'court', 'tellementlong', 'Motdepasse1!'];

    for (const candidat of candidats) {
      const tousLesCriteresPassent =
        CRITERES_MOT_DE_PASSE.every(critere => critere.verifie(candidat));

      expect(valider(candidat) === null).toBe(tousLesCriteresPassent);
    }
  });
});

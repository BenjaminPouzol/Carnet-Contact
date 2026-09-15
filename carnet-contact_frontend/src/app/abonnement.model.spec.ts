import { texteNotification } from './abonnement.model';
import { uneNotification } from './donnees-test';

/**
 * Le serveur n'envoie qu'un TYPE et un acteur : la phrase affichée est composée
 * ici. Un nom changé entre-temps apparaît ainsi à jour, et la formulation reste
 * l'affaire de l'interface.
 */
describe('texteNotification', () => {
  it('annonce un nouvel abonné', () => {
    expect(texteNotification(uneNotification({ type: 'NOUVEL_ABONNE' }))).toBe('Alice vous suit');
  });

  it('annonce une demande reçue', () => {
    expect(texteNotification(uneNotification({ type: 'DEMANDE_RECUE' })))
      .toBe('Alice demande à vous suivre');
  });

  it('annonce une demande acceptée', () => {
    expect(texteNotification(uneNotification({ type: 'DEMANDE_ACCEPTEE' })))
      .toBe('Alice a accepté votre demande');
  });
});

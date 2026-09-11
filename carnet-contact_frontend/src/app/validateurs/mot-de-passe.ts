import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

/**
 * La politique de mot de passe, côté navigateur.
 *
 * Elle double celle du serveur ([`PolitiqueMotDePasse.java`]), et c'est
 * volontaire : le serveur DÉCIDE, le navigateur RENSEIGNE. Sans la version
 * client, l'utilisateur découvrirait son erreur seulement après avoir cliqué
 * sur « Créer mon compte » ; sans celle du serveur, il suffirait d'envoyer une
 * requête directement à l'API pour la contourner. Les deux sont nécessaires, et
 * pour des raisons différentes.
 *
 * Les critères sont décrits UNE fois dans ce tableau — même principe que la
 * constante RESEAUX (section 20). Il sert à la fois à valider et à afficher la
 * liste à cocher : ajouter un sixième critère ne demande qu'une ligne ici, et
 * l'affichage suit tout seul.
 */
export interface CritereMotDePasse {
  cle: string;
  libelle: string;
  verifie: (valeur: string) => boolean;
}

export const LONGUEUR_MINIMALE = 10;

/**
 * Quelques mots de passe parmi les plus utilisés. Liste volontairement
 * minuscule : elle illustre la limite des règles de forme plus qu'elle ne
 * protège — « Motdepasse1! » coche tous les critères et reste mauvais.
 */
const TROP_COURANTS = [
  'motdepasse', 'password', 'azertyuiop', 'qwertyuiop',
  '123456789', '1234567890', 'motdepasse1', 'password1',
  'azerty123', 'qwerty123', 'administrateur', 'bonjour123',
  // Ceux-ci cochent pourtant les cinq critères de forme : c'est exactement
  // pour eux que la liste existe.
  'motdepasse1!', 'password1!', 'azerty123!', 'qwerty123!',
  'bonjour123!', 'motdepasse2026!'
];

export const CRITERES_MOT_DE_PASSE: CritereMotDePasse[] = [
  {
    cle: 'longueur',
    libelle: `${LONGUEUR_MINIMALE} caractères minimum`,
    verifie: v => v.length >= LONGUEUR_MINIMALE
  },
  {
    cle: 'minuscule',
    libelle: 'Une minuscule',
    verifie: v => /[a-z]/.test(v)
  },
  {
    cle: 'majuscule',
    libelle: 'Une majuscule',
    verifie: v => /[A-Z]/.test(v)
  },
  {
    cle: 'chiffre',
    libelle: 'Un chiffre',
    verifie: v => /[0-9]/.test(v)
  },
  {
    // « Ni lettre ni chiffre » plutôt qu'une liste de symboles admis : une
    // liste oublierait toujours un caractère, et refuser « £ » serait absurde.
    cle: 'special',
    libelle: 'Un caractère spécial',
    verifie: v => /[^a-zA-Z0-9]/.test(v)
  },
  {
    cle: 'peuCourant',
    libelle: 'Pas un mot de passe trop courant',
    verifie: v => v !== '' && !TROP_COURANTS.includes(v.toLowerCase())
  }
];

/**
 * Un VALIDATEUR PERSONNALISÉ.
 *
 * Les `Validators.required` et `Validators.email` de la section 7 sont des
 * fonctions de cette forme, fournies par Angular. Rien n'empêche d'écrire les
 * siennes : un validateur est simplement une fonction qui reçoit le contrôle et
 * rend soit `null` (« tout va bien »), soit un objet décrivant l'erreur.
 *
 * La convention est contre-intuitive au début : `null` signifie VALIDE. Elle se
 * comprend en lisant l'objet renvoyé comme « la liste des erreurs » — pas
 * d'erreur, donc rien à renvoyer.
 *
 * On glisse dans cet objet la liste des critères non satisfaits, que le gabarit
 * pourra lire via `control.errors`. Un validateur n'est pas obligé de se limiter
 * à un drapeau : il peut transporter de quoi construire le message.
 */
export const motDePasseSolide: ValidatorFn = (
  control: AbstractControl
): ValidationErrors | null => {
  const valeur: string = control.value ?? '';

  // Champ vide : c'est le rôle de Validators.required de le signaler. Un
  // validateur qui se mêle des cas des autres produit deux messages pour une
  // seule erreur.
  if (valeur === '') {
    return null;
  }

  const manquants = CRITERES_MOT_DE_PASSE
    .filter(critere => !critere.verifie(valeur))
    .map(critere => critere.cle);

  return manquants.length === 0 ? null : { motDePasseFaible: { manquants } };
};

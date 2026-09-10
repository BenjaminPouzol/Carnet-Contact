import { Component, computed, input } from '@angular/core';
import { Contact, RESEAUX } from '../../contact.model';

interface LienReseau {
  cle: string;
  nom: string;
  couleur: string;
  url: string;
}

/**
 * Affiche les liens vers les réseaux sociaux d'un contact, chacun avec son
 * logo, en n'affichant que ceux qui sont renseignés.
 *
 * input() : la façon moderne de déclarer une entrée de composant (l'inverse
 * de output(), vu en section 8). Un input() est un SIGNAL en lecture seule :
 * il se lit avec des parenthèses, et tout computed() qui en dépend se
 * recalcule quand le parent passe une nouvelle valeur.
 */
@Component({
  selector: 'app-reseaux-sociaux',
  imports: [],
  templateUrl: './reseaux-sociaux.html',
  styleUrl: './reseaux-sociaux.css'
})
export class ReseauxSociaux {
  // .required : le composant ne peut pas être utilisé sans lui fournir un
  // contact, et TypeScript le sait (pas de « | undefined » à gérer).
  contact = input.required<Contact>();

  /**
   * La liste des réseaux effectivement renseignés, construite à partir du
   * tableau RESEAUX du modèle. On ne répète donc pas six fois le même bloc
   * dans le gabarit : on en écrit un seul, parcouru par @for.
   */
  liens = computed<LienReseau[]>(() => {
    const c = this.contact();

    return RESEAUX
      .map(reseau => ({
        cle: reseau.cle,
        nom: reseau.nom,
        couleur: reseau.couleur,
        url: this.versUrl(c[reseau.cle], reseau.cle)
      }))
      // On écarte les réseaux vides : le gabarit n'a plus de @if à faire.
      .filter(lien => lien.url !== '');
  });

  /**
   * L'utilisateur peut coller une URL complète ou juste un pseudo. On
   * accepte les deux et on reconstruit une adresse cliquable, sinon un
   * pseudo seul donnerait un lien relatif cassé.
   */
  private versUrl(valeur: string | null | undefined, cle: string): string {
    if (!valeur || valeur.trim() === '') {
      return '';
    }

    const propre = valeur.trim();
    if (propre.startsWith('http://') || propre.startsWith('https://')) {
      return propre;
    }

    const pseudo = propre.replace(/^@/, '');
    const bases: Record<string, string> = {
      instagram: 'https://instagram.com/',
      twitter: 'https://x.com/',
      facebook: 'https://facebook.com/',
      twitch: 'https://twitch.tv/',
      youtube: 'https://youtube.com/@',
      linkedin: 'https://linkedin.com/in/'
    };

    return bases[cle] + pseudo;
  }
}

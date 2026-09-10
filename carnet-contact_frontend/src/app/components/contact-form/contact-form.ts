import { Component, inject, output, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { EtatHttpService } from '../../services/etat-http';
import { Contact, RESEAUX } from '../../contact.model';

@Component({
  selector: 'app-contact-form',
  imports: [ReactiveFormsModule],
  templateUrl: './contact-form.html',
  styleUrl: './contact-form.css'
})
export class ContactForm {
  private fb = inject(FormBuilder);
  private etatHttp = inject(EtatHttpService);

  chargement = this.etatHttp.chargement;

  // Exposé au gabarit pour générer les six champs par une boucle @for.
  reseaux = RESEAUX;

  // Le formulaire compte maintenant 13 champs : les afficher tous d'un bloc
  // serait décourageant. On replie l'optionnel derrière un bouton.
  detailsOuverts = signal(false);

  contactAjoute = output<Contact>();

  contactForm = this.fb.group({
    nom: ['', Validators.required],
    prenom: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    telephone: [''],
    // Validators.email sans required : le champ reste facultatif, mais s'il
    // est rempli, il doit ressembler à une adresse.
    emailPro: ['', Validators.email],
    photoUrl: [''],
    instagram: [''],
    twitter: [''],
    facebook: [''],
    twitch: [''],
    youtube: [''],
    linkedin: ['']
  });

  basculerDetails(): void {
    this.detailsOuverts.update(ouvert => !ouvert);
  }

  onSubmit(): void {
    if (this.contactForm.valid) {
      this.contactAjoute.emit(this.contactForm.value as Contact);
      this.contactForm.reset();
      this.detailsOuverts.set(false);
    }
  }
}

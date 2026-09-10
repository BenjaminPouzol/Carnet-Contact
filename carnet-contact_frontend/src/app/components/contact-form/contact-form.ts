import { Component, inject, output } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { EtatHttpService } from '../../services/etat-http';
import { Contact } from '../../contact.model';

@Component({
  selector: 'app-contact-form',
  imports: [ReactiveFormsModule],
  templateUrl: './contact-form.html',
  styleUrl: './contact-form.css'
})
export class ContactForm {
  private fb = inject(FormBuilder);
  private etatHttp = inject(EtatHttpService);

  // Référence vers le signal du service transverse : true tant qu'au moins
  // une requête HTTP est en vol (alimenté par chargementInterceptor).
  chargement = this.etatHttp.chargement;

  contactAjoute = output<Contact>();

  contactForm = this.fb.group({
    nom: ['', Validators.required],
    prenom: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    telephone: ['']
  });

  onSubmit(): void {
    if (this.contactForm.valid) {
      this.contactAjoute.emit(this.contactForm.value as Contact);
      this.contactForm.reset();
    }
  }
}

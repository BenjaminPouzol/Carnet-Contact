import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { Component, OnInit, effect, inject } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ContactService } from '../../services/contact';
import { EtatHttpService } from '../../services/etat-http';
import { Contact, RESEAUX } from '../../contact.model';

@Component({
  selector: 'app-contact-edit',
  imports: [ReactiveFormsModule, RouterLink, ButtonModule, InputTextModule],
  templateUrl: './contact-edit.html',
  styleUrl: './contact-edit.css'
})
export class ContactEdit implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private fb = inject(FormBuilder);
  private contactService = inject(ContactService);
  private etatHttp = inject(EtatHttpService);

  private id = Number(this.route.snapshot.paramMap.get('id'));

  reseaux = RESEAUX;

  // Le contact à modifier, chargé seul (voir le commentaire de ContactDetail).
  contact = this.contactService.contactCourant;

  // true pendant une requête HTTP : sert à désactiver le bouton Enregistrer.
  chargement = this.etatHttp.chargement;

  contactForm = this.fb.group({
    nom: ['', Validators.required],
    prenom: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    telephone: [''],
    emailPro: ['', Validators.email],
    photoUrl: [''],
    instagram: [''],
    twitter: [''],
    facebook: [''],
    twitch: [''],
    youtube: [''],
    linkedin: ['']
  });

  private formulaireRempli = false;

  constructor() {
    // effect() exécute du code À CHAQUE FOIS qu'un signal qu'il lit change.
    // C'est le pendant "effet de bord" de computed() (qui, lui, calcule une
    // valeur). Ici : dès que contact() cesse d'être undefined (données
    // arrivées du serveur), on pré-remplit le formulaire — une seule fois,
    // sinon un rechargement de la liste écraserait les saisies en cours.
    effect(() => {
      const c = this.contact();
      if (c && !this.formulaireRempli) {
        // Les champs optionnels valent null côté serveur, alors qu'un champ
        // de formulaire attend une chaîne : on convertit, sinon Angular
        // afficherait littéralement "null" dans les cases vides.
        this.contactForm.patchValue({
          nom: c.nom,
          prenom: c.prenom,
          email: c.email,
          telephone: c.telephone ?? '',
          emailPro: c.emailPro ?? '',
          photoUrl: c.photoUrl ?? '',
          instagram: c.instagram ?? '',
          twitter: c.twitter ?? '',
          facebook: c.facebook ?? '',
          twitch: c.twitch ?? '',
          youtube: c.youtube ?? '',
          linkedin: c.linkedin ?? ''
        });
        this.formulaireRempli = true;
      }
    });
  }

  ngOnInit(): void {
    this.contactService.chargerContact(this.id);
  }

  onSubmit(): void {
    if (this.contactForm.invalid) return;

    this.contactService.modifierContact({
      id: this.id,
      ...this.contactForm.value
    } as Contact);

    // Navigation déclenchée par du code (pas par un clic) : Router.navigate()
    // prend le même tableau de segments que [routerLink].
    this.router.navigate(['/contact', this.id]);
  }
}

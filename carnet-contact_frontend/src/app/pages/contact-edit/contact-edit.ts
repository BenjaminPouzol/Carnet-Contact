import { Component, OnInit, computed, effect, inject } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ContactService } from '../../services/contact';
import { Contact } from '../../contact.model';

@Component({
  selector: 'app-contact-edit',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './contact-edit.html',
  styleUrl: './contact-edit.css'
})
export class ContactEdit implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private fb = inject(FormBuilder);
  private contactService = inject(ContactService);

  private id = Number(this.route.snapshot.paramMap.get('id'));

  // Le contact à modifier, retrouvé dans le signal partagé.
  contact = computed(() =>
    this.contactService.contacts().find(c => c.id === this.id)
  );

  // true pendant une requête HTTP : sert à désactiver le bouton Enregistrer.
  chargement = this.contactService.chargement;

  contactForm = this.fb.group({
    nom: ['', Validators.required],
    prenom: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    telephone: ['']
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
        this.contactForm.patchValue(c);
        this.formulaireRempli = true;
      }
    });
  }

  ngOnInit(): void {
    // Accès direct à l'URL /contact/:id/modifier : le signal serait vide.
    this.contactService.chargerContacts();
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

package com.example.carnet_contact_backend.image;

import com.example.carnet_contact_backend.model.Categorie;
import com.example.carnet_contact_backend.model.Contact;
import com.example.carnet_contact_backend.model.Image;
import com.example.carnet_contact_backend.model.Publication;
import com.example.carnet_contact_backend.model.Role;
import com.example.carnet_contact_backend.model.Utilisateur;
import com.example.carnet_contact_backend.repository.ContactRepository;
import com.example.carnet_contact_backend.repository.ImageRepository;
import com.example.carnet_contact_backend.repository.PublicationRepository;
import com.example.carnet_contact_backend.repository.UtilisateurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Le nettoyage des images qui ne servent plus.
 *
 * On appelle directement `nettoyer(limite)`, jamais la méthode planifiée : la
 * limite passée en paramètre permet de simuler « il y a une heure » sans
 * attendre une heure — le même principe que le jeton déjà expiré de
 * JwtServiceTest.
 */
@SpringBootTest
@Transactional
class NettoyageImagesTest {

    private static final Instant MAINTENANT = Instant.parse("2026-09-15T12:00:00Z");
    private static final Instant LIMITE = MAINTENANT.minus(Duration.ofHours(1));

    @Autowired
    private NettoyageImages nettoyage;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private ContactRepository contactRepository;

    @Autowired
    private PublicationRepository publicationRepository;

    private Utilisateur alice;

    @BeforeEach
    void preparer() {
        alice = new Utilisateur();
        alice.setEmail("alice@exemple.fr");
        alice.setMotDePasse("peu-importe");
        alice.setNomAffichage("Alice");
        alice.setRole(Role.UTILISATEUR);
        alice.setActif(true);
        alice.setDateInscription(MAINTENANT);
        alice = utilisateurRepository.save(alice);
    }

    private Image image(Instant dateEnvoi) {
        Image image = new Image();
        image.setDonnees(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});
        image.setTypeContenu("image/jpeg");
        image.setTaille(3);
        image.setDateEnvoi(dateEnvoi);
        image.setProprietaire(alice);
        return imageRepository.save(image);
    }

    private static String adresse(Image image) {
        return "http://localhost:8080/api/images/" + image.getId();
    }

    @Test
    @DisplayName("Une image ancienne que rien ne référence est supprimée")
    void ancienneNonReferencee_estSupprimee() {
        Image orpheline = image(MAINTENANT.minus(Duration.ofHours(2)));

        assertThat(nettoyage.nettoyer(LIMITE)).isEqualTo(1);
        assertThat(imageRepository.findById(orpheline.getId())).isEmpty();
    }

    /**
     * Le délai de grâce. Une image envoyée il y a dix minutes n'est référencée
     * par rien — mais le formulaire qui l'a envoyée n'est peut-être pas encore
     * enregistré. La supprimer effacerait l'aperçu sous les yeux de l'utilisateur.
     */
    @Test
    @DisplayName("Une image récente non référencée est gardée")
    void recenteNonReferencee_estGardee() {
        Image recente = image(MAINTENANT.minus(Duration.ofMinutes(10)));

        assertThat(nettoyage.nettoyer(LIMITE)).isZero();
        assertThat(imageRepository.findById(recente.getId())).isPresent();
    }

    @Test
    @DisplayName("Une image utilisée comme photo de contact est gardée")
    void referenceeParUnContact_estGardee() {
        Image photo = image(MAINTENANT.minus(Duration.ofDays(1)));

        Contact contact = new Contact();
        contact.setNom("Dupont");
        contact.setPrenom("Jean");
        contact.setEmail("jean@exemple.fr");
        contact.setPhotoUrl(adresse(photo));
        contact.setProprietaire(alice);
        contactRepository.save(contact);

        assertThat(nettoyage.nettoyer(LIMITE)).isZero();
        assertThat(imageRepository.findById(photo.getId())).isPresent();
    }

    @Test
    @DisplayName("Une image utilisée comme photo de profil est gardée")
    void referenceeParUnProfil_estGardee() {
        Image photo = image(MAINTENANT.minus(Duration.ofDays(1)));
        alice.setPhotoUrl(adresse(photo));
        utilisateurRepository.save(alice);

        assertThat(nettoyage.nettoyer(LIMITE)).isZero();
        assertThat(imageRepository.findById(photo.getId())).isPresent();
    }

    @Test
    @DisplayName("Une image utilisée dans une publication est gardée")
    void referenceeParUnePublication_estGardee() {
        Image illustration = image(MAINTENANT.minus(Duration.ofDays(1)));

        Publication publication = new Publication();
        publication.setAuteur(alice);
        publication.setCategorie(Categorie.SPORT);
        publication.setContenu("Sortie vélo");
        publication.setImageUrl(adresse(illustration));
        publication.setDatePublication(MAINTENANT);
        publicationRepository.save(publication);

        assertThat(nettoyage.nettoyer(LIMITE)).isZero();
        assertThat(imageRepository.findById(illustration.getId())).isPresent();
    }
}

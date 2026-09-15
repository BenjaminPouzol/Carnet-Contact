package com.example.carnet_contact_backend.controller;

import com.example.carnet_contact_backend.image.FormatImage;
import com.example.carnet_contact_backend.model.Image;
import com.example.carnet_contact_backend.model.Utilisateur;
import com.example.carnet_contact_backend.repository.ImageRepository;
import com.example.carnet_contact_backend.repository.UtilisateurRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;

/**
 * Envoi et lecture des images.
 *
 * Une image envoyée devient une ADRESSE, que le client range dans le champ URL
 * qui existait déjà (photo de profil, photo de contact, image de publication).
 * Le reste de l'application ne fait donc aucune différence entre une image
 * envoyée ici et une image hébergée ailleurs.
 */
@RestController
@RequestMapping("/api/images")
public class ImageController {

    private final ImageRepository imageRepository;
    private final UtilisateurRepository utilisateurRepository;

    public ImageController(ImageRepository imageRepository, UtilisateurRepository utilisateurRepository) {
        this.imageRepository = imageRepository;
        this.utilisateurRepository = utilisateurRepository;
    }

    public record ImageEnvoyee(String url) {}

    private Utilisateur utilisateurConnecte(String email) {
        return utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    /**
     * consumes = multipart/form-data : la route n'accepte que le format d'envoi
     * de fichiers. Le corps y est découpé en « parties » séparées par une
     * frontière ; chacune peut contenir des octets bruts, là où le JSON ne
     * transporte que du texte.
     *
     * MultipartFile : la partie « fichier », que Spring a déjà extraite du corps.
     * Une partie absente donne un 400 avant même d'entrer ici ; un fichier trop
     * gros (spring.servlet.multipart.max-file-size) aussi, en 413.
     *
     * 201 Created plutôt que 200 : une ressource nouvelle existe, et l'en-tête
     * Location donne son adresse — la même que celle du corps.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImageEnvoyee> envoyer(
            @RequestParam("fichier") MultipartFile fichier,
            @AuthenticationPrincipal String email) throws IOException {
        Utilisateur moi = utilisateurConnecte(email);

        if (fichier.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le fichier est vide.");
        }

        byte[] donnees = fichier.getBytes();

        // 415 Unsupported Media Type : le code prévu pour « je ne sais pas
        // traiter ce type de contenu ». On ignore volontairement
        // fichier.getContentType(), que le client choisit librement.
        String type = FormatImage.typeDe(donnees)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Formats acceptés : JPEG, PNG, WebP, GIF."));

        Image image = new Image();
        image.setDonnees(donnees);
        image.setTypeContenu(type);
        image.setTaille(donnees.length);
        image.setDateEnvoi(Instant.now());
        image.setProprietaire(moi);
        imageRepository.save(image);

        // Une adresse ABSOLUE (http://hote:port/api/images/…). Une adresse
        // relative serait résolue par le navigateur contre le serveur Angular,
        // pas contre ce backend : une balise <img> ne passe pas par
        // l'intercepteur qui préfixe les appels HttpClient.
        //
        // fromCurrentContextPath() la construit à partir de la requête reçue :
        // l'adresse du serveur n'est écrite nulle part en dur.
        URI adresse = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/images/{id}")
                .buildAndExpand(image.getId())
                .toUri();

        return ResponseEntity.created(adresse).body(new ImageEnvoyee(adresse.toString()));
    }

    /**
     * Lecture PUBLIQUE (voir SecurityConfig) : une balise <img> n'envoie jamais
     * l'en-tête Authorization.
     *
     * ResponseEntity<byte[]> : le corps n'est pas du JSON mais les octets bruts,
     * accompagnés du Content-Type qui dit au navigateur comment les interpréter.
     *
     * Cache-Control « immutable » : le contenu d'un identifiant ne change jamais
     * (une nouvelle image a un nouvel UUID). Le navigateur peut garder sa copie
     * un an sans jamais redemander.
     */
    @GetMapping("/{id}")
    public ResponseEntity<byte[]> lire(@PathVariable String id) {
        Image image = imageRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.getTypeContenu()))
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000, immutable")
                .body(image.getDonnees());
    }
}

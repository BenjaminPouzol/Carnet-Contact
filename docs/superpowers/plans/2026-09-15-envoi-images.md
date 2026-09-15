# Envoi d'images — plan d'implémentation

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Envoyer une image depuis son appareil (profil, contact, publication), en plus de la saisie d'URL, avec nettoyage automatique des images inutilisées.

**Architecture:** `POST /api/images` (multipart) stocke les octets en base et renvoie une URL absolue ; `GET /api/images/{id}` est public. Le frontend place cette URL dans le champ existant via un composant `champ-image`. Une tâche `@Scheduled` supprime les images non référencées envoyées depuis plus d'une heure.

**Tech Stack:** Spring Boot 4.1.1 (Java 21, JPA, H2, Spring Security), Angular 21 (zoneless, signals, formulaires réactifs, PrimeNG 21), Vitest.

**Spec:** `docs/superpowers/specs/2026-09-15-envoi-images-design.md`

## Global Constraints

- Taille max : 5 Mo (`5 * 1024 * 1024` octets côté client, `5MB` côté Spring).
- Formats : `image/jpeg`, `image/png`, `image/webp`, `image/gif` — déterminés par la signature des octets côté serveur. SVG refusé.
- Délai de grâce 1 h (`carnet.images.delai-grace-ms=3600000`), intervalle 30 min (`carnet.images.nettoyage-intervalle-ms=1800000`).
- Aucune entité existante (`Contact`, `Utilisateur`, `Publication`) ne change de colonnes.
- Commentaires pédagogiques en français, sur le « pourquoi ».
- Backend : commandes lancées depuis `carnet-contact-backend/` (`.\mvnw.cmd`). Frontend : depuis `carnet-contact_frontend/` (`npx ng …`).
- Messages de commit terminés par `Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>`.

---

### Task 1: Signature des fichiers image (`FormatImage`)

**Files:**
- Create: `carnet-contact-backend/src/main/java/com/example/carnet_contact_backend/image/FormatImage.java`
- Test: `carnet-contact-backend/src/test/java/com/example/carnet_contact_backend/image/FormatImageTest.java`

**Interfaces:**
- Produces: `static Optional<String> FormatImage.typeDe(byte[] contenu)` — type MIME ou vide.

- [ ] **Step 1: Write the failing test**

```java
package com.example.carnet_contact_backend.image;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.assertj.core.api.Assertions.assertThat;

class FormatImageTest {

    static Stream<Arguments> signaturesReconnues() {
        return Stream.of(
                Arguments.of(octets(0xFF, 0xD8, 0xFF, 0xE0, 0x00, 0x10), "image/jpeg"),
                Arguments.of(octets(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00), "image/png"),
                Arguments.of("GIF87a-----".getBytes(US_ASCII), "image/gif"),
                Arguments.of("GIF89a-----".getBytes(US_ASCII), "image/gif"),
                Arguments.of("RIFF\0\0\0\0WEBPVP8 ".getBytes(US_ASCII), "image/webp"));
    }

    @ParameterizedTest
    @MethodSource("signaturesReconnues")
    @DisplayName("Reconnaît les quatre formats par leurs premiers octets")
    void reconnaitLesSignatures(byte[] contenu, String typeAttendu) {
        assertThat(FormatImage.typeDe(contenu)).contains(typeAttendu);
    }

    @Test
    @DisplayName("Un texte renommé en .jpg reste un texte")
    void refuseUnTexteRenomme() {
        assertThat(FormatImage.typeDe("bonjour".getBytes(US_ASCII))).isEmpty();
    }

    @Test
    @DisplayName("Un SVG est refusé : c'est du texte qui peut porter du JavaScript")
    void refuseUnSvg() {
        byte[] svg = "<svg xmlns=\"http://www.w3.org/2000/svg\"><script>alert(1)</script></svg>".getBytes(US_ASCII);
        assertThat(FormatImage.typeDe(svg)).isEmpty();
    }

    @Test
    @DisplayName("Un RIFF qui n'est pas un WebP (un WAV) est refusé")
    void refuseUnRiffAudio() {
        assertThat(FormatImage.typeDe("RIFF\0\0\0\0WAVEfmt ".getBytes(US_ASCII))).isEmpty();
    }

    @Test
    @DisplayName("Un contenu trop court ou vide est refusé")
    void refuseUnContenuTropCourt() {
        assertThat(FormatImage.typeDe(octets(0xFF, 0xD8))).isEmpty();
        assertThat(FormatImage.typeDe(new byte[0])).isEmpty();
    }

    private static byte[] octets(int... valeurs) {
        byte[] resultat = new byte[valeurs.length];
        for (int i = 0; i < valeurs.length; i++) {
            resultat[i] = (byte) valeurs[i];
        }
        return resultat;
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\mvnw.cmd test -Dtest=FormatImageTest`
Expected: échec de compilation, `FormatImage` introuvable.

- [ ] **Step 3: Write minimal implementation**

```java
package com.example.carnet_contact_backend.image;

import java.util.Optional;

/**
 * Reconnaît le format d'une image à ses premiers octets, sa « signature ».
 *
 * Ni le nom du fichier ni le Content-Type annoncé par le navigateur ne font
 * foi : les deux se choisissent librement côté client. Les octets, eux, sont
 * ce que le navigateur interprétera réellement à l'affichage.
 */
public final class FormatImage {

    private FormatImage() {
    }

    public static Optional<String> typeDe(byte[] contenu) {
        if (contenu == null) {
            return Optional.empty();
        }
        if (commencePar(contenu, 0xFF, 0xD8, 0xFF)) {
            return Optional.of("image/jpeg");
        }
        if (commencePar(contenu, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)) {
            return Optional.of("image/png");
        }
        if (commencePar(contenu, 'G', 'I', 'F', '8', '7', 'a') || commencePar(contenu, 'G', 'I', 'F', '8', '9', 'a')) {
            return Optional.of("image/gif");
        }
        // RIFF est un conteneur générique (WAV, AVI…) : seul « WEBP » à l'octet 8
        // distingue une image.
        if (commencePar(contenu, 'R', 'I', 'F', 'F') && contenu.length >= 12
                && contenu[8] == 'W' && contenu[9] == 'E' && contenu[10] == 'B' && contenu[11] == 'P') {
            return Optional.of("image/webp");
        }
        return Optional.empty();
    }

    private static boolean commencePar(byte[] contenu, int... signature) {
        if (contenu.length < signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            // & 0xFF : un byte Java est signé (0xFF vaut -1), l'int attendu non.
            if ((contenu[i] & 0xFF) != signature[i]) {
                return false;
            }
        }
        return true;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\mvnw.cmd test -Dtest=FormatImageTest`
Expected: 9 tests, 0 échec.

- [ ] **Step 5: Commit**

```bash
git add carnet-contact-backend/src/main/java/com/example/carnet_contact_backend/image/FormatImage.java carnet-contact-backend/src/test/java/com/example/carnet_contact_backend/image/FormatImageTest.java
git commit -m "Images : reconnaissance du format par la signature des octets"
```

---

### Task 2: Entité, envoi et lecture publique

**Files:**
- Create: `carnet-contact-backend/src/main/java/com/example/carnet_contact_backend/model/Image.java`
- Create: `carnet-contact-backend/src/main/java/com/example/carnet_contact_backend/repository/ImageRepository.java`
- Create: `carnet-contact-backend/src/main/java/com/example/carnet_contact_backend/controller/ImageController.java`
- Modify: `carnet-contact-backend/src/main/java/com/example/carnet_contact_backend/security/SecurityConfig.java` (après la règle `/h2-console/**`)
- Modify: `carnet-contact-backend/src/main/resources/application.properties` (fin de fichier)
- Test: `carnet-contact-backend/src/test/java/com/example/carnet_contact_backend/controller/ImageControllerTest.java`

**Interfaces:**
- Consumes: `FormatImage.typeDe(byte[])`.
- Produces: entité `Image` (`String id`, `byte[] donnees`, `String typeContenu`, `long taille`, `Instant dateEnvoi`, `Utilisateur proprietaire`) ; `ImageRepository extends JpaRepository<Image, String>` avec `void supprimerCellesDe(Long utilisateurId)` ; `POST /api/images` → 201 `{"url": "..."}` ; `GET /api/images/{id}`.

- [ ] **Step 1: Write the failing test**

```java
package com.example.carnet_contact_backend.controller;

import com.example.carnet_contact_backend.model.Role;
import com.example.carnet_contact_backend.model.Utilisateur;
import com.example.carnet_contact_backend.repository.UtilisateurRepository;
import com.example.carnet_contact_backend.security.JwtService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ImageControllerTest {

    static final byte[] PNG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D};

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private JwtService jwtService;

    private String jeton;

    @BeforeEach
    void preparer() {
        Utilisateur alice = new Utilisateur();
        alice.setEmail("alice@exemple.fr");
        alice.setMotDePasse("peu-importe");
        alice.setNomAffichage("Alice");
        alice.setRole(Role.UTILISATEUR);
        alice.setActif(true);
        alice.setDateInscription(Instant.now());
        utilisateurRepository.save(alice);
        jeton = jwtService.genererJeton(alice.getEmail(), Role.UTILISATEUR);
    }

    private static MockMultipartFile fichier(String nomPartie, byte[] contenu) {
        return new MockMultipartFile(nomPartie, "photo.png", "image/png", contenu);
    }

    private String envoyer(byte[] contenu) throws Exception {
        String corps = mockMvc.perform(multipart("/api/images")
                        .file(fichier("fichier", contenu))
                        .header("Authorization", "Bearer " + jeton))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(corps, "$.url");
    }

    @Test
    @DisplayName("Un envoi valide renvoie 201 et une adresse absolue")
    void envoi_renvoieUneUrlAbsolue() throws Exception {
        String url = envoyer(PNG);
        assertThat(url).startsWith("http://localhost/api/images/");
        assertThat(url.substring(url.lastIndexOf('/') + 1)).hasSize(36);
    }

    @Test
    @DisplayName("Un faux PNG est refusé en 415, quel que soit son nom")
    void fauxPng_renvoie415() throws Exception {
        mockMvc.perform(multipart("/api/images")
                        .file(fichier("fichier", "pas une image".getBytes()))
                        .header("Authorization", "Bearer " + jeton))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    @DisplayName("Un fichier vide est refusé en 400")
    void fichierVide_renvoie400() throws Exception {
        mockMvc.perform(multipart("/api/images")
                        .file(fichier("fichier", new byte[0]))
                        .header("Authorization", "Bearer " + jeton))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Sans partie « fichier », la requête est refusée en 400")
    void partieAbsente_renvoie400() throws Exception {
        mockMvc.perform(multipart("/api/images")
                        .file(fichier("autre", PNG))
                        .header("Authorization", "Bearer " + jeton))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("L'envoi exige un jeton (401)")
    void envoiSansJeton_renvoie401() throws Exception {
        mockMvc.perform(multipart("/api/images").file(fichier("fichier", PNG)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("La lecture est publique et rend les mêmes octets, avec le bon type")
    void lecture_estPublique() throws Exception {
        String url = envoyer(PNG);
        String id = url.substring(url.lastIndexOf('/') + 1);

        mockMvc.perform(get("/api/images/" + id))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(content().bytes(PNG))
                .andExpect(header().string("Cache-Control", containsString("immutable")));
    }

    @Test
    @DisplayName("Une image inconnue renvoie 404")
    void imageInconnue_renvoie404() throws Exception {
        mockMvc.perform(get("/api/images/00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isNotFound());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\mvnw.cmd test -Dtest=ImageControllerTest`
Expected: 404 sur `POST /api/images` (ou 401 pour les lectures), tests en échec.

- [ ] **Step 3: Write the implementation**

`model/Image.java` :

```java
package com.example.carnet_contact_backend.model;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Une image envoyée depuis l'appareil d'un utilisateur.
 *
 * Elle n'est rattachée à aucun contact ni publication : ceux-ci ne connaissent
 * que son ADRESSE, comme ils connaîtraient celle d'une image hébergée ailleurs.
 */
@Entity
public class Image {

    /**
     * Un UUID plutôt qu'un compteur : l'image se lit sans jeton, son identifiant
     * est donc la seule protection. /api/images/42 inviterait à essayer 43 ; un
     * UUID aléatoire ne se devine pas.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    // @Lob : un contenu volumineux, rangé dans une colonne BLOB plutôt que
    // dans un VARCHAR limité.
    @Lob
    @Column(nullable = false)
    private byte[] donnees;

    // Déduit des octets par FormatImage, jamais recopié de la requête.
    @Column(nullable = false, length = 20)
    private String typeContenu;

    @Column(nullable = false)
    private long taille;

    @Column(nullable = false)
    private Instant dateEnvoi;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proprietaire_id")
    private Utilisateur proprietaire;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public byte[] getDonnees() { return donnees; }
    public void setDonnees(byte[] donnees) { this.donnees = donnees; }
    public String getTypeContenu() { return typeContenu; }
    public void setTypeContenu(String typeContenu) { this.typeContenu = typeContenu; }
    public long getTaille() { return taille; }
    public void setTaille(long taille) { this.taille = taille; }
    public Instant getDateEnvoi() { return dateEnvoi; }
    public void setDateEnvoi(Instant dateEnvoi) { this.dateEnvoi = dateEnvoi; }
    public Utilisateur getProprietaire() { return proprietaire; }
    public void setProprietaire(Utilisateur proprietaire) { this.proprietaire = proprietaire; }
}
```

`repository/ImageRepository.java` :

```java
package com.example.carnet_contact_backend.repository;

import com.example.carnet_contact_backend.model.Image;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ImageRepository extends JpaRepository<Image, String> {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM Image i WHERE i.proprietaire.id = :utilisateurId")
    void supprimerCellesDe(@Param("utilisateurId") Long utilisateurId);
}
```

`controller/ImageController.java` :

```java
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

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImageEnvoyee> envoyer(
            @RequestParam("fichier") MultipartFile fichier,
            @AuthenticationPrincipal String email) throws IOException {
        Utilisateur moi = utilisateurConnecte(email);

        if (fichier.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le fichier est vide.");
        }

        byte[] donnees = fichier.getBytes();
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

        URI adresse = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/images/{id}")
                .buildAndExpand(image.getId())
                .toUri();

        return ResponseEntity.created(adresse).body(new ImageEnvoyee(adresse.toString()));
    }

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
```

`SecurityConfig.java`, après `.requestMatchers("/h2-console/**").permitAll()` :

```java
                        // Les images sont affichées par des balises <img>, qui
                        // n'envoient jamais le jeton : leur lecture doit être
                        // publique. Seul le GET est ouvert — envoyer une image
                        // reste réservé aux comptes connectés.
                        .requestMatchers(HttpMethod.GET, "/api/images/*").permitAll()
```

`application.properties`, en fin de fichier :

```properties

# --- Envoi d'images ---
# Par defaut, Spring refuse tout fichier de plus de 1 Mo. La requete entiere
# garde 1 Mo de marge pour l'enveloppe multipart autour du fichier.
spring.servlet.multipart.max-file-size=5MB
spring.servlet.multipart.max-request-size=6MB
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\mvnw.cmd test -Dtest=ImageControllerTest`
Expected: 7 tests, 0 échec.

- [ ] **Step 5: Commit**

```bash
git add carnet-contact-backend/src
git commit -m "Images : envoi multipart, stockage en base et lecture publique"
```

---

### Task 3: Nettoyage des images inutilisées

**Files:**
- Create: `carnet-contact-backend/src/main/java/com/example/carnet_contact_backend/image/NettoyageImages.java`
- Modify: `carnet-contact-backend/src/main/java/com/example/carnet_contact_backend/repository/ImageRepository.java`
- Modify: `carnet-contact-backend/src/main/java/com/example/carnet_contact_backend/CarnetContactBackendApplication.java`
- Modify: `carnet-contact-backend/src/main/resources/application.properties`
- Test: `carnet-contact-backend/src/test/java/com/example/carnet_contact_backend/image/NettoyageImagesTest.java`

**Interfaces:**
- Consumes: `Image`, `ImageRepository`.
- Produces: `int ImageRepository.supprimerOrphelinesAvant(Instant limite)` ; `int NettoyageImages.nettoyer(Instant limite)`.

- [ ] **Step 1: Write the failing test**

```java
package com.example.carnet_contact_backend.image;

import com.example.carnet_contact_backend.model.*;
import com.example.carnet_contact_backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class NettoyageImagesTest {

    private static final Instant MAINTENANT = Instant.parse("2026-09-15T12:00:00Z");
    private static final Instant LIMITE = MAINTENANT.minus(Duration.ofHours(1));

    @Autowired private NettoyageImages nettoyage;
    @Autowired private ImageRepository imageRepository;
    @Autowired private UtilisateurRepository utilisateurRepository;
    @Autowired private ContactRepository contactRepository;
    @Autowired private PublicationRepository publicationRepository;

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

    /** Le délai de grâce : le formulaire qui l'a envoyée n'est peut-être pas encore enregistré. */
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
    }

    @Test
    @DisplayName("Une image utilisée comme photo de profil est gardée")
    void referenceeParUnProfil_estGardee() {
        Image photo = image(MAINTENANT.minus(Duration.ofDays(1)));
        alice.setPhotoUrl(adresse(photo));
        utilisateurRepository.save(alice);

        assertThat(nettoyage.nettoyer(LIMITE)).isZero();
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
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\mvnw.cmd test -Dtest=NettoyageImagesTest`
Expected: échec de compilation, `NettoyageImages` introuvable.

- [ ] **Step 3: Write the implementation**

Ajouter à `ImageRepository` (imports `java.time.Instant`, `org.springframework.transaction.annotation.Transactional`) :

```java
    /**
     * Supprime les images envoyées avant `limite` qu'aucun des trois champs
     * d'adresse ne référence.
     *
     * Les trois tables sont consultées, et pas seulement celle qui a changé :
     * la même adresse peut servir à plusieurs endroits.
     *
     * @Transactional ici, sur le dépôt : la méthode planifiée qui l'appelle
     * passe par un appel interne à son service, que le proxy transactionnel de
     * Spring ne voit pas.
     */
    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            DELETE FROM Image i
            WHERE i.dateEnvoi < :limite
              AND NOT EXISTS (SELECT c.id FROM Contact c WHERE c.photoUrl LIKE CONCAT('%/api/images/', i.id))
              AND NOT EXISTS (SELECT u.id FROM Utilisateur u WHERE u.photoUrl LIKE CONCAT('%/api/images/', i.id))
              AND NOT EXISTS (SELECT p.id FROM Publication p WHERE p.imageUrl LIKE CONCAT('%/api/images/', i.id))
            """)
    int supprimerOrphelinesAvant(@Param("limite") Instant limite);
```

`image/NettoyageImages.java` :

```java
package com.example.carnet_contact_backend.image;

import com.example.carnet_contact_backend.repository.ImageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class NettoyageImages {

    private static final Logger journal = LoggerFactory.getLogger(NettoyageImages.class);

    private final ImageRepository imageRepository;
    private final Duration delaiGrace;

    public NettoyageImages(
            ImageRepository imageRepository,
            @Value("${carnet.images.delai-grace-ms}") long delaiGraceMs) {
        this.imageRepository = imageRepository;
        this.delaiGrace = Duration.ofMillis(delaiGraceMs);
    }

    /** La limite en paramètre : un test choisit « il y a une heure » sans attendre une heure. */
    public int nettoyer(Instant limite) {
        return imageRepository.supprimerOrphelinesAvant(limite);
    }

    @Scheduled(
            fixedDelayString = "${carnet.images.nettoyage-intervalle-ms}",
            initialDelayString = "${carnet.images.nettoyage-intervalle-ms}")
    public void nettoyageRegulier() {
        int supprimees = nettoyer(Instant.now().minus(delaiGrace));
        if (supprimees > 0) {
            journal.info("{} image(s) inutilisée(s) supprimée(s)", supprimees);
        }
    }
}
```

`CarnetContactBackendApplication.java` : ajouter `@EnableScheduling` (import `org.springframework.scheduling.annotation.EnableScheduling`) sous `@SpringBootApplication`.

`application.properties`, à la suite du bloc « Envoi d'images » :

```properties

# Une image envoyee n'est referencee par rien tant que son formulaire n'est pas
# enregistre : on ne supprime qu'apres ce delai de grace (1 h).
carnet.images.delai-grace-ms=3600000
# Intervalle entre deux nettoyages (30 min), et attente avant le premier.
carnet.images.nettoyage-intervalle-ms=1800000
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\mvnw.cmd test -Dtest=NettoyageImagesTest`
Expected: 5 tests, 0 échec.

- [ ] **Step 5: Commit**

```bash
git add carnet-contact-backend/src
git commit -m "Images : nettoyage planifié des images inutilisées"
```

---

### Task 4: Suppression d'un compte et commentaires devenus faux

**Files:**
- Modify: `carnet-contact-backend/src/main/java/com/example/carnet_contact_backend/controller/AdminController.java` (constructeur, `supprimer`)
- Modify: `carnet-contact-backend/src/main/java/com/example/carnet_contact_backend/model/Contact.java:27`, `Utilisateur.java:44`, `Publication.java:47`
- Test: `carnet-contact-backend/src/test/java/com/example/carnet_contact_backend/controller/AdminControllerTest.java`

**Interfaces:**
- Consumes: `ImageRepository.supprimerCellesDe(Long)`.

- [ ] **Step 1: Write the failing test** — ajouter à `AdminControllerTest` (imports `com.example.carnet_contact_backend.repository.ImageRepository`, `org.springframework.mock.web.MockMultipartFile`) :

```java
    @Autowired
    private ImageRepository imageRepository;

    @Test
    @DisplayName("Supprimer un compte emporte les images qu'il a envoyées")
    void supprimerUnCompte_emporteSesImages() throws Exception {
        byte[] png = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00};
        mockMvc.perform(multipart("/api/images")
                        .file(new MockMultipartFile("fichier", "p.png", "image/png", png))
                        .header("Authorization", "Bearer " + jetonSimple))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/admin/comptes/" + simple.getId())
                        .header("Authorization", "Bearer " + jetonPatron))
                .andExpect(status().isNoContent());

        entityManager.flush();
        entityManager.clear();

        assertThat(imageRepository.count()).isZero();
    }
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\mvnw.cmd test -Dtest=AdminControllerTest`
Expected: échec — violation de clé étrangère `proprietaire_id` au flush.

- [ ] **Step 3: Write the implementation**

Dans `AdminController` : champ `private final ImageRepository imageRepository;`, paramètre ajouté au constructeur, et dans `supprimer`, juste avant `jetonRepository.supprimerTousPour(cible.getId());` :

```java
        // Ses images envoyées : la tâche de nettoyage ne les aurait pas
        // attendues, la clé étrangère vers le compte bloquerait la suppression.
        imageRepository.supprimerCellesDe(cible.getId());
```

Commentaires : `Contact.java` « Photo du contact : une URL — saisie, ou obtenue en envoyant une image (/api/images). » ; `Utilisateur.java` « Photo de profil : une URL, saisie ou obtenue par l'envoi d'une image. » ; `Publication.java` « Une adresse d'image, saisie ou obtenue par l'envoi d'un fichier (/api/images). »

- [ ] **Step 4: Run the whole backend suite**

Run: `.\mvnw.cmd test`
Expected: 91 + 9 + 7 + 5 + 1 = 113 tests, 0 échec.

- [ ] **Step 5: Commit**

```bash
git add carnet-contact-backend/src
git commit -m "Images : suppression avec le compte, commentaires mis à jour"
```

---

### Task 5: Vérification au `curl` (dont le 413 réel)

- [ ] **Step 1:** `netstat -ano | findstr :8125` — le port doit être libre.
- [ ] **Step 2:** lancer le backend en arrière-plan : `.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--server.port=8125"`, attendre « Started ».
- [ ] **Step 3:** inscription (`POST /api/auth/inscription`, corps écrit dans un fichier, mot de passe `Carnet-Verif-2026!`), récupérer `jeton`.
- [ ] **Step 4:** fabriquer dans le scratchpad un PNG minimal, un faux JPEG (texte), un fichier de 5,5 Mo commençant par la signature PNG ; vérifier :
  - envoi du PNG → 201 et `url` ;
  - `curl -i <url>` sans jeton → 200, `Content-Type: image/png`, `Cache-Control … immutable` ;
  - faux JPEG → 415 ;
  - 5,5 Mo → **413**. Si le code obtenu n'est pas 413, ajouter un `@RestControllerAdvice` qui transforme `MaxUploadSizeExceededException` en 413, avec un test `MockMvc`, puis revérifier.
- [ ] **Step 5:** arrêter le serveur (processus du port 8125) et revérifier `netstat`.

---

### Task 6: Service d'envoi et messages 413 / 415 (frontend)

**Files:**
- Create: `carnet-contact_frontend/src/app/services/image.ts`
- Test: `carnet-contact_frontend/src/app/services/image.spec.ts`
- Modify: `carnet-contact_frontend/src/app/interceptors/erreur-interceptor.ts:29-33`
- Test: `carnet-contact_frontend/src/app/interceptors/erreur-interceptor.spec.ts`

**Interfaces:**
- Produces: `ImageService.envoyer(fichier: File): Observable<string>` ; constantes `TAILLE_MAX_IMAGE: number`, `FORMATS_IMAGE: readonly string[]`.

- [ ] **Step 1: Write the failing tests**

`image.spec.ts` :

```ts
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ImageService } from './image';

describe('ImageService', () => {
  let service: ImageService;
  let backend: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(ImageService);
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => backend.verify());

  it('envoie le fichier en FormData, sans Content-Type écrit à la main, et rend l\'URL', () => {
    const fichier = new File(['octets'], 'photo.png', { type: 'image/png' });
    let url: string | undefined;

    service.envoyer(fichier).subscribe(u => (url = u));

    const requete = backend.expectOne('/api/images');
    expect(requete.request.method).toBe('POST');
    expect(requete.request.body).toBeInstanceOf(FormData);
    expect(((requete.request.body as FormData).get('fichier') as File).name).toBe('photo.png');
    expect(requete.request.headers.has('Content-Type')).toBe(false);

    requete.flush({ url: 'http://localhost:8080/api/images/abc' });
    expect(url).toBe('http://localhost:8080/api/images/abc');
  });
});
```

Ajout à `erreur-interceptor.spec.ts` :

```ts
  it('explique un fichier trop volumineux (413) et un format refusé (415)', () => {
    http.post('/api/images', {}).subscribe({ error: () => {} });
    backend.expectOne('/api/images').flush('', { status: 413, statusText: 'Payload Too Large' });
    expect(etat.erreur()).toBe('L\'image dépasse la taille autorisée (413).');

    http.post('/api/images', {}).subscribe({ error: () => {} });
    backend.expectOne('/api/images').flush('', { status: 415, statusText: 'Unsupported Media Type' });
    expect(etat.erreur()).toBe('Ce format de fichier n\'est pas accepté (415).');
  });
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `npx ng test --watch=false`
Expected: `image.spec.ts` ne compile pas ; le test 413/415 obtient « Une erreur inattendue… ».

- [ ] **Step 3: Write the implementation**

`services/image.ts` :

```ts
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { contexte } from '../interceptors/http-contexte';

/** Les mêmes limites que le serveur (spring.servlet.multipart, FormatImage). */
export const TAILLE_MAX_IMAGE = 5 * 1024 * 1024;
export const FORMATS_IMAGE: readonly string[] = ['image/jpeg', 'image/png', 'image/webp', 'image/gif'];

@Injectable({
  providedIn: 'root'
})
export class ImageService {
  private http = inject(HttpClient);

  /**
   * FormData : le corps « multipart » qu'un formulaire HTML enverrait, capable de
   * transporter des octets bruts là où le JSON ne transporte que du texte.
   *
   * Aucun Content-Type n'est posé à la main, volontairement : le navigateur doit
   * l'écrire lui-même, car il y ajoute la « frontière » qui sépare les parties.
   */
  envoyer(fichier: File): Observable<string> {
    const donnees = new FormData();
    donnees.append('fichier', fichier);

    return this.http.post<{ url: string }>('/api/images', donnees, {
      context: contexte({ libelle: 'Impossible d\'envoyer l\'image' })
    }).pipe(
      map(reponse => reponse.url)
    );
  }
}
```

`erreur-interceptor.ts`, dans `raisonTechnique()`, après `case 409` :

```ts
    case 413:
      return 'l\'image dépasse la taille autorisée (413)';
    case 415:
      return 'ce format de fichier n\'est pas accepté (415)';
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `npx ng test --watch=false`
Expected: tous les tests au vert (88 + 2).

- [ ] **Step 5: Commit**

```bash
git add carnet-contact_frontend/src/app/services/image.ts carnet-contact_frontend/src/app/services/image.spec.ts carnet-contact_frontend/src/app/interceptors
git commit -m "Images : service d'envoi FormData et messages 413 / 415"
```

---

### Task 7: Composant `champ-image`

**Files:**
- Create: `carnet-contact_frontend/src/app/components/champ-image/champ-image.ts`, `.html`, `.css`
- Test: `carnet-contact_frontend/src/app/components/champ-image/champ-image.spec.ts`

**Interfaces:**
- Consumes: `ImageService.envoyer`, `TAILLE_MAX_IMAGE`, `FORMATS_IMAGE`.
- Produces: `<app-champ-image [controle]="FormControl<string | null>" identifiant="…" libelle="…" placeholder="…" />` ; méthodes publiques `envoyer(fichier: File)`, `fichierChoisi(evenement: Event)` ; signaux `envoiEnCours`, `erreurLocale`.

- [ ] **Step 1: Write the failing test**

```ts
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormControl } from '@angular/forms';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ChampImage } from './champ-image';

describe('ChampImage', () => {
  let fixture: ComponentFixture<ChampImage>;
  let composant: ChampImage;
  let backend: HttpTestingController;
  let controle: FormControl<string | null>;

  const image = (taille = 10, type = 'image/png') =>
    new File([new Uint8Array(taille)], 'photo.png', { type });

  beforeEach(async () => {
    TestBed.configureTestingModule({
      imports: [ChampImage],
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    backend = TestBed.inject(HttpTestingController);

    controle = new FormControl<string | null>('https://exemple.fr/ancienne.jpg');
    fixture = TestBed.createComponent(ChampImage);
    fixture.componentRef.setInput('controle', controle);
    fixture.componentRef.setInput('identifiant', 'photo');
    fixture.componentRef.setInput('libelle', 'Photo');
    composant = fixture.componentInstance;
    await fixture.whenStable();
  });

  afterEach(() => backend.verify());

  it('refuse un format hors liste sans rien envoyer', () => {
    composant.envoyer(image(10, 'image/svg+xml'));

    expect(composant.erreurLocale()).toContain('JPEG');
    backend.expectNone('/api/images');
  });

  it('refuse une image de plus de 5 Mo sans rien envoyer', () => {
    composant.envoyer(image(5 * 1024 * 1024 + 1));

    expect(composant.erreurLocale()).toContain('5 Mo');
    backend.expectNone('/api/images');
  });

  it('place l\'URL reçue dans le contrôle du formulaire parent', () => {
    composant.envoyer(image());
    expect(composant.envoiEnCours()).toBe(true);

    backend.expectOne('/api/images').flush({ url: 'http://localhost:8080/api/images/abc' });

    expect(controle.value).toBe('http://localhost:8080/api/images/abc');
    expect(controle.dirty).toBe(true);
    expect(composant.envoiEnCours()).toBe(false);
  });

  it('garde l\'ancienne adresse si l\'envoi échoue', () => {
    composant.envoyer(image());

    backend.expectOne('/api/images').flush('', { status: 415, statusText: 'Unsupported Media Type' });

    expect(controle.value).toBe('https://exemple.fr/ancienne.jpg');
    expect(composant.envoiEnCours()).toBe(false);
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npx ng test --watch=false`
Expected: `champ-image.spec.ts` ne compile pas.

- [ ] **Step 3: Write the implementation**

`champ-image.ts` :

```ts
import { Component, inject, input, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { finalize } from 'rxjs';
import { FORMATS_IMAGE, ImageService, TAILLE_MAX_IMAGE } from '../../services/image';

/**
 * Un champ d'adresse d'image, doublé d'un bouton pour envoyer un fichier.
 *
 * Il reçoit le FormControl du formulaire parent plutôt que d'implémenter
 * ControlValueAccessor (ce qui permettrait `formControlName`) : même résultat
 * ici, pour beaucoup moins de code. Une image envoyée devient une adresse, que
 * le composant écrit dans ce contrôle — le parent ne voit aucune différence
 * avec une adresse collée.
 */
@Component({
  selector: 'app-champ-image',
  imports: [ReactiveFormsModule, ButtonModule, InputTextModule],
  templateUrl: './champ-image.html',
  styleUrl: './champ-image.css'
})
export class ChampImage {
  private images = inject(ImageService);

  controle = input.required<FormControl<string | null>>();
  identifiant = input.required<string>();
  libelle = input.required<string>();
  placeholder = input('');

  readonly formatsAcceptes = FORMATS_IMAGE.join(',');

  envoiEnCours = signal(false);
  erreurLocale = signal<string | null>(null);

  fichierChoisi(evenement: Event): void {
    const selecteur = evenement.target as HTMLInputElement;
    const fichier = selecteur.files?.[0];

    // Vidé tout de suite : sinon, choisir à nouveau le même fichier ne
    // déclencherait pas d'événement « change ».
    selecteur.value = '';

    if (fichier) {
      this.envoyer(fichier);
    }
  }

  /**
   * Vérification locale avant l'envoi : un confort (réponse immédiate, pas de
   * 5 Mo envoyés pour rien), pas une sécurité — le serveur revérifie tout.
   */
  envoyer(fichier: File): void {
    this.erreurLocale.set(null);

    if (!FORMATS_IMAGE.includes(fichier.type)) {
      this.erreurLocale.set('Formats acceptés : JPEG, PNG, WebP ou GIF.');
      return;
    }
    if (fichier.size > TAILLE_MAX_IMAGE) {
      this.erreurLocale.set('L\'image dépasse 5 Mo.');
      return;
    }

    this.envoiEnCours.set(true);

    this.images.envoyer(fichier).pipe(
      finalize(() => this.envoiEnCours.set(false))
    ).subscribe({
      next: url => {
        this.controle().setValue(url);
        this.controle().markAsDirty();
      },
      // La bannière vient de erreurInterceptor. On ne touche pas au contrôle :
      // l'adresse précédente reste en place.
      error: () => {}
    });
  }
}
```

`champ-image.html` :

```html
<label [for]="identifiant()">{{ libelle() }}</label>

<div class="rangee">
  <input pInputText [id]="identifiant()" [formControl]="controle()" [placeholder]="placeholder()" />

  <!-- Le vrai sélecteur de fichier est caché : son apparence native ne se
       stylise presque pas. Ce bouton-ci le déclenche par .click(). -->
  <p-button
    type="button"
    severity="secondary"
    [outlined]="true"
    icon="pi pi-upload"
    [label]="envoiEnCours() ? 'Envoi en cours…' : 'Choisir une image'"
    [disabled]="envoiEnCours()"
    (onClick)="selecteur.click()" />

  <input #selecteur type="file" hidden [accept]="formatsAcceptes" (change)="fichierChoisi($event)" />
</div>

@if (erreurLocale(); as message) {
  <p class="erreur-champ" role="alert">{{ message }}</p>
}
```

`champ-image.css` :

```css
:host {
  display: block;
}

.rangee {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

/* Le champ prend la place restante ; sous 14rem, le bouton passe à la ligne. */
.rangee input {
  flex: 1 1 14rem;
  min-width: 0;
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npx ng test --watch=false`
Expected: 4 nouveaux tests au vert.

- [ ] **Step 5: Commit**

```bash
git add carnet-contact_frontend/src/app/components/champ-image
git commit -m "Images : composant champ-image (adresse ou fichier)"
```

---

### Task 8: Intégration dans les quatre formulaires

**Files:**
- Modify: `carnet-contact_frontend/src/app/pages/profil/profil.ts`, `profil.html:15,24-27`
- Modify: `carnet-contact_frontend/src/app/components/contact-form/contact-form.ts`, `contact-form.html:49-53`, `contact-form.spec.ts`
- Modify: `carnet-contact_frontend/src/app/pages/contact-edit/contact-edit.ts`, `contact-edit.html:30-33`
- Modify: `carnet-contact_frontend/src/app/components/publication-form/publication-form.ts`, `publication-form.html:27-39`

**Interfaces:**
- Consumes: `ChampImage` (Task 7).

- [ ] **Step 1: Profil** — importer `ChampImage` dans `imports`. Remplacer le bloc `photoUrl` par :

```html
    <app-champ-image
      class="champ"
      identifiant="photoUrl"
      libelle="Photo de profil"
      placeholder="https://exemple.fr/moi.jpg"
      [controle]="formulaire.controls.photoUrl" />
```

et le texte d'aide par `Collez une adresse, ou choisissez une image sur votre appareil.`

- [ ] **Step 2: Formulaire de contact** — importer `ChampImage`. Remplacer le `<div>` `photoUrl` par :

```html
          <app-champ-image
            identifiant="photoUrl"
            libelle="Photo"
            placeholder="https://exemple.fr/jean.jpg"
            [controle]="contactForm.controls.photoUrl" />
```

Dans `contact-form.spec.ts`, fournir `HttpClient` (le champ en a besoin) :

```ts
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
// …
    await TestBed.configureTestingModule({
      imports: [ContactForm],
      providers: [provideHttpClient(), provideHttpClientTesting()]
    }).compileComponents();
```

- [ ] **Step 3: Édition d'un contact** — importer `ChampImage`. Remplacer le `<div>` `photoUrl` par :

```html
        <app-champ-image
          identifiant="photoUrl"
          libelle="Photo"
          [controle]="contactForm.controls.photoUrl" />
```

- [ ] **Step 4: Publication** — importer `ChampImage`. Remplacer le label et l'input de l'image (lignes 28-32) par :

```html
    <app-champ-image
      [identifiant]="'image-' + numero"
      libelle="Image (adresse ou fichier, facultative)"
      placeholder="https://exemple.fr/photo.jpg"
      [controle]="formulaire.controls.imageUrl" />
```

en gardant le message d'erreur de format et l'aperçu qui suivent.

- [ ] **Step 5: Run all frontend tests and the build**

Run: `npx ng test --watch=false` puis `npx ng build`
Expected: tous les tests au vert (94) ; build sans erreur ni dépassement de budget.

- [ ] **Step 6: Commit**

```bash
git add carnet-contact_frontend/src/app
git commit -m "Images : envoi possible depuis le profil, les contacts et les publications"
```

---

### Task 9: Documentation

**Files:**
- Modify: `docs/support-apprentissage-angular-spring.md`
- Modify: `docs/progression-pedagogique.md`
- Modify (renvois) : tout fichier de `docs/` citant `#36-backend-spring-boot`, `#37-git-et-github`, `#38-pense-bête-de-dépannage` (vérifier par recherche, dont `docs/cours-angular.md`)

- [ ] **Step 1:** insérer la **section 36 « Envoi de fichiers »** après la section 35, au format du document (prose du pourquoi, bloc générique commenté, encadré « Dans le projet » avec liens relatifs et extraits réels, tableaux à deux colonnes). Sous-sections :
  1. Le format multipart et `MultipartFile` (limites `spring.servlet.multipart.*`, 400 / 413 / 415).
  2. Stocker des octets : `@Lob`, identifiant UUID.
  3. Ne pas croire le client : la signature d'un fichier (`FormatImage`), pourquoi pas le SVG.
  4. Une ressource publique dans une API protégée : `<img>` n'envoie pas le jeton, `permitAll` sur le seul `GET`, URL absolue (`ServletUriComponentsBuilder`), `Cache-Control: immutable`.
  5. Tâches planifiées : `@EnableScheduling`, `@Scheduled(fixedDelayString, initialDelayString)`, délai de grâce, `@Transactional` sur le dépôt (appel interne non intercepté par le proxy).
  6. Côté Angular : `FormData` sans `Content-Type`, `<input type="file">` caché et `.click()`, vider `value`, vérification locale = confort.
  7. Un composant qui reçoit un `FormControl` (et l'alternative `ControlValueAccessor`).
- [ ] **Step 2:** renuméroter Backend / Git / Pense-bête en 37 / 38 / 39 (sommaire, titres, ancres) et corriger les renvois trouvés par recherche.
- [ ] **Step 3:** pense-bête : ajouter les erreurs réellement rencontrées pendant les tâches 1 à 8, plus « `Content-Type` posé à la main sur un `FormData` → 400 / « no multipart boundary » », « même fichier choisi deux fois → pas d'événement change », « `@Transactional` ignoré sur un appel interne ».
- [ ] **Step 4:** `progression-pedagogique.md` : **Partie 15 — Envoi d'images**, dans le format des parties précédentes (demande, choix soumis, numérotation continue à partir de 131, vérifications faites et non faites, notions ajoutées au support).
- [ ] **Step 5: Commit**

```bash
git add docs
git commit -m "Support : section 36 (envoi de fichiers), progression Partie 15"
```

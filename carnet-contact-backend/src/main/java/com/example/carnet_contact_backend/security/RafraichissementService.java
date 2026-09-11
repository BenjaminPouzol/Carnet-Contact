package com.example.carnet_contact_backend.security;

import com.example.carnet_contact_backend.model.JetonRafraichissement;
import com.example.carnet_contact_backend.model.Utilisateur;
import com.example.carnet_contact_backend.repository.JetonRafraichissementRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

/**
 * Fabrique, vérifie et révoque les jetons de rafraîchissement.
 *
 * Contrairement à JwtService, qui ne fait que des calculs, ce service parle à
 * la base : c'est le prix à payer pour pouvoir annuler un jeton.
 */
@Service
public class RafraichissementService {

    private final JetonRafraichissementRepository repository;
    private final long dureeMs;

    /**
     * SecureRandom, et non Random. Les deux produisent des nombres « au
     * hasard », mais Random est PRÉVISIBLE : à partir de quelques valeurs
     * observées, on retrouve sa graine et donc toutes les suivantes. Pour un
     * secret d'authentification, c'est disqualifiant.
     */
    private final SecureRandom aleatoire = new SecureRandom();

    public RafraichissementService(
            JetonRafraichissementRepository repository,
            @Value("${carnet.jwt.rafraichissement-duree-ms}") long dureeMs) {
        this.repository = repository;
        this.dureeMs = dureeMs;
    }

    /**
     * Émet un nouveau jeton pour un compte.
     *
     * La valeur est un simple tirage aléatoire, pas un JWT : il n'y a rien à
     * lire dedans. Le serveur la cherche en base, la ligne trouvée porte toute
     * l'information. On parle d'un jeton « opaque » — par opposition au JWT,
     * dont n'importe qui peut lire le contenu.
     */
    @Transactional
    public JetonRafraichissement emettre(Utilisateur utilisateur) {
        byte[] octets = new byte[32];
        aleatoire.nextBytes(octets);

        JetonRafraichissement jeton = new JetonRafraichissement();
        // withoutPadding : évite les « = » finaux, gênants dans une URL ou un
        // JSON recopié à la main.
        jeton.setValeur(Base64.getUrlEncoder().withoutPadding().encodeToString(octets));
        jeton.setUtilisateur(utilisateur);
        jeton.setExpiration(Instant.now().plusMillis(dureeMs));
        jeton.setRevoque(false);

        return repository.save(jeton);
    }

    /**
     * Vérifie un jeton présenté par le client et en émet un neuf — c'est la
     * ROTATION.
     *
     * Pourquoi ne pas simplement laisser le client réutiliser le même pendant
     * sept jours ? Parce qu'un jeton volé serait alors exploitable pendant
     * sept jours sans que rien ne le trahisse. Avec la rotation, le voleur et
     * le vrai utilisateur se disputent un jeton à usage unique : dès que l'un
     * s'en sert, l'autre se retrouve avec une valeur révoquée et se voit
     * déconnecté — l'anomalie devient visible.
     *
     * Optional.empty() couvre les trois refus possibles : valeur inconnue,
     * jeton déjà révoqué, jeton périmé.
     */
    @Transactional
    public Optional<JetonRafraichissement> faireTourner(String valeurPresentee) {
        return repository.findByValeur(valeurPresentee)
                .filter(JetonRafraichissement::estUtilisable)
                .map(ancien -> {
                    ancien.setRevoque(true);
                    repository.save(ancien);
                    return emettre(ancien.getUtilisateur());
                });
    }

    /** Déconnexion : ce jeton-là ne servira plus. */
    @Transactional
    public void revoquer(String valeur) {
        repository.findByValeur(valeur).ifPresent(jeton -> {
            jeton.setRevoque(true);
            repository.save(jeton);
        });
    }
}

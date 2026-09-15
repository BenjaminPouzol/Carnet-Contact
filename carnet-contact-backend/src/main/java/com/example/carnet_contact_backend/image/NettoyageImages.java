package com.example.carnet_contact_backend.image;

import com.example.carnet_contact_backend.repository.ImageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

/**
 * Supprime régulièrement les images que plus rien n'utilise : photo remplacée,
 * contact ou publication supprimés, formulaire abandonné après l'envoi.
 *
 * Tout est fait ici, à un seul endroit, plutôt que dans chaque route qui change
 * une image. C'est la leçon des intercepteurs : une règle recopiée dans six
 * routes finit par être oubliée dans la septième.
 */
@Service
public class NettoyageImages {

    private static final Logger journal = LoggerFactory.getLogger(NettoyageImages.class);

    private final ImageRepository imageRepository;
    private final Duration delaiGrace;

    /**
     * @Value injecte une valeur d'application.properties. Le délai n'est pas
     * écrit en dur : on peut le changer sans recompiler.
     */
    public NettoyageImages(
            ImageRepository imageRepository,
            @Value("${carnet.images.delai-grace-ms}") long delaiGraceMs) {
        this.imageRepository = imageRepository;
        this.delaiGrace = Duration.ofMillis(delaiGraceMs);
    }

    /**
     * Supprime les images non référencées envoyées avant `limite`.
     *
     * La limite est un paramètre plutôt qu'un calcul interne sur l'heure
     * actuelle : un test peut ainsi dire « il y a une heure » sans attendre une
     * heure.
     */
    public int nettoyer(Instant limite) {
        return imageRepository.supprimerOrphelinesAvant(limite);
    }

    /**
     * @Scheduled : Spring appelle cette méthode tout seul, à intervalle régulier,
     * sans qu'aucune requête HTTP ne la déclenche. Il faut pour cela
     * @EnableScheduling sur la classe principale.
     *
     * fixedDelay : l'attente court à partir de la FIN de l'exécution
     * précédente — deux nettoyages ne peuvent pas se chevaucher. initialDelay :
     * pas d'exécution au démarrage, ce qui évite aussi qu'elle tombe au milieu
     * des tests.
     *
     * Le délai de grâce protège une image fraîchement envoyée : tant que son
     * formulaire n'est pas enregistré, rien ne la référence encore.
     */
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

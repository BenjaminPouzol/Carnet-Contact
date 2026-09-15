package com.example.carnet_contact_backend.image;

import java.util.Optional;

/**
 * Reconnaît le format d'une image à ses premiers octets, sa « signature ».
 *
 * Ni le nom du fichier ni le Content-Type annoncé par le navigateur ne font
 * foi : les deux se choisissent librement côté client. Renommer `script.html`
 * en `photo.jpg` ne change pas un seul de ses octets. Les octets, eux, sont ce
 * que le navigateur interprétera réellement à l'affichage.
 *
 * Le SVG n'est volontairement pas reconnu : c'est du texte XML, qui peut
 * contenir du JavaScript exécuté si on ouvre l'image seule dans un onglet.
 *
 * Même forme que PolitiqueMotDePasse : classe finale, constructeur privé,
 * méthode statique. Il n'y a aucun état à garder, donc rien à instancier.
 */
public final class FormatImage {

    private FormatImage() {
    }

    /** Le type MIME reconnu, ou vide si le contenu n'est pas une image acceptée. */
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
        if (commencePar(contenu, 'G', 'I', 'F', '8', '7', 'a')
                || commencePar(contenu, 'G', 'I', 'F', '8', '9', 'a')) {
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
            // & 0xFF : un byte Java est SIGNÉ (0xFF y vaut -1). Le masque le
            // ramène à sa valeur 0-255 avant de le comparer à la signature.
            if ((contenu[i] & 0xFF) != signature[i]) {
                return false;
            }
        }
        return true;
    }
}

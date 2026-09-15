package com.example.carnet_contact_backend.image;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test unitaire pur, sans contexte Spring : FormatImage ne dépend de rien, il
 * se teste en millisecondes (même approche que PolitiqueMotDePasseTest).
 */
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
        byte[] svg = "<svg xmlns=\"http://www.w3.org/2000/svg\"><script>alert(1)</script></svg>"
                .getBytes(US_ASCII);
        assertThat(FormatImage.typeDe(svg)).isEmpty();
    }

    /** RIFF est un conteneur générique : un fichier audio WAV commence pareil. */
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

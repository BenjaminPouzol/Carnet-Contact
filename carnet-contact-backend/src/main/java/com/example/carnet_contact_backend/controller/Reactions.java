package com.example.carnet_contact_backend.controller;

import com.example.carnet_contact_backend.model.ReactionEmoji;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Ce que les réactions des messages et celles des publications ont en commun :
 * la liste des emojis acceptés, et le regroupement « 👍 3, dont la mienne ».
 *
 * Tant que seuls les messages avaient des réactions, cette logique vivait dans
 * MessageController. Le fil d'actualité en a besoin à son tour : la recopier
 * aurait garanti qu'un jour les deux versions divergent.
 *
 * Classe finale, constructeur privé, méthodes statiques — comme
 * PolitiqueMotDePasse : aucun état, rien à injecter.
 */
public final class Reactions {

    /**
     * Les seules réactions acceptées.
     *
     * Une liste fermée plutôt que « n'importe quel emoji » : « est-ce dans cette
     * liste ? » se vérifie trivialement, là où « est-ce un emoji ? » est une
     * question étonnamment difficile (séquences composées, modificateurs de
     * teinte, drapeaux).
     *
     * Le serveur ne fait pas confiance au client : Angular ne propose que ces
     * cinq-là, mais rien n'empêche d'appeler l'API à la main avec autre chose.
     */
    public static final List<String> EMOJIS_AUTORISES = List.of("👍", "❤️", "😂", "😮", "😢");

    private static final Set<String> EMOJIS_VALIDES = Set.copyOf(EMOJIS_AUTORISES);

    /**
     * Les réactions d'un élément, regroupées par emoji.
     *
     * `parMoi` n'existe nulle part en base : c'est une lecture relative à celui
     * qui regarde, que le serveur est le mieux placé pour calculer.
     */
    public record ReactionResume(String emoji, long nombre, boolean parMoi) {}

    private Reactions() {
    }

    public static boolean estAutorise(String emoji) {
        return emoji != null && EMOJIS_VALIDES.contains(emoji);
    }

    /**
     * Regroupe les réactions d'un LOT d'éléments, élément par élément.
     *
     * `List<? extends ReactionEmoji>` : une liste de Reaction ou de
     * ReactionPublication, peu importe — les deux respectent le contrat.
     *
     * La répartition se fait en une passe (groupingBy). L'ancienne version
     * refiltrait toute la liste pour chaque message : un coût qui croissait comme
     * le produit du nombre de messages par le nombre de réactions.
     *
     * @return pour chaque id d'élément, ses réactions résumées ; un élément sans
     *         réaction est absent de la map.
     */
    public static Map<Long, List<ReactionResume>> resumerParCible(
            List<? extends ReactionEmoji> reactions, Long moiId) {

        Map<Long, List<ReactionEmoji>> parCible = reactions.stream()
                .collect(Collectors.groupingBy(ReactionEmoji::idCible));

        Map<Long, List<ReactionResume>> resultat = new HashMap<>();
        parCible.forEach((id, liste) -> resultat.put(id, resumer(liste, moiId)));
        return resultat;
    }

    private static List<ReactionResume> resumer(List<ReactionEmoji> reactions, Long moiId) {
        // LinkedHashMap + tri par id : les emojis gardent l'ordre de leur
        // première apparition. Une HashMap ordinaire les ferait changer de place
        // d'un rafraîchissement à l'autre.
        Map<String, long[]> parEmoji = new LinkedHashMap<>();

        reactions.stream()
                .sorted(Comparator.comparing(ReactionEmoji::getId))
                .forEach(r -> {
                    long[] compte = parEmoji.computeIfAbsent(r.getEmoji(), e -> new long[2]);
                    compte[0]++;
                    if (r.getUtilisateur().getId().equals(moiId)) {
                        compte[1] = 1;
                    }
                });

        return parEmoji.entrySet().stream()
                .map(e -> new ReactionResume(e.getKey(), e.getValue()[0], e.getValue()[1] == 1))
                .toList();
    }
}

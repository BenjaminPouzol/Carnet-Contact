package com.example.carnet_contact_backend.repository;

import com.example.carnet_contact_backend.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    /**
     * La conversation entre deux comptes : les messages dans un sens OU dans
     * l'autre, du plus ancien au plus récent.
     *
     * Ici le nom de méthode dérivé aurait été illisible
     * (findByExpediteurIdAndDestinataireIdOrDestinataireIdAnd...). @Query
     * permet d'écrire la requête à la main, en JPQL : le même langage que SQL
     * mais qui parle d'ENTITÉS et de leurs champs Java (Message, m.expediteur)
     * au lieu de tables et de colonnes.
     */
    @Query("""
            SELECT m FROM Message m
            WHERE (m.expediteur.id = :moi AND m.destinataire.id = :autre)
               OR (m.expediteur.id = :autre AND m.destinataire.id = :moi)
            ORDER BY m.dateEnvoi ASC
            """)
    List<Message> conversation(@Param("moi") Long moi, @Param("autre") Long autre);

    // Les messages reçus et non encore lus, pour la pastille de notification.
    List<Message> findByDestinataireIdAndLuFalse(Long destinataireId);

    // Ceux reçus d'un interlocuteur précis : on les marque lus à l'ouverture
    // du fil.
    List<Message> findByDestinataireIdAndExpediteurIdAndLuFalse(Long destinataireId, Long expediteurId);

    // Pour le tableau d'administration : combien de messages échangés, envoyés
    // comme reçus. Les deux paramètres reçoivent le même identifiant.
    long countByExpediteurIdOrDestinataireId(Long expediteurId, Long destinataireId);

    @Modifying
    @Query("DELETE FROM Message m WHERE m.expediteur.id = :utilisateurId "
            + "OR m.destinataire.id = :utilisateurId")
    void supprimerCeuxDe(@Param("utilisateurId") Long utilisateurId);

    // Qui m'a déjà écrit : ceux-là, je peux leur répondre sans les suivre.
    // DISTINCT : un identifiant par personne, pas un par message.
    @Query("SELECT DISTINCT m.expediteur.id FROM Message m WHERE m.destinataire.id = :id")
    List<Long> idsQuiMOntEcrit(@Param("id") Long id);

    // À qui j'ai déjà écrit : la conversation reste dans ma liste, même si je
    // ne peux plus y répondre.
    @Query("SELECT DISTINCT m.destinataire.id FROM Message m WHERE m.expediteur.id = :id")
    List<Long> idsAQuiJAiEcrit(@Param("id") Long id);
}

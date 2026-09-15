package com.example.carnet_contact_backend.abonnement;

import com.example.carnet_contact_backend.model.Role;
import com.example.carnet_contact_backend.model.StatutAbonnement;
import com.example.carnet_contact_backend.model.Utilisateur;
import com.example.carnet_contact_backend.repository.AbonnementRepository;
import com.example.carnet_contact_backend.repository.BlocageRepository;
import com.example.carnet_contact_backend.repository.MessageRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Charge, pour un compte, tout ce que VueRelations a besoin de savoir.
 *
 * Cinq requêtes, toujours cinq, quelle que soit la taille de ce qu'on affiche
 * ensuite : une page de vingt comptes ou un profil seul coûtent pareil. Les
 * règles, elles, ne sont pas ici — elles sont dans VueRelations, qui travaille
 * en mémoire sur ces ensembles.
 */
@Service
public class Relations {

    private final AbonnementRepository abonnementRepository;
    private final BlocageRepository blocageRepository;
    private final MessageRepository messageRepository;

    public Relations(
            AbonnementRepository abonnementRepository,
            BlocageRepository blocageRepository,
            MessageRepository messageRepository) {
        this.abonnementRepository = abonnementRepository;
        this.blocageRepository = blocageRepository;
        this.messageRepository = messageRepository;
    }

    /**
     * `moi` doit être le compte RECHARGÉ depuis la base (ce que font tous les
     * contrôleurs avec utilisateurConnecte) : c'est de lui qu'on lit le rôle
     * administrateur, et non du jeton, qui peut dater d'avant une rétrogradation.
     */
    public VueRelations vuePour(Utilisateur moi) {
        Long id = moi.getId();

        Map<Long, StatutAbonnement> sortants = new HashMap<>();
        for (Object[] ligne : abonnementRepository.statutsSortants(id)) {
            sortants.put((Long) ligne[0], (StatutAbonnement) ligne[1]);
        }

        Set<Long> abonnesAcceptes = new HashSet<>(
                abonnementRepository.idsAbonnes(id, StatutAbonnement.ACCEPTE));

        Set<Long> mOntEcrit = new HashSet<>(messageRepository.idsQuiMOntEcrit(id));

        // Les deux sens réunis : pour les règles, peu importe qui a bloqué qui.
        Set<Long> bloques = new HashSet<>(blocageRepository.idsBloquesPar(id));
        bloques.addAll(blocageRepository.idsQuiOntBloque(id));

        return new VueRelations(
                id, moi.getRole() == Role.ADMIN, sortants, abonnesAcceptes, mOntEcrit, bloques);
    }
}

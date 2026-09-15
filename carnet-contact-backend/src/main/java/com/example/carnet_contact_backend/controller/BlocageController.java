package com.example.carnet_contact_backend.controller;

import com.example.carnet_contact_backend.abonnement.Notifications;
import com.example.carnet_contact_backend.model.Blocage;
import com.example.carnet_contact_backend.model.Utilisateur;
import com.example.carnet_contact_backend.repository.AbonnementRepository;
import com.example.carnet_contact_backend.repository.BlocageRepository;
import com.example.carnet_contact_backend.repository.UtilisateurRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

/**
 * Bloquer un compte : la mesure forte.
 *
 * Retirer un abonné (AbonnementController) laisse la porte ouverte : il pourra
 * redemander. Bloquer la ferme dans les deux sens. Tant que le blocage existe,
 * aucun des deux comptes ne voit l'autre ni n'interagit avec lui — la règle
 * elle-même vit dans VueRelations, ce contrôleur ne fait que poser et retirer
 * la ligne.
 */
@RestController
@RequestMapping("/api/blocages")
public class BlocageController {

    private final UtilisateurRepository utilisateurRepository;
    private final BlocageRepository blocageRepository;
    private final AbonnementRepository abonnementRepository;
    private final Notifications notifications;

    public BlocageController(
            UtilisateurRepository utilisateurRepository,
            BlocageRepository blocageRepository,
            AbonnementRepository abonnementRepository,
            Notifications notifications) {
        this.utilisateurRepository = utilisateurRepository;
        this.blocageRepository = blocageRepository;
        this.abonnementRepository = abonnementRepository;
        this.notifications = notifications;
    }

    private Utilisateur utilisateurConnecte(String email) {
        return utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    /**
     * Bloquer, et effacer ce qui reliait les deux comptes : abonnements et
     * notifications, dans les deux sens.
     *
     * Le blocage est enregistré EN PREMIER. Les deux suppressions qui suivent
     * écrivent d'abord ce qui est en attente (flushAutomatically), puis vident le
     * contexte : le blocage est donc déjà en base quand elles s'exécutent.
     */
    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> bloquer(@PathVariable Long id, @AuthenticationPrincipal String email) {
        Utilisateur moi = utilisateurConnecte(email);

        if (moi.getId().equals(id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vous ne pouvez pas vous bloquer vous-même.");
        }

        Utilisateur cible = utilisateurRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (!blocageRepository.existsByBloqueurIdAndBloqueId(moi.getId(), id)) {
            Blocage blocage = new Blocage();
            blocage.setBloqueur(moi);
            blocage.setBloque(cible);
            blocage.setDateBlocage(Instant.now());
            blocageRepository.save(blocage);
        }

        abonnementRepository.supprimerEntre(moi.getId(), id);
        notifications.retirerEntre(moi.getId(), id);

        return ResponseEntity.noContent().build();
    }

    /** Débloquer ne restaure rien : ni abonnement, ni notification. */
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> debloquer(@PathVariable Long id, @AuthenticationPrincipal String email) {
        Utilisateur moi = utilisateurConnecte(email);
        blocageRepository.supprimerPar(moi.getId(), id);
        return ResponseEntity.noContent().build();
    }

    /** Les comptes que J'AI bloqués. Celui qui est bloqué, lui, n'en sait rien. */
    @GetMapping
    public List<AuteurPublic> mesBlocages(@AuthenticationPrincipal String email) {
        Utilisateur moi = utilisateurConnecte(email);

        return blocageRepository.parBloqueur(moi.getId()).stream()
                .map(Blocage::getBloque)
                .sorted(Comparator.comparing(Utilisateur::getNomAffichage, String.CASE_INSENSITIVE_ORDER))
                .map(AuteurPublic::de)
                .toList();
    }
}

package com.example.carnet_contact_backend.repository;

import com.example.carnet_contact_backend.model.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {

    // REQUÊTES DÉRIVÉES : Spring Data lit le NOM de la méthode et écrit le SQL
    // tout seul. findByEmail devient "SELECT ... WHERE email = ?".
    // Optional<> plutôt que null : le type dit explicitement que le résultat
    // peut être absent, et le compilateur force à traiter ce cas.
    Optional<Utilisateur> findByEmail(String email);

    boolean existsByEmail(String email);

    // "Not" dans le nom se traduit par "WHERE id <> ?" : tous les comptes
    // sauf celui passé en paramètre (pour lister ses interlocuteurs possibles).
    List<Utilisateur> findByIdNotOrderByNomAffichageAsc(Long id);
}

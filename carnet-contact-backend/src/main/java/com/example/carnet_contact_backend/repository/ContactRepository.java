package com.example.carnet_contact_backend.repository;

import com.example.carnet_contact_backend.model.Contact;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ContactRepository extends JpaRepository<Contact, Long> {

    /**
     * Une PAGE des contacts d'un utilisateur, filtrée par un terme de
     * recherche libre.
     *
     * Pageable n'est pas un paramètre comme les autres : Spring Data le
     * reconnaît et le traduit lui-même en LIMIT / OFFSET / ORDER BY. La
     * requête écrite ici ne contient donc AUCUNE notion de page — on décrit
     * seulement « quelles lignes », le découpage est ajouté par-dessus.
     *
     * Le terme vide est traité dans la requête (`:recherche = ''`) plutôt que
     * par une seconde méthode « sans filtre » : un seul chemin de code, donc
     * une seule chose à maintenir.
     *
     * LIKE CONCAT('%', :recherche, '%') : « contient ». Les deux LOWER()
     * rendent la comparaison insensible à la casse — H2 ne l'est pas par
     * défaut sur toutes les collations.
     */
    @Query("""
            SELECT c FROM Contact c
            WHERE c.proprietaire.id = :proprietaireId
              AND (:recherche = ''
                   OR LOWER(c.nom)    LIKE LOWER(CONCAT('%', :recherche, '%'))
                   OR LOWER(c.prenom) LIKE LOWER(CONCAT('%', :recherche, '%'))
                   OR LOWER(c.email)  LIKE LOWER(CONCAT('%', :recherche, '%')))
            """)
    Page<Contact> rechercher(
            @Param("proprietaireId") Long proprietaireId,
            @Param("recherche") String recherche,
            Pageable pagination);

    // Recherche par id ET par propriétaire. C'est la protection contre l'accès
    // au contact d'un autre : demander /api/contacts/42 quand le 42 n'est pas
    // à soi ne renvoie pas 42, il ne renvoie rien.
    Optional<Contact> findByIdAndProprietaireId(Long id, Long proprietaireId);
}

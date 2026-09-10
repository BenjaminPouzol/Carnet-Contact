package com.example.carnet_contact_backend.repository;

import com.example.carnet_contact_backend.model.Contact;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ContactRepository extends JpaRepository<Contact, Long> {

    // Les contacts d'un utilisateur donné. Toute lecture passe désormais par
    // là : findAll() ne doit plus être utilisé, il renverrait les contacts de
    // tout le monde.
    List<Contact> findByProprietaireIdOrderByNomAsc(Long proprietaireId);

    // Recherche par id ET par propriétaire. C'est la protection contre l'accès
    // au contact d'un autre : demander /api/contacts/42 quand le 42 n'est pas
    // à soi ne renvoie pas 42, il ne renvoie rien.
    Optional<Contact> findByIdAndProprietaireId(Long id, Long proprietaireId);
}

package com.example.carnet_contact_backend.repository;

import com.example.carnet_contact_backend.model.Blocage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BlocageRepository extends JpaRepository<Blocage, Long> {

    boolean existsByBloqueurIdAndBloqueId(Long bloqueurId, Long bloqueId);

    // Les deux sens d'un blocage, en deux requêtes simples que Relations réunit.
    @Query("SELECT b.bloque.id FROM Blocage b WHERE b.bloqueur.id = :id")
    List<Long> idsBloquesPar(@Param("id") Long id);

    @Query("SELECT b.bloqueur.id FROM Blocage b WHERE b.bloque.id = :id")
    List<Long> idsQuiOntBloque(@Param("id") Long id);

    // La liste « Comptes bloqués » : seul le bloqueur la voit.
    @Query("SELECT b FROM Blocage b JOIN FETCH b.bloque WHERE b.bloqueur.id = :id")
    List<Blocage> parBloqueur(@Param("id") Long id);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM Blocage b WHERE b.bloqueur.id = :bloqueur AND b.bloque.id = :bloque")
    void supprimerPar(@Param("bloqueur") Long bloqueur, @Param("bloque") Long bloque);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM Blocage b WHERE b.bloqueur.id = :id OR b.bloque.id = :id")
    void supprimerCeuxDe(@Param("id") Long id);
}

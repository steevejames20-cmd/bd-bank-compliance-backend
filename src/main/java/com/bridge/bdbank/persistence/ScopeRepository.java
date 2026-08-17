package com.bridge.bdbank.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository JPA pour l'entité Scope.
 * Fournit les méthodes CRUD de base et des requêtes personnalisées pour la gestion des périmètres.
 */
@Repository
public interface ScopeRepository extends JpaRepository<Scope, Long> {

    /**
     * Trouve le périmètre actif actuellement.
     */
    Optional<Scope> findByActiveTrue();

    /**
     * Trouve un périmètre par son nom.
     */
    Optional<Scope> findByName(String name);

    /**
     * Désactive tous les périmètres (utile avant d'en acter un nouveau).
     */
    @Modifying
    @Query("UPDATE Scope s SET s.active = false WHERE s.active = true")
    void deactivateAll();
}
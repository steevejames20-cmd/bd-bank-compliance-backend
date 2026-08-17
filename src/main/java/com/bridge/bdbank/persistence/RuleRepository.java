package com.bridge.bdbank.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository JPA pour l'entité Rule.
 * Fournit les méthodes CRUD de base et des requêtes personnalisées.
 */
@Repository
public interface RuleRepository extends JpaRepository<Rule, Long> {

    /**
     * Trouve toutes les règles actives.
     */
    List<Rule> findByActiveTrue();

    /**
     * Trouve une règle par son texte DSL exact.
     */
    Optional<Rule> findByDslText(String dslText);

    /**
     * Trouve toutes les règles pour une table cible donnée.
     */
    List<Rule> findByTargetTable(String targetTable);

    /**
     * Trouve toutes les règles actives pour une table cible donnée.
     */
    List<Rule> findByTargetTableAndActiveTrue(String targetTable);

    /**
     * Compte le nombre de règles actives.
     */
    long countByActiveTrue();
}
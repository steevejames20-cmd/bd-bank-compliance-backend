package com.bridge.bdbank.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository pour les mappings de renommage des tables et colonnes.
 */
@Repository
public interface TableMappingRepository extends JpaRepository<TableMapping, Long> {

    /**
     * Trouve tous les mappings actifs pour une table donnée.
     */
    List<TableMapping> findByOriginalTableNameAndActiveTrue(String originalTableName);

    /**
     * Trouve le mapping de renommage pour une table spécifique.
     */
    Optional<TableMapping> findByOriginalTableNameAndOriginalColumnNameIsNullAndActiveTrue(String originalTableName);

    /**
     * Trouve le mapping de renommage pour une colonne spécifique d'une table.
     */
    Optional<TableMapping> findByOriginalTableNameAndOriginalColumnNameAndActiveTrue(
            String originalTableName, String originalColumnName);

    /**
     * Vérifie si un mapping existe pour une combinaison table/colonne.
     */
    boolean existsByOriginalTableNameAndOriginalColumnNameAndActiveTrue(
            String originalTableName, String originalColumnName);
}

package com.bridge.bdbank.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository JPA pour l'entité SchemaAlias.
 */
@Repository
public interface SchemaAliasRepository extends JpaRepository<SchemaAlias, Long> {

    /**
     * Tous les alias définis pour une table donnée (alias de table inclus).
     */
    List<SchemaAlias> findByRealTableName(String realTableName);

    /**
     * L'alias de la table elle-même (realColumnName null), s'il existe.
     */
    Optional<SchemaAlias> findByRealTableNameAndRealColumnNameIsNull(String realTableName);

    /**
     * L'alias d'une colonne précise d'une table, s'il existe.
     */
    Optional<SchemaAlias> findByRealTableNameAndRealColumnName(String realTableName, String realColumnName);
}

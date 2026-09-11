package com.bridge.bdbank.introspection;

import com.bridge.bdbank.persistence.SchemaAlias;
import com.bridge.bdbank.persistence.SchemaAliasRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Gère les alias d'affichage posés par l'administrateur sur les tables et
 * colonnes de la bd_bank, dans l'espace "Schéma &amp; Périmètre".
 * <p>
 * Un alias ne modifie jamais la bd_bank : Bridge y est strictement en
 * lecture (voir SchemaIntrospectionService). L'alias ne vit que dans la
 * persistance interne de l'outil et ne change que l'affichage côté
 * interface, à la place du nom technique réel (ex: "sld-cli" -> "solde").
 */
@Service
@RequiredArgsConstructor
public class SchemaAliasService {

    private static final Logger log = LoggerFactory.getLogger(SchemaAliasService.class);

    private final SchemaIntrospectionService schemaIntrospectionService;
    private final SchemaAliasRepository schemaAliasRepository;

    /**
     * Liste toutes les tables de la bd_bank avec leur nom d'affichage
     * (alias si défini, sinon le nom technique réel).
     */
    public List<AliasedTableInfo> listAliasedTables() {
        List<TableInfo> tables = schemaIntrospectionService.listTables();

        Map<String, String> displayNamesByTable = schemaAliasRepository.findAll().stream()
            .filter(alias -> alias.getRealColumnName() == null)
            .collect(Collectors.toMap(SchemaAlias::getRealTableName, SchemaAlias::getDisplayName));

        return tables.stream()
            .map(table -> new AliasedTableInfo(
                table.name(),
                displayNamesByTable.getOrDefault(table.name(), table.name())))
            .toList();
    }

    /**
     * Liste les colonnes d'une table avec leur nom d'affichage (alias si
     * défini, sinon le nom technique réel).
     *
     * @throws com.bridge.bdbank.introspection.TableNotFoundException si la table n'existe pas dans la bd_bank
     */
    public List<AliasedColumnInfo> listAliasedColumns(String tableName) {
        List<ColumnInfo> columns = schemaIntrospectionService.listColumns(tableName);

        Map<String, String> displayNamesByColumn = schemaAliasRepository.findByRealTableName(tableName).stream()
            .filter(alias -> alias.getRealColumnName() != null)
            .collect(Collectors.toMap(SchemaAlias::getRealColumnName, SchemaAlias::getDisplayName));

        return columns.stream()
            .map(column -> new AliasedColumnInfo(
                column.name(),
                displayNamesByColumn.getOrDefault(column.name(), column.name()),
                column.typeName(),
                column.nullable()))
            .toList();
    }

    /**
     * Liste tous les alias définis, sans filtre (vue brute pour l'administration).
     */
    public List<SchemaAlias> listAllAliases() {
        return schemaAliasRepository.findAll();
    }

    /**
     * Pose ou met à jour l'alias d'une table.
     *
     * @throws com.bridge.bdbank.introspection.TableNotFoundException si la table n'existe pas dans la bd_bank
     */
    @Transactional
    public SchemaAlias upsertTableAlias(String tableName, String displayName) {
        assertTableExists(tableName);

        SchemaAlias alias = schemaAliasRepository.findByRealTableNameAndRealColumnNameIsNull(tableName)
            .orElseGet(() -> SchemaAlias.builder()
                .realTableName(tableName)
                .realColumnName(null)
                .build());
        alias.setDisplayName(displayName);

        SchemaAlias saved = schemaAliasRepository.save(alias);
        log.info("Alias de table posé : '{}' -> '{}'", tableName, displayName);
        return saved;
    }

    /**
     * Pose ou met à jour l'alias d'une colonne d'une table.
     *
     * @throws com.bridge.bdbank.introspection.TableNotFoundException si la table n'existe pas dans la bd_bank
     * @throws IllegalArgumentException si la colonne n'existe pas dans cette table
     */
    @Transactional
    public SchemaAlias upsertColumnAlias(String tableName, String columnName, String displayName) {
        assertColumnExists(tableName, columnName);

        SchemaAlias alias = schemaAliasRepository.findByRealTableNameAndRealColumnName(tableName, columnName)
            .orElseGet(() -> SchemaAlias.builder()
                .realTableName(tableName)
                .realColumnName(columnName)
                .build());
        alias.setDisplayName(displayName);

        SchemaAlias saved = schemaAliasRepository.save(alias);
        log.info("Alias de colonne posé : '{}.{}' -> '{}'", tableName, columnName, displayName);
        return saved;
    }

    /**
     * Indique si un alias existe (utile pour renvoyer un 404 propre côté contrôleur).
     */
    public boolean existsById(Long aliasId) {
        return schemaAliasRepository.existsById(aliasId);
    }

    /**
     * Supprime un alias (la table/colonne réaffiche alors son nom technique réel).
     * Le contrôleur vérifie l'existence au préalable via {@link #existsById}
     * pour renvoyer un 404 propre ; appeler cette méthode sur un ID inconnu
     * lève une EmptyResultDataAccessException (comportement standard de
     * Spring Data JPA).
     */
    @Transactional
    public void deleteAlias(Long aliasId) {
        schemaAliasRepository.deleteById(aliasId);
    }

    private void assertTableExists(String tableName) {
        Set<String> tableNames = schemaIntrospectionService.listTables().stream()
            .map(TableInfo::name)
            .collect(Collectors.toSet());
        if (!tableNames.contains(tableName)) {
            throw new TableNotFoundException(tableName);
        }
    }

    private void assertColumnExists(String tableName, String columnName) {
        // listColumns lève déjà TableNotFoundException si la table n'existe pas.
        Set<String> columnNames = schemaIntrospectionService.listColumns(tableName).stream()
            .map(ColumnInfo::name)
            .collect(Collectors.toSet());
        if (!columnNames.contains(columnName)) {
            throw new IllegalArgumentException(
                "Colonne '" + columnName + "' introuvable dans la table '" + tableName + "'");
        }
    }
}

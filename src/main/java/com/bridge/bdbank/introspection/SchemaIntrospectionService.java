package com.bridge.bdbank.introspection;

import com.bridge.bdbank.connection.SqlErrorTranslator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/**
 * Module d'introspection : liste les tables et colonnes de la bd_bank,
 * uniquement via l'API JDBC standard ({@link DatabaseMetaData}) — aucune
 * requête SQL écrite à la main, donc aucun risque d'injection ici, et un
 * code qui fonctionne pareil sur MySQL ou PostgreSQL.
 *
 * Utilise la datasource secondaire (bankDataSource) pour se connecter à la
 * bd_bank, distincte de la datasource principale H2 pour la persistance.
 */
@Service
public class SchemaIntrospectionService {

    private static final Logger log = LoggerFactory.getLogger(SchemaIntrospectionService.class);

    private final DataSource dataSource;

    public SchemaIntrospectionService(@Qualifier("bankDataSource") DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Liste les tables "normales" (on exclut les vues et tables système)
     * de la base actuellement configurée.
     */
    public List<TableInfo> listTables() {
        List<TableInfo> tables = new ArrayList<>();

        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String catalog = connection.getCatalog();

            // catalog = la base actuelle (ex. "bd_bank_test") -> on ne voit
            // que ses propres tables, jamais celles d'autres bases sur le
            // même serveur. schemaPattern à null : peu importe le schéma
            // (utile pour PostgreSQL, où les tables vivent dans "public").
            try (ResultSet rs = metaData.getTables(catalog, null, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    tables.add(new TableInfo(rs.getString("TABLE_NAME"), rs.getString("TABLE_CAT")));
                }
            }
        } catch (SQLException e) {
            throw SqlErrorTranslator.translate(e);
        }

        log.info("Introspection : {} table(s) trouvée(s) -> {}", tables.size(), tables);
        return tables;
    }

    /**
     * Liste les colonnes d'une table donnée, avec leur type SQL.
     *
     * @throws TableNotFoundException si la table n'existe pas dans la bd_bank.
     */
    public List<ColumnInfo> listColumns(String tableName) {
        List<ColumnInfo> columns = new ArrayList<>();

        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String catalog = connection.getCatalog();

            try (ResultSet rs = metaData.getColumns(catalog, null, tableName, "%")) {
                while (rs.next()) {
                    columns.add(new ColumnInfo(
                            rs.getString("COLUMN_NAME"),
                            rs.getString("TYPE_NAME"),
                            rs.getInt("DATA_TYPE"),
                            "YES".equalsIgnoreCase(rs.getString("IS_NULLABLE"))
                    ));
                }
            }
        } catch (SQLException e) {
            throw SqlErrorTranslator.translate(e);
        }

        if (columns.isEmpty()) {
            throw new TableNotFoundException(tableName);
        }

        log.info("Introspection : {} colonne(s) pour la table '{}' -> {}", columns.size(), tableName, columns);
        return columns;
    }

    /**
     * Récupère les colonnes formant la clé primaire d'une table, dans
     * l'ordre de la clé (important pour les clés composites).
     *
     * @throws MissingPrimaryKeyException si la table n'a aucune clé primaire déclarée.
     */
    public List<String> getPrimaryKeyColumns(String tableName) {
        // TreeMap triée par KEY_SEQ : getPrimaryKeys() ne garantit pas l'ordre
        // des lignes retournées, alors que l'ordre d'une clé composite compte
        // (ex: (compte_id, date) et non l'inverse).
        var columnsBySequence = new TreeMap<Short, String>();

        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String catalog = connection.getCatalog();

            try (ResultSet rs = metaData.getPrimaryKeys(catalog, null, tableName)) {
                while (rs.next()) {
                    columnsBySequence.put(rs.getShort("KEY_SEQ"), rs.getString("COLUMN_NAME"));
                }
            }
        } catch (SQLException e) {
            throw SqlErrorTranslator.translate(e);
        }

        if (columnsBySequence.isEmpty()) {
            throw new MissingPrimaryKeyException(tableName);
        }

        List<String> columns = new ArrayList<>(columnsBySequence.values());
        log.info("Introspection : clé primaire de '{}' -> {}", tableName, columns);
        return columns;
    }
}

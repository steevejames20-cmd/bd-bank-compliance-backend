package com.bridge.bdbank.introspection;

import com.bridge.bdbank.connection.SqlErrorTranslator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Module d'introspection : liste les tables et colonnes de la bd_bank,
 * uniquement via l'API JDBC standard ({@link DatabaseMetaData}) — aucune
 * requête SQL écrite à la main, donc aucun risque d'injection ici, et un
 * code qui fonctionne pareil sur MySQL ou PostgreSQL.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SchemaIntrospectionService {

    private final DataSource dataSource;

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
}

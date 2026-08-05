package com.bridge.bdbank.connection;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

/**
 * Couche de connexion générique à la bd_bank.
 *
 * Écrite volontairement avec les seules API standard JDBC
 * ({@link java.sql}/{@link javax.sql}) : aucun appel spécifique à MySQL ou
 * PostgreSQL ici. Le SGBD réellement utilisé ne dépend que de la
 * configuration (driver + URL dans application.yml), jamais de ce code.
 *
 * Le {@link DataSource} injecté est celui configuré automatiquement par
 * Spring Boot (pool HikariCP) à partir de spring.datasource.* : on ne
 * rouvre pas une connexion à chaque appel, on emprunte au pool.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JdbcConnectionService {

    private final DataSource dataSource;

    /**
     * Ouvre une connexion via le pool et vérifie qu'elle est utilisable.
     *
     * @return les informations du SGBD cible (nom, version, URL), utiles
     * pour les logs et le futur endpoint de diagnostic.
     * @throws DatabaseConnectionException si la connexion échoue, avec un
     * message adapté au type de problème (base injoignable, identifiants
     * invalides, base inexistante...). La {@link SQLException} d'origine
     * reste accessible via {@code getCause()} pour le débogage.
     */
    public DatabaseInfo testConnection() {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            DatabaseInfo info = new DatabaseInfo(
                    metaData.getDatabaseProductName(),
                    metaData.getDatabaseProductVersion(),
                    metaData.getURL()
            );
            log.info("Connexion bd_bank OK -> {} {} ({})",
                    info.productName(), info.productVersion(), info.url());
            return info;
        } catch (SQLException e) {
            DatabaseConnectionException translated = SqlErrorTranslator.translate(e);
            log.error(translated.getMessage(), e);
            throw translated;
        }
    }

    /**
     * Infos de connexion, indépendantes du SGBD (même structure que ce
     * soit MySQL ou PostgreSQL derrière).
     */
    public record DatabaseInfo(String productName, String productVersion, String url) {
    }
}

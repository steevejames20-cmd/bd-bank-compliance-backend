package com.bridge.bdbank.connection;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Vérifie la connexion à la bd_bank au démarrage de l'application, pour
 * avoir un retour immédiat et visible (dans les logs) que la config J1/J2
 * (base de test + compte lecture seule + datasource) fonctionne de bout
 * en bout.
 * <p>
 * Si la connexion échoue, on logue un bloc clairement visible avec le
 * message actionnable (voir {@link JdbcConnectionService#testConnection()})
 * avant de stopper le démarrage (fail-fast) : un outil de conformité qui
 * ne peut pas lire la bd_bank ne doit pas démarrer "à moitié".
 * <p>
 * {@code @Order(1)} : doit s'exécuter avant StartupSchemaIntrospection (J3)
 * — inutile de tenter l'introspection si la connexion elle-même échoue.
 */
@Component
@Order(1)
@ConditionalOnProperty(name = "bdbank.startup.checks.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class StartupConnectionCheck implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StartupConnectionCheck.class);

    private final JdbcConnectionService jdbcConnectionService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcConnectionService.testConnection();
        } catch (DatabaseConnectionException e) {
            log.error("==================================================");
            log.error("ÉCHEC DE CONNEXION À LA BD_BANK AU DÉMARRAGE");
            log.error(e.getMessage());
            log.error("==================================================");
            throw e;
        }
    }
}

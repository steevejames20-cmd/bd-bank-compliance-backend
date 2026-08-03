package com.bridge.bdbank.connection;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Vérifie la connexion à la bd_bank au démarrage de l'application, pour
 * avoir un retour immédiat et visible (dans les logs) que la config J1/J2
 * (base de test + compte lecture seule + datasource) fonctionne de bout
 * en bout.
 * <p>
 * Purement diagnostique à ce stade : si la connexion échoue, l'exception
 * remonte telle quelle et empêche le démarrage (fail-fast). Une gestion
 * d'erreur plus fine et un message clair pour l'admin arrivent en J4.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StartupConnectionCheck implements ApplicationRunner {

    private final JdbcConnectionService jdbcConnectionService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        jdbcConnectionService.testConnection();
    }
}

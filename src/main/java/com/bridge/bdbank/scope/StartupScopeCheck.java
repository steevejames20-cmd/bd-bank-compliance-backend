package com.bridge.bdbank.scope;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Affiche au démarrage les tables réellement surveillées, une fois
 * validées contre le schéma réel.
 * <p>
 * {@code @Order(3)} : après la connexion (1) et l'introspection brute (2)
 * - le périmètre n'a de sens que si les deux précédents ont réussi.
 */
@Slf4j
@Component
@Order(3)
@ConditionalOnProperty(name = "bdbank.startup.checks.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class StartupScopeCheck implements ApplicationRunner {

    private final ScopeService scopeService;

    @Override
    public void run(ApplicationArguments args) {
        scopeService.getScopedTables();
    }
}

package com.bridge.bdbank.scope;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
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
@RequiredArgsConstructor
public class StartupScopeCheck implements ApplicationRunner {

    private final ScopeService scopeService;

    @Override
    public void run(ApplicationArguments args) {
        scopeService.getScopedTables();
    }
}

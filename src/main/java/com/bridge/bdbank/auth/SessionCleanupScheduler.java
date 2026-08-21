package com.bridge.bdbank.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler pour le nettoyage automatique des sessions expirées.
 * S'exécute toutes les heures pour nettoyer les sessions inactives.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SessionCleanupScheduler {

    private final AuthenticationService authenticationService;

    /**
     * Nettoie les sessions expirées toutes les heures.
     */
    @Scheduled(fixedRate = 3600000) // 1 heure en millisecondes
    public void cleanupExpiredSessions() {
        log.debug("Démarrage du nettoyage des sessions expirées");
        authenticationService.cleanupExpiredSessions();
    }
}
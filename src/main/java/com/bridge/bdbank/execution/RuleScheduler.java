package com.bridge.bdbank.execution;

import com.bridge.bdbank.persistence.FrequencyConfig;
import com.bridge.bdbank.persistence.FrequencyConfig.FrequencyType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ScheduledFuture;

/**
 * Scheduler pour l'exécution automatique des règles de conformité.
 * Supporte les intervalles simples et les expressions cron avec un minimum de 3 minutes.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RuleScheduler {

    private final RuleExecutionService ruleExecutionService;
    private final FrequencyConfigService frequencyConfigService;
    private final TaskScheduler taskScheduler;

    private ScheduledFuture<?> scheduledTask;

    /**
     * Exécution planifiée par défaut vérifiant si une configuration active existe.
     * Cette méthode s'exécute toutes les minutes pour vérifier les changements de configuration.
     */
    @Scheduled(fixedRate = 60000) // Toutes les minutes
    public void checkAndExecute() {
        frequencyConfigService.getActiveConfig().ifPresentOrElse(
            this::executeWithConfig,
            () -> log.debug("Aucune configuration de fréquence active, aucune exécution planifiée")
        );
    }

    /**
     * Exécute les règles selon la configuration fournie.
     * 
     * @param config La configuration de fréquence active
     */
    private void executeWithConfig(FrequencyConfig config) {
        if (!config.getActive()) {
            log.debug("Configuration inactive, aucune exécution");
            return;
        }

        try {
            log.info("Début de l'exécution planifiée des règles - {}", 
                frequencyConfigService.getActiveConfigDescription());

            int alertsGenerated = ruleExecutionService.executeAllActiveRules();

            // Mettre à jour la date de dernière exécution
            frequencyConfigService.updateLastExecution();

            log.info("Exécution planifiée terminée - {} alerte(s) générée(s)", alertsGenerated);

        } catch (Exception e) {
            log.error("Erreur lors de l'exécution planifiée des règles", e);
        }
    }

    /**
     * Met à jour dynamiquement la planification selon la configuration active.
     * Cette méthode peut être appelée quand la configuration change.
     */
    public void reschedule() {
        // Annuler la tâche existante si elle y a
        if (scheduledTask != null && !scheduledTask.isCancelled()) {
            scheduledTask.cancel(false);
            log.info("Tâche planifiée existante annulée");
        }

        frequencyConfigService.getActiveConfig().ifPresent(config -> {
            if (!config.getActive()) {
                log.info("Configuration inactive, pas de nouvelle planification");
                return;
            }

            Instant startTime = Instant.now().plus(Duration.ofMinutes(1)); // Commencer dans 1 minute

            switch (config.getType()) {
                case INTERVAL:
                    Duration interval = Duration.ofMinutes(config.getIntervalMinutes());
                    scheduledTask = taskScheduler.scheduleAtFixedRate(
                        this::executeScheduledTask,
                        startTime,
                        interval
                    );
                    log.info("Nouvelle planification configurée - Intervalle: {} minutes", 
                        config.getIntervalMinutes());
                    break;

                case CRON:
                    try {
                        CronTrigger cronTrigger = new CronTrigger(config.getCronExpression());
                        scheduledTask = taskScheduler.schedule(
                            this::executeScheduledTask,
                            cronTrigger
                        );
                        log.info("Nouvelle planification configurée - Expression cron: {}", 
                            config.getCronExpression());
                    } catch (IllegalArgumentException e) {
                        log.error("Expression cron invalide: {}", config.getCronExpression(), e);
                    }
                    break;

                default:
                    log.warn("Type de fréquence non supporté: {}", config.getType());
            }
        });
    }

    /**
     * Méthode wrapper pour l'exécution planifiée.
     */
    private void executeScheduledTask() {
        frequencyConfigService.getActiveConfig().ifPresent(this::executeWithConfig);
    }

    /**
     * Arrête le scheduler.
     */
    public void stopScheduler() {
        if (scheduledTask != null && !scheduledTask.isCancelled()) {
            scheduledTask.cancel(false);
            log.info("Scheduler arrêté");
        }
    }

    /**
     * Vérifie si le scheduler est actif.
     * 
     * @return true si une tâche est planifiée et active
     */
    public boolean isSchedulerActive() {
        return scheduledTask != null && !scheduledTask.isCancelled() && !scheduledTask.isDone();
    }
}
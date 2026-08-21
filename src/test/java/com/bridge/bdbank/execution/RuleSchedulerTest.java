package com.bridge.bdbank.execution;

import com.bridge.bdbank.persistence.FrequencyConfig;
import com.bridge.bdbank.persistence.FrequencyConfig.FrequencyType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.TaskScheduler;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour le RuleScheduler.
 * Verifie le fonctionnement du planificateur de regles.
 */
@ExtendWith(MockitoExtension.class)
class RuleSchedulerTest {

    @Mock
    private RuleExecutionService ruleExecutionService;

    @Mock
    private FrequencyConfigService frequencyConfigService;

    @Mock
    private TaskScheduler taskScheduler;

    private RuleScheduler ruleScheduler;

    @BeforeEach
    void setUp() {
        ruleScheduler = new RuleScheduler(ruleExecutionService, frequencyConfigService, taskScheduler);
    }

    @Test
    void shouldExecuteWithIntervalConfig() {
        // Setup
        FrequencyConfig config = FrequencyConfig.builder()
            .id(1L)
            .type(FrequencyType.INTERVAL)
            .intervalMinutes(5)
            .active(true)
            .build();

        when(frequencyConfigService.getActiveConfig()).thenReturn(Optional.of(config));
        when(frequencyConfigService.getActiveConfigDescription())
            .thenReturn("Execution toutes les 5 minutes");
        when(ruleExecutionService.executeAllActiveRules()).thenReturn(3);
        doNothing().when(frequencyConfigService).updateLastExecution();

        // Exécution
        ruleScheduler.checkAndExecute();

        // Vérifications
        verify(ruleExecutionService).executeAllActiveRules();
        verify(frequencyConfigService).updateLastExecution();
    }

    @Test
    void shouldExecuteWithCronConfig() {
        // Setup
        FrequencyConfig config = FrequencyConfig.builder()
            .id(1L)
            .type(FrequencyType.CRON)
            .cronExpression("0 */5 * * * *")
            .active(true)
            .build();

        when(frequencyConfigService.getActiveConfig()).thenReturn(Optional.of(config));
        when(frequencyConfigService.getActiveConfigDescription())
            .thenReturn("Execution selon l'expression cron: 0 */5 * * * *");
        when(ruleExecutionService.executeAllActiveRules()).thenReturn(2);
        doNothing().when(frequencyConfigService).updateLastExecution();

        // Exécution
        ruleScheduler.checkAndExecute();

        // Vérifications
        verify(ruleExecutionService).executeAllActiveRules();
        verify(frequencyConfigService).updateLastExecution();
    }

    @Test
    void shouldNotExecuteWhenNoActiveConfig() {
        when(frequencyConfigService.getActiveConfig()).thenReturn(Optional.empty());

        ruleScheduler.checkAndExecute();

        verify(ruleExecutionService, never()).executeAllActiveRules();
        verify(frequencyConfigService, never()).updateLastExecution();
    }

    @Test
    void shouldNotExecuteWhenConfigInactive() {
        // Setup
        FrequencyConfig config = FrequencyConfig.builder()
            .id(1L)
            .type(FrequencyType.INTERVAL)
            .intervalMinutes(5)
            .active(false)
            .build();

        when(frequencyConfigService.getActiveConfig()).thenReturn(Optional.of(config));

        // Exécution
        ruleScheduler.checkAndExecute();

        // Vérifications
        verify(ruleExecutionService, never()).executeAllActiveRules();
        verify(frequencyConfigService, never()).updateLastExecution();
    }

    @Test
    void shouldHandleExecutionErrors() {
        // Setup
        FrequencyConfig config = FrequencyConfig.builder()
            .id(1L)
            .type(FrequencyType.INTERVAL)
            .intervalMinutes(5)
            .active(true)
            .build();

        when(frequencyConfigService.getActiveConfig()).thenReturn(Optional.of(config));
        when(frequencyConfigService.getActiveConfigDescription())
            .thenReturn("Execution toutes les 5 minutes");
        when(ruleExecutionService.executeAllActiveRules())
            .thenThrow(new RuntimeException("Erreur de connexion"));

        // Exécution
        ruleScheduler.checkAndExecute();

        // Vérifications - l'erreur doit être gérée sans exception
        verify(ruleExecutionService).executeAllActiveRules();
        verify(frequencyConfigService, never()).updateLastExecution();
    }

    @Test
    void shouldRescheduleWithInterval() {
        // Setup
        FrequencyConfig config = FrequencyConfig.builder()
            .id(1L)
            .type(FrequencyType.INTERVAL)
            .intervalMinutes(5)
            .active(true)
            .build();

        when(frequencyConfigService.getActiveConfig()).thenReturn(Optional.of(config));
        when(taskScheduler.scheduleAtFixedRate(any(Runnable.class), any(Instant.class), any(Duration.class)))
            .thenReturn(null);

        // Exécution
        ruleScheduler.reschedule();

        // Vérifications
        verify(taskScheduler).scheduleAtFixedRate(any(Runnable.class), any(Instant.class), eq(Duration.ofMinutes(5)));
    }

    @Test
    void shouldRescheduleWithCron() {
        // Setup
        FrequencyConfig config = FrequencyConfig.builder()
            .id(1L)
            .type(FrequencyType.CRON)
            .cronExpression("0 */5 * * * *")
            .active(true)
            .build();

        when(frequencyConfigService.getActiveConfig()).thenReturn(Optional.of(config));

        // Exécution - verification simple que la méthode ne lance pas d'exception
        ruleScheduler.reschedule();
    }

    @Test
    void shouldNotRescheduleWhenConfigInactive() {
        // Setup
        FrequencyConfig config = FrequencyConfig.builder()
            .id(1L)
            .type(FrequencyType.INTERVAL)
            .intervalMinutes(5)
            .active(false)
            .build();

        when(frequencyConfigService.getActiveConfig()).thenReturn(Optional.of(config));

        // Exécution
        ruleScheduler.reschedule();

        // Vérifications
        verify(taskScheduler, never()).scheduleAtFixedRate(any(), any(), any());
    }

    @Test
    void shouldNotRescheduleWhenNoConfig() {
        when(frequencyConfigService.getActiveConfig()).thenReturn(Optional.empty());

        ruleScheduler.reschedule();

        verify(taskScheduler, never()).scheduleAtFixedRate(any(), any(), any());
    }

    @Test
    void shouldStopScheduler() {
        // Note: ce test est limite car on ne peut pas facilement simuler l'etat interne
        // On verifie simplement que la methode ne lance pas d'exception
        ruleScheduler.stopScheduler();
    }

    @Test
    void shouldReturnFalseWhenSchedulerNotActive() {
        // Par defaut, le scheduler ne devrait pas etre actif
        boolean isActive = ruleScheduler.isSchedulerActive();
        assertThat(isActive).isFalse();
    }
}
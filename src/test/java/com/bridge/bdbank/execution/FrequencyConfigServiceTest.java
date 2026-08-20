package com.bridge.bdbank.execution;

import com.bridge.bdbank.persistence.FrequencyConfig;
import com.bridge.bdbank.persistence.FrequencyConfigRepository;
import com.bridge.bdbank.persistence.FrequencyConfig.FrequencyType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour le FrequencyConfigService.
 * Verifie la gestion de la configuration de frequence avec validation.
 */
@ExtendWith(MockitoExtension.class)
class FrequencyConfigServiceTest {

    @Mock
    private FrequencyConfigRepository frequencyConfigRepository;

    private FrequencyConfigService frequencyConfigService;

    @BeforeEach
    void setUp() {
        frequencyConfigService = new FrequencyConfigService(frequencyConfigRepository);
    }

    @Test
    void shouldGetActiveConfig() {
        // Setup
        FrequencyConfig config = FrequencyConfig.builder()
            .id(1L)
            .type(FrequencyType.INTERVAL)
            .intervalMinutes(5)
            .active(true)
            .build();

        when(frequencyConfigRepository.findByActiveTrue()).thenReturn(Optional.of(config));

        // Exécution
        Optional<FrequencyConfig> result = frequencyConfigService.getActiveConfig();

        // Vérifications
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
        assertThat(result.get().getType()).isEqualTo(FrequencyType.INTERVAL);
        verify(frequencyConfigRepository).findByActiveTrue();
    }

    @Test
    void shouldReturnEmptyWhenNoActiveConfig() {
        when(frequencyConfigRepository.findByActiveTrue()).thenReturn(Optional.empty());

        Optional<FrequencyConfig> result = frequencyConfigService.getActiveConfig();

        assertThat(result).isEmpty();
        verify(frequencyConfigRepository).findByActiveTrue();
    }

    @Test
    void shouldCreateValidIntervalConfig() {
        // Setup
        FrequencyConfig config = FrequencyConfig.builder()
            .type(FrequencyType.INTERVAL)
            .intervalMinutes(5)
            .active(true)
            .build();

        when(frequencyConfigRepository.findByActiveTrue()).thenReturn(Optional.empty());
        when(frequencyConfigRepository.save(any(FrequencyConfig.class))).thenAnswer(invocation -> {
            FrequencyConfig saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        // Exécution
        FrequencyConfig result = frequencyConfigService.createConfig(config);

        // Vérifications
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getType()).isEqualTo(FrequencyType.INTERVAL);
        assertThat(result.getIntervalMinutes()).isEqualTo(5);
        verify(frequencyConfigRepository).save(any(FrequencyConfig.class));
    }

    @Test
    void shouldCreateValidCronConfig() {
        // Setup
        FrequencyConfig config = FrequencyConfig.builder()
            .type(FrequencyType.CRON)
            .cronExpression("0 */5 * * * *")
            .active(true)
            .build();

        when(frequencyConfigRepository.findByActiveTrue()).thenReturn(Optional.empty());
        when(frequencyConfigRepository.save(any(FrequencyConfig.class))).thenAnswer(invocation -> {
            FrequencyConfig saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        // Exécution
        FrequencyConfig result = frequencyConfigService.createConfig(config);

        // Vérifications
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getType()).isEqualTo(FrequencyType.CRON);
        assertThat(result.getCronExpression()).isEqualTo("0 */5 * * * *");
        verify(frequencyConfigRepository).save(any(FrequencyConfig.class));
    }

    @Test
    void shouldThrowExceptionWhenIntervalLessThan3Minutes() {
        FrequencyConfig config = FrequencyConfig.builder()
            .type(FrequencyType.INTERVAL)
            .intervalMinutes(2) // Moins de 3 minutes
            .active(true)
            .build();

        assertThatThrownBy(() -> frequencyConfigService.createConfig(config))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("3 minutes");

        verify(frequencyConfigRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenIntervalWithoutIntervalMinutes() {
        FrequencyConfig config = FrequencyConfig.builder()
            .type(FrequencyType.INTERVAL)
            .intervalMinutes(null)
            .active(true)
            .build();

        assertThatThrownBy(() -> frequencyConfigService.createConfig(config))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("obligatoire");

        verify(frequencyConfigRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenCronWithoutExpression() {
        FrequencyConfig config = FrequencyConfig.builder()
            .type(FrequencyType.CRON)
            .cronExpression(null)
            .active(true)
            .build();

        assertThatThrownBy(() -> frequencyConfigService.createConfig(config))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("obligatoire");

        verify(frequencyConfigRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenCronExpressionInvalid() {
        FrequencyConfig config = FrequencyConfig.builder()
            .type(FrequencyType.CRON)
            .cronExpression("invalid-cron")
            .active(true)
            .build();

        assertThatThrownBy(() -> frequencyConfigService.createConfig(config))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("valide");

        verify(frequencyConfigRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenTypeNull() {
        FrequencyConfig config = FrequencyConfig.builder()
            .type(null)
            .active(true)
            .build();

        assertThatThrownBy(() -> frequencyConfigService.createConfig(config))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("obligatoire");

        verify(frequencyConfigRepository, never()).save(any());
    }

    @Test
    void shouldDeactivatePreviousConfigWhenNewActive() {
        // Setup
        FrequencyConfig existingConfig = FrequencyConfig.builder()
            .id(1L)
            .type(FrequencyType.INTERVAL)
            .intervalMinutes(10)
            .active(true)
            .build();

        FrequencyConfig newConfig = FrequencyConfig.builder()
            .type(FrequencyType.INTERVAL)
            .intervalMinutes(5)
            .active(true)
            .build();

        when(frequencyConfigRepository.findByActiveTrue()).thenReturn(Optional.of(existingConfig));
        when(frequencyConfigRepository.save(any(FrequencyConfig.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Exécution
        frequencyConfigService.createConfig(newConfig);

        // Vérifications
        verify(frequencyConfigRepository).save(existingConfig);
        assertThat(existingConfig.getActive()).isFalse();
        verify(frequencyConfigRepository).save(newConfig);
    }

    @Test
    void shouldActivateConfig() {
        // Setup
        FrequencyConfig config = FrequencyConfig.builder()
            .id(1L)
            .type(FrequencyType.INTERVAL)
            .intervalMinutes(5)
            .active(false)
            .build();

        when(frequencyConfigRepository.findById(1L)).thenReturn(Optional.of(config));
        when(frequencyConfigRepository.findByActiveTrue()).thenReturn(Optional.empty());
        when(frequencyConfigRepository.save(any(FrequencyConfig.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Exécution
        FrequencyConfig result = frequencyConfigService.activateConfig(1L);

        // Vérifications
        assertThat(result.getActive()).isTrue();
        verify(frequencyConfigRepository).save(config);
    }

    @Test
    void shouldDeactivateConfig() {
        // Setup
        FrequencyConfig config = FrequencyConfig.builder()
            .id(1L)
            .type(FrequencyType.INTERVAL)
            .intervalMinutes(5)
            .active(true)
            .build();

        when(frequencyConfigRepository.findById(1L)).thenReturn(Optional.of(config));
        when(frequencyConfigRepository.save(any(FrequencyConfig.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Exécution
        FrequencyConfig result = frequencyConfigService.deactivateConfig(1L);

        // Vérifications
        assertThat(result.getActive()).isFalse();
        verify(frequencyConfigRepository).save(config);
    }

    @Test
    void shouldThrowExceptionWhenConfigNotFoundForActivation() {
        when(frequencyConfigRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> frequencyConfigService.activateConfig(999L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("999");
    }

    @Test
    void shouldUpdateLastExecutionDate() {
        // Setup
        FrequencyConfig config = FrequencyConfig.builder()
            .id(1L)
            .type(FrequencyType.INTERVAL)
            .intervalMinutes(5)
            .active(true)
            .build();

        when(frequencyConfigRepository.findByActiveTrue()).thenReturn(Optional.of(config));
        when(frequencyConfigRepository.save(any(FrequencyConfig.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Exécution
        frequencyConfigService.updateLastExecution();

        // Vérifications
        verify(frequencyConfigRepository).save(config);
        assertThat(config.getLastExecutionAt()).isNotNull();
    }

    @Test
    void shouldReturnActiveConfigDescription() {
        // Setup
        FrequencyConfig config = FrequencyConfig.builder()
            .id(1L)
            .type(FrequencyType.INTERVAL)
            .intervalMinutes(5)
            .active(true)
            .build();

        when(frequencyConfigRepository.findByActiveTrue()).thenReturn(Optional.of(config));

        // Exécution
        String description = frequencyConfigService.getActiveConfigDescription();

        // Vérifications
        assertThat(description).contains("5 minutes");
    }

    @Test
    void shouldReturnNoActiveConfigDescription() {
        when(frequencyConfigRepository.findByActiveTrue()).thenReturn(Optional.empty());

        String description = frequencyConfigService.getActiveConfigDescription();

        assertThat(description).isEqualTo("Aucune configuration active");
    }
}
package com.bridge.bdbank.execution;

import com.bridge.bdbank.persistence.Alert;
import com.bridge.bdbank.persistence.AlertRepository;
import com.bridge.bdbank.persistence.AlertStatus;
import com.bridge.bdbank.persistence.Rule;
import com.bridge.bdbank.persistence.RuleRepository;
import com.bridge.bdbank.persistence.RuleSeverity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour le AlertHistoryService.
 * Vérifie les fonctionnalités de consultation et de filtrage de l'historique des alertes.
 */
@ExtendWith(MockitoExtension.class)
class AlertHistoryServiceTest {

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private RuleRepository ruleRepository;

    private AlertHistoryService alertHistoryService;

    @BeforeEach
    void setUp() {
        alertHistoryService = new AlertHistoryService(alertRepository, ruleRepository);
    }

    @Test
    void devraitRecupererToutesLesAlertesAvecPagination() {
        // Setup
        Pageable pageable = PageRequest.of(0, 10);
        Alert alert1 = createAlert(1L, 1L, AlertStatus.ACTIVE, "entity1");
        Alert alert2 = createAlert(2L, 1L, AlertStatus.RESOLVED, "entity2");
        Page<Alert> expectedPage = new PageImpl<>(List.of(alert1, alert2), pageable, 2);

        when(alertRepository.findAll(pageable)).thenReturn(expectedPage);

        // Exécution
        Page<Alert> result = alertHistoryService.getAllAlerts(pageable);

        // Vérifications
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).containsExactly(alert1, alert2);
        verify(alertRepository).findAll(pageable);
    }

    @Test
    void devraitRecupererAlertesParStatut() {
        // Setup
        Pageable pageable = PageRequest.of(0, 10);
        Alert alert1 = createAlert(1L, 1L, AlertStatus.ACTIVE, "entity1");
        Page<Alert> expectedPage = new PageImpl<>(List.of(alert1), pageable, 1);

        when(alertRepository.findByStatus(AlertStatus.ACTIVE, pageable)).thenReturn(expectedPage);

        // Exécution
        Page<Alert> result = alertHistoryService.getAlertsByStatus(AlertStatus.ACTIVE, pageable);

        // Vérifications
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(AlertStatus.ACTIVE);
        verify(alertRepository).findByStatus(AlertStatus.ACTIVE, pageable);
    }

    @Test
    void devraitRecupererToutesLesAlertesQuandStatutEstNull() {
        // Setup
        Pageable pageable = PageRequest.of(0, 10);
        Alert alert1 = createAlert(1L, 1L, AlertStatus.ACTIVE, "entity1");
        Page<Alert> expectedPage = new PageImpl<>(List.of(alert1), pageable, 1);

        when(alertRepository.findAll(pageable)).thenReturn(expectedPage);

        // Exécution
        Page<Alert> result = alertHistoryService.getAlertsByStatus(null, pageable);

        // Vérifications
        assertThat(result.getContent()).hasSize(1);
        verify(alertRepository).findAll(pageable);
        verify(alertRepository, never()).findByStatus(any(), any());
    }

    @Test
    void devraitRecupererAlertesParRegle() {
        // Setup
        Pageable pageable = PageRequest.of(0, 10);
        Alert alert1 = createAlert(1L, 5L, AlertStatus.ACTIVE, "entity1");
        Page<Alert> expectedPage = new PageImpl<>(List.of(alert1), pageable, 1);

        when(alertRepository.findByRuleId(5L, pageable)).thenReturn(expectedPage);

        // Exécution
        Page<Alert> result = alertHistoryService.getAlertsByRule(5L, pageable);

        // Vérifications
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getRuleId()).isEqualTo(5L);
        verify(alertRepository).findByRuleId(5L, pageable);
    }

    @Test
    void devraitRecupererAlertesParRegleEtStatut() {
        // Setup
        Pageable pageable = PageRequest.of(0, 10);
        Alert alert1 = createAlert(1L, 5L, AlertStatus.ACTIVE, "entity1");
        Page<Alert> expectedPage = new PageImpl<>(List.of(alert1), pageable, 1);

        when(alertRepository.findByRuleIdAndStatus(5L, AlertStatus.ACTIVE, pageable))
            .thenReturn(expectedPage);

        // Exécution
        Page<Alert> result = alertHistoryService.getAlertsByRuleAndStatus(5L, AlertStatus.ACTIVE, pageable);

        // Vérifications
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getRuleId()).isEqualTo(5L);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(AlertStatus.ACTIVE);
        verify(alertRepository).findByRuleIdAndStatus(5L, AlertStatus.ACTIVE, pageable);
    }

    @Test
    void devraitRecupererAlertesParRegleSansFiltreStatut() {
        // Setup
        Pageable pageable = PageRequest.of(0, 10);
        Alert alert1 = createAlert(1L, 5L, AlertStatus.ACTIVE, "entity1");
        Page<Alert> expectedPage = new PageImpl<>(List.of(alert1), pageable, 1);

        when(alertRepository.findByRuleId(5L, pageable)).thenReturn(expectedPage);

        // Exécution
        Page<Alert> result = alertHistoryService.getAlertsByRuleAndStatus(5L, null, pageable);

        // Vérifications
        assertThat(result.getContent()).hasSize(1);
        verify(alertRepository).findByRuleId(5L, pageable);
        verify(alertRepository, never()).findByRuleIdAndStatus(any(), any(), any());
    }

    @Test
    void devraitRecupererAlertesDetecteesApresDate() {
        // Setup
        Pageable pageable = PageRequest.of(0, 10);
        LocalDateTime date = LocalDateTime.now().minusDays(1);
        Alert alert1 = createAlert(1L, 1L, AlertStatus.ACTIVE, "entity1");
        alert1.setDetectedAt(LocalDateTime.now());
        Page<Alert> expectedPage = new PageImpl<>(List.of(alert1), pageable, 1);

        when(alertRepository.findByDetectedAtAfter(date, pageable)).thenReturn(expectedPage);

        // Exécution
        Page<Alert> result = alertHistoryService.getAlertsDetectedAfter(date, pageable);

        // Vérifications
        assertThat(result.getContent()).hasSize(1);
        verify(alertRepository).findByDetectedAtAfter(date, pageable);
    }

    @Test
    void devraitRecupererAlertesResoluesApresDate() {
        // Setup
        Pageable pageable = PageRequest.of(0, 10);
        LocalDateTime date = LocalDateTime.now().minusDays(1);
        Alert alert1 = createAlert(1L, 1L, AlertStatus.RESOLVED, "entity1");
        alert1.setResolvedAt(LocalDateTime.now());
        Page<Alert> expectedPage = new PageImpl<>(List.of(alert1), pageable, 1);

        when(alertRepository.findByStatusAndResolvedAtAfter(AlertStatus.RESOLVED, date, pageable))
            .thenReturn(expectedPage);

        // Exécution
        Page<Alert> result = alertHistoryService.getAlertsResolvedAfter(date, pageable);

        // Vérifications
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(AlertStatus.RESOLVED);
        verify(alertRepository).findByStatusAndResolvedAtAfter(AlertStatus.RESOLVED, date, pageable);
    }

    @Test
    void devraitRecupererAlerteParId() {
        // Setup
        Alert alert = createAlert(1L, 1L, AlertStatus.ACTIVE, "entity1");
        when(alertRepository.findById(1L)).thenReturn(Optional.of(alert));

        // Exécution
        Alert result = alertHistoryService.getAlertById(1L);

        // Vérifications
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(alertRepository).findById(1L);
    }

    @Test
    void devraitLancerExceptionQuandAlerteNonTrouvee() {
        // Setup
        when(alertRepository.findById(999L)).thenReturn(Optional.empty());

        // Exécution & Vérifications
        assertThatThrownBy(() -> alertHistoryService.getAlertById(999L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Alerte non trouvée avec l'ID: 999");
    }

    @Test
    void devraitRecupererStatistiquesPourRegle() {
        // Setup
        Rule rule = Rule.builder()
            .id(1L)
            .dslText("age < 18")
            .targetTable("clients")
            .severity(RuleSeverity.MEDIUM)
            .active(true)
            .build();

        when(ruleRepository.findById(1L)).thenReturn(Optional.of(rule));
        when(alertRepository.countByRuleIdAndStatus(1L, AlertStatus.ACTIVE)).thenReturn(5L);
        when(alertRepository.countByRuleIdAndStatus(1L, AlertStatus.RESOLVED)).thenReturn(3L);

        // Exécution
        AlertHistoryService.AlertStatistics result = alertHistoryService.getAlertStatisticsForRule(1L);

        // Vérifications
        assertThat(result.ruleId()).isEqualTo(1L);
        assertThat(result.ruleDslText()).isEqualTo("age < 18");
        assertThat(result.activeCount()).isEqualTo(5L);
        assertThat(result.resolvedCount()).isEqualTo(3L);
        assertThat(result.totalCount()).isEqualTo(8L);
    }

    @Test
    void devraitLancerExceptionQuandRegleNonTrouveePourStatistiques() {
        // Setup
        when(ruleRepository.findById(999L)).thenReturn(Optional.empty());

        // Exécution & Vérifications
        assertThatThrownBy(() -> alertHistoryService.getAlertStatisticsForRule(999L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Règle non trouvée avec l'ID: 999");
    }

    @Test
    void devraitRecupererStatistiquesGlobales() {
        // Setup
        Rule rule1 = Rule.builder().id(1L).dslText("rule1").targetTable("t1").severity(RuleSeverity.HIGH).active(true).build();
        Rule rule2 = Rule.builder().id(2L).dslText("rule2").targetTable("t2").severity(RuleSeverity.MEDIUM).active(true).build();
        Rule rule3 = Rule.builder().id(3L).dslText("rule3").targetTable("t3").severity(RuleSeverity.LOW).active(true).build();

        when(ruleRepository.findAll()).thenReturn(List.of(rule1, rule2, rule3));
        when(alertRepository.countByStatus(AlertStatus.ACTIVE)).thenReturn(10L);
        when(alertRepository.countByStatus(AlertStatus.RESOLVED)).thenReturn(5L);
        
        // Simuler que 2 règles ont des alertes actives
        when(alertRepository.countByRuleIdAndStatus(1L, AlertStatus.ACTIVE)).thenReturn(5L);
        when(alertRepository.countByRuleIdAndStatus(2L, AlertStatus.ACTIVE)).thenReturn(5L);
        when(alertRepository.countByRuleIdAndStatus(3L, AlertStatus.ACTIVE)).thenReturn(0L);

        // Exécution
        AlertHistoryService.GlobalAlertStatistics result = alertHistoryService.getGlobalAlertStatistics();

        // Vérifications
        assertThat(result.activeCount()).isEqualTo(10L);
        assertThat(result.resolvedCount()).isEqualTo(5L);
        assertThat(result.totalCount()).isEqualTo(15L);
        assertThat(result.rulesWithActiveAlerts()).isEqualTo(2L);
    }

    private Alert createAlert(Long id, Long ruleId, AlertStatus status, String entityId) {
        return Alert.builder()
            .id(id)
            .ruleId(ruleId)
            .status(status)
            .violatingEntityId(entityId)
            .consecutiveDetections(1)
            .detectedAt(LocalDateTime.now())
            .build();
    }
}
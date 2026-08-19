package com.bridge.bdbank.execution;

import com.bridge.bdbank.dsl.DslParserService;
import com.bridge.bdbank.dsl.ParsedCondition;
import com.bridge.bdbank.dsl.ParsedRule;
import com.bridge.bdbank.persistence.Alert;
import com.bridge.bdbank.persistence.AlertRepository;
import com.bridge.bdbank.persistence.AlertStatus;
import com.bridge.bdbank.persistence.Rule;
import com.bridge.bdbank.persistence.RuleRepository;
import com.bridge.bdbank.persistence.RuleSeverity;
import com.bridge.bdbank.translation.RuleTranslator;
import com.bridge.bdbank.translation.TranslatedQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour le RuleExecutionService.
 * Utilise des mocks pour isoler la logique d'exécution des règles.
 */
@ExtendWith(MockitoExtension.class)
class RuleExecutionServiceTest {

    @Mock
    private RuleRepository ruleRepository;

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private DslParserService dslParserService;

    @Mock
    private RuleTranslator ruleTranslator;

    @Mock
    private DataSource bankDataSource;

    private RuleExecutionService ruleExecutionService;

    @BeforeEach
    void setUp() {
        ruleExecutionService = new RuleExecutionService(
            ruleRepository, alertRepository, dslParserService, ruleTranslator, bankDataSource);
    }

    @Test
    void devraitRetournerZeroSiAucuneRegleActive() {
        when(ruleRepository.findByActiveTrue()).thenReturn(List.of());

        int result = ruleExecutionService.executeAllActiveRules();

        assertThat(result).isEqualTo(0);
        verify(ruleRepository).findByActiveTrue();
        verifyNoInteractions(dslParserService, ruleTranslator, bankDataSource);
    }

    @Test
    void devraitExecuterUneRegleEtGenererDesAlertes() throws Exception {
        // Setup
        Rule rule = Rule.builder()
            .id(1L)
            .dslText("age > 18")
            .targetTable("clients")
            .severity(RuleSeverity.MEDIUM)
            .active(true)
            .build();

        ParsedRule parsedRule = new ParsedRule(
            new ParsedCondition(null, null, null), null);

        TranslatedQuery query = new TranslatedQuery(
            "SELECT id FROM clients WHERE age <= ?",
            List.of(18L),
            "clients",
            List.of("id"),
            List.of("age")
        );

        when(ruleRepository.findByActiveTrue()).thenReturn(List.of(rule));
        when(dslParserService.parse("age > 18")).thenReturn(parsedRule);
        when(ruleTranslator.translate(parsedRule, "clients")).thenReturn(query);
        when(alertRepository.findByRuleIdAndViolatingEntityId(1L, "123")).thenReturn(Optional.empty());

        // Mock de la connexion SQL
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(bankDataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true).thenReturn(false);
        when(resultSet.getString(1)).thenReturn("123");

        // Exécution
        int result = ruleExecutionService.executeAllActiveRules();

        // Vérifications
        assertThat(result).isEqualTo(1);
        verify(dslParserService).parse("age > 18");
        verify(ruleTranslator).translate(parsedRule, "clients");
        verify(alertRepository).save(any(Alert.class));
    }

    @Test
    void devraitMettreAJournerUneAlerteExistante() throws Exception {
        // Setup
        Rule rule = Rule.builder()
            .id(1L)
            .dslText("solde < 0")
            .targetTable("comptes")
            .severity(RuleSeverity.HIGH)
            .active(true)
            .build();

        ParsedRule parsedRule = new ParsedRule(
            new ParsedCondition(null, null, null), null);

        TranslatedQuery query = new TranslatedQuery(
            "SELECT id FROM comptes WHERE solde < ?",
            List.of(0L),
            "comptes",
            List.of("id"),
            List.of("solde")
        );

        Alert existingAlert = Alert.builder()
            .id(100L)
            .ruleId(1L)
            .status(AlertStatus.ACTIVE)
            .violatingEntityId("456")
            .consecutiveDetections(2)
            .build();

        when(ruleRepository.findByActiveTrue()).thenReturn(List.of(rule));
        when(dslParserService.parse("solde < 0")).thenReturn(parsedRule);
        when(ruleTranslator.translate(parsedRule, "comptes")).thenReturn(query);
        when(alertRepository.findByRuleIdAndViolatingEntityId(1L, "456"))
            .thenReturn(Optional.of(existingAlert));

        // Mock de la connexion SQL
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(bankDataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true).thenReturn(false);
        when(resultSet.getString(1)).thenReturn("456");

        // Exécution
        int result = ruleExecutionService.executeAllActiveRules();

        // Vérifications
        assertThat(result).isEqualTo(1);
        
        ArgumentCaptor<Alert> alertCaptor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository).save(alertCaptor.capture());
        
        Alert updatedAlert = alertCaptor.getValue();
        assertThat(updatedAlert.getConsecutiveDetections()).isEqualTo(3);
        assertThat(updatedAlert.getStatus()).isEqualTo(AlertStatus.ACTIVE);
    }

    @Test
    void devraitGererLesErreursDExecution() {
        Rule rule = Rule.builder()
            .id(1L)
            .dslText("règle invalide")
            .targetTable("clients")
            .severity(RuleSeverity.LOW)
            .active(true)
            .build();

        when(ruleRepository.findByActiveTrue()).thenReturn(List.of(rule));
        when(dslParserService.parse("règle invalide"))
            .thenThrow(new RuntimeException("Erreur de parsing"));

        int result = ruleExecutionService.executeAllActiveRules();

        assertThat(result).isEqualTo(0);
        verify(dslParserService).parse("règle invalide");
    }

    @Test
    void devraitReactiverUneAlerteResolue() throws Exception {
        // Setup
        Rule rule = Rule.builder()
            .id(1L)
            .dslText("age > 18")
            .targetTable("clients")
            .severity(RuleSeverity.MEDIUM)
            .active(true)
            .build();

        ParsedRule parsedRule = new ParsedRule(
            new ParsedCondition(null, null, null), null);

        TranslatedQuery query = new TranslatedQuery(
            "SELECT id FROM clients WHERE age <= ?",
            List.of(18L),
            "clients",
            List.of("id"),
            List.of("age")
        );

        Alert resolvedAlert = Alert.builder()
            .id(100L)
            .ruleId(1L)
            .status(AlertStatus.RESOLVED)
            .violatingEntityId("789")
            .consecutiveDetections(1)
            .resolvedAt(java.time.LocalDateTime.now().minusDays(1))
            .build();

        when(ruleRepository.findByActiveTrue()).thenReturn(List.of(rule));
        when(dslParserService.parse("age > 18")).thenReturn(parsedRule);
        when(ruleTranslator.translate(parsedRule, "clients")).thenReturn(query);
        when(alertRepository.findByRuleIdAndViolatingEntityId(1L, "789"))
            .thenReturn(Optional.of(resolvedAlert));

        // Mock de la connexion SQL
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(bankDataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true).thenReturn(false);
        when(resultSet.getString(1)).thenReturn("789");

        // Exécution
        int result = ruleExecutionService.executeAllActiveRules();

        // Vérifications
        assertThat(result).isEqualTo(1);
        
        ArgumentCaptor<Alert> alertCaptor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository).save(alertCaptor.capture());
        
        Alert reactivatedAlert = alertCaptor.getValue();
        assertThat(reactivatedAlert.getStatus()).isEqualTo(AlertStatus.ACTIVE);
        assertThat(reactivatedAlert.getResolvedAt()).isNull();
    }

    @Test
    void devraitAutoResoudreUneAlerteQuiNestPlusEnViolation() throws Exception {
        // Setup
        Rule rule = Rule.builder()
            .id(1L)
            .dslText("solde < 0")
            .targetTable("comptes")
            .severity(RuleSeverity.HIGH)
            .active(true)
            .build();

        ParsedRule parsedRule = new ParsedRule(
            new ParsedCondition(null, null, null), null);

        TranslatedQuery query = new TranslatedQuery(
            "SELECT id FROM comptes WHERE solde < ?",
            List.of(0L),
            "comptes",
            List.of("id"),
            List.of("solde")
        );

        // Alertes actives existantes
        Alert alert1 = Alert.builder()
            .id(100L)
            .ruleId(1L)
            .status(AlertStatus.ACTIVE)
            .violatingEntityId("123")
            .consecutiveDetections(3)
            .build();

        Alert alert2 = Alert.builder()
            .id(101L)
            .ruleId(1L)
            .status(AlertStatus.ACTIVE)
            .violatingEntityId("456")
            .consecutiveDetections(2)
            .build();

        when(ruleRepository.findByActiveTrue()).thenReturn(List.of(rule));
        when(dslParserService.parse("solde < 0")).thenReturn(parsedRule);
        when(ruleTranslator.translate(parsedRule, "comptes")).thenReturn(query);
        
        // Seule l'alerte 123 est encore en violation (456 a été corrigée)
        when(alertRepository.findListByRuleIdAndStatus(1L, AlertStatus.ACTIVE))
            .thenReturn(List.of(alert1, alert2));
        when(alertRepository.findByRuleIdAndViolatingEntityId(1L, "123"))
            .thenReturn(Optional.of(alert1));

        // Mock de la connexion SQL - seule l'entité 123 est encore en violation
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(bankDataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true).thenReturn(false);
        when(resultSet.getString(1)).thenReturn("123"); // Seule 123 est encore en violation

        // Exécution
        int result = ruleExecutionService.executeAllActiveRules();

        // Vérifications
        assertThat(result).isEqualTo(1); // Une seule alerte générée/mise à jour
        
        // Vérifier que l'alerte 456 a été marquée comme résolue
        ArgumentCaptor<Alert> alertCaptor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository, times(2)).save(alertCaptor.capture());
        
        List<Alert> savedAlerts = alertCaptor.getAllValues();
        
        // La première sauvegarde est l'auto-résolution de l'alerte 456
        Alert resolvedAlert = savedAlerts.get(0);
        assertThat(resolvedAlert.getId()).isEqualTo(101L);
        assertThat(resolvedAlert.getStatus()).isEqualTo(AlertStatus.RESOLVED);
        
        // La deuxième sauvegarde est la mise à jour de l'alerte 123
        Alert updatedAlert = savedAlerts.get(1);
        assertThat(updatedAlert.getId()).isEqualTo(100L);
        assertThat(updatedAlert.getStatus()).isEqualTo(AlertStatus.ACTIVE);
        assertThat(updatedAlert.getConsecutiveDetections()).isEqualTo(4);
    }
}
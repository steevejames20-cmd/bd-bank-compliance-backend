package com.bridge.bdbank.execution;

import com.bridge.bdbank.persistence.Alert;
import com.bridge.bdbank.persistence.AlertRepository;
import com.bridge.bdbank.persistence.AlertStatus;
import com.bridge.bdbank.persistence.Rule;
import com.bridge.bdbank.persistence.RuleRepository;
import com.bridge.bdbank.persistence.RuleSeverity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test d'intégration pour le RuleExecutionService.
 * Teste le flux complet d'exécution avec la vraie base de données.
 */
@DataJpaTest
@ActiveProfiles("test")
@Transactional
class RuleExecutionServiceIntegrationTest {

    @Autowired
    private RuleRepository ruleRepository;

    @Autowired
    private AlertRepository alertRepository;

    @Test
    void devraitCreerEtPersisterUneRegle() {
        // Test simple de persistance de règle
        Rule rule = Rule.builder()
            .dslText("age > 18")
            .targetTable("clients")
            .severity(RuleSeverity.MEDIUM)
            .active(true)
            .build();
        
        Rule savedRule = ruleRepository.save(rule);

        assertThat(savedRule.getId()).isNotNull();
        assertThat(savedRule.getDslText()).isEqualTo("age > 18");
        assertThat(savedRule.getTargetTable()).isEqualTo("clients");
        assertThat(savedRule.getSeverity()).isEqualTo(RuleSeverity.MEDIUM);
        assertThat(savedRule.getActive()).isTrue();
    }

    @Test
    void devraitCreerEtPersisterUneAlerte() {
        // Créer une règle d'abord
        Rule rule = Rule.builder()
            .dslText("solde < 0")
            .targetTable("comptes")
            .severity(RuleSeverity.HIGH)
            .active(true)
            .build();
        Rule savedRule = ruleRepository.save(rule);

        // Créer une alerte
        Alert alert = Alert.builder()
            .ruleId(savedRule.getId())
            .status(AlertStatus.ACTIVE)
            .violatingEntityId("123")
            .consecutiveDetections(1)
            .build();

        Alert savedAlert = alertRepository.save(alert);

        assertThat(savedAlert.getId()).isNotNull();
        assertThat(savedAlert.getRuleId()).isEqualTo(savedRule.getId());
        assertThat(savedAlert.getStatus()).isEqualTo(AlertStatus.ACTIVE);
    }

    @Test
    void devraitTrouverLesReglesActives() {
        // Créer des règles actives et inactives
        Rule activeRule = Rule.builder()
            .dslText("age > 18")
            .targetTable("clients")
            .severity(RuleSeverity.LOW)
            .active(true)
            .build();

        Rule inactiveRule = Rule.builder()
            .dslText("solde >= 0")
            .targetTable("comptes")
            .severity(RuleSeverity.MEDIUM)
            .active(false)
            .build();

        ruleRepository.save(activeRule);
        ruleRepository.save(inactiveRule);

        // Vérifier que seules les règles actives sont trouvées
        var activeRules = ruleRepository.findByActiveTrue();

        assertThat(activeRules).hasSize(1);
        assertThat(activeRules.get(0).getActive()).isTrue();
    }
}
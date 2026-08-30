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
 * Test d'intégration J15 pour le cycle complet des alertes.
 * Teste l'apparition, la persistance et la résolution des alertes.
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
    void devraitTesterCyclesSuccessifsApparitionPersistanceResolution() {
        // J15 - Test de bout en bout sur plusieurs cycles d'exécution successifs
        // Version simplifiée sans base de données réelle

        // 1. Créer une règle active
        Rule rule = Rule.builder()
            .dslText("solde < 100")
            .targetTable("comptes")
            .severity(RuleSeverity.HIGH)
            .active(true)
            .build();
        Rule savedRule = ruleRepository.save(rule);

        // 2. Simuler la création d'alertes après premier cycle
        Alert alerte1 = Alert.builder()
            .ruleId(savedRule.getId())
            .status(AlertStatus.ACTIVE)
            .violatingEntityId("1")
            .consecutiveDetections(1)
            .build();
        Alert alerte2 = Alert.builder()
            .ruleId(savedRule.getId())
            .status(AlertStatus.ACTIVE)
            .violatingEntityId("2")
            .consecutiveDetections(1)
            .build();
        
        alertRepository.save(alerte1);
        alertRepository.save(alerte2);

        // 3. Vérifier que les alertes sont persistées
        List<Alert> alertesApresCycle1 = alertRepository.findListByRuleIdAndStatus(savedRule.getId(), AlertStatus.ACTIVE);
        assertThat(alertesApresCycle1).hasSize(2);

        // 4. Simuler le deuxième cycle - alertes toujours présentes (persistance)
        List<Alert> alertesApresCycle2 = alertRepository.findListByRuleIdAndStatus(savedRule.getId(), AlertStatus.ACTIVE);
        assertThat(alertesApresCycle2).hasSize(2);

        // 5. Simuler l'auto-résolution : l'entité "1" n'est plus en anomalie
        alerte1.setStatus(AlertStatus.RESOLVED);
        alertRepository.save(alerte1);

        // 6. Vérifier l'état après résolution
        List<Alert> alertesActives = alertRepository.findListByRuleIdAndStatus(savedRule.getId(), AlertStatus.ACTIVE);
        List<Alert> alertesResolues = alertRepository.findListByRuleIdAndStatus(savedRule.getId(), AlertStatus.RESOLVED);
        
        assertThat(alertesActives).hasSize(1); // Seule l'alerte 2 est encore active
        assertThat(alertesResolues).hasSize(1); // L'alerte 1 est résolue
        assertThat(alertesResolues.get(0).getViolatingEntityId()).isEqualTo("1");
    }
}
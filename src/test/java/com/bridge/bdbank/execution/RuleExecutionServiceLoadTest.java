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

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test de charge J15 pour le RuleExecutionService.
 * Tests de charge légers sur quelques milliers d'alertes.
 */
@DataJpaTest
@ActiveProfiles("test")
@Transactional
class RuleExecutionServiceLoadTest {

    @Autowired
    private RuleRepository ruleRepository;

    @Autowired
    private AlertRepository alertRepository;

    private static final int TEST_ROW_COUNT = 4000; // 4000 alertes pour le test de charge

    @Test
    void devraitTesterChargeAvec4000Alertes() {
        // J15 - Test de charge léger sur 4000 alertes en persistance

        // 1. Créer une règle active
        Rule rule = Rule.builder()
            .dslText("montant > 10000")
            .targetTable("transactions")
            .severity(RuleSeverity.MEDIUM)
            .active(true)
            .build();
        Rule savedRule = ruleRepository.save(rule);

        // 2. Mesurer le temps de création de 4000 alertes
        long debut = System.currentTimeMillis();
        
        List<Alert> alertes = new ArrayList<>();
        for (int i = 1; i <= TEST_ROW_COUNT; i++) {
            Alert alert = Alert.builder()
                .ruleId(savedRule.getId())
                .status(AlertStatus.ACTIVE)
                .violatingEntityId(String.valueOf(i))
                .consecutiveDetections(1)
                .build();
            alertes.add(alert);
        }
        
        alertRepository.saveAll(alertes);
        
        long fin = System.currentTimeMillis();
        long tempsCreation = fin - debut;

        // 3. Vérifier que les alertes ont été créées
        List<Alert> alertesRecuperees = alertRepository.findListByRuleIdAndStatus(savedRule.getId(), AlertStatus.ACTIVE);
        assertThat(alertesRecuperees).hasSize(TEST_ROW_COUNT);

        // 4. Vérifier que le temps de création est raisonnable (< 5 secondes pour 4000 alertes)
        assertThat(tempsCreation).isLessThan(5000);

        System.out.println("Test de charge terminé :");
        System.out.println("- Alertes créées : " + TEST_ROW_COUNT);
        System.out.println("- Temps de création : " + tempsCreation + " ms");
        System.out.println("- Performance : " + (TEST_ROW_COUNT / (tempsCreation / 1000.0)) + " alertes/seconde");
    }

    @Test
    void devraitTesterChargeAvecMultiplesRegles() {
        // Test de charge avec plusieurs règles et alertes

        // 1. Créer plusieurs règles actives
        Rule rule1 = Rule.builder()
            .dslText("montant > 10000")
            .targetTable("transactions")
            .severity(RuleSeverity.HIGH)
            .active(true)
            .build();

        Rule rule2 = Rule.builder()
            .dslText("montant < 100")
            .targetTable("transactions")
            .severity(RuleSeverity.LOW)
            .active(true)
            .build();

        Rule rule3 = Rule.builder()
            .dslText("montant > 5000 AND montant < 8000")
            .targetTable("transactions")
            .severity(RuleSeverity.MEDIUM)
            .active(true)
            .build();

        Rule savedRule1 = ruleRepository.save(rule1);
        Rule savedRule2 = ruleRepository.save(rule2);
        Rule savedRule3 = ruleRepository.save(rule3);

        // 2. Mesurer le temps de création d'alertes pour plusieurs règles
        long debut = System.currentTimeMillis();
        
        int alertesParRegle = 1000;
        for (int i = 1; i <= alertesParRegle; i++) {
            // Alertes pour règle 1
            Alert alert1 = Alert.builder()
                .ruleId(savedRule1.getId())
                .status(AlertStatus.ACTIVE)
                .violatingEntityId("r1-" + i)
                .consecutiveDetections(1)
                .build();
            
            // Alertes pour règle 2
            Alert alert2 = Alert.builder()
                .ruleId(savedRule2.getId())
                .status(AlertStatus.ACTIVE)
                .violatingEntityId("r2-" + i)
                .consecutiveDetections(1)
                .build();
            
            // Alertes pour règle 3
            Alert alert3 = Alert.builder()
                .ruleId(savedRule3.getId())
                .status(AlertStatus.ACTIVE)
                .violatingEntityId("r3-" + i)
                .consecutiveDetections(1)
                .build();
            
            alertRepository.save(alert1);
            alertRepository.save(alert2);
            alertRepository.save(alert3);
        }
        
        long fin = System.currentTimeMillis();
        long tempsCreation = fin - debut;

        // 3. Vérifier les résultats
        assertThat(tempsCreation).isLessThan(10000); // Tolérance pour 3000 alertes

        System.out.println("Test de charge multi-règles terminé :");
        System.out.println("- Règles : 3");
        System.out.println("- Alertes par règle : " + alertesParRegle);
        System.out.println("- Total alertes : " + (alertesParRegle * 3));
        System.out.println("- Temps de création : " + tempsCreation + " ms");
    }
}
package com.bridge.bdbank.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test de base pour vérifier que la persistance JPA fonctionne correctement.
 * Teste la création et la récupération des entités Rule, Alert et Scope.
 */
@DataJpaTest
@ActiveProfiles("test")
class PersistenceTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private RuleRepository ruleRepository;

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private ScopeRepository scopeRepository;

    @Test
    void devraitCreerEtRecupererUneRegle() {
        // Créer une règle
        Rule rule = Rule.builder()
                .dslText("age < 18")
                .targetTable("clients")
                .severity(RuleSeverity.MEDIUM)
                .active(true)
                .build();

        Rule savedRule = ruleRepository.save(rule);

        // Vérifier que la règle a été sauvegardée avec un ID
        assertThat(savedRule.getId()).isNotNull();
        assertThat(savedRule.getCreatedAt()).isNotNull();
        assertThat(savedRule.getUpdatedAt()).isNotNull();

        // Récupérer la règle
        Rule foundRule = ruleRepository.findById(savedRule.getId()).orElse(null);

        assertThat(foundRule).isNotNull();
        assertThat(foundRule.getDslText()).isEqualTo("age < 18");
        assertThat(foundRule.getTargetTable()).isEqualTo("clients");
        assertThat(foundRule.getSeverity()).isEqualTo(RuleSeverity.MEDIUM);
        assertThat(foundRule.getActive()).isTrue();
    }

    @Test
    void devraitCreerEtRecupererUneAlerte() {
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

        // Vérifier que l'alerte a été sauvegardée
        assertThat(savedAlert.getId()).isNotNull();
        assertThat(savedAlert.getDetectedAt()).isNotNull();
        assertThat(savedAlert.getStatus()).isEqualTo(AlertStatus.ACTIVE);

        // Récupérer l'alerte
        Alert foundAlert = alertRepository.findById(savedAlert.getId()).orElse(null);

        assertThat(foundAlert).isNotNull();
        assertThat(foundAlert.getRuleId()).isEqualTo(savedRule.getId());
        assertThat(foundAlert.getViolatingEntityId()).isEqualTo("123");
    }

    @Test
    void devraitCreerEtRecupererUnScope() {
        // Créer un scope
        Scope scope = Scope.builder()
                .name("test-scope")
                .description("Scope de test")
                .active(true)
                .tables(new java.util.HashSet<>())
                .build();
        scope.getTables().add("clients");
        scope.getTables().add("comptes");

        Scope savedScope = scopeRepository.save(scope);

        // Vérifier que le scope a été sauvegardé
        assertThat(savedScope.getId()).isNotNull();
        assertThat(savedScope.getCreatedAt()).isNotNull();
        assertThat(savedScope.getTables()).hasSize(2);

        // Récupérer le scope
        Scope foundScope = scopeRepository.findById(savedScope.getId()).orElse(null);

        assertThat(foundScope).isNotNull();
        assertThat(foundScope.getName()).isEqualTo("test-scope");
        assertThat(foundScope.getTables()).contains("clients", "comptes");
    }

    @Test
    void devraitTrouverLesReglesActives() {
        // Créer des règles actives et inactives
        Rule activeRule1 = Rule.builder()
                .dslText("age < 18")
                .targetTable("clients")
                .severity(RuleSeverity.LOW)
                .active(true)
                .build();

        Rule activeRule2 = Rule.builder()
                .dslText("solde < 0")
                .targetTable("comptes")
                .severity(RuleSeverity.MEDIUM)
                .active(true)
                .build();

        Rule inactiveRule = Rule.builder()
                .dslText("email != null")
                .targetTable("clients")
                .severity(RuleSeverity.HIGH)
                .active(false)
                .build();

        ruleRepository.save(activeRule1);
        ruleRepository.save(activeRule2);
        ruleRepository.save(inactiveRule);

        // Vérifier que seules les règles actives sont trouvées
        var activeRules = ruleRepository.findByActiveTrue();

        assertThat(activeRules).hasSize(2);
        assertThat(activeRules).allMatch(rule -> rule.getActive());
    }

    @Test
    void devraitTrouverLesAlertesParRegleEtStatut() {
        // Créer une règle
        Rule rule = Rule.builder()
                .dslText("test")
                .targetTable("test")
                .severity(RuleSeverity.LOW)
                .active(true)
                .build();
        Rule savedRule = ruleRepository.save(rule);

        // Créer des alertes avec différents statuts
        Alert activeAlert = Alert.builder()
                .ruleId(savedRule.getId())
                .status(AlertStatus.ACTIVE)
                .violatingEntityId("1")
                .build();

        Alert resolvedAlert = Alert.builder()
                .ruleId(savedRule.getId())
                .status(AlertStatus.RESOLVED)
                .violatingEntityId("2")
                .build();

        alertRepository.save(activeAlert);
        alertRepository.save(resolvedAlert);

        // Vérifier la recherche par règle et statut
        var activeAlerts = alertRepository.findListByRuleIdAndStatus(savedRule.getId(), AlertStatus.ACTIVE);

        assertThat(activeAlerts).hasSize(1);
        assertThat(activeAlerts.get(0).getViolatingEntityId()).isEqualTo("1");
    }
}
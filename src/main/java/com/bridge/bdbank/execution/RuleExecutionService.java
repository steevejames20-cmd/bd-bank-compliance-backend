package com.bridge.bdbank.execution;

import com.bridge.bdbank.dsl.DslParserService;
import com.bridge.bdbank.dsl.ParsedRule;
import com.bridge.bdbank.persistence.Alert;
import com.bridge.bdbank.persistence.AlertRepository;
import com.bridge.bdbank.persistence.AlertStatus;
import com.bridge.bdbank.persistence.Rule;
import com.bridge.bdbank.persistence.RuleRepository;
import com.bridge.bdbank.translation.RuleTranslator;
import com.bridge.bdbank.translation.TranslatedQuery;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Moteur d'exécution séquentielle des règles de conformité.
 * 
 * Parcourt toutes les règles actives, exécute les requêtes SQL correspondantes
 * sur la bd_bank, et génère des alertes pour chaque anomalie détectée.
 * Les règles décrivent directement les anomalies à rechercher (ex: "solde < 0").
 */
@Service
@RequiredArgsConstructor
public class RuleExecutionService {

    private static final Logger log = LoggerFactory.getLogger(RuleExecutionService.class);

    private final RuleRepository ruleRepository;
    private final AlertRepository alertRepository;
    private final DslParserService dslParserService;
    private final RuleTranslator ruleTranslator;
    private final DataSource bankDataSource;

    /**
     * Exécute toutes les règles actives et génère les alertes correspondantes.
     * 
     * @return Le nombre total d'alertes générées
     */
    @Transactional
    public int executeAllActiveRules() {
        List<Rule> activeRules = ruleRepository.findByActiveTrue();
        
        if (activeRules.isEmpty()) {
            log.info("Aucune règle active à exécuter");
            return 0;
        }

        log.info("Exécution de {} règle(s) active(s)", activeRules.size());
        int totalAlertsGenerated = 0;

        for (Rule rule : activeRules) {
            try {
                int alertsGenerated = executeRule(rule);
                totalAlertsGenerated += alertsGenerated;
                log.info("Règle {} (id: {}) : {} alerte(s) générée(s)", 
                    rule.getDslText(), rule.getId(), alertsGenerated);
            } catch (Exception e) {
                log.error("Erreur lors de l'exécution de la règle {} (id: {}) : {}", 
                    rule.getDslText(), rule.getId(), e.getMessage(), e);
            }
        }

        log.info("Exécution terminée : {} alerte(s) générée(s) au total", totalAlertsGenerated);
        return totalAlertsGenerated;
    }

    /**
     * Exécute une règle spécifique et génère les alertes correspondantes.
     * 
     * @param rule La règle à exécuter
     * @return Le nombre d'alertes générées pour cette règle
     */
    private int executeRule(Rule rule) {
        // 1. Parser la règle DSL
        ParsedRule parsedRule = dslParserService.parse(rule.getDslText());

        // 2. Traduire en SQL
        TranslatedQuery query = ruleTranslator.translate(parsedRule, rule.getTargetTable());

        // 3. Récupérer les alertes actives existantes pour cette règle (pour auto-résolution)
        List<Alert> existingActiveAlerts = alertRepository.findListByRuleIdAndStatus(rule.getId(), AlertStatus.ACTIVE);

        // 4. Exécuter la requête sur la bd_bank
        List<String> violatingEntities = executeQuery(query);

        // 5. Auto-résolution : marquer comme résolues les alertes qui ne sont plus en anomalie
        int resolvedAlerts = autoResolveAlerts(existingActiveAlerts, violatingEntities);
        log.info("Règle {} (id: {}) : {} alerte(s) auto-résolue(s)", 
            rule.getDslText(), rule.getId(), resolvedAlerts);

        // 6. Générer/mettre à jour les alertes pour chaque anomalie
        int generatedAlerts = generateAlerts(rule, query, violatingEntities);

        return generatedAlerts;
    }

    /**
     * Auto-résolution des alertes : marque comme résolues les alertes actives
     * dont l'entité n'est plus en anomalie.
     * 
     * @param existingActiveAlerts Les alertes actives existantes
     * @param currentViolatingEntities Les entités actuellement en anomalie
     * @return Le nombre d'alertes résolues
     */
    private int autoResolveAlerts(List<Alert> existingActiveAlerts, List<String> currentViolatingEntities) {
        int resolvedCount = 0;

        for (Alert alert : existingActiveAlerts) {
            // Si l'entité n'est plus dans la liste des anomalies, l'alerte est résolue
            if (!currentViolatingEntities.contains(alert.getViolatingEntityId())) {
                alert.setStatus(AlertStatus.RESOLVED);
                alertRepository.save(alert);
                resolvedCount++;
                log.debug("Alerte {} auto-résolue (entité {} n'est plus en anomalie)", 
                    alert.getId(), alert.getViolatingEntityId());
            }
        }

        return resolvedCount;
    }

    /**
     * Exécute la requête SQL traduite sur la bd_bank.
     * 
     * @param query La requête traduite à exécuter
     * @return La liste des entités en anomalie (identifiants)
     */
    private List<String> executeQuery(TranslatedQuery query) {
        List<String> violatingEntities = new ArrayList<>();

        try (Connection connection = bankDataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(query.sql())) {

            // Paramétrer la requête
            List<Object> params = query.params();
            for (int i = 0; i < params.size(); i++) {
                statement.setObject(i + 1, params.get(i));
            }

            // Exécuter et récupérer les résultats
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    // La première colonne contient l'identifiant de l'entité en anomalie
                    violatingEntities.add(resultSet.getString(1));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de l'exécution de la requête SQL", e);
        }

        return violatingEntities;
    }

    /**
     * Génère des alertes pour chaque entité en anomalie.
     * 
     * @param rule La règle qui a généré les anomalies
     * @param query La requête exécutée (pour les colonnes concernées)
     * @param violatingEntities La liste des entités en anomalie
     * @return Le nombre d'alertes générées
     */
    private int generateAlerts(Rule rule, TranslatedQuery query, List<String> violatingEntities) {
        int alertsGenerated = 0;

        for (String entityId : violatingEntities) {
            // Vérifier si une alerte existe déjà pour cette combinaison règle/entité
            alertRepository.findByRuleIdAndViolatingEntityId(rule.getId(), entityId)
                .ifPresentOrElse(
                    existingAlert -> {
                        // Mise à jour de l'alerte existante
                        existingAlert.setConsecutiveDetections(existingAlert.getConsecutiveDetections() + 1);
                        if (existingAlert.getStatus() == AlertStatus.RESOLVED) {
                            existingAlert.setStatus(AlertStatus.ACTIVE);
                            existingAlert.setResolvedAt(null);
                        }
                        alertRepository.save(existingAlert);
                    },
                    () -> {
                        // Création d'une nouvelle alerte
                        Alert alert = Alert.builder()
                            .ruleId(rule.getId())
                            .status(AlertStatus.ACTIVE)
                            .violatingEntityId(entityId)
                            .consecutiveDetections(1)
                            .involvedColumns(query.involvedColumns())
                            .build();
                        alertRepository.save(alert);
                    }
                );
            alertsGenerated++;
        }

        return alertsGenerated;
    }
}
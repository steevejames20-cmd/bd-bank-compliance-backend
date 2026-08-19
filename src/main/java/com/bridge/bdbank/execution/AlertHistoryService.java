package com.bridge.bdbank.execution;

import com.bridge.bdbank.persistence.Alert;
import com.bridge.bdbank.persistence.AlertRepository;
import com.bridge.bdbank.persistence.AlertStatus;
import com.bridge.bdbank.persistence.Rule;
import com.bridge.bdbank.persistence.RuleRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service pour la consultation de l'historique des alertes.
 * Permet de rechercher, filtrer et paginer les alertes selon différents critères.
 */
@Service
@RequiredArgsConstructor
public class AlertHistoryService {

    private static final Logger log = LoggerFactory.getLogger(AlertHistoryService.class);

    private final AlertRepository alertRepository;
    private final RuleRepository ruleRepository;

    /**
     * Récupère toutes les alertes avec pagination.
     * 
     * @param pageable Les informations de pagination
     * @return Une page d'alertes
     */
    public Page<Alert> getAllAlerts(Pageable pageable) {
        log.debug("Récupération de toutes les alertes avec pagination");
        return alertRepository.findAll(pageable);
    }

    /**
     * Récupère les alertes filtrées par statut avec pagination.
     * 
     * @param status Le statut des alertes à récupérer (null pour tous les statuts)
     * @param pageable Les informations de pagination
     * @return Une page d'alertes filtrées
     */
    public Page<Alert> getAlertsByStatus(AlertStatus status, Pageable pageable) {
        if (status == null) {
            log.debug("Récupération de toutes les alertes avec pagination");
            return alertRepository.findAll(pageable);
        }
        
        log.debug("Récupération des alertes avec statut {} et pagination", status);
        return alertRepository.findByStatus(status, pageable);
    }

    /**
     * Récupère les alertes pour une règle spécifique avec pagination.
     * 
     * @param ruleId L'identifiant de la règle
     * @param pageable Les informations de pagination
     * @return Une page d'alertes pour la règle
     */
    public Page<Alert> getAlertsByRule(Long ruleId, Pageable pageable) {
        log.debug("Récupération des alertes pour la règle {} avec pagination", ruleId);
        return alertRepository.findByRuleId(ruleId, pageable);
    }

    /**
     * Récupère les alertes pour une règle spécifique filtrées par statut.
     * 
     * @param ruleId L'identifiant de la règle
     * @param status Le statut des alertes (null pour tous les statuts)
     * @param pageable Les informations de pagination
     * @return Une page d'alertes filtrées
     */
    public Page<Alert> getAlertsByRuleAndStatus(Long ruleId, AlertStatus status, Pageable pageable) {
        if (status == null) {
            log.debug("Récupération des alertes pour la règle {} avec pagination", ruleId);
            return alertRepository.findByRuleId(ruleId, pageable);
        }
        
        log.debug("Récupération des alertes pour la règle {} avec statut {} et pagination", ruleId, status);
        return alertRepository.findByRuleIdAndStatus(ruleId, status, pageable);
    }

    /**
     * Récupère les alertes détectées après une certaine date.
     * 
     * @param dateTime La date limite
     * @param pageable Les informations de pagination
     * @return Une page d'alertes détectées après la date
     */
    public Page<Alert> getAlertsDetectedAfter(LocalDateTime dateTime, Pageable pageable) {
        log.debug("Récupération des alertes détectées après {} avec pagination", dateTime);
        return alertRepository.findByDetectedAtAfter(dateTime, pageable);
    }

    /**
     * Récupère les alertes résolues après une certaine date.
     * 
     * @param dateTime La date limite
     * @param pageable Les informations de pagination
     * @return Une page d'alertes résolues après la date
     */
    public Page<Alert> getAlertsResolvedAfter(LocalDateTime dateTime, Pageable pageable) {
        log.debug("Récupération des alertes résolues après {} avec pagination", dateTime);
        return alertRepository.findByStatusAndResolvedAtAfter(AlertStatus.RESOLVED, dateTime, pageable);
    }

    /**
     * Récupère une alerte spécifique par son identifiant.
     * 
     * @param alertId L'identifiant de l'alerte
     * @return L'alerte trouvée
     * @throws IllegalArgumentException si l'alerte n'existe pas
     */
    public Alert getAlertById(Long alertId) {
        log.debug("Récupération de l'alerte {}", alertId);
        return alertRepository.findById(alertId)
            .orElseThrow(() -> new IllegalArgumentException("Alerte non trouvée avec l'ID: " + alertId));
    }

    /**
     * Récupère les statistiques d'alertes pour une règle.
     * 
     * @param ruleId L'identifiant de la règle
     * @return Un objet contenant les statistiques
     */
    public AlertStatistics getAlertStatisticsForRule(Long ruleId) {
        Rule rule = ruleRepository.findById(ruleId)
            .orElseThrow(() -> new IllegalArgumentException("Règle non trouvée avec l'ID: " + ruleId));

        long activeCount = alertRepository.countByRuleIdAndStatus(ruleId, AlertStatus.ACTIVE);
        long resolvedCount = alertRepository.countByRuleIdAndStatus(ruleId, AlertStatus.RESOLVED);
        long totalCount = activeCount + resolvedCount;

        log.debug("Statistiques pour la règle {} : {} actives, {} résolues, {} total", 
            ruleId, activeCount, resolvedCount, totalCount);

        return new AlertStatistics(ruleId, rule.getDslText(), activeCount, resolvedCount, totalCount);
    }

    /**
     * Récupère les statistiques globales des alertes.
     * 
     * @return Un objet contenant les statistiques globales
     */
    public GlobalAlertStatistics getGlobalAlertStatistics() {
        long activeCount = alertRepository.countByStatus(AlertStatus.ACTIVE);
        long resolvedCount = alertRepository.countByStatus(AlertStatus.RESOLVED);
        long totalCount = activeCount + resolvedCount;

        List<Rule> allRules = ruleRepository.findAll();
        long rulesWithActiveAlerts = allRules.stream()
            .filter(rule -> alertRepository.countByRuleIdAndStatus(rule.getId(), AlertStatus.ACTIVE) > 0)
            .count();

        log.debug("Statistiques globales : {} actives, {} résolues, {} total, {} règles avec alertes actives", 
            activeCount, resolvedCount, totalCount, rulesWithActiveAlerts);

        return new GlobalAlertStatistics(activeCount, resolvedCount, totalCount, rulesWithActiveAlerts);
    }

    /**
     * Record pour les statistiques d'alertes d'une règle.
     */
    public record AlertStatistics(
        Long ruleId,
        String ruleDslText,
        long activeCount,
        long resolvedCount,
        long totalCount
    ) {}

    /**
     * Record pour les statistiques globales des alertes.
     */
    public record GlobalAlertStatistics(
        long activeCount,
        long resolvedCount,
        long totalCount,
        long rulesWithActiveAlerts
    ) {}
}
package com.bridge.bdbank.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository JPA pour l'entité Alert.
 * Fournit les méthodes CRUD de base et des requêtes personnalisées pour la gestion des alertes.
 */
@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {

    /**
     * Trouve toutes les alertes actives.
     */
    List<Alert> findByStatus(AlertStatus status);

    /**
     * Trouve toutes les alertes pour une règle donnée.
     */
    List<Alert> findByRuleId(Long ruleId);

    /**
     * Trouve toutes les alertes actives pour une règle donnée.
     */
    List<Alert> findByRuleIdAndStatus(Long ruleId, AlertStatus status);

    /**
     * Trouve une alerte par sa règle et l'entité en violation.
     * Utile pour l'auto-résolution : vérifier si une alerte existe déjà pour cette combinaison.
     */
    Optional<Alert> findByRuleIdAndViolatingEntityId(Long ruleId, String violatingEntityId);

    /**
     * Trouve les alertes résolues depuis une certaine date.
     */
    List<Alert> findByStatusAndResolvedAtAfter(AlertStatus status, LocalDateTime resolvedAt);

    /**
     * Compte le nombre d'alertes actives pour une règle.
     */
    long countByRuleIdAndStatus(Long ruleId, AlertStatus status);

    /**
     * Supprime toutes les alertes résolues antérieures à une certaine date.
     * Utile pour le nettoyage périodique de l'historique.
     */
    void deleteByStatusAndResolvedAtBefore(AlertStatus status, LocalDateTime resolvedAt);
}
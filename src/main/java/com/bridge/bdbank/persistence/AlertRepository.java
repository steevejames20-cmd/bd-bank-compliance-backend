package com.bridge.bdbank.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    Page<Alert> findByStatus(AlertStatus status, Pageable pageable);

    /**
     * Trouve toutes les alertes pour une règle donnée.
     */
    Page<Alert> findByRuleId(Long ruleId, Pageable pageable);

    /**
     * Trouve toutes les alertes actives pour une règle donnée.
     */
    Page<Alert> findByRuleIdAndStatus(Long ruleId, AlertStatus status, Pageable pageable);

    /**
     * Trouve toutes les alertes actives pour une règle donnée (sans pagination).
     * Utile pour l'auto-résolution.
     */
    List<Alert> findListByRuleIdAndStatus(Long ruleId, AlertStatus status);

    /**
     * Trouve une alerte par sa règle et l'entité en anomalie.
     * Utile pour l'auto-résolution : vérifier si une alerte existe déjà pour cette combinaison.
     */
    Optional<Alert> findByRuleIdAndViolatingEntityId(Long ruleId, String violatingEntityId);

    /**
     * Trouve les alertes résolues depuis une certaine date.
     */
    Page<Alert> findByStatusAndResolvedAtAfter(AlertStatus status, LocalDateTime resolvedAt, Pageable pageable);

    /**
     * Compte le nombre d'alertes actives pour une règle.
     */
    long countByRuleIdAndStatus(Long ruleId, AlertStatus status);

    /**
     * Compte le nombre d'alertes par statut.
     */
    long countByStatus(AlertStatus status);

    /**
     * Supprime toutes les alertes résolues antérieures à une certaine date.
     * Utile pour le nettoyage périodique de l'historique.
     */
    void deleteByStatusAndResolvedAtBefore(AlertStatus status, LocalDateTime resolvedAt);

    /**
     * Trouve toutes les alertes actives pour une règle donnée et un ensemble d'entités.
     * Utile pour l'auto-résolution : identifier les alertes actives correspondant aux entités encore en anomalie.
     */
    List<Alert> findByRuleIdAndStatusAndViolatingEntityIdIn(Long ruleId, AlertStatus status, List<String> violatingEntityIds);

    /**
     * Trouve les alertes détectées après une certaine date.
     */
    Page<Alert> findByDetectedAtAfter(LocalDateTime dateTime, Pageable pageable);
}
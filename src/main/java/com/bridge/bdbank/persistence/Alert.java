package com.bridge.bdbank.persistence;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Entité représentant une alerte générée par une règle de conformité.
 * Contient les informations sur la violation détectée et son statut de résolution.
 */
@Entity
@Table(name = "alerts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Référence à la règle qui a généré cette alerte
     */
    @Column(name = "rule_id", nullable = false)
    private Long ruleId;

    /**
     * Statut de l'alerte (active ou résolue)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertStatus status;

    /**
     * Date à laquelle l'alerte a été détectée pour la première fois
     */
    @Column(name = "detected_at", nullable = false, updatable = false)
    private LocalDateTime detectedAt;

    /**
     * Date à laquelle l'alerte a été marquée comme résolue (null si encore active)
     */
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    /**
     * Identifiant de la ligne ou du groupe en violation
     * (clé primaire pour les règles ligne-à-ligne, clé de groupe pour les agrégats)
     */
    @Column(name = "violating_entity_id", nullable = false)
    private String violatingEntityId;

    /**
     * Liste des colonnes concernées par la violation (sans les valeurs réelles)
     * Ex: ["solde", "decouvert_autorise"] pour une règle "solde <= decouvert_autorise"
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "alert_columns", joinColumns = @JoinColumn(name = "alert_id"))
    @Column(name = "column_name")
    private List<String> involvedColumns;

    /**
     * Nombre de fois que cette alerte a été détectée consécutivement
     * (utile pour suivre la persistance d'une anomalie)
     */
    @Column(name = "consecutive_detections", nullable = false)
    private Integer consecutiveDetections;

    @PrePersist
    protected void onCreate() {
        detectedAt = LocalDateTime.now();
        if (status == null) {
            status = AlertStatus.ACTIVE;
        }
        if (consecutiveDetections == null) {
            consecutiveDetections = 1;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        if (status == AlertStatus.RESOLVED && resolvedAt == null) {
            resolvedAt = LocalDateTime.now();
        } else if (status == AlertStatus.ACTIVE) {
            resolvedAt = null;
        }
    }
}
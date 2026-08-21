package com.bridge.bdbank.persistence;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entité représentant la configuration de fréquence d'exécution des règles.
 * Permet de définir un intervalle simple ou une expression cron pour le scheduler.
 */
@Entity
@Table(name = "frequency_config")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FrequencyConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Type de fréquence : INTERVAL pour un intervalle simple en minutes,
     * CRON pour une expression cron complexe.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FrequencyType type;

    /**
     * Intervalle en minutes (utilisé quand type = INTERVAL).
     * Minimum imposé : 3 minutes.
     */
    @Column
    private Integer intervalMinutes;

    /**
     * Expression cron (utilisé quand type = CRON).
     */
    @Column(length = 100)
    private String cronExpression;

    /**
     * Indique si le scheduler est actif.
     * Si false, les règles ne sont pas exécutées automatiquement.
     */
    @Column(nullable = false)
    private Boolean active;

    /**
     * Date de création de la configuration.
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Date de dernière mise à jour de la configuration.
     */
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Date de dernière exécution effective du scheduler.
     */
    @Column
    private LocalDateTime lastExecutionAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (active == null) {
            active = false; // Désactivé par défaut pour éviter les exécutions non voulues
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Types de fréquence supportés.
     */
    public enum FrequencyType {
        /**
         * Intervalle simple en minutes
         */
        INTERVAL,
        
        /**
         * Expression cron complexe
         */
        CRON
    }
}
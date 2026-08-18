package com.bridge.bdbank.persistence;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entité représentant une règle de conformité stockée en base.
 * Contient la règle DSL, la table cible, la gravité et le statut d'activation.
 */
@Entity
@Table(name = "rules")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Rule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * La règle écrite en DSL (ex: "age > 18", "SUM(transactions.montant) > 1000 GROUP BY client_id")
     */
    @Column(nullable = false, length = 1000)
    private String dslText;

    /**
     * La table cible sur laquelle la règle s'applique (ex: "clients", "transactions")
     */
    @Column(nullable = false, length = 100)
    private String targetTable;

    /**
     * Gravité de la règle pour la priorisation des alertes
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RuleSeverity severity;

    /**
     * Indique si la règle est active et doit être exécutée par le scheduler
     */
    @Column(nullable = false)
    private Boolean active;

    /**
     * Date de création de la règle
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Date de dernière mise à jour de la règle
     */
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
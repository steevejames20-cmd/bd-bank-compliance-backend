package com.bridge.bdbank.persistence;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

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
     * Nom métier de la règle, choisi par l'utilisateur avant l'enregistrement
     * (ex: "Solde négatif non autorisé"). Sert à identifier la règle dans
     * l'interface, à la différence de dslText qui est la traduction technique.
     */
    @Column(nullable = false, length = 150)
    private String name;

    /**
     * Description libre de la règle, saisie par l'utilisateur en complément
     * du nom (facultative).
     */
    @Column(length = 1000)
    private String description;

    /**
     * La règle écrite en DSL (ex: "age < 18", "SUM(transactions.montant) > 1000 GROUP BY client_id")
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
     * Adresses mail à notifier lorsqu'une alerte apparaît (nouvelle anomalie
     * ou anomalie réactivée) pour cette règle. Le contenu du mail reflète la
     * gravité (severity) de la règle. Vide si aucune notification souhaitée.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "rule_notification_emails", joinColumns = @JoinColumn(name = "rule_id"))
    @Column(name = "email", length = 254)
    @Builder.Default
    private Set<String> notificationEmails = new HashSet<>();

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
        if (notificationEmails == null) {
            notificationEmails = new HashSet<>();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
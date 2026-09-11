package com.bridge.bdbank.persistence;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;

/**
 * Stocke les renommages logiques des tables et colonnes de la bd_bank.
 * Ces renommages sont purement visuels dans l'interface Bridge et ne modifient
 * pas la structure réelle de la base de données bd_bank.
 */
@Entity
@Table(name = "table_mappings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE table_mappings SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
public class TableMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Nom original de la table dans la bd_bank (ex: "clients")
     */
    @Column(name = "original_table_name", nullable = false, length = 100)
    private String originalTableName;

    /**
     * Nom logique affiché dans l'interface Bridge (ex: "Clients")
     */
    @Column(name = "logical_table_name", length = 100)
    private String logicalTableName;

    /**
     * Nom original de la colonne dans la bd_bank (ex: "id_client")
     */
    @Column(name = "original_column_name", length = 100)
    private String originalColumnName;

    /**
     * Nom logique affiché dans l'interface Bridge (ex: "ID Client")
     */
    @Column(name = "logical_column_name", length = 100)
    private String logicalColumnName;

    /**
     * Description optionnelle du renommage
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * Indique si ce mapping est actif
     */
    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

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

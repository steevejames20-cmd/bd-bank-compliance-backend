package com.bridge.bdbank.persistence;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Alias d'affichage pour une table ou une colonne réelle de la bd_bank.
 * <p>
 * Bridge est strictement en lecture sur la bd_bank : cet alias ne renomme
 * jamais rien côté base, il ne fait que fournir un nom lisible côté
 * interface (espace "Schéma &amp; Périmètre") à la place du nom technique
 * réel récupéré par introspection (ex: "accounts"/"sld-cli" -> "comptes"/"solde").
 * <p>
 * Quand realColumnName est null, l'alias porte sur la table elle-même.
 * Sinon, il porte sur une colonne précise de cette table.
 */
@Entity
@Table(name = "schema_aliases", uniqueConstraints = {
    @UniqueConstraint(name = "uk_schema_alias_table_column", columnNames = {"real_table_name", "real_column_name"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchemaAlias {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Nom technique réel de la table, tel que renvoyé par l'introspection JDBC.
     */
    @Column(name = "real_table_name", nullable = false, length = 100)
    private String realTableName;

    /**
     * Nom technique réel de la colonne. Null si l'alias porte sur la table elle-même.
     */
    @Column(name = "real_column_name", length = 100)
    private String realColumnName;

    /**
     * Nom lisible affiché côté interface à la place du nom technique.
     */
    @Column(name = "display_name", nullable = false, length = 150)
    private String displayName;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

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

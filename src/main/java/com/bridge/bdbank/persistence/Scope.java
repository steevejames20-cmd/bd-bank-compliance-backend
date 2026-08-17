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
 * Entité représentant le périmètre de tables surveillées.
 * Permet de persister la configuration du périmètre pour modification à chaud.
 */
@Entity
@Table(name = "scopes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Scope {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Nom unique pour identifier cette configuration de périmètre
     * (ex: "default", "production", "test")
     */
    @Column(nullable = false, unique = true, length = 100)
    private String name;

    /**
     * Description de ce périmètre
     */
    @Column(length = 500)
    private String description;

    /**
     * Indique si c'est le périmètre actif actuellement
     */
    @Column(nullable = false)
    private Boolean active;

    /**
     * Liste des tables incluses dans ce périmètre
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "scope_tables", joinColumns = @JoinColumn(name = "scope_id"))
    @Column(name = "table_name", length = 100)
    private Set<String> tables;

    /**
     * Date de création de cette configuration
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Date de dernière mise à jour
     */
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (active == null) {
            active = false;
        }
        if (tables == null) {
            tables = new HashSet<>();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
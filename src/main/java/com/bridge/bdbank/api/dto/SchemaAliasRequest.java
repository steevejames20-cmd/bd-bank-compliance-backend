package com.bridge.bdbank.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour poser ou mettre à jour un alias.
 * columnName reste vide pour un alias de table.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchemaAliasRequest {

    @NotBlank(message = "Le nom de la table réelle est obligatoire")
    private String tableName;

    /**
     * Nom réel de la colonne. Laisser vide pour un alias portant sur la table elle-même.
     */
    private String columnName;

    @NotBlank(message = "Le nom d'affichage est obligatoire")
    private String displayName;
}

package com.bridge.bdbank.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour la création/mise à jour d'un mapping de renommage.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableMappingRequest {

    /**
     * Nom original de la table dans la bd_bank (obligatoire)
     */
    @NotBlank(message = "Le nom original de la table est obligatoire")
    @Size(max = 100, message = "Le nom original ne peut pas dépasser 100 caractères")
    private String originalTableName;

    /**
     * Nom logique affiché dans l'interface Bridge (optionnel pour le renommage de table)
     */
    @Size(max = 100, message = "Le nom logique ne peut pas dépasser 100 caractères")
    private String logicalTableName;

    /**
     * Nom original de la colonne dans la bd_bank (optionnel pour le renommage de table)
     */
    @Size(max = 100, message = "Le nom original de la colonne ne peut pas dépasser 100 caractères")
    private String originalColumnName;

    /**
     * Nom logique affiché dans l'interface Bridge (optionnel pour le renommage de colonne)
     */
    @Size(max = 100, message = "Le nom logique de la colonne ne peut pas dépasser 100 caractères")
    private String logicalColumnName;

    /**
     * Description optionnelle du renommage
     */
    @Size(max = 500, message = "La description ne peut pas dépasser 500 caractères")
    private String description;

    /**
     * Indique si ce mapping est actif
     */
    @Builder.Default
    private Boolean active = true;
}

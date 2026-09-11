package com.bridge.bdbank.api.dto;

import com.bridge.bdbank.persistence.RuleSeverity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * DTO pour la création et mise à jour de règles.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleRequest {

    @NotBlank(message = "Le nom de la règle est obligatoire")
    private String name;

    private String description;

    @NotBlank(message = "Le texte DSL est obligatoire")
    private String dslText;

    @NotBlank(message = "La table cible est obligatoire")
    private String targetTable;

    @NotNull(message = "La gravité est obligatoire")
    private RuleSeverity severity;

    @Builder.Default
    private Boolean active = true;

    /**
     * Adresses mail à notifier pour cette règle (facultatif).
     */
    @Builder.Default
    private Set<String> notificationEmails = Set.of();
}
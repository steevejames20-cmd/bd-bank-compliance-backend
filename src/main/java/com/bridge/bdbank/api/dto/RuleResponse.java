package com.bridge.bdbank.api.dto;

import com.bridge.bdbank.persistence.RuleSeverity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO pour la réponse contenant les informations d'une règle.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleResponse {

    private Long id;
    private String dslText;
    private String targetTable;
    private RuleSeverity severity;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
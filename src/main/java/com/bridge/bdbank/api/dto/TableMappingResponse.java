package com.bridge.bdbank.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO pour la réponse d'un mapping de renommage.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableMappingResponse {

    private Long id;
    private String originalTableName;
    private String logicalTableName;
    private String originalColumnName;
    private String logicalColumnName;
    private String description;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

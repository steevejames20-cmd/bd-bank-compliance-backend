package com.bridge.bdbank.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de réponse pour un alias de table/colonne.
 * columnName est null pour un alias de table.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchemaAliasResponse {

    private Long id;
    private String tableName;
    private String columnName;
    private String displayName;
    private LocalDateTime updatedAt;
}

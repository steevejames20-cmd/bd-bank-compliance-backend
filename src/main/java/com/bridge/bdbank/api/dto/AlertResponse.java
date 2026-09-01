package com.bridge.bdbank.api.dto;

import com.bridge.bdbank.persistence.AlertStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO pour la réponse d'une alerte.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertResponse {
    
    private Long id;
    private Long ruleId;
    private AlertStatus status;
    private LocalDateTime detectedAt;
    private LocalDateTime resolvedAt;
    private String violatingEntityId;
    private List<String> involvedColumns;
    private Integer consecutiveDetections;
}
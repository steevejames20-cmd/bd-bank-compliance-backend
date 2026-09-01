package com.bridge.bdbank.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour la réponse de configuration de fréquence.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FrequencyConfigResponse {
    
    private String interval;        // ex: "5m", "1h", "30m"
    private String cronExpression;  // ex: "0 */5 * * * *" pour tous les 5 minutes
    private String enabled;          // "true" ou "false"
}
package com.bridge.bdbank.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de réponse pour une entrée d'historique de notification.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationHistoryResponse {

    private Long id;
    private Long ruleId;
    private String ruleName;
    private Long alertId;
    private String recipients;
    private String subject;
    private String body;
    private LocalDateTime sentAt;
    private boolean success;
    private String errorMessage;
}

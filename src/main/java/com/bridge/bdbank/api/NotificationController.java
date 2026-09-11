package com.bridge.bdbank.api;

import com.bridge.bdbank.api.dto.NotificationHistoryResponse;
import com.bridge.bdbank.auth.AuthenticationException;
import com.bridge.bdbank.auth.AuthenticationService;
import com.bridge.bdbank.persistence.NotificationHistory;
import com.bridge.bdbank.persistence.NotificationHistoryRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Contrôleur REST pour l'historique des notifications envoyées (audit).
 * Chaque tentative d'envoi (réussie ou échouée) déclenchée par
 * NotificationService y est consultable, avec le contenu exact du mail
 * au moment de l'envoi.
 */
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Historique des notifications mail envoyées par règle")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationHistoryRepository notificationHistoryRepository;
    private final AuthenticationService authenticationService;

    /**
     * Liste l'historique des notifications envoyées, du plus récent au plus
     * ancien, avec filtre optionnel par règle.
     * GET /notifications/history?ruleId=1&page=0&size=25
     */
    @Operation(summary = "Lister l'historique des notifications", description = "Récupère les notifications envoyées (réussies ou échouées), avec filtre optionnel par règle")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Historique récupéré avec succès"),
        @ApiResponse(responseCode = "401", description = "Non authentifié"),
        @ApiResponse(responseCode = "500", description = "Erreur interne du serveur")
    })
    @GetMapping("/history")
    public ResponseEntity<Page<NotificationHistoryResponse>> listHistory(
            @Parameter(description = "Filtre par ID de règle") @RequestParam(required = false) Long ruleId,
            @Parameter(description = "Numéro de page (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de la page") @RequestParam(defaultValue = "25") int size,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        authenticate(authHeader);

        Pageable pageable = PageRequest.of(page, size);
        Page<NotificationHistory> result = ruleId != null
            ? notificationHistoryRepository.findByRuleIdOrderBySentAtDesc(ruleId, pageable)
            : notificationHistoryRepository.findAllByOrderBySentAtDesc(pageable);

        return ResponseEntity.ok(result.map(this::toResponse));
    }

    private void authenticate(String authHeader) {
        String token = extractToken(authHeader);
        if (token != null) {
            authenticationService.validateToken(token);
        } else {
            throw new AuthenticationException("Token manquant");
        }
    }

    private String extractToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        return authHeader.substring(7);
    }

    private NotificationHistoryResponse toResponse(NotificationHistory history) {
        return NotificationHistoryResponse.builder()
            .id(history.getId())
            .ruleId(history.getRuleId())
            .ruleName(history.getRuleName())
            .alertId(history.getAlertId())
            .recipients(history.getRecipients())
            .subject(history.getSubject())
            .body(history.getBody())
            .sentAt(history.getSentAt())
            .success(history.isSuccess())
            .errorMessage(history.getErrorMessage())
            .build();
    }
}

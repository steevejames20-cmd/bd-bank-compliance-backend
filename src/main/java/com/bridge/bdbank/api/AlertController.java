package com.bridge.bdbank.api;

import com.bridge.bdbank.api.dto.AlertResponse;
import com.bridge.bdbank.auth.AuthenticationException;
import com.bridge.bdbank.auth.AuthenticationService;
import com.bridge.bdbank.persistence.Alert;
import com.bridge.bdbank.persistence.AlertRepository;
import com.bridge.bdbank.persistence.AlertStatus;
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
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Contrôleur REST pour la gestion des alertes.
 * Fournit les endpoints pour lister et consulter les alertes.
 */
@RestController
@RequestMapping("/alerts")
@RequiredArgsConstructor
@Tag(name = "Alertes", description = "Gestion des alertes générées par les règles de conformité")
@SecurityRequirement(name = "bearerAuth")
public class AlertController {

    private final AlertRepository alertRepository;
    private final AuthenticationService authenticationService;

    private static final int DEFAULT_PAGE_SIZE = 25;

    /**
     * Liste toutes les alertes avec pagination et filtre optionnel sur le statut.
     * GET /alerts?page=0&size=25&status=ACTIVE
     */
    @Operation(summary = "Lister les alertes", description = "Récupère toutes les alertes avec pagination et filtre optionnel par statut")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Liste des alertes récupérée avec succès"),
        @ApiResponse(responseCode = "401", description = "Non authentifié"),
        @ApiResponse(responseCode = "500", description = "Erreur interne du serveur")
    })
    @GetMapping
    public ResponseEntity<Page<AlertResponse>> listAlerts(
            @Parameter(description = "Numéro de page (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de la page") @RequestParam(defaultValue = "25") int size,
            @Parameter(description = "Filtre par statut d'alerte") @RequestParam(required = false) AlertStatus status,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        authenticate(authHeader);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("detectedAt").descending());
        Page<Alert> alerts;
        
        if (status != null) {
            alerts = alertRepository.findByStatus(status, pageable);
        } else {
            alerts = alertRepository.findAll(pageable);
        }
        
        Page<AlertResponse> response = alerts.map(this::toResponse);
        return ResponseEntity.ok(response);
    }

    /**
     * Récupère une alerte par son ID.
     * GET /alerts/{id}
     */
    @Operation(summary = "Récupérer une alerte", description = "Récupère les détails d'une alerte spécifique par son ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Alerte récupérée avec succès"),
        @ApiResponse(responseCode = "401", description = "Non authentifié"),
        @ApiResponse(responseCode = "404", description = "Alerte non trouvée"),
        @ApiResponse(responseCode = "500", description = "Erreur interne du serveur")
    })
    @GetMapping("/{id}")
    public ResponseEntity<AlertResponse> getAlert(
            @Parameter(description = "ID de l'alerte") @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        authenticate(authHeader);
        
        return alertRepository.findById(id)
            .map(alert -> ResponseEntity.ok(toResponse(alert)))
            .orElse(ResponseEntity.status(404).build());
    }

    /**
     * Authentifie l'utilisateur via le token.
     */
    private void authenticate(String authHeader) {
        String token = extractToken(authHeader);
        if (token != null) {
            authenticationService.validateToken(token);
        } else {
            throw new AuthenticationException("Token manquant");
        }
    }

    /**
     * Extrait le token du header Authorization.
     */
    private String extractToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        return authHeader.substring(7);
    }

    /**
     * Convertit une entité Alert en DTO AlertResponse.
     */
    private AlertResponse toResponse(Alert alert) {
        return AlertResponse.builder()
            .id(alert.getId())
            .ruleId(alert.getRuleId())
            .status(alert.getStatus())
            .detectedAt(alert.getDetectedAt())
            .resolvedAt(alert.getResolvedAt())
            .violatingEntityId(alert.getViolatingEntityId())
            .involvedColumns(alert.getInvolvedColumns())
            .consecutiveDetections(alert.getConsecutiveDetections())
            .build();
    }
}
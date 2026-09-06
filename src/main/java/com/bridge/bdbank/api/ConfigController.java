package com.bridge.bdbank.api;

import com.bridge.bdbank.api.dto.FrequencyConfigRequest;
import com.bridge.bdbank.api.dto.FrequencyConfigResponse;
import com.bridge.bdbank.auth.AuthenticationException;
import com.bridge.bdbank.auth.AuthenticationService;
import com.bridge.bdbank.execution.FrequencyConfigService;
import com.bridge.bdbank.persistence.FrequencyConfig;
import com.bridge.bdbank.persistence.FrequencyConfigRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Contrôleur REST pour la configuration de l'application.
 * Fournit les endpoints pour la configuration de fréquence d'exécution.
 */
@RestController
@RequestMapping("/config")
@RequiredArgsConstructor
@Tag(name = "Configuration", description = "Configuration de l'application")
@SecurityRequirement(name = "bearerAuth")
public class ConfigController {

    private final FrequencyConfigRepository frequencyConfigRepository;
    private final FrequencyConfigService frequencyConfigService;
    private final AuthenticationService authenticationService;

    /**
     * Récupère la configuration de fréquence actuelle.
     * GET /config/frequency
     */
    @Operation(summary = "Récupérer la configuration de fréquence", description = "Récupère la configuration actuelle de fréquence d'exécution")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Configuration récupérée avec succès"),
        @ApiResponse(responseCode = "401", description = "Non authentifié"),
        @ApiResponse(responseCode = "404", description = "Aucune configuration active trouvée"),
        @ApiResponse(responseCode = "500", description = "Erreur interne du serveur")
    })
    @GetMapping("/frequency")
    public ResponseEntity<FrequencyConfigResponse> getFrequencyConfig(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        authenticate(authHeader);
        
        return frequencyConfigRepository.findByActiveTrue()
            .map(config -> ResponseEntity.ok(toResponse(config)))
            .orElse(ResponseEntity.status(404).build());
    }

    /**
     * Met à jour la configuration de fréquence.
     * PUT /config/frequency
     */
    @Operation(summary = "Mettre à jour la configuration de fréquence", description = "Met à jour la configuration de fréquence d'exécution")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Configuration mise à jour avec succès"),
        @ApiResponse(responseCode = "400", description = "Requête invalide"),
        @ApiResponse(responseCode = "401", description = "Non authentifié"),
        @ApiResponse(responseCode = "500", description = "Erreur interne du serveur")
    })
    @PutMapping("/frequency")
    public ResponseEntity<FrequencyConfigResponse> updateFrequencyConfig(
            @Parameter(description = "Configuration de fréquence à mettre à jour") @RequestBody FrequencyConfigRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        authenticate(authHeader);
        
        // Utiliser le service pour mettre à jour la configuration
        FrequencyConfig updatedConfig = frequencyConfigService.updateConfig(request);
        
        return ResponseEntity.ok(toResponse(updatedConfig));
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
     * Convertit une entité FrequencyConfig en DTO FrequencyConfigResponse.
     */
    private FrequencyConfigResponse toResponse(FrequencyConfig config) {
        return FrequencyConfigResponse.builder()
            .interval(config.getType() == FrequencyConfig.FrequencyType.INTERVAL 
                ? config.getIntervalMinutes() + "m" 
                : null)
            .cronExpression(config.getCronExpression())
            .enabled(config.getActive().toString())
            .nextCycleAt(frequencyConfigService.computeNextCycleAt(config))
            .build();
    }
}
package com.bridge.bdbank.api;

import com.bridge.bdbank.api.dto.FrequencyConfigRequest;
import com.bridge.bdbank.api.dto.FrequencyConfigResponse;
import com.bridge.bdbank.auth.AuthenticationException;
import com.bridge.bdbank.auth.AuthenticationService;
import com.bridge.bdbank.execution.FrequencyConfigService;
import com.bridge.bdbank.persistence.FrequencyConfig;
import com.bridge.bdbank.persistence.FrequencyConfigRepository;
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
public class ConfigController {

    private final FrequencyConfigRepository frequencyConfigRepository;
    private final FrequencyConfigService frequencyConfigService;
    private final AuthenticationService authenticationService;

    /**
     * Récupère la configuration de fréquence actuelle.
     * GET /config/frequency
     */
    @GetMapping("/frequency")
    public ResponseEntity<FrequencyConfigResponse> getFrequencyConfig(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            authenticate(authHeader);
            
            return frequencyConfigRepository.findByActiveTrue()
                .map(config -> ResponseEntity.ok(toResponse(config)))
                .orElse(ResponseEntity.status(404).build());
        } catch (AuthenticationException e) {
            return ResponseEntity.status(401).build();
        }
    }

    /**
     * Met à jour la configuration de fréquence.
     * PUT /config/frequency
     */
    @PutMapping("/frequency")
    public ResponseEntity<FrequencyConfigResponse> updateFrequencyConfig(
            @RequestBody FrequencyConfigRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            authenticate(authHeader);
            
            // Utiliser le service pour mettre à jour la configuration
            FrequencyConfig updatedConfig = frequencyConfigService.updateConfig(request);
            
            return ResponseEntity.ok(toResponse(updatedConfig));
        } catch (AuthenticationException e) {
            return ResponseEntity.status(401).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).build();
        }
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
            .build();
    }
}
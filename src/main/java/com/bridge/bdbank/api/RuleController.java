package com.bridge.bdbank.api;

import com.bridge.bdbank.api.dto.RuleRequest;
import com.bridge.bdbank.api.dto.RuleResponse;
import com.bridge.bdbank.auth.AuthenticationException;
import com.bridge.bdbank.auth.AuthenticationService;
import com.bridge.bdbank.persistence.Rule;
import com.bridge.bdbank.persistence.RuleRepository;
import com.bridge.bdbank.validation.RuleValidationService;
import com.bridge.bdbank.validation.ValidationResult;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur REST pour la gestion des règles de conformité.
 * Fournit les endpoints CRUD et la validation des règles.
 */
@RestController
@RequestMapping("/rules")
@RequiredArgsConstructor
@Tag(name = "Règles", description = "Gestion des règles de conformité")
@SecurityRequirement(name = "bearerAuth")
public class RuleController {

    private final RuleRepository ruleRepository;
    private final AuthenticationService authenticationService;
    private final RuleValidationService ruleValidationService;

    private static final int DEFAULT_PAGE_SIZE = 25;

    /**
     * Liste toutes les règles avec pagination.
     * GET /rules?page=0&size=25
     */
    @Operation(summary = "Lister les règles", description = "Récupère toutes les règles avec pagination")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Liste des règles récupérée avec succès"),
        @ApiResponse(responseCode = "401", description = "Non authentifié"),
        @ApiResponse(responseCode = "500", description = "Erreur interne du serveur")
    })
    @GetMapping
    public ResponseEntity<Page<RuleResponse>> listRules(
            @Parameter(description = "Numéro de page (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de la page") @RequestParam(defaultValue = "25") int size,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        authenticate(authHeader);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Rule> rules = ruleRepository.findAll(pageable);
        
        Page<RuleResponse> response = rules.map(this::toResponse);
        return ResponseEntity.ok(response);
    }

    /**
     * Récupère une règle par son ID.
     * GET /rules/{id}
     */
    @Operation(summary = "Récupérer une règle", description = "Récupère les détails d'une règle spécifique par son ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Règle récupérée avec succès"),
        @ApiResponse(responseCode = "401", description = "Non authentifié"),
        @ApiResponse(responseCode = "404", description = "Règle non trouvée"),
        @ApiResponse(responseCode = "500", description = "Erreur interne du serveur")
    })
    @GetMapping("/{id}")
    public ResponseEntity<RuleResponse> getRule(
            @Parameter(description = "ID de la règle") @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        authenticate(authHeader);
        
        return ruleRepository.findById(id)
            .map(rule -> ResponseEntity.ok(toResponse(rule)))
            .orElse(ResponseEntity.status(404).build());
    }

    /**
     * Crée une nouvelle règle.
     * POST /rules
     */
    @Operation(summary = "Créer une règle", description = "Crée une nouvelle règle de conformité")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Règle créée avec succès"),
        @ApiResponse(responseCode = "400", description = "Requête invalide"),
        @ApiResponse(responseCode = "401", description = "Non authentifié"),
        @ApiResponse(responseCode = "500", description = "Erreur interne du serveur")
    })
    @PostMapping
    public ResponseEntity<RuleResponse> createRule(
            @Parameter(description = "Détails de la règle à créer") @RequestBody RuleRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        authenticate(authHeader);
        
        Rule rule = Rule.builder()
            .dslText(request.getDslText())
            .targetTable(request.getTargetTable())
            .severity(request.getSeverity())
            .active(request.getActive())
            .build();
        
        Rule savedRule = ruleRepository.save(rule);
        return ResponseEntity.status(201).body(toResponse(savedRule));
    }

    /**
     * Met à jour une règle existante.
     * PUT /rules/{id}
     */
    @Operation(summary = "Mettre à jour une règle", description = "Met à jour une règle existante")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Règle mise à jour avec succès"),
        @ApiResponse(responseCode = "400", description = "Requête invalide"),
        @ApiResponse(responseCode = "401", description = "Non authentifié"),
        @ApiResponse(responseCode = "404", description = "Règle non trouvée"),
        @ApiResponse(responseCode = "500", description = "Erreur interne du serveur")
    })
    @PutMapping("/{id}")
    public ResponseEntity<RuleResponse> updateRule(
            @Parameter(description = "ID de la règle à mettre à jour") @PathVariable Long id,
            @Parameter(description = "Détails de la règle à mettre à jour") @RequestBody RuleRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        authenticate(authHeader);
        
        return ruleRepository.findById(id)
            .map(rule -> {
                rule.setDslText(request.getDslText());
                rule.setTargetTable(request.getTargetTable());
                rule.setSeverity(request.getSeverity());
                rule.setActive(request.getActive());
                Rule updatedRule = ruleRepository.save(rule);
                return ResponseEntity.ok(toResponse(updatedRule));
            })
            .orElse(ResponseEntity.status(404).build());
    }

    /**
     * Supprime une règle.
     * DELETE /rules/{id}
     */
    @Operation(summary = "Supprimer une règle", description = "Supprime une règle existante")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Règle supprimée avec succès"),
        @ApiResponse(responseCode = "401", description = "Non authentifié"),
        @ApiResponse(responseCode = "404", description = "Règle non trouvée"),
        @ApiResponse(responseCode = "500", description = "Erreur interne du serveur")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRule(
            @Parameter(description = "ID de la règle à supprimer") @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        authenticate(authHeader);
        
        if (ruleRepository.existsById(id)) {
            ruleRepository.deleteById(id);
            return ResponseEntity.status(204).build();
        }
        return ResponseEntity.status(404).build();
    }

    /**
     * Valide une règle sans la sauvegarder.
     * POST /rules/validate
     */
    @Operation(summary = "Valider une règle", description = "Valide une règle sans la sauvegarder")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Validation effectuée avec succès"),
        @ApiResponse(responseCode = "400", description = "Requête invalide"),
        @ApiResponse(responseCode = "401", description = "Non authentifié"),
        @ApiResponse(responseCode = "500", description = "Erreur interne du serveur")
    })
    @PostMapping("/validate")
    public ResponseEntity<ValidationResult> validateRule(
            @Parameter(description = "Détails de la règle à valider") @RequestBody RuleRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        authenticate(authHeader);
        
        ValidationResult result = ruleValidationService.validate(
            request.getDslText(), 
            request.getTargetTable()
        );
        return ResponseEntity.ok(result);
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
     * Convertit une entité Rule en DTO RuleResponse.
     */
    private RuleResponse toResponse(Rule rule) {
        return RuleResponse.builder()
            .id(rule.getId())
            .dslText(rule.getDslText())
            .targetTable(rule.getTargetTable())
            .severity(rule.getSeverity())
            .active(rule.getActive())
            .createdAt(rule.getCreatedAt())
            .updatedAt(rule.getUpdatedAt())
            .build();
    }
}
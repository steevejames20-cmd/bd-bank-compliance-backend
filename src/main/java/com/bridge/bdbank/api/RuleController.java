package com.bridge.bdbank.api;

import com.bridge.bdbank.api.dto.RuleRequest;
import com.bridge.bdbank.api.dto.RuleResponse;
import com.bridge.bdbank.auth.AuthenticationException;
import com.bridge.bdbank.auth.AuthenticationService;
import com.bridge.bdbank.persistence.Rule;
import com.bridge.bdbank.persistence.RuleRepository;
import com.bridge.bdbank.validation.RuleValidationService;
import com.bridge.bdbank.validation.ValidationResult;
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
public class RuleController {

    private final RuleRepository ruleRepository;
    private final AuthenticationService authenticationService;
    private final RuleValidationService ruleValidationService;

    private static final int DEFAULT_PAGE_SIZE = 25;

    /**
     * Liste toutes les règles avec pagination.
     * GET /rules?page=0&size=25
     */
    @GetMapping
    public ResponseEntity<Page<RuleResponse>> listRules(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            authenticate(authHeader);
            
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            Page<Rule> rules = ruleRepository.findAll(pageable);
            
            Page<RuleResponse> response = rules.map(this::toResponse);
            return ResponseEntity.ok(response);
        } catch (AuthenticationException e) {
            return ResponseEntity.status(401).build();
        }
    }

    /**
     * Récupère une règle par son ID.
     * GET /rules/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<RuleResponse> getRule(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            authenticate(authHeader);
            
            return ruleRepository.findById(id)
                .map(rule -> ResponseEntity.ok(toResponse(rule)))
                .orElse(ResponseEntity.status(404).build());
        } catch (AuthenticationException e) {
            return ResponseEntity.status(401).build();
        }
    }

    /**
     * Crée une nouvelle règle.
     * POST /rules
     */
    @PostMapping
    public ResponseEntity<RuleResponse> createRule(
            @RequestBody RuleRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            authenticate(authHeader);
            
            Rule rule = Rule.builder()
                .dslText(request.getDslText())
                .targetTable(request.getTargetTable())
                .severity(request.getSeverity())
                .active(request.getActive())
                .build();
            
            Rule savedRule = ruleRepository.save(rule);
            return ResponseEntity.status(201).body(toResponse(savedRule));
        } catch (AuthenticationException e) {
            return ResponseEntity.status(401).build();
        }
    }

    /**
     * Met à jour une règle existante.
     * PUT /rules/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<RuleResponse> updateRule(
            @PathVariable Long id,
            @RequestBody RuleRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
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
        } catch (AuthenticationException e) {
            return ResponseEntity.status(401).build();
        }
    }

    /**
     * Supprime une règle.
     * DELETE /rules/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRule(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            authenticate(authHeader);
            
            if (ruleRepository.existsById(id)) {
                ruleRepository.deleteById(id);
                return ResponseEntity.status(204).build();
            }
            return ResponseEntity.status(404).build();
        } catch (AuthenticationException e) {
            return ResponseEntity.status(401).build();
        }
    }

    /**
     * Valide une règle sans la sauvegarder.
     * POST /rules/validate
     */
    @PostMapping("/validate")
    public ResponseEntity<ValidationResult> validateRule(
            @RequestBody RuleRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            authenticate(authHeader);
            
            ValidationResult result = ruleValidationService.validate(
                request.getDslText(), 
                request.getTargetTable()
            );
            return ResponseEntity.ok(result);
        } catch (AuthenticationException e) {
            return ResponseEntity.status(401).build();
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
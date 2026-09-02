package com.bridge.bdbank.api;

import com.bridge.bdbank.auth.AuthenticationException;
import com.bridge.bdbank.auth.AuthenticationService;
import com.bridge.bdbank.introspection.TableInfo;
import com.bridge.bdbank.scope.ScopeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * Contrôleur REST pour la gestion du périmètre de surveillance.
 * Fournit les endpoints pour lire et modifier le périmètre.
 */
@RestController
@RequestMapping("/scope")
@RequiredArgsConstructor
@Tag(name = "Périmètre", description = "Gestion du périmètre de surveillance")
@SecurityRequirement(name = "bearerAuth")
public class ScopeController {

    private final ScopeService scopeService;
    private final AuthenticationService authenticationService;

    /**
     * Récupère le périmètre actuel (tables surveillées).
     * GET /scope
     */
    @Operation(summary = "Récupérer le périmètre", description = "Récupère les tables actuellement surveillées")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Périmètre récupéré avec succès"),
        @ApiResponse(responseCode = "401", description = "Non authentifié"),
        @ApiResponse(responseCode = "500", description = "Erreur interne du serveur")
    })
    @GetMapping
    public ResponseEntity<List<TableInfo>> getScope(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        authenticate(authHeader);
        List<TableInfo> scopedTables = scopeService.getScopedTables();
        return ResponseEntity.ok(scopedTables);
    }

    /**
     * Met à jour le périmètre de surveillance.
     * PUT /scope
     */
    @Operation(summary = "Mettre à jour le périmètre", description = "Met à jour les tables surveillées (non implémenté)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Périmètre mis à jour"),
        @ApiResponse(responseCode = "401", description = "Non authentifié"),
        @ApiResponse(responseCode = "500", description = "Erreur interne du serveur")
    })
    @PutMapping
    public ResponseEntity<Void> updateScope(
            @Parameter(description = "Liste des tables à surveiller") @RequestBody Set<String> tables,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        authenticate(authHeader);
        scopeService.updateScope(tables);
        return ResponseEntity.ok().build();
    }

    /**
     * Vérifie si une table fait partie du périmètre.
     * GET /scope/{table}
     */
    @Operation(summary = "Vérifier si une table est dans le périmètre", description = "Vérifie si une table spécifique fait partie du périmètre de surveillance")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Vérification effectuée avec succès"),
        @ApiResponse(responseCode = "401", description = "Non authentifié"),
        @ApiResponse(responseCode = "500", description = "Erreur interne du serveur")
    })
    @GetMapping("/{table}")
    public ResponseEntity<Boolean> isInScope(
            @Parameter(description = "Nom de la table") @PathVariable String table,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        authenticate(authHeader);
        boolean inScope = scopeService.isInScope(table);
        return ResponseEntity.ok(inScope);
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
}
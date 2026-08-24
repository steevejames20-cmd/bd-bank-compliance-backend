package com.bridge.bdbank.api;

import com.bridge.bdbank.auth.AuthenticationException;
import com.bridge.bdbank.auth.AuthenticationService;
import com.bridge.bdbank.introspection.TableInfo;
import com.bridge.bdbank.scope.ScopeService;
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
public class ScopeController {

    private final ScopeService scopeService;
    private final AuthenticationService authenticationService;

    /**
     * Récupère le périmètre actuel (tables surveillées).
     * GET /scope
     */
    @GetMapping
    public ResponseEntity<List<TableInfo>> getScope(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            authenticate(authHeader);
            List<TableInfo> scopedTables = scopeService.getScopedTables();
            return ResponseEntity.ok(scopedTables);
        } catch (com.bridge.bdbank.scope.UnknownScopedTableException e) {
            return ResponseEntity.status(500).body(List.of());
        } catch (AuthenticationException e) {
            return ResponseEntity.status(401).build();
        }
    }

    /**
     * Met à jour le périmètre de surveillance.
     * PUT /scope
     */
    @PutMapping
    public ResponseEntity<Void> updateScope(
            @RequestBody Set<String> tables,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            authenticate(authHeader);
            // Note: La mise à jour du périmètre nécessiterait de modifier ScopeProperties
            // Pour l'instant, on retourne un statut 501 (Not Implemented)
            return ResponseEntity.status(501).build();
        } catch (AuthenticationException e) {
            return ResponseEntity.status(401).build();
        }
    }

    /**
     * Vérifie si une table fait partie du périmètre.
     * GET /scope/{table}
     */
    @GetMapping("/{table}")
    public ResponseEntity<Boolean> isInScope(
            @PathVariable String table,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            authenticate(authHeader);
            boolean inScope = scopeService.isInScope(table);
            return ResponseEntity.ok(inScope);
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
}
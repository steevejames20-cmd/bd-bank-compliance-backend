package com.bridge.bdbank.api;

import com.bridge.bdbank.auth.AuthenticationException;
import com.bridge.bdbank.auth.AuthenticationService;
import com.bridge.bdbank.introspection.SchemaIntrospectionService;
import com.bridge.bdbank.introspection.TableInfo;
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

/**
 * Contrôleur REST pour l'introspection du schéma de base de données.
 * Fournit les endpoints pour lister les tables et leurs colonnes.
 */
@RestController
@RequestMapping("/schema")
@RequiredArgsConstructor
@Tag(name = "Schéma", description = "Introspection du schéma de base de données")
@SecurityRequirement(name = "bearerAuth")
public class SchemaController {

    private final SchemaIntrospectionService schemaIntrospectionService;
    private final AuthenticationService authenticationService;

    /**
     * Liste toutes les tables disponibles dans la base de données.
     * GET /schema/tables
     */
    @Operation(summary = "Lister les tables", description = "Récupère toutes les tables disponibles dans la base de données")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Liste des tables récupérée avec succès"),
        @ApiResponse(responseCode = "401", description = "Non authentifié"),
        @ApiResponse(responseCode = "500", description = "Erreur interne du serveur")
    })
    @GetMapping("/tables")
    public ResponseEntity<List<TableInfo>> listTables(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        authenticate(authHeader);
        List<TableInfo> tables = schemaIntrospectionService.listTables();
        return ResponseEntity.ok(tables);
    }

    /**
     * Liste les colonnes d'une table spécifique.
     * GET /schema/tables/{table}/columns
     */
    @Operation(summary = "Lister les colonnes d'une table", description = "Récupère les colonnes d'une table spécifique")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Liste des colonnes récupérée avec succès"),
        @ApiResponse(responseCode = "401", description = "Non authentifié"),
        @ApiResponse(responseCode = "404", description = "Table non trouvée"),
        @ApiResponse(responseCode = "500", description = "Erreur interne du serveur")
    })
    @GetMapping("/tables/{table}/columns")
    public ResponseEntity<?> listColumns(
            @Parameter(description = "Nom de la table") @PathVariable String table,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        authenticate(authHeader);
        List<?> columns = schemaIntrospectionService.listColumns(table);
        return ResponseEntity.ok(columns);
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
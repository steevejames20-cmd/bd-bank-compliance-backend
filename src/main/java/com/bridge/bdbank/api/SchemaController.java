package com.bridge.bdbank.api;

import com.bridge.bdbank.auth.AuthenticationException;
import com.bridge.bdbank.auth.AuthenticationService;
import com.bridge.bdbank.introspection.SchemaIntrospectionService;
import com.bridge.bdbank.introspection.TableInfo;
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
public class SchemaController {

    private final SchemaIntrospectionService schemaIntrospectionService;
    private final AuthenticationService authenticationService;

    /**
     * Liste toutes les tables disponibles dans la base de données.
     * GET /schema/tables
     */
    @GetMapping("/tables")
    public ResponseEntity<List<TableInfo>> listTables(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            authenticate(authHeader);
            List<TableInfo> tables = schemaIntrospectionService.listTables();
            return ResponseEntity.ok(tables);
        } catch (AuthenticationException e) {
            return ResponseEntity.status(401).build();
        }
    }

    /**
     * Liste les colonnes d'une table spécifique.
     * GET /schema/tables/{table}/columns
     */
    @GetMapping("/tables/{table}/columns")
    public ResponseEntity<?> listColumns(
            @PathVariable String table,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            authenticate(authHeader);
            List<?> columns = schemaIntrospectionService.listColumns(table);
            return ResponseEntity.ok(columns);
        } catch (com.bridge.bdbank.introspection.TableNotFoundException e) {
            return ResponseEntity.status(404).build();
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
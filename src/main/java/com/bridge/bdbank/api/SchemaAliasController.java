package com.bridge.bdbank.api;

import com.bridge.bdbank.api.dto.SchemaAliasRequest;
import com.bridge.bdbank.api.dto.SchemaAliasResponse;
import com.bridge.bdbank.auth.AuthenticationException;
import com.bridge.bdbank.auth.AuthenticationService;
import com.bridge.bdbank.introspection.AliasedColumnInfo;
import com.bridge.bdbank.introspection.AliasedTableInfo;
import com.bridge.bdbank.introspection.SchemaAliasService;
import com.bridge.bdbank.persistence.SchemaAlias;
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
 * Contrôleur REST pour l'espace "Schéma &amp; Périmètre" : permet à
 * l'administrateur de poser des noms d'affichage plus lisibles sur les
 * tables/colonnes réelles de la bd_bank (ex: "sld-cli" -> "solde").
 * <p>
 * Ne modifie jamais la bd_bank elle-même : Bridge y reste strictement en
 * lecture, l'alias ne vit que côté persistance interne de l'outil.
 */
@RestController
@RequestMapping("/schema/aliases")
@RequiredArgsConstructor
@Tag(name = "Alias de schéma", description = "Gestion des noms d'affichage des tables/colonnes (Schéma & Périmètre)")
@SecurityRequirement(name = "bearerAuth")
public class SchemaAliasController {

    private final SchemaAliasService schemaAliasService;
    private final AuthenticationService authenticationService;

    /**
     * Liste toutes les tables de la bd_bank avec leur nom d'affichage.
     * GET /schema/aliases/tables
     */
    @Operation(summary = "Lister les tables avec alias", description = "Récupère toutes les tables avec leur nom d'affichage (alias si défini)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Liste récupérée avec succès"),
        @ApiResponse(responseCode = "401", description = "Non authentifié"),
        @ApiResponse(responseCode = "500", description = "Erreur interne du serveur")
    })
    @GetMapping("/tables")
    public ResponseEntity<List<AliasedTableInfo>> listAliasedTables(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        authenticate(authHeader);
        return ResponseEntity.ok(schemaAliasService.listAliasedTables());
    }

    /**
     * Liste les colonnes d'une table avec leur nom d'affichage.
     * GET /schema/aliases/tables/{table}/columns
     */
    @Operation(summary = "Lister les colonnes avec alias", description = "Récupère les colonnes d'une table avec leur nom d'affichage (alias si défini)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Liste récupérée avec succès"),
        @ApiResponse(responseCode = "401", description = "Non authentifié"),
        @ApiResponse(responseCode = "404", description = "Table non trouvée"),
        @ApiResponse(responseCode = "500", description = "Erreur interne du serveur")
    })
    @GetMapping("/tables/{table}/columns")
    public ResponseEntity<List<AliasedColumnInfo>> listAliasedColumns(
            @Parameter(description = "Nom réel de la table") @PathVariable String table,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        authenticate(authHeader);
        return ResponseEntity.ok(schemaAliasService.listAliasedColumns(table));
    }

    /**
     * Liste tous les alias définis (vue brute d'administration).
     * GET /schema/aliases
     */
    @Operation(summary = "Lister les alias définis", description = "Récupère tous les alias de tables/colonnes actuellement définis")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Liste récupérée avec succès"),
        @ApiResponse(responseCode = "401", description = "Non authentifié"),
        @ApiResponse(responseCode = "500", description = "Erreur interne du serveur")
    })
    @GetMapping
    public ResponseEntity<List<SchemaAliasResponse>> listAliases(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        authenticate(authHeader);
        List<SchemaAliasResponse> response = schemaAliasService.listAllAliases().stream()
            .map(this::toResponse)
            .toList();
        return ResponseEntity.ok(response);
    }

    /**
     * Pose ou met à jour un alias de table ou de colonne.
     * PUT /schema/aliases
     */
    @Operation(summary = "Poser un alias", description = "Pose ou met à jour le nom d'affichage d'une table ou d'une colonne")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Alias enregistré avec succès"),
        @ApiResponse(responseCode = "400", description = "Requête invalide (ex: colonne inexistante)"),
        @ApiResponse(responseCode = "401", description = "Non authentifié"),
        @ApiResponse(responseCode = "404", description = "Table non trouvée dans la bd_bank"),
        @ApiResponse(responseCode = "500", description = "Erreur interne du serveur")
    })
    @PutMapping
    public ResponseEntity<SchemaAliasResponse> upsertAlias(
            @Parameter(description = "Détails de l'alias à poser") @RequestBody SchemaAliasRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        authenticate(authHeader);

        boolean isColumnAlias = request.getColumnName() != null && !request.getColumnName().isBlank();
        SchemaAlias alias = isColumnAlias
            ? schemaAliasService.upsertColumnAlias(request.getTableName(), request.getColumnName(), request.getDisplayName())
            : schemaAliasService.upsertTableAlias(request.getTableName(), request.getDisplayName());

        return ResponseEntity.ok(toResponse(alias));
    }

    /**
     * Supprime un alias (la table/colonne réaffiche alors son nom technique réel).
     * DELETE /schema/aliases/{id}
     */
    @Operation(summary = "Supprimer un alias", description = "Supprime un alias existant")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Alias supprimé avec succès"),
        @ApiResponse(responseCode = "401", description = "Non authentifié"),
        @ApiResponse(responseCode = "404", description = "Alias non trouvé"),
        @ApiResponse(responseCode = "500", description = "Erreur interne du serveur")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAlias(
            @Parameter(description = "ID de l'alias à supprimer") @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        authenticate(authHeader);
        if (!schemaAliasService.existsById(id)) {
            return ResponseEntity.status(404).build();
        }
        schemaAliasService.deleteAlias(id);
        return ResponseEntity.status(204).build();
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
     * Convertit une entité SchemaAlias en DTO SchemaAliasResponse.
     */
    private SchemaAliasResponse toResponse(SchemaAlias alias) {
        return SchemaAliasResponse.builder()
            .id(alias.getId())
            .tableName(alias.getRealTableName())
            .columnName(alias.getRealColumnName())
            .displayName(alias.getDisplayName())
            .updatedAt(alias.getUpdatedAt())
            .build();
    }
}

package com.bridge.bdbank.api;

import com.bridge.bdbank.api.dto.TableMappingRequest;
import com.bridge.bdbank.api.dto.TableMappingResponse;
import com.bridge.bdbank.auth.AuthenticationService;
import com.bridge.bdbank.mapping.TableMappingService;
import com.bridge.bdbank.persistence.TableMapping;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller pour la gestion des renommages logiques des tables et colonnes.
 */
@RestController
@RequestMapping("/api/mappings")
public class TableMappingController {

    private final TableMappingService tableMappingService;
    private final AuthenticationService authenticationService;

    public TableMappingController(TableMappingService tableMappingService,
                                   AuthenticationService authenticationService) {
        this.tableMappingService = tableMappingService;
        this.authenticationService = authenticationService;
    }

    /**
     * Liste tous les mappings actifs.
     */
    @GetMapping
    public ResponseEntity<List<TableMappingResponse>> listMappings(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        authenticationService.validateToken(authHeader);
        
        List<TableMapping> mappings = tableMappingService.findAllActive();
        List<TableMappingResponse> response = mappings.stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    /**
     * Liste les mappings pour une table spécifique.
     */
    @GetMapping("/table/{tableName}")
    public ResponseEntity<List<TableMappingResponse>> listMappingsByTable(
            @PathVariable String tableName,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        authenticationService.validateToken(authHeader);
        
        List<TableMapping> mappings = tableMappingService.findByTableName(tableName);
        List<TableMappingResponse> response = mappings.stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    /**
     * Crée un nouveau mapping.
     */
    @PostMapping
    public ResponseEntity<TableMappingResponse> createMapping(
            @Valid @RequestBody TableMappingRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        authenticationService.validateToken(authHeader);
        
        TableMapping mapping = TableMapping.builder()
                .originalTableName(request.getOriginalTableName())
                .logicalTableName(request.getLogicalTableName())
                .originalColumnName(request.getOriginalColumnName())
                .logicalColumnName(request.getLogicalColumnName())
                .description(request.getDescription())
                .active(request.getActive())
                .build();
        
        TableMapping created = tableMappingService.createMapping(mapping);
        return ResponseEntity.ok(toResponse(created));
    }

    /**
     * Met à jour un mapping existant.
     */
    @PutMapping("/{id}")
    public ResponseEntity<TableMappingResponse> updateMapping(
            @PathVariable Long id,
            @Valid @RequestBody TableMappingRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        authenticationService.validateToken(authHeader);
        
        TableMapping mapping = TableMapping.builder()
                .logicalTableName(request.getLogicalTableName())
                .logicalColumnName(request.getLogicalColumnName())
                .description(request.getDescription())
                .active(request.getActive())
                .build();
        
        TableMapping updated = tableMappingService.updateMapping(id, mapping);
        return ResponseEntity.ok(toResponse(updated));
    }

    /**
     * Supprime un mapping (soft delete).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMapping(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        authenticationService.validateToken(authHeader);
        
        tableMappingService.deleteMapping(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Active ou désactive un mapping.
     */
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<TableMappingResponse> toggleMapping(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        authenticationService.validateToken(authHeader);
        
        TableMapping toggled = tableMappingService.toggleMapping(id);
        return ResponseEntity.ok(toResponse(toggled));
    }

    private TableMappingResponse toResponse(TableMapping mapping) {
        return TableMappingResponse.builder()
                .id(mapping.getId())
                .originalTableName(mapping.getOriginalTableName())
                .logicalTableName(mapping.getLogicalTableName())
                .originalColumnName(mapping.getOriginalColumnName())
                .logicalColumnName(mapping.getLogicalColumnName())
                .description(mapping.getDescription())
                .active(mapping.getActive())
                .createdAt(mapping.getCreatedAt())
                .updatedAt(mapping.getUpdatedAt())
                .build();
    }
}

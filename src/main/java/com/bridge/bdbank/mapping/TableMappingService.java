package com.bridge.bdbank.mapping;

import com.bridge.bdbank.persistence.TableMapping;
import com.bridge.bdbank.persistence.TableMappingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service pour gérer les renommages logiques des tables et colonnes.
 * Ces renommages sont stockés dans la base H2 et appliqués uniquement
 * dans l'interface Bridge, sans modifier la structure de la bd_bank.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TableMappingService {

    private final TableMappingRepository tableMappingRepository;

    /**
     * Récupère tous les mappings actifs.
     */
    public List<TableMapping> findAllActive() {
        return tableMappingRepository.findAll().stream()
                .filter(TableMapping::getActive)
                .toList();
    }

    /**
     * Récupère tous les mappings actifs pour une table donnée.
     */
    public List<TableMapping> findByTableName(String tableName) {
        return tableMappingRepository.findByOriginalTableNameAndActiveTrue(tableName);
    }

    /**
     * Applique le renommage logique à un nom de table.
     * Si aucun mapping n'existe, retourne le nom original.
     */
    public String getLogicalTableName(String originalTableName) {
        Optional<TableMapping> mapping = tableMappingRepository
                .findByOriginalTableNameAndOriginalColumnNameIsNullAndActiveTrue(originalTableName);
        return mapping.map(TableMapping::getLogicalTableName).orElse(originalTableName);
    }

    /**
     * Applique le renommage logique à un nom de colonne.
     * Si aucun mapping n'existe, retourne le nom original.
     */
    public String getLogicalColumnName(String originalTableName, String originalColumnName) {
        Optional<TableMapping> mapping = tableMappingRepository
                .findByOriginalTableNameAndOriginalColumnNameAndActiveTrue(
                        originalTableName, originalColumnName);
        return mapping.map(TableMapping::getLogicalColumnName).orElse(originalColumnName);
    }

    /**
     * Crée un nouveau mapping de renommage.
     */
    @Transactional
    public TableMapping createMapping(TableMapping mapping) {
        log.info("Création d'un nouveau mapping: {} -> {}", 
                mapping.getOriginalTableName(), mapping.getLogicalTableName());
        return tableMappingRepository.save(mapping);
    }

    /**
     * Met à jour un mapping existant.
     */
    @Transactional
    public TableMapping updateMapping(Long id, TableMapping mapping) {
        TableMapping existing = tableMappingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Mapping non trouvé avec l'ID: " + id));
        
        existing.setLogicalTableName(mapping.getLogicalTableName());
        existing.setLogicalColumnName(mapping.getLogicalColumnName());
        existing.setDescription(mapping.getDescription());
        existing.setActive(mapping.getActive());
        
        log.info("Mise à jour du mapping ID {}: {} -> {}", id, 
                existing.getOriginalTableName(), existing.getLogicalTableName());
        return tableMappingRepository.save(existing);
    }

    /**
     * Supprime logiquement un mapping (soft delete).
     */
    @Transactional
    public void deleteMapping(Long id) {
        TableMapping mapping = tableMappingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Mapping non trouvé avec l'ID: " + id));
        mapping.setActive(false);
        tableMappingRepository.save(mapping);
        log.info("Suppression logique du mapping ID {}", id);
    }

    /**
     * Active ou désactive un mapping.
     */
    @Transactional
    public TableMapping toggleMapping(Long id) {
        TableMapping mapping = tableMappingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Mapping non trouvé avec l'ID: " + id));
        mapping.setActive(!mapping.getActive());
        log.info("Toggle du mapping ID {}: active = {}", id, mapping.getActive());
        return tableMappingRepository.save(mapping);
    }
}

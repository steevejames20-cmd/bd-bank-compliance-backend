package com.bridge.bdbank.scope;

import com.bridge.bdbank.introspection.SchemaIntrospectionService;
import com.bridge.bdbank.introspection.TableInfo;
import com.bridge.bdbank.persistence.Scope;
import com.bridge.bdbank.persistence.ScopeRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.HashSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import java.util.stream.Collectors;

/**
 * Le périmètre : quelles tables de la bd_bank l'outil doit réellement
 * surveiller, parmi toutes celles que l'introspection (J3) peut détecter.
 * <p>
 * Réutilise directement {@link SchemaIntrospectionService} plutôt que de
 * réinterroger la base autrement - le périmètre n'est qu'un filtre
 * appliqué par-dessus l'introspection, pas un mécanisme séparé.
 */
@Service
@RequiredArgsConstructor
public class ScopeService {

    private static final Logger log = LoggerFactory.getLogger(ScopeService.class);

    private final SchemaIntrospectionService schemaIntrospectionService;
    private final ScopeProperties scopeProperties;
    private ScopeRepository scopeRepository;

    @Autowired
    public void setScopeRepository(ScopeRepository scopeRepository) {
        this.scopeRepository = scopeRepository;
    }

    /**
     * Retourne les tables du périmètre, validées contre le schéma réel.
     *
     * @throws UnknownScopedTableException si une table déclarée dans le
     * périmètre n'existe pas réellement dans la bd_bank.
     */
    public List<TableInfo> getScopedTables() {
        List<TableInfo> allTables = schemaIntrospectionService.listTables();
        Set<String> allTableNames = allTables.stream()
                .map(TableInfo::name)
                .collect(Collectors.toSet());

        List<String> declaredScope = scopeRepository == null
            ? scopeProperties.getTables()
            : scopeRepository.findByActiveTrue()
                .map(scope -> scope.getTables().stream().toList())
                .orElse(scopeProperties.getTables());

        for (String tableName : declaredScope) {
            if (!allTableNames.contains(tableName)) {
                throw new UnknownScopedTableException(tableName);
            }
        }

        List<TableInfo> scoped = allTables.stream()
                .filter(table -> declaredScope.contains(table.name()))
                .toList();

        log.info("Périmètre surveillé : {} table(s) -> {}",
                scoped.size(), scoped.stream().map(TableInfo::name).toList());
        return scoped;
    }

    @Transactional
    public void updateScope(Set<String> tables) {
        if (tables == null || tables.isEmpty()) {
            throw new IllegalArgumentException("Le périmètre doit contenir au moins une table");
        }

        Set<String> availableTables = schemaIntrospectionService.listTables().stream()
                .map(TableInfo::name)
                .collect(Collectors.toSet());
        for (String table : tables) {
            if (!availableTables.contains(table)) {
                throw new UnknownScopedTableException(table);
            }
        }

        if (scopeRepository == null) {
            throw new IllegalStateException("La persistance du périmètre n'est pas disponible");
        }
        scopeRepository.deactivateAll();
        Scope scope = scopeRepository.findByName("default").orElseGet(() -> Scope.builder().name("default").build());
        scope.setDescription("Périmètre principal de surveillance");
        scope.setTables(new HashSet<>(tables));
        scope.setActive(true);
        scopeRepository.save(scope);
    }

    /**
     * Utile plus tard (semaines 2-3) pour vérifier rapidement si une table
     * référencée par une règle DSL fait bien partie du périmètre autorisé.
     */
    public boolean isInScope(String tableName) {
        return scopeProperties.getTables().contains(tableName);
    }
}

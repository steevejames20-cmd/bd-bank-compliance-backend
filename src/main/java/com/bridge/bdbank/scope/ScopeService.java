package com.bridge.bdbank.scope;

import com.bridge.bdbank.introspection.SchemaIntrospectionService;
import com.bridge.bdbank.introspection.TableInfo;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
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

        List<String> declaredScope = scopeProperties.getTables();

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

    /**
     * Utile plus tard (semaines 2-3) pour vérifier rapidement si une table
     * référencée par une règle DSL fait bien partie du périmètre autorisé.
     */
    public boolean isInScope(String tableName) {
        return scopeProperties.getTables().contains(tableName);
    }
}

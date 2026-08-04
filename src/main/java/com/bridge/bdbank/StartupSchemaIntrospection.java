package com.bridge.bdbank.introspection;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Affiche dans les logs, au démarrage, les tables et colonnes découvertes
 * sur la bd_bank. Purement diagnostique pour le J3 (comme
 * StartupConnectionCheck l'était pour le J2) — une vraie exposition de ces
 * infos (via l'API REST) arrive en semaine 4.
 * <p>
 * {@code @Order(2)} : s'exécute après StartupConnectionCheck. Sans
 * annotation {@code @Order} explicite, Spring exécute les runners dans un
 * ordre non garanti (par défaut, priorité la plus basse) — donc les deux
 * classes déclarent leur ordre explicitement plutôt que de compter sur un
 * comportement implicite.
 */
@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class StartupSchemaIntrospection implements ApplicationRunner {

    private final SchemaIntrospectionService schemaIntrospectionService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        List<TableInfo> tables = schemaIntrospectionService.listTables();

        for (TableInfo table : tables) {
            schemaIntrospectionService.listColumns(table.name());
        }
    }
}

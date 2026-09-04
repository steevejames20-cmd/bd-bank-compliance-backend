package com.bridge.bdbank.connection;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Vérifie que la couche JDBC parle bien à la base de test MySQL
 * (`bd_bank_test`, compte lecture seule).
 *
 * Prérequis : `docker compose up -d`.
 */
@SpringBootTest(properties = {
    "bdbank.startup.checks.enabled=false",
    "bdbank.scope.tables="
})
class JdbcConnectionServiceTest {

    @Autowired
    private JdbcConnectionService jdbcConnectionService;

    @Test
    void devraitSeConnecterALaDatasourceDeTest() throws Exception {
        JdbcConnectionService.DatabaseInfo info = jdbcConnectionService.testConnection();

        assertThat(info.productName()).isNotBlank();
        assertThat(info.productVersion()).isNotBlank();
        assertThat(info.url()).contains("jdbc:");
    }
}

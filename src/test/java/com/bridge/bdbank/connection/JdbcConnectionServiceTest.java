package com.bridge.bdbank.connection;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Vérifie que la couche de connexion fonctionne avec la datasource de test,
 * sans dépendre d'une base MySQL locale.
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

package com.bridge.bdbank.connection;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Vérifie que la couche de connexion atteint réellement la base de test
 * locale (docker compose up -d requis, cf. README).
 */
@SpringBootTest
class JdbcConnectionServiceTest {

    @Autowired
    private JdbcConnectionService jdbcConnectionService;

    @Test
    void devraitSeConnecterALaBaseDeTest() throws Exception {
        JdbcConnectionService.DatabaseInfo info = jdbcConnectionService.testConnection();

        assertThat(info.productName()).isEqualToIgnoringCase("MySQL");
        assertThat(info.productVersion()).isNotBlank();
    }
}

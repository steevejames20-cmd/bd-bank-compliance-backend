package com.bridge.bdbank.connection;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests unitaires "purs" (pas de {@code @SpringBootTest}) : on construit
 * ici une DataSource manuellement mal configurée, sans passer par le
 * contexte Spring - plus rapide, et surtout ça permet de tester des cas
 * d'échec qui empêcheraient le contexte applicatif normal de démarrer.
 */
class JdbcConnectionServiceErrorHandlingTest {

    @Test
    void devraitDonnerUnMessageClairSiIdentifiantsInvalides() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setUrl("jdbc:mysql://localhost:3306/bd_bank_test"
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        dataSource.setUsername("bdbank_readonly");
        dataSource.setPassword("mot_de_passe_volontairement_faux");

        JdbcConnectionService service = new JdbcConnectionService(dataSource);

        assertThatThrownBy(service::testConnection)
                .isInstanceOf(DatabaseConnectionException.class)
                .hasMessageContaining("Identifiants invalides");
    }

    @Test
    void devraitDonnerUnMessageClairSiBaseInjoignable() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        // Port volontairement incorrect (rien n'écoute ici) + timeout court
        // pour ne pas attendre le timeout TCP par défaut (~20s+).
        dataSource.setUrl("jdbc:mysql://localhost:39999/bd_bank_test?connectTimeout=2000");
        dataSource.setUsername("bdbank_readonly");
        dataSource.setPassword("change_me");

        JdbcConnectionService service = new JdbcConnectionService(dataSource);

        assertThatThrownBy(service::testConnection)
                .isInstanceOf(DatabaseConnectionException.class)
                .hasMessageContaining("injoignable");
    }
}

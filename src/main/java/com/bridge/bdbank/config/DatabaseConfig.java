package com.bridge.bdbank.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * Configuration des bases de données multiples :
 * - H2 (primaire) : persistance interne (règles, alertes, scopes)
 * - MySQL/PostgreSQL (secondaire) : bd_bank en lecture seule
 */
@Configuration
public class DatabaseConfig {

    @Value("${spring.datasource.url}")
    private String h2Url;

    @Value("${spring.datasource.username}")
    private String h2Username;

    @Value("${spring.datasource.password}")
    private String h2Password;

    @Value("${DB_HOST:localhost}")
    private String dbHost;

    @Value("${DB_PORT:3306}")
    private String dbPort;

    @Value("${DB_NAME:bd_bank_test}")
    private String dbName;

    @Value("${DB_USER:bdbank_readonly}")
    private String dbUser;

    @Value("${DB_PASSWORD:change_me}")
    private String dbPassword;

    /**
     * Datasource principale H2 pour la persistance interne
     * Utilisée par JPA/Hibernate pour les entités Rule, Alert, Scope
     */
    @Bean
    @Primary
    public DataSource primaryDataSource() {
        return DataSourceBuilder.create()
                .url(h2Url)
                .username(h2Username)
                .password(h2Password)
                .driverClassName("org.h2.Driver")
                .build();
    }

    /**
     * Datasource secondaire pour la bd_bank (lecture seule)
     * Utilisée par les services de connexion, introspection et traduction
     */
    @Bean
    public DataSource bankDataSource() {
        String url = String.format("jdbc:mysql://%s:%s/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
                dbHost, dbPort, dbName);

        return DataSourceBuilder.create()
                .url(url)
                .username(dbUser)
                .password(dbPassword)
                .driverClassName("com.mysql.cj.jdbc.Driver")
                .build();
    }

    /**
     * JdbcTemplate pour la bd_bank (lecture seule)
     * Utilisé par les services qui ne nécessitent pas JPA
     */
    @Bean
    public JdbcTemplate bankJdbcTemplate() {
        return new JdbcTemplate(bankDataSource());
    }
}
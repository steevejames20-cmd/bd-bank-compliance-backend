/**
 * Configuration Spring (DataSource dédiée à la bd_bank, introspection, etc.).
 *
 * Pour le moment (J1), la connexion à la base de test locale est déclarée
 * simplement via {@code application.yml} (auto-configuration Spring Boot).
 * La couche de connexion JDBC explicite et la gestion multi-SGBD
 * (MySQL/PostgreSQL) arrivent en J2.
 */
package com.bridge.bdbank.config;

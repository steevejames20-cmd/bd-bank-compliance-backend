package com.bridge.bdbank.connection;

import java.sql.SQLException;

/**
 * Traduit une {@link SQLException} technique en {@link DatabaseConnectionException}
 * avec un message actionnable, en se basant sur le SQLState (code standard
 * JDBC sur 5 caractères, portable entre drivers - contrairement au code
 * d'erreur "vendor", spécifique à chaque SGBD).
 * <p>
 * Classes SQLState utilisées ici :
 * "08" = erreur de connexion réseau (base injoignable)
 * "28" = autorisation invalide (mauvais identifiants)
 * "3D" = catalogue/base inexistant(e)
 * <p>
 * Partagé entre {@link JdbcConnectionService} et le module d'introspection :
 * les deux peuvent rencontrer exactement les mêmes catégories de pannes
 * (perte de connexion en cours de route, par exemple), pas la peine de
 * dupliquer cette logique.
 */
public final class SqlErrorTranslator {

    private SqlErrorTranslator() {
        // classe utilitaire, non instanciable
    }

    public static DatabaseConnectionException translate(SQLException e) {
        String sqlState = e.getSQLState();
        String sqlStateClass = (sqlState != null && sqlState.length() >= 2)
                ? sqlState.substring(0, 2)
                : "";

        String message = switch (sqlStateClass) {
            case "08" -> "Impossible de joindre la bd_bank (base injoignable). "
                    + "Vérifie que 'docker compose up -d' a bien été lancé, "
                    + "et que DB_HOST/DB_PORT sont corrects dans .env.";
            case "28" -> "Identifiants invalides pour se connecter à la bd_bank. "
                    + "Vérifie DB_USER/DB_PASSWORD dans .env.";
            case "3D" -> "La base de données configurée (DB_NAME) n'existe pas. "
                    + "Vérifie DB_NAME dans .env, ou que le seed a bien été initialisé.";
            default -> "Erreur de connexion à la bd_bank (SQLState=" + sqlState + "). "
                    + "Voir la cause ci-dessous pour le détail technique.";
        };

        return new DatabaseConnectionException(message, e);
    }
}

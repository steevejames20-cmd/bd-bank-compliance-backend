package com.bridge.bdbank.connection;

/**
 * Erreur de connexion à la bd_bank, avec un message actionnable pour la
 * personne qui lance l'application (plutôt que la {@link java.sql.SQLException}
 * brute, illisible pour qui n'a pas écrit le code).
 * <p>
 * RuntimeException volontairement : ce n'est pas une erreur "récupérable"
 * dans le flux normal - si la bd_bank n'est pas joignable, l'outil ne peut
 * de toute façon rien faire.
 */
public class DatabaseConnectionException extends RuntimeException {

    public DatabaseConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}

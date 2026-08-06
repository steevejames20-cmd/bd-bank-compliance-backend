package com.bridge.bdbank.scope;

/**
 * Levée quand le périmètre déclaré (bdbank.scope.tables /
 * BDBANK_SCOPE_TABLES) référence une table qui n'existe pas dans la
 * bd_bank — une faute de frappe dans la config, la plupart du temps.
 */
public class UnknownScopedTableException extends RuntimeException {

    public UnknownScopedTableException(String tableName) {
        super("La table '" + tableName + "' déclarée dans le périmètre "
                + "(BDBANK_SCOPE_TABLES) n'existe pas dans la bd_bank. "
                + "Vérifie l'orthographe dans ton .env.");
    }
}

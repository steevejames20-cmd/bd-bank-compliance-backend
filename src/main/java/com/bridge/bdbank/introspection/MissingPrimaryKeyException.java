package com.bridge.bdbank.introspection;

/**
 * Levée quand une table n'a aucune clé primaire déclarée en base — sans
 * clé, impossible d'identifier précisément une ligne dans une alerte.
 */
public class MissingPrimaryKeyException extends RuntimeException {

    public MissingPrimaryKeyException(String tableName) {
        super("Aucune clé primaire trouvée pour la table '" + tableName + "'");
    }
}

package com.bridge.bdbank.translation;

/**
 * Levée quand une règle référence une colonne absente de la table cible —
 * vérifiée contre le schéma introspecté, jamais supposée valide.
 */
public class UnknownColumnException extends RuntimeException {

    public UnknownColumnException(String columnName, String tableName) {
        super("Colonne '" + columnName + "' introuvable dans la table '" + tableName + "'");
    }
}

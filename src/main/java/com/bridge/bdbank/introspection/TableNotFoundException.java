package com.bridge.bdbank.introspection;

/**
 * Levée quand on demande les colonnes d'une table qui n'existe pas dans la
 * bd_bank (faute de frappe, table hors du périmètre surveillé...).
 * <p>
 * Sans cette vérification explicite, {@code getColumns()} renvoie
 * silencieusement une liste vide pour une table inexistante — impossible
 * à distinguer d'une vraie table à zéro colonne (qui n'existe pas en
 * pratique). Autant échouer clairement plutôt que de laisser ce cas
 * ambigu remonter en silence.
 */
public class TableNotFoundException extends RuntimeException {

    public TableNotFoundException(String tableName) {
        super("Table '" + tableName + "' introuvable dans la bd_bank "
                + "(vérifie l'orthographe, ou que la table fait bien partie du périmètre surveillé).");
    }
}

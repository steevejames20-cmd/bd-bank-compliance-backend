package com.bridge.bdbank.translation;

/**
 * Levée quand une règle préfixe explicitement une colonne avec un nom de
 * table différent de la table cible de la règle (ex: règle appliquée à
 * "comptes" mais colonne écrite "clients.age").
 * <p>
 * Ajoutée en cours de J8 (non annoncée dans le plan initial) : nécessaire
 * dès qu'on valide le préfixe de table optionnel du DSL contre la table
 * cible passée en paramètre.
 */
public class TableMismatchException extends RuntimeException {

    public TableMismatchException(String columnTable, String targetTable) {
        super("La règle référence la table '" + columnTable
                + "' mais est appliquée à la table '" + targetTable + "'");
    }
}

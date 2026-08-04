package com.bridge.bdbank.introspection;

/**
 * Une colonne découverte par introspection sur une table de la bd_bank.
 *
 * @param sqlType code numérique standard JDBC (java.sql.Types) — utile plus
 *                tard pour le DSL (semaine 2), quand il faudra savoir si une
 *                colonne est comparable à un nombre, une date, etc.
 */
public record ColumnInfo(String name, String typeName, int sqlType, boolean nullable) {
}

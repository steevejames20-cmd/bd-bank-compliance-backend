package com.bridge.bdbank.dsl;

/**
 * Référence à une colonne (ex. "clients.age" -> table="clients",
 * column="age"). {@code table} est null si non précisé dans la règle.
 */
public record ColumnOperand(String table, String column) implements Operand {
}

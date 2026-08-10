package com.bridge.bdbank.dsl;

/**
 * Une valeur littérale (nombre, chaîne ou booléen) - {@code value} est
 * typé {@code Object} pour les mêmes raisons qu'au J6 : le type réel
 * (Long, Double, String, Boolean) dépend de ce qui a été parsé.
 */
public record LiteralOperand(Object value) implements Operand {
}

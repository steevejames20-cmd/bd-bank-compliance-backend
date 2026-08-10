package com.bridge.bdbank.dsl;

/**
 * Une fonction d'agrégat appliquée à une colonne (ex. "SUM(transactions.montant)").
 */
public record AggregateOperand(AggregateFunction function, ColumnOperand column) implements Operand {
}

package com.bridge.bdbank.dsl;

/**
 * Un opérande de part et d'autre d'une comparaison : une colonne, une
 * valeur littérale, ou un agrégat (SUM, COUNT...) appliqué à une colonne.
 * <p>
 * {@code sealed} avec la liste fermée des implémentations : la traduction
 * en SQL (J8/J9) pourra faire un {@code switch} exhaustif sur les 3 cas,
 * et le compilateur signalera une erreur si un cas est oublié - plutôt
 * qu'un bug silencieux découvert seulement à l'exécution.
 */
public sealed interface Operand permits ColumnOperand, LiteralOperand, AggregateOperand {
}

package com.bridge.bdbank.dsl;

/**
 * Le résultat structuré du parsing d'une condition (ex. "clients.age < 18",
 * "comptes.solde < comptes.decouvert_autorise", ou
 * "SUM(transactions.montant) > 1000").
 * <p>
 * Depuis le J7, {@code left} et {@code right} sont chacun un {@link Operand}
 * générique (colonne, valeur ou agrégat) plutôt que des champs séparés
 * table/column/value : avant, seule la gauche pouvait être une colonne,
 * ce qui ne suffit plus pour les comparaisons colonne-colonne.
 */
public record ParsedCondition(Operand left, ComparisonOperator operator, Operand right) {
}

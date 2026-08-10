package com.bridge.bdbank.dsl;

/**
 * Le résultat structuré du parsing d'une condition ligne-à-ligne
 * (ex. "clients.age > 18" -> table="clients", column="age", operator=GT,
 * value=18).
 * <p>
 * {@code table} est null si la règle ne préfixe pas la colonne (ex. juste
 * "age > 18") - dans ce cas, la table est déterminée autrement (à définir
 * lors de la persistance des règles, semaine 3).
 * <p>
 * {@code value} est volontairement un Object (Long, Double, String ou
 * Boolean selon le type détecté) plutôt qu'un type unique : la traduction
 * en SQL (J8) devra de toute façon distinguer ces cas pour générer la
 * bonne syntaxe SQL (guillemets pour une chaîne, pas pour un nombre...).
 */
public record ParsedCondition(String table, String column, ComparisonOperator operator, Object value) {
}

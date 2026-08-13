package com.bridge.bdbank.dsl;

/**
 * Résultat complet du parsing d'une règle DSL : la condition (J6/J7) et,
 * depuis le J9, la relation optionnelle qui l'accompagne (jointure ou
 * regroupement) quand la règle porte sur un agrégat ou sur deux tables.
 * {@code relation} vaut {@code null} pour une règle ligne-à-ligne simple
 * (colonne vs valeur, ou colonne vs colonne sur la même table).
 */
public record ParsedRule(ParsedCondition condition, Relation relation) {
}

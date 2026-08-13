package com.bridge.bdbank.dsl;

/**
 * La relation optionnelle qui accompagne une condition quand elle implique
 * une jointure entre deux tables ou un regroupement pour un agrégat (J9).
 * {@code sealed} pour permettre un switch exhaustif côté traduction, comme
 * {@link Operand}.
 */
public sealed interface Relation permits JoinRelation, GroupByRelation {
}

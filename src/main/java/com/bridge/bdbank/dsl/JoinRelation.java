package com.bridge.bdbank.dsl;

/**
 * Jointure explicite entre deux tables, écrite par l'admin avec la clause
 * ON (ex: "... ON commandes.produit_id == stock.produit_id"). Les deux
 * colonnes précisent toujours leur table - c'est ce qui permet au
 * traducteur de savoir comment relier les deux tables.
 */
public record JoinRelation(ColumnOperand left, ColumnOperand right) implements Relation {
}

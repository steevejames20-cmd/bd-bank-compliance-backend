package com.bridge.bdbank.dsl;

/**
 * Regroupement explicite pour un agrégat sur une seule table, écrit avec
 * la clause GROUP BY (ex: "SUM(transactions.montant) > 1000 GROUP BY client_id").
 */
public record GroupByRelation(String column) implements Relation {
}

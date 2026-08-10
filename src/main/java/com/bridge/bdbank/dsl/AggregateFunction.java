package com.bridge.bdbank.dsl;

/**
 * Fonctions d'agrégat supportées par le DSL (J7).
 */
public enum AggregateFunction {
    SUM, COUNT, AVG, MAX, MIN;

    static AggregateFunction fromTokenType(int tokenType) {
        return switch (tokenType) {
            case RuleDslLexer.SUM -> SUM;
            case RuleDslLexer.COUNT -> COUNT;
            case RuleDslLexer.AVG -> AVG;
            case RuleDslLexer.MAX -> MAX;
            case RuleDslLexer.MIN -> MIN;
            default -> throw new IllegalStateException("Fonction d'agrégat inconnue (type token=" + tokenType + ")");
        };
    }
}

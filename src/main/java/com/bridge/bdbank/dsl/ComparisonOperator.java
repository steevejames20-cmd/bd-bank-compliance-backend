package com.bridge.bdbank.dsl;

/**
 * Opérateurs de comparaison supportés par le DSL. Volontairement
 * indépendant d'ANTLR dans sa forme (pas de dépendance vers les classes
 * générées ici) - seule la méthode de mapping en dépend, pour que le reste
 * du code (traduction SQL au J8, par exemple) manipule un type Java
 * ordinaire plutôt que les constantes de token internes du lexer.
 */
public enum ComparisonOperator {
    GT, LT, GE, LE, EQ, NE;

    static ComparisonOperator fromTokenType(int tokenType) {
        return switch (tokenType) {
            case RuleDslLexer.GT -> GT;
            case RuleDslLexer.LT -> LT;
            case RuleDslLexer.GE -> GE;
            case RuleDslLexer.LE -> LE;
            case RuleDslLexer.EQ -> EQ;
            case RuleDslLexer.NE -> NE;
            default -> throw new IllegalStateException("Opérateur inconnu (type token=" + tokenType + ")");
        };
    }
}

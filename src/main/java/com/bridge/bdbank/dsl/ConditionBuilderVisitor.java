package com.bridge.bdbank.dsl;

/**
 * Construit un {@link ParsedCondition} en parcourant l'arbre syntaxique
 * généré par ANTLR pour une règle.
 * <p>
 * Typé {@code Object} (et non {@code ParsedCondition}) volontairement :
 * les règles de la grammaire ne produisent pas toutes le même type
 * (dslRule/condition produisent un ParsedCondition, mais value produit un
 * Long/Double/String/Boolean selon le cas) - un seul visiteur ANTLR n'a
 * qu'un seul type de retour possible, donc on prend le plus général
 * (Object) et on caste où nécessaire. C'est l'approche standard pour ce
 * genre de grammaire hétérogène.
 */
class ConditionBuilderVisitor extends RuleDslBaseVisitor<Object> {

    @Override
    public Object visitDslRule(RuleDslParser.DslRuleContext ctx) {
        return visit(ctx.condition());
    }

    @Override
    public Object visitCondition(RuleDslParser.ConditionContext ctx) {
        RuleDslParser.ColumnContext columnCtx = ctx.column();

        String table = columnCtx.table != null ? columnCtx.table.getText() : null;
        String column = columnCtx.col.getText();
        ComparisonOperator operator = ComparisonOperator.fromTokenType(ctx.op.getType());
        Object value = visit(ctx.value());

        return new ParsedCondition(table, column, operator, value);
    }

    @Override
    public Object visitNumberValue(RuleDslParser.NumberValueContext ctx) {
        String text = ctx.getText();
        // Distingue entier/décimal pour ne pas perdre en précision inutilement.
        return text.contains(".") ? (Object) Double.parseDouble(text) : (Object) Long.parseLong(text);
    }

    @Override
    public Object visitStringValue(RuleDslParser.StringValueContext ctx) {
        String text = ctx.getText();
        return text.substring(1, text.length() - 1); // retire les guillemets englobants
    }

    @Override
    public Object visitBooleanValue(RuleDslParser.BooleanValueContext ctx) {
        return Boolean.parseBoolean(ctx.getText());
    }
}

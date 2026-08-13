package com.bridge.bdbank.dsl;

/**
 * Construit un {@link ParsedRule} en parcourant l'arbre syntaxique généré
 * par ANTLR pour une règle.
 * <p>
 * Typé {@code Object} (et non {@code ParsedRule}) volontairement : les
 * règles de la grammaire ne produisent pas toutes le même type
 * (dslRule produit un ParsedRule, condition produit un ParsedCondition,
 * operand produit un Operand, value produit un Long/Double/String/Boolean
 * selon le cas) - un seul visiteur ANTLR n'a qu'un seul type de retour
 * possible, donc on prend le plus général (Object) et on caste où
 * nécessaire.
 * <p>
 * Depuis le J9, {@code visitDslRule} construit aussi la {@link Relation}
 * optionnelle (jointure ou regroupement) quand la règle en comporte une.
 */
class ConditionBuilderVisitor extends RuleDslBaseVisitor<Object> {

    @Override
    public Object visitDslRule(RuleDslParser.DslRuleContext ctx) {
        ParsedCondition condition = (ParsedCondition) visit(ctx.condition());

        Relation relation = null;
        if (ctx.joinClause() != null) {
            relation = buildJoinRelation(ctx.joinClause());
        } else if (ctx.groupByClause() != null) {
            relation = buildGroupByRelation(ctx.groupByClause());
        }

        return new ParsedRule(condition, relation);
    }

    private JoinRelation buildJoinRelation(RuleDslParser.JoinClauseContext ctx) {
        ColumnOperand left = (ColumnOperand) visit(ctx.leftCol);
        ColumnOperand right = (ColumnOperand) visit(ctx.rightCol);
        return new JoinRelation(left, right);
    }

    private GroupByRelation buildGroupByRelation(RuleDslParser.GroupByClauseContext ctx) {
        return new GroupByRelation(ctx.col.getText());
    }

    @Override
    public Object visitCondition(RuleDslParser.ConditionContext ctx) {
        Operand left = (Operand) visit(ctx.left);
        ComparisonOperator operator = ComparisonOperator.fromTokenType(ctx.op.getType());
        Operand right = (Operand) visit(ctx.right);

        return new ParsedCondition(left, operator, right);
    }

    @Override
    public Object visitColumnOperand(RuleDslParser.ColumnOperandContext ctx) {
        return visit(ctx.column());
    }

    @Override
    public Object visitColumn(RuleDslParser.ColumnContext ctx) {
        String table = ctx.table != null ? ctx.table.getText() : null;
        String column = ctx.col.getText();
        return new ColumnOperand(table, column);
    }

    @Override
    public Object visitValueOperand(RuleDslParser.ValueOperandContext ctx) {
        Object literal = visit(ctx.value());
        return new LiteralOperand(literal);
    }

    @Override
    public Object visitAggregateOperand(RuleDslParser.AggregateOperandContext ctx) {
        return visit(ctx.aggregate());
    }

    @Override
    public Object visitAggregate(RuleDslParser.AggregateContext ctx) {
        AggregateFunction function = AggregateFunction.fromTokenType(ctx.func.getType());
        ColumnOperand column = (ColumnOperand) visit(ctx.column());
        return new AggregateOperand(function, column);
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

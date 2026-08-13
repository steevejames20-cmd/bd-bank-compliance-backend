package com.bridge.bdbank.translation;

import com.bridge.bdbank.dsl.AggregateFunction;
import com.bridge.bdbank.dsl.AggregateOperand;
import com.bridge.bdbank.dsl.ColumnOperand;
import com.bridge.bdbank.dsl.ComparisonOperator;
import com.bridge.bdbank.dsl.GroupByRelation;
import com.bridge.bdbank.dsl.JoinRelation;
import com.bridge.bdbank.dsl.LiteralOperand;
import com.bridge.bdbank.dsl.Operand;
import com.bridge.bdbank.dsl.ParsedCondition;
import com.bridge.bdbank.dsl.ParsedRule;
import com.bridge.bdbank.dsl.Relation;
import com.bridge.bdbank.introspection.ColumnInfo;
import com.bridge.bdbank.introspection.SchemaIntrospectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;

/**
 * Traduit une {@link ParsedRule} (condition + relation optionnelle) en
 * requête SQL paramétrée qui retourne les lignes ou groupes EN VIOLATION
 * de la règle - pas ce qui est conforme.
 * <p>
 * Cinq cas gérés :
 * <ol>
 *   <li>colonne vs valeur, une table (J8) - aucune relation ;</li>
 *   <li>colonne vs colonne, même table (J9) - aucune relation ;</li>
 *   <li>colonne vs colonne, deux tables (J9) - {@link JoinRelation} obligatoire ;</li>
 *   <li>agrégat vs valeur, une table (J9) - {@link GroupByRelation} obligatoire ;</li>
 *   <li>agrégat vs colonne, deux tables (J9, cas "stock/commandes") -
 *       {@link JoinRelation} obligatoire, le regroupement se fait
 *       implicitement sur la clé de jointure côté table jointe.</li>
 * </ol>
 * Agrégat vs agrégat, agrégat vs colonne de sa propre table, et plus de
 * deux tables dans une règle restent hors scope (J9), rejetés via
 * {@link UnsupportedRuleTypeException}.
 * <p>
 * Dès qu'une jointure est utilisée, toutes les colonnes du SQL généré sont
 * qualifiées ("table.colonne") pour éviter toute ambiguïté - notamment
 * indispensable pour la clé de jointure elle-même, présente des deux
 * côtés sous le même nom.
 */
@Service
@RequiredArgsConstructor
public class RuleTranslator {

    private final SchemaIntrospectionService schemaIntrospectionService;

    public TranslatedQuery translate(ParsedRule rule, String targetTable) {
        ParsedCondition condition = rule.condition();
        Relation relation = rule.relation();
        Operand left = condition.left();
        Operand right = condition.right();

        boolean leftIsAgg = left instanceof AggregateOperand;
        boolean rightIsAgg = right instanceof AggregateOperand;

        if (leftIsAgg && rightIsAgg) {
            throw new UnsupportedRuleTypeException("Comparaison agrégat contre agrégat - non gérée au J9");
        }
        if (leftIsAgg || rightIsAgg) {
            return translateAggregateRule(condition, relation, targetTable, leftIsAgg);
        }
        return translateRowLevelRule(condition, relation, targetTable);
    }

    // ------------------------------------------------------------------
    // Cas 1/2/3 : pas d'agrégat (colonne vs valeur, ou colonne vs colonne)
    // ------------------------------------------------------------------

    private TranslatedQuery translateRowLevelRule(ParsedCondition condition, Relation relation, String targetTable) {
        Operand left = condition.left();
        Operand right = condition.right();

        if (left instanceof LiteralOperand && right instanceof LiteralOperand) {
            throw new UnsupportedRuleTypeException("Règle sans colonne (deux valeurs littérales) - invalide");
        }

        if (left instanceof ColumnOperand leftCol && right instanceof ColumnOperand rightCol) {
            return translateColumnToColumn(leftCol, condition.operator(), rightCol, relation, targetTable);
        }

        // Colonne vs valeur (J8) : aucune relation attendue.
        requireNoRelation(relation, "une comparaison colonne/valeur");
        ColumnAndLiteral resolved = resolveColumnAndLiteral(condition);
        return translateColumnToLiteral(resolved, targetTable);
    }

    private TranslatedQuery translateColumnToLiteral(ColumnAndLiteral resolved, String targetTable) {
        ResolvedColumn column = resolveAgainstSingleTable(resolved.column(), targetTable);
        List<String> primaryKeyColumns = schemaIntrospectionService.getPrimaryKeyColumns(targetTable);
        ComparisonOperator violationOperator = negate(resolved.operator());

        var selectColumns = new LinkedHashSet<String>(primaryKeyColumns);
        selectColumns.add(column.column());

        String sql = "SELECT " + String.join(", ", selectColumns)
                + " FROM " + targetTable
                + " WHERE " + column.column() + " " + toSql(violationOperator) + " ?";

        return new TranslatedQuery(sql, List.of(resolved.literal().value()), targetTable,
                primaryKeyColumns, List.of(column.column()));
    }

    private TranslatedQuery translateColumnToColumn(ColumnOperand leftCol, ComparisonOperator operator,
                                                      ColumnOperand rightCol, Relation relation, String targetTable) {
        if (sameTable(leftCol, rightCol, targetTable)) {
            requireNoRelation(relation, "une comparaison colonne/colonne sur la même table");

            ResolvedColumn left = resolveAgainstSingleTable(leftCol, targetTable);
            ResolvedColumn right = resolveAgainstSingleTable(rightCol, targetTable);
            List<String> primaryKeyColumns = schemaIntrospectionService.getPrimaryKeyColumns(targetTable);
            ComparisonOperator violationOperator = negate(operator);

            var selectColumns = new LinkedHashSet<String>(primaryKeyColumns);
            selectColumns.add(left.column());
            selectColumns.add(right.column());

            String sql = "SELECT " + String.join(", ", selectColumns)
                    + " FROM " + targetTable
                    + " WHERE " + left.column() + " " + toSql(violationOperator) + " " + right.column();

            return new TranslatedQuery(sql, List.of(), targetTable, primaryKeyColumns,
                    List.of(left.column(), right.column()));
        }

        // Tables différentes -> jointure obligatoire.
        JoinRelation join = requireJoin(relation, "une comparaison colonne/colonne entre deux tables");
        String joinedTable = resolveJoinedTable(join, targetTable);

        ResolvedColumn left = resolveAgainstJoin(leftCol, targetTable, joinedTable);
        ResolvedColumn right = resolveAgainstJoin(rightCol, targetTable, joinedTable);
        List<String> primaryKeyColumns = qualify(targetTable, schemaIntrospectionService.getPrimaryKeyColumns(targetTable));
        ComparisonOperator violationOperator = negate(operator);
        String joinClause = buildJoinClause(join);

        var selectColumns = new LinkedHashSet<String>(primaryKeyColumns);
        selectColumns.add(left.qualified());
        selectColumns.add(right.qualified());

        String sql = "SELECT " + String.join(", ", selectColumns)
                + " FROM " + targetTable
                + " JOIN " + joinedTable + " ON " + joinClause
                + " WHERE " + left.qualified() + " " + toSql(violationOperator) + " " + right.qualified();

        return new TranslatedQuery(sql, List.of(), targetTable, primaryKeyColumns,
                List.of(left.qualified(), right.qualified()));
    }

    // ------------------------------------------------------------------
    // Cas 4/5 : un agrégat d'un côté de la condition
    // ------------------------------------------------------------------

    private TranslatedQuery translateAggregateRule(ParsedCondition condition, Relation relation,
                                                     String targetTable, boolean leftIsAgg) {
        AggregateOperand aggregate = (AggregateOperand) (leftIsAgg ? condition.left() : condition.right());
        Operand other = leftIsAgg ? condition.right() : condition.left();
        // Si l'agrégat est à droite dans le DSL, on mirrore l'opérateur pour
        // toujours raisonner "agrégat OP autre" ensuite (même principe qu'au
        // J8 pour colonne/valeur inversées).
        ComparisonOperator operator = leftIsAgg ? condition.operator() : mirror(condition.operator());

        if (other instanceof AggregateOperand) {
            throw new UnsupportedRuleTypeException("Comparaison agrégat contre agrégat - non gérée au J9");
        }

        ResolvedColumn aggColumn = resolveAgainstSingleTable(aggregate.column(), targetTable);

        if (other instanceof LiteralOperand literal) {
            GroupByRelation groupBy = requireGroupBy(relation, "un agrégat comparé à une valeur");
            ensureColumnExists(groupBy.column(), targetTable);
            return translateAggregateVsLiteral(aggregate.function(), aggColumn, operator, literal, groupBy, targetTable);
        }

        ColumnOperand otherColumn = (ColumnOperand) other;
        if (sameTable(aggregate.column(), otherColumn, targetTable)) {
            throw new UnsupportedRuleTypeException(
                    "Agrégat comparé à une colonne de la même table - non géré au J9 (ambigu sans regroupement explicite)");
        }

        JoinRelation join = requireJoin(relation, "un agrégat comparé à une colonne d'une autre table");
        String joinedTable = resolveJoinedTable(join, targetTable);
        ResolvedColumn otherResolved = resolveAgainstJoin(otherColumn, targetTable, joinedTable);

        return translateAggregateVsColumn(aggregate.function(), aggColumn, operator, otherResolved, join, targetTable, joinedTable);
    }

    private TranslatedQuery translateAggregateVsLiteral(AggregateFunction function, ResolvedColumn aggColumn,
                                                          ComparisonOperator operator, LiteralOperand literal,
                                                          GroupByRelation groupBy, String targetTable) {
        ComparisonOperator violationOperator = negate(operator);
        String aggExpr = toSql(function) + "(" + aggColumn.column() + ")";

        String sql = "SELECT " + groupBy.column() + ", " + aggExpr
                + " FROM " + targetTable
                + " GROUP BY " + groupBy.column()
                + " HAVING " + aggExpr + " " + toSql(violationOperator) + " ?";

        return new TranslatedQuery(sql, List.of(literal.value()), targetTable,
                List.of(groupBy.column()), List.of(aggColumn.column()));
    }

    private TranslatedQuery translateAggregateVsColumn(AggregateFunction function, ResolvedColumn aggColumn,
                                                         ComparisonOperator operator, ResolvedColumn otherColumn,
                                                         JoinRelation join, String targetTable, String joinedTable) {
        ComparisonOperator violationOperator = negate(operator);
        String joinClause = buildJoinClause(join);
        // Clé de regroupement implicite : la colonne de jointure côté table
        // jointe (ex: stock.produit_id) - chaque groupe correspond à une
        // ligne de cette table, comparée à l'agrégat des lignes liées de la
        // table cible.
        String groupKey = qualifiedJoinColumn(join, joinedTable);
        String aggExpr = toSql(function) + "(" + aggColumn.qualified() + ")";

        String sql = "SELECT " + groupKey + ", " + aggExpr + ", " + otherColumn.qualified()
                + " FROM " + targetTable
                + " JOIN " + joinedTable + " ON " + joinClause
                + " GROUP BY " + groupKey + ", " + otherColumn.qualified()
                + " HAVING " + aggExpr + " " + toSql(violationOperator) + " " + otherColumn.qualified();

        return new TranslatedQuery(sql, List.of(), targetTable,
                List.of(groupKey), List.of(aggColumn.qualified(), otherColumn.qualified()));
    }

    // ------------------------------------------------------------------
    // Résolution / validation des colonnes et relations
    // ------------------------------------------------------------------

    /** Identifie quel côté est la colonne et lequel est la valeur, quel que soit l'ordre écrit dans le DSL. */
    private ColumnAndLiteral resolveColumnAndLiteral(ParsedCondition condition) {
        Operand left = condition.left();
        Operand right = condition.right();

        if (left instanceof ColumnOperand columnOperand && right instanceof LiteralOperand literalOperand) {
            return new ColumnAndLiteral(columnOperand, condition.operator(), literalOperand);
        }

        // Seul cas restant possible ici (agrégats et colonne-colonne déjà
        // écartés plus haut par translateRowLevelRule) : valeur à gauche.
        ColumnOperand columnOperand = (ColumnOperand) right;
        LiteralOperand literalOperand = (LiteralOperand) left;
        return new ColumnAndLiteral(columnOperand, mirror(condition.operator()), literalOperand);
    }

    private boolean sameTable(ColumnOperand a, ColumnOperand b, String targetTable) {
        String ownerA = a.table() != null ? a.table() : targetTable;
        String ownerB = b.table() != null ? b.table() : targetTable;
        return ownerA.equalsIgnoreCase(ownerB);
    }

    private ResolvedColumn resolveAgainstSingleTable(ColumnOperand column, String targetTable) {
        if (column.table() != null && !column.table().equalsIgnoreCase(targetTable)) {
            throw new TableMismatchException(column.table(), targetTable);
        }
        ensureColumnExists(column.column(), targetTable);
        return new ResolvedColumn(targetTable, column.column());
    }

    private ResolvedColumn resolveAgainstJoin(ColumnOperand column, String targetTable, String joinedTable) {
        String owner = column.table() != null ? column.table() : targetTable;
        if (owner.equalsIgnoreCase(targetTable)) {
            ensureColumnExists(column.column(), targetTable);
            return new ResolvedColumn(targetTable, column.column());
        }
        if (owner.equalsIgnoreCase(joinedTable)) {
            ensureColumnExists(column.column(), joinedTable);
            return new ResolvedColumn(joinedTable, column.column());
        }
        throw new TableMismatchException(owner, targetTable + "' ou '" + joinedTable);
    }

    /** Vérifie que la clause ON relie bien la table cible à exactement une autre table, et la retourne. */
    private String resolveJoinedTable(JoinRelation join, String targetTable) {
        String leftTable = join.left().table();
        String rightTable = join.right().table();
        if (leftTable == null || rightTable == null) {
            throw new InvalidRelationException(
                    "Les deux colonnes de la clause ON doivent préciser leur table (ex: commandes.produit_id == stock.produit_id)");
        }

        ensureColumnExists(join.left().column(), leftTable);
        ensureColumnExists(join.right().column(), rightTable);

        boolean leftIsTarget = leftTable.equalsIgnoreCase(targetTable);
        boolean rightIsTarget = rightTable.equalsIgnoreCase(targetTable);

        if (leftIsTarget == rightIsTarget) {
            throw new InvalidRelationException(
                    "La clause ON doit relier la table cible ('" + targetTable + "') à exactement une autre table");
        }
        return leftIsTarget ? rightTable : leftTable;
    }

    private String buildJoinClause(JoinRelation join) {
        return join.left().table() + "." + join.left().column() + " = " + join.right().table() + "." + join.right().column();
    }

    private String qualifiedJoinColumn(JoinRelation join, String table) {
        if (table.equalsIgnoreCase(join.left().table())) {
            return join.left().table() + "." + join.left().column();
        }
        return join.right().table() + "." + join.right().column();
    }

    private List<String> qualify(String table, List<String> columns) {
        return columns.stream().map(c -> table + "." + c).toList();
    }

    private void ensureColumnExists(String columnName, String tableName) {
        List<ColumnInfo> columns = schemaIntrospectionService.listColumns(tableName);
        boolean exists = columns.stream().anyMatch(c -> c.name().equalsIgnoreCase(columnName));
        if (!exists) {
            throw new UnknownColumnException(columnName, tableName);
        }
    }

    private void requireNoRelation(Relation relation, String context) {
        if (relation != null) {
            throw new InvalidRelationException("Aucune clause ON/GROUP BY attendue pour " + context);
        }
    }

    private JoinRelation requireJoin(Relation relation, String context) {
        if (!(relation instanceof JoinRelation join)) {
            throw new InvalidRelationException("Une clause ON (jointure) est obligatoire pour " + context);
        }
        return join;
    }

    private GroupByRelation requireGroupBy(Relation relation, String context) {
        if (!(relation instanceof GroupByRelation groupBy)) {
            throw new InvalidRelationException("Une clause GROUP BY est obligatoire pour " + context);
        }
        return groupBy;
    }

    // ------------------------------------------------------------------
    // Opérateurs / fonctions -> SQL
    // ------------------------------------------------------------------

    /** Inverse une comparaison : passe d'une "condition de conformité" à une "condition de violation". */
    private static ComparisonOperator negate(ComparisonOperator operator) {
        return switch (operator) {
            case GT -> ComparisonOperator.LE;
            case LT -> ComparisonOperator.GE;
            case GE -> ComparisonOperator.LT;
            case LE -> ComparisonOperator.GT;
            case EQ -> ComparisonOperator.NE;
            case NE -> ComparisonOperator.EQ;
        };
    }

    /** Retourne l'opérateur "miroir" quand on échange les deux côtés d'une comparaison. */
    private static ComparisonOperator mirror(ComparisonOperator operator) {
        return switch (operator) {
            case GT -> ComparisonOperator.LT;
            case LT -> ComparisonOperator.GT;
            case GE -> ComparisonOperator.LE;
            case LE -> ComparisonOperator.GE;
            case EQ -> ComparisonOperator.EQ;
            case NE -> ComparisonOperator.NE;
        };
    }

    private static String toSql(ComparisonOperator operator) {
        return switch (operator) {
            case GT -> ">";
            case LT -> "<";
            case GE -> ">=";
            case LE -> "<=";
            case EQ -> "=";
            case NE -> "<>";
        };
    }

    private static String toSql(AggregateFunction function) {
        return switch (function) {
            case SUM -> "SUM";
            case COUNT -> "COUNT";
            case AVG -> "AVG";
            case MAX -> "MAX";
            case MIN -> "MIN";
        };
    }

    /** Une colonne résolue et validée : sait dans quelle table elle se trouve réellement. */
    private record ResolvedColumn(String table, String column) {
        String qualified() {
            return table + "." + column;
        }
    }

    /** Tuple interne : résultat de la résolution colonne/valeur/opérateur normalisé. */
    private record ColumnAndLiteral(ColumnOperand column, ComparisonOperator operator, LiteralOperand literal) {
    }
}

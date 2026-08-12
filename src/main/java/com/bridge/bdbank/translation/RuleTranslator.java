package com.bridge.bdbank.translation;

import com.bridge.bdbank.dsl.AggregateOperand;
import com.bridge.bdbank.dsl.ColumnOperand;
import com.bridge.bdbank.dsl.ComparisonOperator;
import com.bridge.bdbank.dsl.LiteralOperand;
import com.bridge.bdbank.dsl.Operand;
import com.bridge.bdbank.dsl.ParsedCondition;
import com.bridge.bdbank.introspection.ColumnInfo;
import com.bridge.bdbank.introspection.SchemaIntrospectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;

/**
 * Traduit une {@link ParsedCondition} ligne-à-ligne (colonne/valeur, une
 * seule table) en requête SQL paramétrée qui retourne les lignes EN
 * VIOLATION de la règle - pas les lignes conformes.
 * <p>
 * Les comparaisons colonne-colonne et les agrégats sont hors scope ici
 * (J9) : {@link #translate} lève {@link UnsupportedRuleTypeException}
 * dans ces cas plutôt que de tenter une traduction partielle.
 * <p>
 * La table cible est passée en paramètre plutôt que déduite du DSL : "à
 * quelle table appartient une règle" relève de l'entité Règle (semaine 3,
 * pas encore construite). Ça découple ce traducteur de ce qui n'existe
 * pas encore.
 */
@Service
@RequiredArgsConstructor
public class RuleTranslator {

    private final SchemaIntrospectionService schemaIntrospectionService;

    public TranslatedQuery translate(ParsedCondition condition, String targetTable) {
        ColumnAndLiteral resolved = resolveColumnAndLiteral(condition);

        ColumnOperand column = resolved.column();
        if (column.table() != null && !column.table().equalsIgnoreCase(targetTable)) {
            throw new TableMismatchException(column.table(), targetTable);
        }

        ensureColumnExists(column.column(), targetTable);

        List<String> primaryKeyColumns = schemaIntrospectionService.getPrimaryKeyColumns(targetTable);

        ComparisonOperator violationOperator = negate(resolved.operator());

        // LinkedHashSet : la colonne de la règle peut être la même que la
        // clé primaire (ex: règle sur "id") - on évite de la sélectionner
        // deux fois, tout en gardant un ordre stable (clé primaire d'abord).
        var selectColumns = new LinkedHashSet<String>(primaryKeyColumns);
        selectColumns.add(column.column());

        String sql = "SELECT " + String.join(", ", selectColumns)
                + " FROM " + targetTable
                + " WHERE " + column.column() + " " + toSql(violationOperator) + " ?";

        return new TranslatedQuery(
                sql,
                List.of(resolved.literal().value()),
                targetTable,
                primaryKeyColumns,
                List.of(column.column())
        );
    }

    /**
     * Identifie quel côté de la condition est la colonne et lequel est la
     * valeur, quel que soit l'ordre écrit dans le DSL ("age > 18" ou
     * "18 < age"). Quand la colonne est à droite, l'opérateur est
     * "mirroré" pour que le reste du traitement puisse toujours raisonner
     * "colonne OP valeur".
     */
    private ColumnAndLiteral resolveColumnAndLiteral(ParsedCondition condition) {
        Operand left = condition.left();
        Operand right = condition.right();

        if (left instanceof AggregateOperand || right instanceof AggregateOperand) {
            throw new UnsupportedRuleTypeException(
                    "Règle avec fonction d'agrégat - traduite au J9, pas au J8");
        }
        if (left instanceof ColumnOperand && right instanceof ColumnOperand) {
            throw new UnsupportedRuleTypeException(
                    "Comparaison entre deux colonnes - traduite au J9, pas au J8");
        }
        if (left instanceof LiteralOperand && right instanceof LiteralOperand) {
            throw new UnsupportedRuleTypeException(
                    "Règle sans colonne (deux valeurs littérales) - invalide");
        }

        if (left instanceof ColumnOperand columnOperand && right instanceof LiteralOperand literalOperand) {
            return new ColumnAndLiteral(columnOperand, condition.operator(), literalOperand);
        }

        // Cas restant : colonne à droite, valeur à gauche -> on mirrore l'opérateur.
        ColumnOperand columnOperand = (ColumnOperand) right;
        LiteralOperand literalOperand = (LiteralOperand) left;
        return new ColumnAndLiteral(columnOperand, mirror(condition.operator()), literalOperand);
    }

    private void ensureColumnExists(String columnName, String targetTable) {
        List<ColumnInfo> columns = schemaIntrospectionService.listColumns(targetTable);
        boolean exists = columns.stream().anyMatch(c -> c.name().equalsIgnoreCase(columnName));
        if (!exists) {
            throw new UnknownColumnException(columnName, targetTable);
        }
    }

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

    /** Tuple interne : résultat de la résolution colonne/valeur/opérateur normalisé. */
    private record ColumnAndLiteral(ColumnOperand column, ComparisonOperator operator, LiteralOperand literal) {
    }
}

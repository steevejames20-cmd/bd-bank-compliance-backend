package com.bridge.bdbank.translation;

import com.bridge.bdbank.dsl.AggregateFunction;
import com.bridge.bdbank.dsl.AggregateOperand;
import com.bridge.bdbank.dsl.ColumnOperand;
import com.bridge.bdbank.dsl.ComparisonOperator;
import com.bridge.bdbank.dsl.LiteralOperand;
import com.bridge.bdbank.dsl.ParsedCondition;
import com.bridge.bdbank.introspection.ColumnInfo;
import com.bridge.bdbank.introspection.SchemaIntrospectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class RuleTranslatorTest {

    @Mock
    private SchemaIntrospectionService schemaIntrospectionService;

    private RuleTranslator translator;

    @BeforeEach
    void setUp() {
        translator = new RuleTranslator(schemaIntrospectionService);

        // Schema par defaut pour "clients", utilise par la plupart des tests.
        // lenient() : certains tests (colonne inconnue, agregat...) n'utilisent
        // pas forcement ces stubs, Mockito ne doit pas s'en plaindre.
        lenient().when(schemaIntrospectionService.listColumns("clients")).thenReturn(List.of(
                new ColumnInfo("id", "INT", 4, false),
                new ColumnInfo("age", "INT", 4, false),
                new ColumnInfo("pays", "VARCHAR", 12, true),
                new ColumnInfo("actif", "BOOLEAN", 16, false)
        ));
        lenient().when(schemaIntrospectionService.getPrimaryKeyColumns("clients")).thenReturn(List.of("id"));
    }

    @Test
    void devraitTraduireUneConditionSimpleEnSaViolation() {
        var condition = new ParsedCondition(new ColumnOperand(null, "age"), ComparisonOperator.GT, new LiteralOperand(18L));

        TranslatedQuery result = translator.translate(condition, "clients");

        assertThat(result.sql()).isEqualTo("SELECT id, age FROM clients WHERE age <= ?");
        assertThat(result.params()).containsExactly(18L);
        assertThat(result.table()).isEqualTo("clients");
        assertThat(result.primaryKeyColumns()).containsExactly("id");
        assertThat(result.involvedColumns()).containsExactly("age");
    }

    @Test
    void devraitDonnerLeMemeResultatQuelQueSoitLOrdreDeLaCondition() {
        var ordreNormal = new ParsedCondition(new ColumnOperand(null, "age"), ComparisonOperator.GT, new LiteralOperand(18L));
        var ordreInverse = new ParsedCondition(new LiteralOperand(18L), ComparisonOperator.LT, new ColumnOperand(null, "age"));

        TranslatedQuery resultNormal = translator.translate(ordreNormal, "clients");
        TranslatedQuery resultInverse = translator.translate(ordreInverse, "clients");

        assertThat(resultInverse.sql()).isEqualTo(resultNormal.sql());
        assertThat(resultInverse.params()).isEqualTo(resultNormal.params());
    }

    @Test
    void devraitInverserChacunDesSixOperateurs() {
        assertThat(sqlPour(ComparisonOperator.GT)).endsWith("<= ?");
        assertThat(sqlPour(ComparisonOperator.LT)).endsWith(">= ?");
        assertThat(sqlPour(ComparisonOperator.GE)).endsWith("< ?");
        assertThat(sqlPour(ComparisonOperator.LE)).endsWith("> ?");
        assertThat(sqlPour(ComparisonOperator.EQ)).endsWith("<> ?");
        assertThat(sqlPour(ComparisonOperator.NE)).endsWith("= ?");
    }

    private String sqlPour(ComparisonOperator operator) {
        var condition = new ParsedCondition(new ColumnOperand(null, "age"), operator, new LiteralOperand(18L));
        return translator.translate(condition, "clients").sql();
    }

    @Test
    void devraitTraduireUneValeurChaine() {
        var condition = new ParsedCondition(new ColumnOperand("clients", "pays"), ComparisonOperator.EQ, new LiteralOperand("FR"));

        TranslatedQuery result = translator.translate(condition, "clients");

        assertThat(result.sql()).isEqualTo("SELECT id, pays FROM clients WHERE pays <> ?");
        assertThat(result.params()).containsExactly("FR");
    }

    @Test
    void devraitTraduireUneValeurBooleenne() {
        var condition = new ParsedCondition(new ColumnOperand(null, "actif"), ComparisonOperator.NE, new LiteralOperand(false));

        TranslatedQuery result = translator.translate(condition, "clients");

        assertThat(result.sql()).isEqualTo("SELECT id, actif FROM clients WHERE actif = ?");
        assertThat(result.params()).containsExactly(false);
    }

    @Test
    void devraitTraduireUneValeurDecimaleNegative() {
        var condition = new ParsedCondition(new ColumnOperand(null, "age"), ComparisonOperator.LT, new LiteralOperand(-100.50));

        TranslatedQuery result = translator.translate(condition, "clients");

        assertThat(result.params()).containsExactly(-100.50);
    }

    @Test
    void neDevraitPasDupliquerLaColonneQuandElleEstAussiLaClePrimaire() {
        var condition = new ParsedCondition(new ColumnOperand(null, "id"), ComparisonOperator.GT, new LiteralOperand(0L));

        TranslatedQuery result = translator.translate(condition, "clients");

        assertThat(result.sql()).isEqualTo("SELECT id FROM clients WHERE id <= ?");
    }

    @Test
    void devraitAccepterUnPrefixeDeTableCorrect() {
        var condition = new ParsedCondition(new ColumnOperand("clients", "age"), ComparisonOperator.GT, new LiteralOperand(18L));

        TranslatedQuery result = translator.translate(condition, "clients");

        assertThat(result.table()).isEqualTo("clients");
    }

    @Test
    void devraitRejeterUnPrefixeDeTableIncorrect() {
        var condition = new ParsedCondition(new ColumnOperand("comptes", "solde"), ComparisonOperator.GT, new LiteralOperand(0L));

        assertThatThrownBy(() -> translator.translate(condition, "clients"))
                .isInstanceOf(TableMismatchException.class);
    }

    @Test
    void devraitRejeterUneColonneInconnue() {
        var condition = new ParsedCondition(new ColumnOperand(null, "inexistante"), ComparisonOperator.GT, new LiteralOperand(0L));

        assertThatThrownBy(() -> translator.translate(condition, "clients"))
                .isInstanceOf(UnknownColumnException.class);
    }

    @Test
    void devraitRejeterUneRegleAvecAgregat() {
        var condition = new ParsedCondition(
                new AggregateOperand(AggregateFunction.SUM, new ColumnOperand("clients", "age")),
                ComparisonOperator.GT, new LiteralOperand(100L));

        assertThatThrownBy(() -> translator.translate(condition, "clients"))
                .isInstanceOf(UnsupportedRuleTypeException.class);
    }

    @Test
    void devraitRejeterUneComparaisonColonneColonne() {
        var condition = new ParsedCondition(new ColumnOperand("clients", "age"), ComparisonOperator.GT, new ColumnOperand("clients", "id"));

        assertThatThrownBy(() -> translator.translate(condition, "clients"))
                .isInstanceOf(UnsupportedRuleTypeException.class);
    }

    @Test
    void devraitRejeterUneRegleSansColonne() {
        var condition = new ParsedCondition(new LiteralOperand(1L), ComparisonOperator.GT, new LiteralOperand(0L));

        assertThatThrownBy(() -> translator.translate(condition, "clients"))
                .isInstanceOf(UnsupportedRuleTypeException.class);
    }
}

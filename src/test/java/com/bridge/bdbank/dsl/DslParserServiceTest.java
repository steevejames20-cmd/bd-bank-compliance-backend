package com.bridge.bdbank.dsl;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DslParserServiceTest {

    private final DslParserService parser = new DslParserService();

    // --- J6 : colonne / valeur (mis à jour pour la forme left/right du J7) ---

    @Test
    void devraitParserUneConditionSansPrefixeDeTable() {
        ParsedCondition result = parser.parse("age > 18");

        assertThat(result.left()).isEqualTo(new ColumnOperand(null, "age"));
        assertThat(result.operator()).isEqualTo(ComparisonOperator.GT);
        assertThat(result.right()).isEqualTo(new LiteralOperand(18L));
    }

    @Test
    void devraitParserUneConditionAvecPrefixeDeTable() {
        ParsedCondition result = parser.parse("clients.age >= 18");

        assertThat(result.left()).isEqualTo(new ColumnOperand("clients", "age"));
        assertThat(result.operator()).isEqualTo(ComparisonOperator.GE);
        assertThat(result.right()).isEqualTo(new LiteralOperand(18L));
    }

    @Test
    void devraitParserUneValeurChaine() {
        ParsedCondition result = parser.parse("pays == \"FR\"");

        assertThat(result.right()).isEqualTo(new LiteralOperand("FR"));
    }

    @Test
    void devraitParserUneValeurDecimaleNegative() {
        ParsedCondition result = parser.parse("solde < -100.50");

        assertThat(result.right()).isEqualTo(new LiteralOperand(-100.50));
    }

    @Test
    void devraitParserUneValeurBooleenne() {
        ParsedCondition result = parser.parse("actif != false");

        assertThat(result.right()).isEqualTo(new LiteralOperand(false));
    }

    // --- J7 : comparaisons colonne-colonne, multi-tables ---

    @Test
    void devraitParserUneComparaisonEntreDeuxColonnesDeLaMemeTable() {
        ParsedCondition result = parser.parse("comptes.solde > comptes.decouvert_autorise");

        assertThat(result.left()).isEqualTo(new ColumnOperand("comptes", "solde"));
        assertThat(result.operator()).isEqualTo(ComparisonOperator.GT);
        assertThat(result.right()).isEqualTo(new ColumnOperand("comptes", "decouvert_autorise"));
    }

    @Test
    void devraitParserUneComparaisonEntreColonnesDeDeuxTablesDifferentes() {
        ParsedCondition result = parser.parse("transactions.montant > comptes.solde");

        assertThat(result.left()).isEqualTo(new ColumnOperand("transactions", "montant"));
        assertThat(result.right()).isEqualTo(new ColumnOperand("comptes", "solde"));
    }

    // --- J7 : fonctions d'agrégat ---

    @Test
    void devraitParserUnAgregatSum() {
        ParsedCondition result = parser.parse("SUM(transactions.montant) > 1000");

        assertThat(result.left()).isEqualTo(
                new AggregateOperand(AggregateFunction.SUM, new ColumnOperand("transactions", "montant")));
        assertThat(result.right()).isEqualTo(new LiteralOperand(1000L));
    }

    @Test
    void devraitParserChacunDesCinqAgregats() {
        assertThat(((AggregateOperand) parser.parse("COUNT(transactions.id) > 0").left()).function())
                .isEqualTo(AggregateFunction.COUNT);
        assertThat(((AggregateOperand) parser.parse("AVG(comptes.solde) > 0").left()).function())
                .isEqualTo(AggregateFunction.AVG);
        assertThat(((AggregateOperand) parser.parse("MAX(comptes.solde) > 0").left()).function())
                .isEqualTo(AggregateFunction.MAX);
        assertThat(((AggregateOperand) parser.parse("MIN(comptes.solde) > 0").left()).function())
                .isEqualTo(AggregateFunction.MIN);
    }

    // --- Erreurs ---

    @Test
    void devraitEchouerSurUneRegleIncomplete() {
        assertThatThrownBy(() -> parser.parse("age >"))
                .isInstanceOf(DslSyntaxException.class);
    }

    @Test
    void devraitEchouerSurUnOperateurInvalide() {
        assertThatThrownBy(() -> parser.parse("age >>> 18"))
                .isInstanceOf(DslSyntaxException.class);
    }

    @Test
    void devraitEchouerSurDuTexteEnTropApresLaCondition() {
        assertThatThrownBy(() -> parser.parse("age > 18 en_trop"))
                .isInstanceOf(DslSyntaxException.class);
    }

    @Test
    void devraitEchouerSurUnAgregatSansParenthese() {
        assertThatThrownBy(() -> parser.parse("SUM transactions.montant > 1000"))
                .isInstanceOf(DslSyntaxException.class);
    }
}

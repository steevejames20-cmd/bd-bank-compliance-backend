package com.bridge.bdbank.dsl;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DslParserServiceTest {

    private final DslParserService parser = new DslParserService();

    // --- J6 : colonne / valeur ---

    @Test
    void devraitParserUneConditionSansPrefixeDeTable() {
        ParsedRule result = parser.parse("age > 18");

        assertThat(result.condition().left()).isEqualTo(new ColumnOperand(null, "age"));
        assertThat(result.condition().operator()).isEqualTo(ComparisonOperator.GT);
        assertThat(result.condition().right()).isEqualTo(new LiteralOperand(18L));
        assertThat(result.relation()).isNull();
    }

    @Test
    void devraitParserUneConditionAvecPrefixeDeTable() {
        ParsedRule result = parser.parse("clients.age >= 18");

        assertThat(result.condition().left()).isEqualTo(new ColumnOperand("clients", "age"));
        assertThat(result.condition().operator()).isEqualTo(ComparisonOperator.GE);
        assertThat(result.condition().right()).isEqualTo(new LiteralOperand(18L));
    }

    @Test
    void devraitParserUneValeurChaine() {
        ParsedRule result = parser.parse("pays == \"FR\"");

        assertThat(result.condition().right()).isEqualTo(new LiteralOperand("FR"));
    }

    @Test
    void devraitParserUneValeurDecimaleNegative() {
        ParsedRule result = parser.parse("solde < -100.50");

        assertThat(result.condition().right()).isEqualTo(new LiteralOperand(-100.50));
    }

    @Test
    void devraitParserUneValeurBooleenne() {
        ParsedRule result = parser.parse("actif != false");

        assertThat(result.condition().right()).isEqualTo(new LiteralOperand(false));
    }

    // --- J7 : comparaisons colonne-colonne, multi-tables ---

    @Test
    void devraitParserUneComparaisonEntreDeuxColonnesDeLaMemeTable() {
        ParsedRule result = parser.parse("comptes.solde > comptes.decouvert_autorise");

        assertThat(result.condition().left()).isEqualTo(new ColumnOperand("comptes", "solde"));
        assertThat(result.condition().operator()).isEqualTo(ComparisonOperator.GT);
        assertThat(result.condition().right()).isEqualTo(new ColumnOperand("comptes", "decouvert_autorise"));
    }

    @Test
    void devraitParserUneComparaisonEntreColonnesDeDeuxTablesDifferentes() {
        ParsedRule result = parser.parse("transactions.montant > comptes.solde");

        assertThat(result.condition().left()).isEqualTo(new ColumnOperand("transactions", "montant"));
        assertThat(result.condition().right()).isEqualTo(new ColumnOperand("comptes", "solde"));
    }

    // --- J7 : fonctions d'agrégat ---

    @Test
    void devraitParserUnAgregatSum() {
        ParsedRule result = parser.parse("SUM(transactions.montant) > 1000");

        assertThat(result.condition().left()).isEqualTo(
                new AggregateOperand(AggregateFunction.SUM, new ColumnOperand("transactions", "montant")));
        assertThat(result.condition().right()).isEqualTo(new LiteralOperand(1000L));
    }

    @Test
    void devraitParserChacunDesCinqAgregats() {
        assertThat(((AggregateOperand) parser.parse("COUNT(transactions.id) > 0").condition().left()).function())
                .isEqualTo(AggregateFunction.COUNT);
        assertThat(((AggregateOperand) parser.parse("AVG(comptes.solde) > 0").condition().left()).function())
                .isEqualTo(AggregateFunction.AVG);
        assertThat(((AggregateOperand) parser.parse("MAX(comptes.solde) > 0").condition().left()).function())
                .isEqualTo(AggregateFunction.MAX);
        assertThat(((AggregateOperand) parser.parse("MIN(comptes.solde) > 0").condition().left()).function())
                .isEqualTo(AggregateFunction.MIN);
    }

    // --- J9 : clause ON (jointure) ---

    @Test
    void devraitParserUneClauseOn() {
        ParsedRule result = parser.parse("commandes.quantite > stock.quantite_disponible ON commandes.produit_id == stock.produit_id");

        assertThat(result.relation()).isEqualTo(new JoinRelation(
                new ColumnOperand("commandes", "produit_id"),
                new ColumnOperand("stock", "produit_id")));
    }

    // --- J9 : clause GROUP BY ---

    @Test
    void devraitParserUneClauseGroupBy() {
        ParsedRule result = parser.parse("SUM(transactions.montant) > 1000 GROUP BY client_id");

        assertThat(result.relation()).isEqualTo(new GroupByRelation("client_id"));
    }

    @Test
    void neDevraitPasAvoirDeRelationQuandAucuneClauseNestPresente() {
        ParsedRule result = parser.parse("SUM(transactions.montant) > 1000");

        assertThat(result.relation()).isNull();
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

    @Test
    void devraitEchouerSurUneClauseOnIncomplete() {
        assertThatThrownBy(() -> parser.parse("commandes.quantite > stock.quantite_disponible ON commandes.produit_id"))
                .isInstanceOf(DslSyntaxException.class);
    }
}

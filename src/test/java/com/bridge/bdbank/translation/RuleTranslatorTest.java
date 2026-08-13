package com.bridge.bdbank.translation;

import com.bridge.bdbank.dsl.AggregateFunction;
import com.bridge.bdbank.dsl.AggregateOperand;
import com.bridge.bdbank.dsl.ColumnOperand;
import com.bridge.bdbank.dsl.ComparisonOperator;
import com.bridge.bdbank.dsl.GroupByRelation;
import com.bridge.bdbank.dsl.JoinRelation;
import com.bridge.bdbank.dsl.LiteralOperand;
import com.bridge.bdbank.dsl.ParsedCondition;
import com.bridge.bdbank.dsl.ParsedRule;
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

        lenient().when(schemaIntrospectionService.listColumns("clients")).thenReturn(List.of(
                new ColumnInfo("id", "INT", 4, false),
                new ColumnInfo("age", "INT", 4, false),
                new ColumnInfo("pays", "VARCHAR", 12, true),
                new ColumnInfo("actif", "BOOLEAN", 16, false)
        ));
        lenient().when(schemaIntrospectionService.getPrimaryKeyColumns("clients")).thenReturn(List.of("id"));

        lenient().when(schemaIntrospectionService.listColumns("comptes")).thenReturn(List.of(
                new ColumnInfo("id", "INT", 4, false),
                new ColumnInfo("solde", "DECIMAL", 3, false),
                new ColumnInfo("decouvert_autorise", "DECIMAL", 3, false),
                new ColumnInfo("plafond", "DECIMAL", 3, false)
        ));
        lenient().when(schemaIntrospectionService.getPrimaryKeyColumns("comptes")).thenReturn(List.of("id"));

        lenient().when(schemaIntrospectionService.listColumns("commandes")).thenReturn(List.of(
                new ColumnInfo("id", "INT", 4, false),
                new ColumnInfo("quantite", "INT", 4, false),
                new ColumnInfo("produit_id", "INT", 4, false)
        ));
        lenient().when(schemaIntrospectionService.getPrimaryKeyColumns("commandes")).thenReturn(List.of("id"));

        lenient().when(schemaIntrospectionService.listColumns("stock")).thenReturn(List.of(
                new ColumnInfo("produit_id", "INT", 4, false),
                new ColumnInfo("quantite_disponible", "INT", 4, false)
        ));

        lenient().when(schemaIntrospectionService.listColumns("transactions")).thenReturn(List.of(
                new ColumnInfo("id", "INT", 4, false),
                new ColumnInfo("montant", "DECIMAL", 3, false),
                new ColumnInfo("client_id", "INT", 4, false)
        ));
    }

    private static ParsedRule ruleSansRelation(ParsedCondition condition) {
        return new ParsedRule(condition, null);
    }

    // ------------------------------------------------------------------
    // Cas 1 (J8) : colonne vs valeur, une table
    // ------------------------------------------------------------------

    @Test
    void devraitTraduireUneConditionSimpleEnSaViolation() {
        var condition = new ParsedCondition(new ColumnOperand(null, "age"), ComparisonOperator.GT, new LiteralOperand(18L));

        TranslatedQuery result = translator.translate(ruleSansRelation(condition), "clients");

        assertThat(result.sql()).isEqualTo("SELECT id, age FROM clients WHERE age <= ?");
        assertThat(result.params()).containsExactly(18L);
        assertThat(result.primaryKeyColumns()).containsExactly("id");
        assertThat(result.involvedColumns()).containsExactly("age");
    }

    @Test
    void devraitDonnerLeMemeResultatQuelQueSoitLOrdreDeLaCondition() {
        var ordreNormal = new ParsedCondition(new ColumnOperand(null, "age"), ComparisonOperator.GT, new LiteralOperand(18L));
        var ordreInverse = new ParsedCondition(new LiteralOperand(18L), ComparisonOperator.LT, new ColumnOperand(null, "age"));

        TranslatedQuery resultNormal = translator.translate(ruleSansRelation(ordreNormal), "clients");
        TranslatedQuery resultInverse = translator.translate(ruleSansRelation(ordreInverse), "clients");

        assertThat(resultInverse.sql()).isEqualTo(resultNormal.sql());
    }

    @Test
    void devraitTraduireUneValeurChaineEtBooleenneEtDecimaleNegative() {
        var chaine = new ParsedCondition(new ColumnOperand("clients", "pays"), ComparisonOperator.EQ, new LiteralOperand("FR"));
        assertThat(translator.translate(ruleSansRelation(chaine), "clients").sql())
                .isEqualTo("SELECT id, pays FROM clients WHERE pays <> ?");

        var booleen = new ParsedCondition(new ColumnOperand(null, "actif"), ComparisonOperator.NE, new LiteralOperand(false));
        assertThat(translator.translate(ruleSansRelation(booleen), "clients").sql())
                .isEqualTo("SELECT id, actif FROM clients WHERE actif = ?");

        var decimal = new ParsedCondition(new ColumnOperand(null, "age"), ComparisonOperator.LT, new LiteralOperand(-100.50));
        assertThat(translator.translate(ruleSansRelation(decimal), "clients").params()).containsExactly(-100.50);
    }

    @Test
    void neDevraitPasDupliquerLaColonneQuandElleEstAussiLaClePrimaire() {
        var condition = new ParsedCondition(new ColumnOperand(null, "id"), ComparisonOperator.GT, new LiteralOperand(0L));

        TranslatedQuery result = translator.translate(ruleSansRelation(condition), "clients");

        assertThat(result.sql()).isEqualTo("SELECT id FROM clients WHERE id <= ?");
    }

    @Test
    void devraitRejeterUnPrefixeDeTableIncorrect() {
        var condition = new ParsedCondition(new ColumnOperand("comptes", "solde"), ComparisonOperator.GT, new LiteralOperand(0L));

        assertThatThrownBy(() -> translator.translate(ruleSansRelation(condition), "clients"))
                .isInstanceOf(TableMismatchException.class);
    }

    @Test
    void devraitRejeterUneColonneInconnue() {
        var condition = new ParsedCondition(new ColumnOperand(null, "inexistante"), ComparisonOperator.GT, new LiteralOperand(0L));

        assertThatThrownBy(() -> translator.translate(ruleSansRelation(condition), "clients"))
                .isInstanceOf(UnknownColumnException.class);
    }

    @Test
    void devraitRejeterUneRegleSansColonneEtUneComparaisonAgregatContreAgregat() {
        var deuxValeurs = new ParsedCondition(new LiteralOperand(1L), ComparisonOperator.GT, new LiteralOperand(0L));
        assertThatThrownBy(() -> translator.translate(ruleSansRelation(deuxValeurs), "clients"))
                .isInstanceOf(UnsupportedRuleTypeException.class);

        var deuxAgregats = new ParsedCondition(
                new AggregateOperand(AggregateFunction.SUM, new ColumnOperand("comptes", "solde")),
                ComparisonOperator.GT,
                new AggregateOperand(AggregateFunction.AVG, new ColumnOperand("comptes", "solde")));
        assertThatThrownBy(() -> translator.translate(ruleSansRelation(deuxAgregats), "comptes"))
                .isInstanceOf(UnsupportedRuleTypeException.class);
    }

    // ------------------------------------------------------------------
    // Cas 2 (J9) : colonne vs colonne, même table
    // ------------------------------------------------------------------

    @Test
    void devraitTraduireUneComparaisonColonneColonneSurLaMemeTable() {
        var condition = new ParsedCondition(
                new ColumnOperand("comptes", "solde"), ComparisonOperator.GT, new ColumnOperand("comptes", "decouvert_autorise"));

        TranslatedQuery result = translator.translate(ruleSansRelation(condition), "comptes");

        assertThat(result.sql()).isEqualTo("SELECT id, solde, decouvert_autorise FROM comptes WHERE solde <= decouvert_autorise");
        assertThat(result.params()).isEmpty();
    }

    @Test
    void devraitRejeterUneRelationInutilePourUneComparaisonMemeTable() {
        var condition = new ParsedCondition(
                new ColumnOperand("comptes", "solde"), ComparisonOperator.GT, new ColumnOperand("comptes", "decouvert_autorise"));
        var rule = new ParsedRule(condition, new GroupByRelation("id"));

        assertThatThrownBy(() -> translator.translate(rule, "comptes"))
                .isInstanceOf(InvalidRelationException.class);
    }

    // ------------------------------------------------------------------
    // Cas 3 (J9) : colonne vs colonne, deux tables - cas "stock/commandes"
    // ------------------------------------------------------------------

    @Test
    void devraitTraduireUneComparaisonColonneColonneEntreDeuxTablesAvecJointure() {
        var condition = new ParsedCondition(
                new ColumnOperand("commandes", "quantite"), ComparisonOperator.GT, new ColumnOperand("stock", "quantite_disponible"));
        var join = new JoinRelation(new ColumnOperand("commandes", "produit_id"), new ColumnOperand("stock", "produit_id"));
        var rule = new ParsedRule(condition, join);

        TranslatedQuery result = translator.translate(rule, "commandes");

        assertThat(result.sql()).isEqualTo(
                "SELECT commandes.id, commandes.quantite, stock.quantite_disponible"
                        + " FROM commandes JOIN stock ON commandes.produit_id = stock.produit_id"
                        + " WHERE commandes.quantite <= stock.quantite_disponible");
        assertThat(result.primaryKeyColumns()).containsExactly("commandes.id");
    }

    @Test
    void devraitExigerUneJointurePourUneComparaisonEntreDeuxTables() {
        var condition = new ParsedCondition(
                new ColumnOperand("commandes", "quantite"), ComparisonOperator.GT, new ColumnOperand("stock", "quantite_disponible"));

        assertThatThrownBy(() -> translator.translate(ruleSansRelation(condition), "commandes"))
                .isInstanceOf(InvalidRelationException.class);
    }

    @Test
    void devraitRejeterUneClauseOnSansPrefixesDeTableExplicites() {
        var condition = new ParsedCondition(
                new ColumnOperand("commandes", "quantite"), ComparisonOperator.GT, new ColumnOperand("stock", "quantite_disponible"));
        var join = new JoinRelation(new ColumnOperand(null, "produit_id"), new ColumnOperand("stock", "produit_id"));
        var rule = new ParsedRule(condition, join);

        assertThatThrownBy(() -> translator.translate(rule, "commandes"))
                .isInstanceOf(InvalidRelationException.class);
    }

    @Test
    void devraitRejeterUneClauseOnNeReliantPasLaTableCible() {
        var condition = new ParsedCondition(
                new ColumnOperand("commandes", "quantite"), ComparisonOperator.GT, new ColumnOperand("stock", "quantite_disponible"));
        var join = new JoinRelation(new ColumnOperand("stock", "produit_id"), new ColumnOperand("clients", "id"));
        var rule = new ParsedRule(condition, join);

        assertThatThrownBy(() -> translator.translate(rule, "commandes"))
                .isInstanceOf(InvalidRelationException.class);
    }

    // ------------------------------------------------------------------
    // Cas 4 (J9) : agrégat vs valeur, une table
    // ------------------------------------------------------------------

    @Test
    void devraitTraduireUnAgregatContreUneValeurAvecGroupBy() {
        var condition = new ParsedCondition(
                new AggregateOperand(AggregateFunction.SUM, new ColumnOperand("transactions", "montant")),
                ComparisonOperator.GT, new LiteralOperand(1000L));
        var rule = new ParsedRule(condition, new GroupByRelation("client_id"));

        TranslatedQuery result = translator.translate(rule, "transactions");

        assertThat(result.sql()).isEqualTo(
                "SELECT client_id, SUM(montant) FROM transactions GROUP BY client_id HAVING SUM(montant) <= ?");
        assertThat(result.params()).containsExactly(1000L);
        assertThat(result.primaryKeyColumns()).containsExactly("client_id");
        assertThat(result.involvedColumns()).containsExactly("montant");
    }

    @Test
    void devraitExigerUnGroupByPourUnAgregatContreUneValeur() {
        var condition = new ParsedCondition(
                new AggregateOperand(AggregateFunction.SUM, new ColumnOperand("transactions", "montant")),
                ComparisonOperator.GT, new LiteralOperand(1000L));

        assertThatThrownBy(() -> translator.translate(ruleSansRelation(condition), "transactions"))
                .isInstanceOf(InvalidRelationException.class);
    }

    @Test
    void devraitRejeterUneClauseOnLaOuUnGroupByEstAttendu() {
        var condition = new ParsedCondition(
                new AggregateOperand(AggregateFunction.SUM, new ColumnOperand("transactions", "montant")),
                ComparisonOperator.GT, new LiteralOperand(1000L));
        var join = new JoinRelation(new ColumnOperand("transactions", "client_id"), new ColumnOperand("clients", "id"));
        var rule = new ParsedRule(condition, join);

        assertThatThrownBy(() -> translator.translate(rule, "transactions"))
                .isInstanceOf(InvalidRelationException.class);
    }

    // ------------------------------------------------------------------
    // Cas 5 (J9) : agrégat vs colonne, deux tables - cas "stock/commandes"
    // ------------------------------------------------------------------

    @Test
    void devraitTraduireLeCasStockCommandes() {
        var condition = new ParsedCondition(
                new AggregateOperand(AggregateFunction.SUM, new ColumnOperand("commandes", "quantite")),
                ComparisonOperator.GT, new ColumnOperand("stock", "quantite_disponible"));
        var join = new JoinRelation(new ColumnOperand("commandes", "produit_id"), new ColumnOperand("stock", "produit_id"));
        var rule = new ParsedRule(condition, join);

        TranslatedQuery result = translator.translate(rule, "commandes");

        assertThat(result.sql()).isEqualTo(
                "SELECT stock.produit_id, SUM(commandes.quantite), stock.quantite_disponible"
                        + " FROM commandes JOIN stock ON commandes.produit_id = stock.produit_id"
                        + " GROUP BY stock.produit_id, stock.quantite_disponible"
                        + " HAVING SUM(commandes.quantite) <= stock.quantite_disponible");
        assertThat(result.params()).isEmpty();
        assertThat(result.primaryKeyColumns()).containsExactly("stock.produit_id");
        assertThat(result.involvedColumns()).containsExactly("commandes.quantite", "stock.quantite_disponible");
    }

    @Test
    void devraitExigerUneJointurePourUnAgregatContreUneColonneDuneAutreTable() {
        var condition = new ParsedCondition(
                new AggregateOperand(AggregateFunction.SUM, new ColumnOperand("commandes", "quantite")),
                ComparisonOperator.GT, new ColumnOperand("stock", "quantite_disponible"));

        assertThatThrownBy(() -> translator.translate(ruleSansRelation(condition), "commandes"))
                .isInstanceOf(InvalidRelationException.class);
    }

    @Test
    void devraitRejeterUnAgregatCompareAUneColonneDeLaMemeTable() {
        var condition = new ParsedCondition(
                new AggregateOperand(AggregateFunction.SUM, new ColumnOperand("comptes", "solde")),
                ComparisonOperator.GT, new ColumnOperand("comptes", "plafond"));

        assertThatThrownBy(() -> translator.translate(ruleSansRelation(condition), "comptes"))
                .isInstanceOf(UnsupportedRuleTypeException.class);
    }
}

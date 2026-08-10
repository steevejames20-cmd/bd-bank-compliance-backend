package com.bridge.bdbank.dsl;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DslParserServiceTest {

    private final DslParserService parser = new DslParserService();

    @Test
    void devraitParserUneConditionSansPrefixeDeTable() {
        ParsedCondition result = parser.parse("age > 18");

        assertThat(result.table()).isNull();
        assertThat(result.column()).isEqualTo("age");
        assertThat(result.operator()).isEqualTo(ComparisonOperator.GT);
        assertThat(result.value()).isEqualTo(18L);
    }

    @Test
    void devraitParserUneConditionAvecPrefixeDeTable() {
        ParsedCondition result = parser.parse("clients.age >= 18");

        assertThat(result.table()).isEqualTo("clients");
        assertThat(result.column()).isEqualTo("age");
        assertThat(result.operator()).isEqualTo(ComparisonOperator.GE);
        assertThat(result.value()).isEqualTo(18L);
    }

    @Test
    void devraitParserUneValeurChaine() {
        ParsedCondition result = parser.parse("pays == \"FR\"");

        assertThat(result.operator()).isEqualTo(ComparisonOperator.EQ);
        assertThat(result.value()).isEqualTo("FR");
    }

    @Test
    void devraitParserUneValeurDecimaleNegative() {
        ParsedCondition result = parser.parse("solde < -100.50");

        assertThat(result.operator()).isEqualTo(ComparisonOperator.LT);
        assertThat(result.value()).isEqualTo(-100.50);
    }

    @Test
    void devraitParserUneValeurBooleenne() {
        ParsedCondition result = parser.parse("actif != false");

        assertThat(result.operator()).isEqualTo(ComparisonOperator.NE);
        assertThat(result.value()).isEqualTo(false);
    }

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
}

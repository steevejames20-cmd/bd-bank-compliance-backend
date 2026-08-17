package com.bridge.bdbank.validation;

import com.bridge.bdbank.dsl.ColumnOperand;
import com.bridge.bdbank.dsl.ComparisonOperator;
import com.bridge.bdbank.dsl.DslParserService;
import com.bridge.bdbank.dsl.DslSyntaxException;
import com.bridge.bdbank.dsl.LiteralOperand;
import com.bridge.bdbank.dsl.ParsedCondition;
import com.bridge.bdbank.dsl.ParsedRule;
import com.bridge.bdbank.introspection.MissingPrimaryKeyException;
import com.bridge.bdbank.introspection.TableNotFoundException;
import com.bridge.bdbank.translation.InvalidRelationException;
import com.bridge.bdbank.translation.RuleTranslator;
import com.bridge.bdbank.translation.TableMismatchException;
import com.bridge.bdbank.translation.TranslatedQuery;
import com.bridge.bdbank.translation.UnknownColumnException;
import com.bridge.bdbank.translation.UnsupportedRuleTypeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RuleValidationServiceTest {

    @Mock
    private DslParserService dslParserService;

    @Mock
    private RuleTranslator ruleTranslator;

    private RuleValidationService validationService;

    @BeforeEach
    void setUp() {
        validationService = new RuleValidationService(dslParserService, ruleTranslator);
    }

    private static ParsedRule regleMinimale() {
        // Contenu non inspecté par ce test : seul le comportement du
        // service face aux exceptions nous intéresse ici, pas le contenu
        // réel de la règle parsée.
        return new ParsedRule(
                new ParsedCondition(new ColumnOperand(null, "age"), ComparisonOperator.GT, new LiteralOperand(18L)),
                null);
    }

    @Test
    void devraitValiderUneRegleCorrecte() {
        ParsedRule rule = regleMinimale();
        when(dslParserService.parse("age > 18")).thenReturn(rule);
        when(ruleTranslator.translate(rule, "clients")).thenReturn(
                new TranslatedQuery("SELECT id, age FROM clients WHERE age <= ?", List.of(18L), "clients", List.of("id"), List.of("age")));

        ValidationResult result = validationService.validate("age > 18", "clients");

        assertThat(result.valid()).isTrue();
        assertThat(result.errorMessage()).isNull();
    }

    @Test
    void devraitRemonterUneErreurDeSyntaxe() {
        when(dslParserService.parse("age >")).thenThrow(new DslSyntaxException("règle incomplète"));

        ValidationResult result = validationService.validate("age >", "clients");

        assertThat(result.valid()).isFalse();
        assertThat(result.errorMessage()).isEqualTo("règle incomplète");
    }

    @Test
    void devraitRemonterUneTableInconnue() {
        when(dslParserService.parse(anyString())).thenReturn(regleMinimale());
        when(ruleTranslator.translate(any(), anyString())).thenThrow(new TableNotFoundException("inexistante"));

        ValidationResult result = validationService.validate("age > 18", "inexistante");

        assertThat(result.valid()).isFalse();
        assertThat(result.errorMessage()).isNotBlank();
    }

    @Test
    void devraitRemonterUneClePrimaireManquante() {
        when(dslParserService.parse(anyString())).thenReturn(regleMinimale());
        when(ruleTranslator.translate(any(), anyString())).thenThrow(new MissingPrimaryKeyException("clients"));

        ValidationResult result = validationService.validate("age > 18", "clients");

        assertThat(result.valid()).isFalse();
    }

    @Test
    void devraitRemonterUnPrefixeDeTableIncorrect() {
        when(dslParserService.parse(anyString())).thenReturn(regleMinimale());
        when(ruleTranslator.translate(any(), anyString())).thenThrow(new TableMismatchException("comptes", "clients"));

        ValidationResult result = validationService.validate("comptes.solde > 0", "clients");

        assertThat(result.valid()).isFalse();
    }

    @Test
    void devraitRemonterUneColonneInconnue() {
        when(dslParserService.parse(anyString())).thenReturn(regleMinimale());
        when(ruleTranslator.translate(any(), anyString())).thenThrow(new UnknownColumnException("xyz", "clients"));

        ValidationResult result = validationService.validate("xyz > 1", "clients");

        assertThat(result.valid()).isFalse();
        assertThat(result.errorMessage()).contains("xyz");
    }

    @Test
    void devraitRemonterUnTypeDeRegleNonSupporte() {
        when(dslParserService.parse(anyString())).thenReturn(regleMinimale());
        when(ruleTranslator.translate(any(), anyString())).thenThrow(new UnsupportedRuleTypeException("agrégat contre agrégat"));

        ValidationResult result = validationService.validate("SUM(a.x) > SUM(a.y)", "a");

        assertThat(result.valid()).isFalse();
    }

    @Test
    void devraitRemonterUneRelationInvalide() {
        when(dslParserService.parse(anyString())).thenReturn(regleMinimale());
        when(ruleTranslator.translate(any(), anyString())).thenThrow(new InvalidRelationException("clause ON manquante"));

        ValidationResult result = validationService.validate("SUM(a.x) > b.y", "a");

        assertThat(result.valid()).isFalse();
        assertThat(result.errorMessage()).isEqualTo("clause ON manquante");
    }
}

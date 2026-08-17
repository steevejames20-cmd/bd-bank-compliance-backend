package com.bridge.bdbank.validation;

import com.bridge.bdbank.dsl.DslParserService;
import com.bridge.bdbank.dsl.DslSyntaxException;
import com.bridge.bdbank.dsl.ParsedRule;
import com.bridge.bdbank.introspection.MissingPrimaryKeyException;
import com.bridge.bdbank.introspection.TableNotFoundException;
import com.bridge.bdbank.translation.InvalidRelationException;
import com.bridge.bdbank.translation.RuleTranslator;
import com.bridge.bdbank.translation.TableMismatchException;
import com.bridge.bdbank.translation.UnknownColumnException;
import com.bridge.bdbank.translation.UnsupportedRuleTypeException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Valide une règle DSL sans l'exécuter ni la sauvegarder : enchaîne le
 * parsing (J6/J7) et la traduction (J8/J9), et capture toutes les
 * erreurs connues pour retourner un message clair plutôt que de laisser
 * l'exception remonter telle quelle.
 * <p>
 * La table cible reste un paramètre explicite (même choix qu'au J8/J9) :
 * l'association règle -> table sera formalisée par l'entité Règle à la
 * semaine 3.
 */
@Service
@RequiredArgsConstructor
public class RuleValidationService {

    private final DslParserService dslParserService;
    private final RuleTranslator ruleTranslator;

    public ValidationResult validate(String dslText, String targetTable) {
        ParsedRule rule;
        try {
            rule = dslParserService.parse(dslText);
        } catch (DslSyntaxException e) {
            return ValidationResult.error(e.getMessage());
        }

        try {
            ruleTranslator.translate(rule, targetTable);
        } catch (TableNotFoundException | MissingPrimaryKeyException
                 | TableMismatchException | UnknownColumnException
                 | UnsupportedRuleTypeException | InvalidRelationException e) {
            return ValidationResult.error(e.getMessage());
        }

        return ValidationResult.ok();
    }
}

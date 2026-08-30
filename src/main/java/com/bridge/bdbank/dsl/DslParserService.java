package com.bridge.bdbank.dsl;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.springframework.stereotype.Service;

/**
 * Point d'entrée du moteur DSL : transforme une règle écrite en texte
 * (ex. "clients.age < 18", "comptes.solde < comptes.decouvert_autorise",
 * "SUM(transactions.montant) > 1000 GROUP BY client_id") en
 * {@link ParsedRule} exploitable par le reste de l'application.
 * <p>
 * La traduction en SQL (J8/J9) part de ce résultat, aucune requête SQL
 * n'est générée à ce stade.
 */
@Service
public class DslParserService {

    /**
     * @throws DslSyntaxException si la règle est syntaxiquement invalide,
     * avec un message indiquant la ligne/position du problème.
     */
    public ParsedRule parse(String dslText) {
        var lexer = new RuleDslLexer(CharStreams.fromString(dslText));
        lexer.removeErrorListeners();
        lexer.addErrorListener(ThrowingErrorListener.INSTANCE);

        var tokens = new CommonTokenStream(lexer);
        var parser = new RuleDslParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(ThrowingErrorListener.INSTANCE);

        RuleDslParser.DslRuleContext tree = parser.dslRule();

        return (ParsedRule) new ConditionBuilderVisitor().visit(tree);
    }
}

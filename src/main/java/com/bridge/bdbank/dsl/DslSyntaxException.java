package com.bridge.bdbank.dsl;

/**
 * Levée quand une règle DSL est syntaxiquement invalide. Porte un message
 * déjà formaté (ligne, position, description) - c'est ce message qui sera
 * renvoyé à l'admin lors de la saisie (POST /rules/validate, semaine 4).
 */
public class DslSyntaxException extends RuntimeException {

    public DslSyntaxException(String message) {
        super(message);
    }
}

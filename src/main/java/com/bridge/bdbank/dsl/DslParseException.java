package com.bridge.bdbank.dsl;

/**
 * Levée quand une règle DSL ne peut pas être parsée.
 * Utilisée comme alias pour DslSyntaxException dans le gestionnaire d'exceptions global.
 */
public class DslParseException extends RuntimeException {

    public DslParseException(String message) {
        super(message);
    }

    public DslParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
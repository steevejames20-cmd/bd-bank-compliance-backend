package com.bridge.bdbank.translation;

/**
 * Levée quand une règle ne relève pas du cas ligne-à-ligne géré au J8
 * (comparaison multi-colonnes ou agrégat - traduits au J9).
 */
public class UnsupportedRuleTypeException extends RuntimeException {

    public UnsupportedRuleTypeException(String message) {
        super(message);
    }
}

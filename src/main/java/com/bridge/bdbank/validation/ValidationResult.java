package com.bridge.bdbank.validation;

/**
 * Résultat de la validation d'une règle DSL, sans exécution ni
 * sauvegarde. {@code errorMessage} est {@code null} quand {@code valid}
 * vaut {@code true}. Alimentera directement la réponse de
 * POST /rules/validate (semaine 4).
 */
public record ValidationResult(boolean valid, String errorMessage) {

    public static ValidationResult ok() {
        return new ValidationResult(true, null);
    }

    public static ValidationResult error(String message) {
        return new ValidationResult(false, message);
    }
}

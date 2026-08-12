package com.bridge.bdbank.translation;

import java.util.List;

/**
 * Résultat de la traduction d'une règle DSL en requête SQL exécutable.
 * <p>
 * {@code primaryKeyColumns} et {@code involvedColumns} sont distingués
 * volontairement : les premières servent uniquement à identifier la ligne
 * dans l'alerte, les secondes sont les colonnes réellement citées par la
 * règle (cf. contenu de l'alerte décrit dans le schéma fonctionnel).
 */
public record TranslatedQuery(
        String sql,
        List<Object> params,
        String table,
        List<String> primaryKeyColumns,
        List<String> involvedColumns
) {
}

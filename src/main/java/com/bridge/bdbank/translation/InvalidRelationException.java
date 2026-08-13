package com.bridge.bdbank.translation;

/**
 * Levée quand la clause de relation (ON ou GROUP BY) d'une règle est
 * absente alors qu'elle est obligatoire, présente alors qu'elle ne
 * s'applique pas, ou syntaxiquement incohérente (ex: clause ON ne
 * reliant pas la table cible à exactement une autre table).
 */
public class InvalidRelationException extends RuntimeException {

    public InvalidRelationException(String message) {
        super(message);
    }
}

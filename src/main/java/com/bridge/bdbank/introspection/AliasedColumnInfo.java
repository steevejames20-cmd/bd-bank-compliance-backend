package com.bridge.bdbank.introspection;

/**
 * Une colonne de la bd_bank, avec son nom d'affichage à côté du nom
 * technique réel. displayName vaut realName tant qu'aucun alias n'a été
 * défini par l'administrateur dans l'espace "Schéma &amp; Périmètre".
 */
public record AliasedColumnInfo(String realName, String displayName, String typeName, boolean nullable) {
}

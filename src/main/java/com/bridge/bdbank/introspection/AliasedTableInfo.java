package com.bridge.bdbank.introspection;

/**
 * Une table de la bd_bank, avec son nom d'affichage à côté du nom
 * technique réel. displayName vaut realName tant qu'aucun alias n'a été
 * défini par l'administrateur dans l'espace "Schéma &amp; Périmètre".
 */
public record AliasedTableInfo(String realName, String displayName) {
}

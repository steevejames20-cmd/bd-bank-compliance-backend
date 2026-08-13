grammar RuleDsl;

// J9 : une condition peut être suivie d'une clause de relation optionnelle -
// jointure explicite entre deux tables (ON), ou regroupement pour un agrégat
// mono-table (GROUP BY). Absente jusqu'ici : J6/J7 ne parsaient qu'une
// condition seule, ce qui suffisait tant qu'aucune règle multi-tables ou
// agrégat n'était réellement traduite en SQL.
dslRule
    : condition (joinClause | groupByClause)? EOF
    ;

// J7 : les deux côtés sont un "operand" générique (colonne, valeur ou
// agrégat) - permet "comptes.solde > comptes.decouvert_autorise" ou
// "SUM(transactions.montant) > 1000", sans grammaire séparée par cas.
condition
    : left=operand op=(GT | LT | GE | LE | EQ | NE) right=operand
    ;

// Jointure explicite entre deux tables, écrite par l'admin (ex:
// "... ON commandes.produit_id == stock.produit_id"). Les deux colonnes
// doivent préciser leur table : c'est justement ce qui manquait pour que
// le traducteur sache comment relier les deux tables.
joinClause
    : ON leftCol=column EQ rightCol=column
    ;

// Regroupement explicite pour un agrégat sur une seule table (ex:
// "SUM(transactions.montant) > 1000 GROUP BY client_id").
groupByClause
    : GROUP BY col=IDENTIFIER
    ;

operand
    : column        # columnOperand
    | value         # valueOperand
    | aggregate     # aggregateOperand
    ;

// Fonction d'agrégat appliquée à une colonne (ex: SUM(transactions.montant)).
// Toujours avec une colonne entre parenthèses (pas de COUNT(*) pour l'instant).
aggregate
    : func=(SUM | COUNT | AVG | MAX | MIN) LPAREN column RPAREN
    ;

// Préfixe de table optionnel (ex: clients.age ou juste age).
column
    : (table=IDENTIFIER DOT)? col=IDENTIFIER
    ;

// Une alternative par type de valeur (labels #xxx) : le visiteur Java a une
// méthode dédiée par type, plutôt que d'avoir à tester le contenu.
value
    : NUMBER   # numberValue
    | STRING   # stringValue
    | BOOLEAN  # booleanValue
    ;

// --- Lexer ---

// Mots-clés déclarés AVANT IDENTIFIER pour être reconnus en priorité (sinon
// ils matcheraient comme des identifiants classiques). Effet de bord assumé :
// ces mots deviennent réservés, ne peuvent plus être utilisés comme nom de
// colonne (aucune collision dans notre schéma actuel).
SUM   : 'SUM' ;
COUNT : 'COUNT' ;
AVG   : 'AVG' ;
MAX   : 'MAX' ;
MIN   : 'MIN' ;
ON    : 'ON' ;
GROUP : 'GROUP' ;
BY    : 'BY' ;

LPAREN : '(' ;
RPAREN : ')' ;

GT  : '>' ;
LT  : '<' ;
GE  : '>=' ;
LE  : '<=' ;
EQ  : '==' ;
NE  : '!=' ;
DOT : '.' ;

BOOLEAN : 'true' | 'false' ;
NUMBER  : '-'? DIGIT+ ('.' DIGIT+)? ;
STRING  : '"' (~["\r\n])* '"' ;
IDENTIFIER : LETTER (LETTER | DIGIT | '_')* ;

fragment DIGIT  : [0-9] ;
fragment LETTER : [a-zA-Z] ;

WS : [ \t\r\n]+ -> skip ;

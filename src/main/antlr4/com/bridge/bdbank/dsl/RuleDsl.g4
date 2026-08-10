grammar RuleDsl;

// Point d'entrée pour le J6 : une seule condition (règle ligne-à-ligne).
// Les comparaisons colonne-colonne et les agrégats (SUM, COUNT...)
// arrivent en J7 - cette grammaire sera étendue, pas réécrite.
dslRule
    : condition EOF
    ;

condition
    : column op=(GT | LT | GE | LE | EQ | NE) value
    ;

// Préfixe de table optionnel, dès le J6 (ex: clients.age ou juste age).
column
    : (table=IDENTIFIER DOT)? col=IDENTIFIER
    ;

// Une alternative par type de valeur (labels #xxx) : le visiteur Java aura
// une méthode dédiée par type, plutôt que d'avoir à tester le contenu.
value
    : NUMBER   # numberValue
    | STRING   # stringValue
    | BOOLEAN  # booleanValue
    ;

// --- Lexer ---

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

grammar RuleDsl;

// Point d'entrée : une seule condition (règle ligne-à-ligne).
dslRule
    : condition EOF
    ;

// J7 : les deux côtés sont désormais un "operand" générique (colonne,
// valeur ou agrégat) - avant (J6), seule la gauche pouvait être une
// colonne. Ça permet "comptes.solde > comptes.decouvert_autorise" ou
// "SUM(transactions.montant) > 1000", sans grammaire séparée pour chaque cas.
condition
    : left=operand op=(GT | LT | GE | LE | EQ | NE) right=operand
    ;

operand
    : column        # columnOperand
    | value         # valueOperand
    | aggregate     # aggregateOperand
    ;

// Fonction d'agrégat appliquée à une colonne (ex: SUM(transactions.montant)).
// Toujours avec une colonne entre parenthèses pour le J7 (pas de COUNT(*)
// pour l'instant - extension possible plus tard si besoin).
aggregate
    : func=(SUM | COUNT | AVG | MAX | MIN) LPAREN column RPAREN
    ;

// Préfixe de table optionnel (ex: clients.age ou juste age).
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

// Mots-clés des agrégats : déclarés AVANT IDENTIFIER pour être reconnus en
// priorité (sinon "SUM" matcherait comme un identifiant classique). Effet
// de bord assumé : ces 5 mots deviennent réservés, ne peuvent plus être
// utilisés comme nom de colonne (aucune collision dans notre schéma actuel).
SUM   : 'SUM' ;
COUNT : 'COUNT' ;
AVG   : 'AVG' ;
MAX   : 'MAX' ;
MIN   : 'MIN' ;

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

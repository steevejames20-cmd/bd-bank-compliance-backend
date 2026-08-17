-- J10 : ajoute le découvert autorisé sur les comptes, nécessaire pour les
-- tests de bout en bout du traducteur DSL -> SQL (colonne/colonne même
-- table, et agrégat/colonne entre deux tables).
--
-- Contrairement à 001_schema_and_seed.sql, ce fichier ne s'exécute PAS
-- automatiquement sur un conteneur déjà initialisé (docker-entrypoint-
-- initdb.d ne tourne qu'au tout premier démarrage, volume vide). Sur une
-- base déjà en place, applique-le manuellement (commande fournie à côté).
-- Sur une réinstallation complète (volume supprimé), il s'exécutera après
-- 001 automatiquement grâce au préfixe numérique.

ALTER TABLE comptes ADD COLUMN decouvert_autorise DECIMAL(15, 2) NOT NULL DEFAULT 0;

UPDATE comptes SET decouvert_autorise = -500.00 WHERE id = 1;
UPDATE comptes SET decouvert_autorise = -200.00 WHERE id = 2;
UPDATE comptes SET decouvert_autorise = -100.00 WHERE id = 3;
UPDATE comptes SET decouvert_autorise = -1000.00 WHERE id = 4;
UPDATE comptes SET decouvert_autorise = -300.00 WHERE id = 5;
UPDATE comptes SET decouvert_autorise = -50.00 WHERE id = 6;
UPDATE comptes SET decouvert_autorise = -50.00 WHERE id = 7;
UPDATE comptes SET decouvert_autorise = -500.00 WHERE id = 8;

-- Le compte lecture seule a deja SELECT sur toute la base (GRANT ... ON
-- bd_bank_test.*), donc aucun GRANT supplementaire n'est necessaire pour
-- cette nouvelle colonne.

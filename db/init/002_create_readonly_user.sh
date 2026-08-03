#!/bin/bash
# Crée le compte technique de l'application : lecture seule, rien d'autre.
#
# Pourquoi un .sh et pas un .sql comme 001 ? Parce que les scripts .sh dans
# docker-entrypoint-initdb.d ont accès aux variables d'environnement du
# conteneur (ici APP_DB_USER / APP_DB_PASSWORD, définies dans
# docker-compose.yml), ce qu'un simple fichier .sql ne permet pas.
# Cela évite de coder le mot de passe en dur dans ce fichier.
set -e

mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" <<-EOSQL
    CREATE USER IF NOT EXISTS '${APP_DB_USER}'@'%' IDENTIFIED BY '${APP_DB_PASSWORD}';

    -- SELECT uniquement, sur la base surveillée. Pas d'INSERT, UPDATE,
    -- DELETE, DROP ni ALTER : même une erreur de code ne peut pas
    -- modifier ou supprimer des données de la bd_bank.
    GRANT SELECT ON \`${MYSQL_DATABASE}\`.* TO '${APP_DB_USER}'@'%';

    FLUSH PRIVILEGES;
EOSQL

echo "Compte lecture seule '${APP_DB_USER}' créé sur ${MYSQL_DATABASE} (SELECT uniquement)."

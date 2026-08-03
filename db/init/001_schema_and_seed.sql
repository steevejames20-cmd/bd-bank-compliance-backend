-- Jeu de données minimal pour la base de test locale (représente la bd_bank).
-- Exécuté automatiquement au premier démarrage du conteneur MySQL
-- (docker-entrypoint-initdb.d), une seule fois (tant que le volume persiste).
--
-- Objectif : avoir de vraies tables/colonnes à disposition dès le J3
-- (introspection), sans attendre une base de test "réelle".

CREATE TABLE IF NOT EXISTS clients (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    nom           VARCHAR(100) NOT NULL,
    email         VARCHAR(150) NOT NULL UNIQUE,
    date_naissance DATE,
    pays          VARCHAR(2) NOT NULL DEFAULT 'FR'
);

CREATE TABLE IF NOT EXISTS comptes (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    client_id     BIGINT NOT NULL,
    iban          VARCHAR(34) NOT NULL UNIQUE,
    solde         DECIMAL(15, 2) NOT NULL DEFAULT 0,
    devise        VARCHAR(3) NOT NULL DEFAULT 'EUR',
    statut        VARCHAR(20) NOT NULL DEFAULT 'ACTIF',
    CONSTRAINT fk_comptes_client FOREIGN KEY (client_id) REFERENCES clients(id)
);

CREATE TABLE IF NOT EXISTS transactions (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    compte_id     BIGINT NOT NULL,
    montant       DECIMAL(15, 2) NOT NULL,
    type          VARCHAR(20) NOT NULL,
    date_operation DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_transactions_compte FOREIGN KEY (compte_id) REFERENCES comptes(id)
);

INSERT INTO clients (nom, email, date_naissance, pays) VALUES
    ('Amina Fotso', 'amina.fotso@example.com', '1990-04-12', 'CM'),
    ('Julien Marchand', 'julien.marchand@example.com', '1985-11-02', 'FR'),
    ('Grace Mballa', 'grace.mballa@example.com', '2002-07-19', 'CM');

INSERT INTO comptes (client_id, iban, solde, devise, statut) VALUES
    (1, 'CM2110003001234567890123456', 1500.00, 'XAF', 'ACTIF'),
    (2, 'FR7630006000011234567890189', -250.75, 'EUR', 'ACTIF'),
    (3, 'CM2110003009876543210987654', 0.00, 'XAF', 'SUSPENDU');

INSERT INTO transactions (compte_id, montant, type, date_operation) VALUES
    (1, 500.00, 'DEPOT', '2026-07-20 09:15:00'),
    (1, -120.50, 'RETRAIT', '2026-07-22 14:03:00'),
    (2, -300.00, 'RETRAIT', '2026-07-25 18:47:00'),
    (3, 0.00, 'DEPOT', '2026-07-28 11:00:00');

-- Note sécurité (cf. docs/schema-fonctionnement.md, volet 6) :
-- ce script tourne avec les droits root (comportement par défaut de
-- l'image MySQL pour les fichiers .sql d'init) afin de pouvoir créer les
-- tables et insérer les données de seed. L'application, elle, n'utilise
-- JAMAIS ce compte : voir 002_create_readonly_user.sh pour le compte
-- technique dédié, strictement en lecture seule (SELECT uniquement).

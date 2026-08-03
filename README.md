# bd-bank-compliance-backend

Outil de vérification de conformité des données — Bridge bd_bank.

Backend Java (Spring Boot) qui traduit des règles métier écrites en DSL en
requêtes SQL, les exécute en lecture seule sur la base d'une banque
(bd_bank), et génère des alertes en cas de non-conformité.

Le détail du fonctionnement, de la roadmap et du contrat d'API se trouve
dans le dossier [`docs/`](./docs) (ajouté sur la branche `jour-01-init-projet`).

## Démarrage local (J1)

> **Sous Windows (PowerShell)** : partout où tu vois `set -a; source .env; set +a`
> (syntaxe bash), utilise à la place `.\scripts\load-env.ps1`. Le reste des
> commandes (`docker`, `mvn`, `git`) est identique.

1. Copier `.env.example` en `.env` et ajuster les valeurs si besoin :
   ```bash
   cp .env.example .env
   ```
2. Lancer la base de test locale (MySQL, avec un jeu de données de départ) :
   ```bash
   docker compose up -d
   ```
3. Charger les variables d'environnement dans le shell, puis lancer l'application :
   ```bash
   set -a; source .env; set +a
   mvn spring-boot:run
   ```
4. Vérifier que tout compile et que le contexte Spring démarre (= la connexion
   à la base de test est valide) :
   ```bash
   mvn clean compile
   mvn test
   ```

> Le compte MySQL utilisé ici (`DB_USER`/`DB_PASSWORD`) a des droits larges
> pour permettre le seed initial. Le compte technique dédié en lecture seule
> (SELECT uniquement), tel que décrit dans `docs/schema-fonctionnement.md`,
> sera mis en place en J2.

## Workflow Git

Une branche par journée de travail de la roadmap, nommée `jour-XX-slug`
(ex. `jour-01-init-projet`, `jour-02-connexion-jdbc`). Chaque branche est
mergée dans `main` une fois l'objectif du jour validé.

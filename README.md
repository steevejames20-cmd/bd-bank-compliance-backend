# bd-bank-compliance-backend

Outil de vérification de conformité des données — Bridge bd_bank.

Backend Java (Spring Boot) qui traduit des règles métier écrites en DSL en
requêtes SQL, les exécute en lecture seule sur la base d'une banque
(bd_bank), et génère des alertes en cas de non-conformité.

Le détail du fonctionnement, de la roadmap et du contrat d'API se trouve
dans le dossier [`docs/`](./docs) (ajouté sur la branche `jour-01-init-projet`).

## Workflow Git

Une branche par journée de travail de la roadmap, nommée `jour-XX-slug`
(ex. `jour-01-init-projet`, `jour-02-connexion-jdbc`). Chaque branche est
mergée dans `main` une fois l'objectif du jour validé.

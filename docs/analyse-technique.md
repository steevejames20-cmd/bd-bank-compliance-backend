# Analyse technique du projet Bridge bd_bank

Date de référence : 2 septembre 2026

## 1. Résumé

Bridge bd_bank est un outil interne de contrôle de conformité des données
bancaires. Un administrateur définit des règles métier dans un DSL propriétaire.
Le backend transforme ces règles en requêtes SQL paramétrées, les exécute en
lecture seule sur une base relationnelle et conserve les anomalies détectées
sous forme d'alertes.

Le projet vise une utilisation locale ou sur un réseau interne. La version 1
prévoit un seul administrateur, une base MySQL ou PostgreSQL côté données
bancaires et une base H2 interne pour les règles, les alertes et la configuration.

## 2. Objectifs fonctionnels

- Découvrir les tables et les colonnes d'une base bancaire.
- Définir le périmètre des tables à surveiller.
- Écrire des contrôles simples, multi-colonnes ou agrégés.
- Traduire le DSL en SQL sans exposer de SQL au frontend.
- Exécuter les contrôles à intervalles réguliers.
- Créer une alerte par anomalie détectée et par règle.
- Maintenir l'historique et auto-résoudre une alerte quand l'anomalie disparaît.
- Administrer l'ensemble depuis une console web.

## 3. Stack technique

### Backend

- Java 25.
- Spring Boot 3.5.16.
- Spring Web pour l'API REST.
- Spring Data JPA et Hibernate pour la persistance interne.
- JDBC et `JdbcTemplate` pour l'accès à `bd_bank`.
- MySQL Connector/J et driver PostgreSQL.
- H2 fichier pour la base interne locale.
- ANTLR 4.13.2 pour le parsing du DSL.
- SpringDoc OpenAPI 2.7.0 pour Swagger.
- BCrypt pour le hashage des mots de passe.
- Maven pour le build et la gestion des dépendances.

### Frontend

- React.
- Vite.
- `lucide-react` pour les icônes.
- CSS dédié responsive, avec polices système : Apple/Segoe UI/Helvetica/Arial.
- Mode réel via `VITE_DEMO_MODE=false` et mode de démonstration via `true`.

### Environnement local

- Docker Compose pour MySQL.
- PowerShell sous Windows.
- Variables de configuration dans `.env`, exclu du dépôt.

## 4. Architecture logique

```text
Administrateur
     |
     v
Frontend React/Vite
     | JSON + Authorization: Bearer
     v
API REST Spring Boot
     |
     +--> AuthController / sécurité de session
     +--> SchemaController / introspection
     +--> ScopeController / périmètre
     +--> RuleController / règles et validation
     +--> AlertController / alertes
     +--> ConfigController / fréquence
     |
     +--> Services métier
     |      +--> DslParserService
     |      +--> RuleTranslator
     |      +--> RuleExecutionService
     |      +--> RuleScheduler
     |      +--> AlertHistoryService
     |
     +--> H2 interne : règles, alertes, scope, fréquence, users
     |
     +--> DataSource bd_bank en lecture seule
             |
             +--> MySQL ou PostgreSQL
```

Le frontend ne parle jamais directement à la base et ne construit jamais de
requête SQL. Le backend sépare la base de l'outil de la base bancaire : H2 est
la datasource primaire JPA, tandis que `bankDataSource` est qualifiée pour la
connexion, l'introspection, la traduction et l'exécution des règles.

## 5. Flux métier principal

1. L'administrateur ouvre la console et s'authentifie.
2. Le frontend récupère le schéma et le périmètre courant.
3. L'administrateur choisit les tables à surveiller.
4. Il écrit une règle DSL et choisit sa table cible et sa gravité.
5. Le backend parse la règle avec ANTLR.
6. Le traducteur vérifie les colonnes et les relations, puis produit du SQL
   paramétré.
7. Le scheduler exécute les règles actives séquentiellement.
8. Les lignes ou groupes en anomalie sont lus avec leur identifiant et les
   colonnes impliquées.
9. Le moteur crée ou met à jour les alertes.
10. Une alerte active qui n'est plus retrouvée au cycle suivant devient résolue.

## 6. DSL pris en charge

La règle décrit directement l'anomalie recherchée. Il n'y a plus d'inversion
implicite de l'opérateur.

### Règle ligne à ligne

```text
comptes.solde < 0
```

Le SQL correspondant recherche les comptes dont le solde est inférieur à zéro.

### Comparaison de colonnes

```text
comptes.solde < comptes.decouvert_autorise
```

### Comparaison entre tables

```text
transactions.montant < comptes.decouvert_autorise ON transactions.compte_id == comptes.id
```

### Agrégat

```text
SUM(transactions.montant) < 0 GROUP BY compte_id
```

Les fonctions couvertes sont `SUM`, `COUNT`, `AVG`, `MAX` et `MIN`. Les jointures
et les regroupements sont écrits explicitement dans la règle quand ils sont
nécessaires.

## 7. Données internes

### Rule

Une règle contient un identifiant, le texte DSL, la table cible, une gravité,
un statut actif et des dates de création et de mise à jour.

### Alert

Une alerte contient la règle associée, le statut `ACTIVE` ou `RESOLVED`, la date
de détection, la date de résolution éventuelle, l'identifiant de l'entité en
anomalie, les colonnes impliquées et le nombre de détections consécutives.
Les valeurs bancaires complètes ne sont pas stockées dans l'alerte.

### Scope

Le périmètre contient un ensemble de noms de tables. Le service valide chaque
nom contre l'introspection MySQL/PostgreSQL avant de l'activer.

### User

La version locale prévoit un seul utilisateur administrateur. Le mot de passe
est stocké sous forme de hash BCrypt.

## 8. API REST

Toutes les routes protégées utilisent `Authorization: Bearer <token>`.

| Méthode | Route | Fonction |
|---|---|---|
| POST | `/auth/login` | Ouvrir une session |
| POST | `/auth/logout` | Invalider une session |
| GET | `/auth/me` | Vérifier la session |
| POST | `/auth/setup` | Créer le premier administrateur avec `X-Setup-Key` |
| GET | `/schema/tables` | Lister les tables détectées |
| GET | `/schema/tables/{table}/columns` | Lister les colonnes et types |
| GET | `/scope` | Lire le périmètre actif |
| PUT | `/scope` | Remplacer le périmètre actif |
| GET/POST/PUT/DELETE | `/rules` | Gérer les règles |
| POST | `/rules/validate` | Valider une règle sans la sauvegarder |
| GET | `/alerts` | Lister les alertes avec pagination et filtre |
| GET | `/alerts/{id}` | Lire le détail d'une alerte |
| GET/PUT | `/config/frequency` | Lire ou modifier la fréquence |

Les contrôleurs utilisent `page` et `size` pour les listes paginées. La taille
par défaut est 25.

## 9. Sécurité et confidentialité

- Le compte SQL `bdbank_readonly` possède uniquement `SELECT` sur `bd_bank_test`.
- Les identifiants de base et la clé d'initialisation viennent de `.env`.
- `.env` est exclu du dépôt.
- Le premier compte se crée seulement via `/setup`, avec une clé serveur et une
  vérification qu'aucun utilisateur n'existe déjà.
- Les mots de passe sont hashés avec BCrypt.
- Cinq échecs de connexion déclenchent un verrouillage temporaire de 30 minutes.
- Les tokens de session sont aléatoires, conservés en mémoire, invalidés au
  logout et expirent après 15 minutes sans activité.
- Les alertes ne remontent pas les valeurs bancaires complètes.
- Le CORS autorise les origines locales `localhost:5173` et `127.0.0.1:5173`.

Pour un déploiement bancaire réel, il faudra ajouter une gestion de secrets,
un stockage de sessions partagé, HTTPS, une politique CORS stricte et une
authentification renforcée.

## 10. Tests et validation

Les tests couvrent le parsing, la traduction, la validation, la persistance,
l'exécution, le scheduler, le lockout et les contrôleurs REST. Les scénarios
MySQL sont identifiés par le tag `e2e` et nécessitent Docker ainsi que les
variables d'environnement chargées.

Commandes principales :

```powershell
# Base de test
Docker compose up -d

# Variables du backend
.\scripts\load-env.ps1

# Build et tests standards
mvn clean compile
mvn test

# Tests e2e avec MySQL
mvn test -Dgroups=e2e
```

Le projet a aussi été vérifié par une démonstration API réelle : login, profil,
schéma, lecture et mise à jour du périmètre, règles, alertes et configuration
de fréquence ont répondu avec les codes attendus.

## 11. État de livraison

### Backend livré et vérifié

- Fondations, connexion read-only et introspection.
- DSL ANTLR, traduction SQL et validation.
- Persistance, exécution, alertes, auto-résolution et scheduler.
- API REST, documentation OpenAPI, CORS et authentification locale.
- Mise à jour persistée du périmètre.
- Tests métier, API et sécurité ciblée.

### Frontend livré

La console React/Vite contient une synthèse, un centre d'alertes, une liste de
règles, un éditeur DSL, la vue schéma/périmètre, la configuration du scheduler,
le login et l'initialisation du premier compte.

Le chargement initial utilise l'API réelle quand `VITE_DEMO_MODE=false`. Le mode
démo fournit des données locales pour les présentations sans backend. Certaines
actions de démonstration de la console modifient encore l'état local de l'UI ;
les actions CRUD frontend devront être reliées aux appels REST correspondants
pour une intégration produit complète.

## 12. Limites et pistes v2

- Stockage des sessions en mémoire, adapté au local mais pas à plusieurs
  instances.
- Un seul administrateur.
- Pas de notification externe.
- Pas d'analyse incrémentale.
- Pagination backend à stabiliser sous forme de DTO dédié plutôt que de
  sérialiser directement `PageImpl`.
- Ajouter des tests e2e API automatisés et une démonstration reproductible CI.
- Ajouter une autocomplétion DSL basée sur le schéma introspecté.

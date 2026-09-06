# bd-bank-compliance-backend

Bridge bd_bank est un outil interne qui vérifie des règles métier sur des
données bancaires sans exposer la base au frontend.

Backend Java (Spring Boot) qui traduit des règles métier écrites en DSL en
requêtes SQL, les exécute en lecture seule sur la base d'une banque
(bd_bank), et génère des alertes en cas d'anomalie détectée.

Le projet expose une API REST pour gérer les règles, les alertes, le schéma,
le périmètre et la fréquence d'analyse. Le détail des routes est disponible
dans Swagger après le démarrage du backend, à l'adresse
`http://localhost:8080/swagger-ui.html`.

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

> Le compte MySQL utilisé ici (`DB_USER`/`DB_PASSWORD`) est le compte technique
> dédié à l'application. Il est limité à la lecture (`SELECT`) sur la base de
> test et ne doit pas être utilisé pour modifier les données.

## J2 — Compte lecture seule + couche de connexion JDBC

Le compte applicatif est maintenant réellement en lecture seule (`GRANT
SELECT` explicite dans `db/init/002_create_readonly_user.sh`), et une
couche `JdbcConnectionService` générique (MySQL/PostgreSQL) vérifie la
connexion au démarrage.

1. Réinitialiser la base de test pour que les nouveaux scripts d'init
   s'exécutent (ils ne tournent qu'au premier démarrage d'un volume vide) :

   ```bash
   docker compose down -v
   docker compose up -d
   ```

2. Vérifier que le compte est bien restreint au SELECT :

   ```bash
   # OK
   docker exec -it bd-bank-test-db mysql -u bdbank_readonly -p"change_me" bd_bank_test -e "SELECT * FROM clients;"

   # Doit être refusé (ERROR 1142 : INSERT command denied)
   docker exec -it bd-bank-test-db mysql -u bdbank_readonly -p"change_me" bd_bank_test -e "INSERT INTO clients (nom, email) VALUES ('test','test@test.com');"
   ```

3. Lancer l'app et vérifier dans les logs la ligne `Connexion bd_bank OK -> MySQL ...` :

   ```bash
   set -a; source .env; set +a
   mvn spring-boot:run
   ```

4. Lancer les tests (nécessite la base de test démarrée) :

   ```bash
   mvn test
   ```

## Workflow Git

Une branche par journée de travail de la roadmap, nommée `jour-XX-slug`
(ex. `jour-01-init-projet`, `jour-02-connexion-jdbc`). Chaque branche est
mergée dans `main` une fois l'objectif du jour validé.

## Démarrage du frontend

Dans un second terminal, depuis le dossier `Frontend` :

```powershell
cd Frontend
npm install
npm run dev
```

Le frontend est disponible sur `http://127.0.0.1:5173`. Pour utiliser l'API
réelle, `Frontend/.env` doit contenir `VITE_API_URL=http://localhost:8080` et
`VITE_DEMO_MODE=false`.

En mode réel, le dashboard recharge les tables, le périmètre, les règles, les
alertes et la fréquence depuis l'API. La posture de contrôle est calculée à
partir du taux de règles actives, de la couverture du périmètre et du taux
d'alertes résolues. Le bouton d'actualisation force un nouveau chargement.

Depuis le détail d'une alerte, le bouton de statut permet de la marquer comme
résolue ou de la réactiver automatiquement selon les cycles d'analyse. Pour
activer ou désactiver une règle, utilisez l'action correspondante dans la liste
des règles ; elle met à jour la règle via `PUT /rules/{id}`.

## Initialisation du compte administrateur

Le projet est prévu pour un seul administrateur. Si aucun utilisateur n'existe
encore, définir une clé privée dans `.env` :

```env
BDBANK_SETUP_KEY=une-valeur-longue-et-secrete
```

Redémarrer Spring Boot, puis ouvrir directement `http://127.0.0.1:5173/setup`.
Le formulaire demande cette clé, un identifiant valide et un mot de passe d'au
moins 12 caractères. L'endpoint `/auth/setup` est refusé dès qu'un compte
existe et n'est pas affiché dans la navigation.

Après l'initialisation, se connecter sur `http://127.0.0.1:5173/`. Les routes
protégées utilisent le token Bearer retourné par `/auth/login`; une session est
invalidée au logout et expire après 15 minutes sans activité.

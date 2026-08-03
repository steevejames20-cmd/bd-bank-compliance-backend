**ROADMAP DE DÉVELOPPEMENT**

*Outil de vérification de conformité des données --- Bridge bd_bank*

Durée totale : 5 semaines --- Stack : Java, MySQL/PostgreSQL, DSL
propriétaire

**Principe de construction de la roadmap**

La roadmap suit l\'ordre logique de dépendance technique : on ne peut
pas construire l\'interprétation des règles avant d\'avoir la connexion
à la base ; on ne peut pas construire les alertes avant d\'avoir un
moteur de détection fonctionnel ; la sécurité de base (comptes, .env,
lecture seule) est intégrée dès la semaine 1 plutôt qu\'ajoutée
après-coup, car elle conditionne l\'architecture.

Chaque semaine se termine par un objectif vérifiable (un livrable
testable), pas juste une liste de tâches.

**SEMAINE 1 --- Fondations --- Connexion & introspection de la bd_bank**

+----------+-----------------------------------------------------------+
| *        | **Tâches**                                                |
| *Jours** |                                                           |
+==========+===========================================================+
| **J1**   | -   Initialisation du projet Java (structure du repo,     |
|          |     gestion de dépendances Maven/Gradle)                  |
|          |                                                           |
|          | -   Mise en place du .env + .gitignore dès le premier     |
|          |     commit                                                |
|          |                                                           |
|          | -   Choix et configuration de la base de test locale      |
|          |     (MySQL ou PostgreSQL)                                 |
+----------+-----------------------------------------------------------+
| **J2**   | -   Création d\'un compte SQL dédié en lecture seule      |
|          |     (SELECT uniquement) sur la base de test               |
|          |                                                           |
|          | -   Développement de la couche de connexion JDBC          |
|          |     (générique MySQL/PostgreSQL)                          |
+----------+-----------------------------------------------------------+
| **J3**   | -   Développement du module d\'introspection : lister les |
|          |     tables disponibles                                    |
|          |                                                           |
|          | -   Développement de l\'introspection des colonnes et de  |
|          |     leurs types par table                                 |
+----------+-----------------------------------------------------------+
| **J4**   | -   Tests de connexion et d\'introspection sur des cas    |
|          |     réels (plusieurs tables, plusieurs types de colonnes) |
|          |                                                           |
|          | -   Gestion des erreurs de connexion (base injoignable,   |
|          |     identifiants invalides, etc.)                         |
+----------+-----------------------------------------------------------+
| **J5**   | -   Mise en place du mécanisme de déclaration du          |
|          |     périmètre (sélection des tables à surveiller)         |
|          |                                                           |
|          | -   Tests unitaires de la couche de connexion et          |
|          |     d\'introspection                                      |
+----------+-----------------------------------------------------------+

> **Objectif de fin de semaine :** l\'outil se connecte en lecture seule
> à la base de test, liste ses tables/colonnes, et permet de définir un
> périmètre de tables surveillées.

**SEMAINE 2 --- Le cœur du moteur --- DSL et traduction en SQL**

+----------+-----------------------------------------------------------+
| *        | **Tâches**                                                |
| *Jours** |                                                           |
+==========+===========================================================+
| **J6**   | -   Conception de la grammaire du DSL (syntaxe des règles |
|          |     ligne-à-ligne : ex age \> 18)                         |
|          |                                                           |
|          | -   Choix de l\'outil de parsing (ex: ANTLR) et mise en   |
|          |     place du parseur de base                              |
+----------+-----------------------------------------------------------+
| **J7**   | -   Extension de la grammaire aux comparaisons            |
|          |     multi-colonnes et multi-tables (ex: table.colonne \>  |
|          |     table2.colonne)                                       |
|          |                                                           |
|          | -   Extension aux fonctions d\'agrégat : SUM, COUNT, AVG, |
|          |     MAX, MIN                                              |
+----------+-----------------------------------------------------------+
| **J8**   | -   Développement du traducteur DSL → SQL pour les règles |
|          |     ligne-à-ligne                                         |
|          |                                                           |
|          | -   Tests de traduction sur des règles simples et des cas |
|          |     limites                                               |
+----------+-----------------------------------------------------------+
| **J9**   | -   Développement du traducteur DSL → SQL pour les règles |
|          |     agrégat/multi-tables (cas type stock/commandes)       |
|          |                                                           |
|          | -   Gestion des relations explicites entre tables écrites |
|          |     par l\'admin                                          |
+----------+-----------------------------------------------------------+
| **J10**  | -   Validation syntaxique immédiate à la saisie (retour   |
|          |     d\'erreur clair si règle invalide ou colonne          |
|          |     inexistante)                                          |
|          |                                                           |
|          | -   Tests de bout en bout : règle texte → SQL généré →    |
|          |     exécution sur la base de test                         |
+----------+-----------------------------------------------------------+

> **Objectif de fin de semaine :** une règle écrite en DSL
> (ligne-à-ligne ou agrégat) est correctement traduite en SQL, exécutée
> sur la base de test, et retourne les bonnes lignes en violation.

*Point de coordination important : dès la fin de cette semaine (ou en
tout début de semaine 3), le contrat d\'API (liste des endpoints,
formats de requêtes/réponses) doit être figé et transmis au collègue en
charge du frontend, même si l\'implémentation réelle n\'est pas encore
terminée. Il peut ainsi démarrer son intégration contre une API simulée
(mock) en attendant la semaine 4.*

**SEMAINE 3 --- Détection, alertes & persistance**

+----------+-----------------------------------------------------------+
| *        | **Tâches**                                                |
| *Jours** |                                                           |
+==========+===========================================================+
| **J11**  | -   Modélisation des entités : Règle (avec gravité),      |
|          |     Alerte (avec statut actif/résolu), Périmètre          |
|          |                                                           |
|          | -   Mise en place de la base de persistance interne de    |
|          |     l\'outil (stockage des règles et des alertes)         |
+----------+-----------------------------------------------------------+
| **J12**  | -   Développement du moteur d\'exécution séquentielle des |
|          |     règles (boucle sur toutes les règles actives)         |
|          |                                                           |
|          | -   Génération d\'une alerte groupée par règle en         |
|          |     violation (ID + colonnes concernées, sans valeurs     |
|          |     réelles)                                              |
+----------+-----------------------------------------------------------+
| **J13**  | -   Logique d\'auto-résolution : une alerte               |
|          |     disparaît/passe à « résolue » si l\'anomalie n\'est   |
|          |     plus détectée au cycle suivant                        |
|          |                                                           |
|          | -   Historique des alertes consultable                    |
+----------+-----------------------------------------------------------+
| **J14**  | -   Mise en place du planificateur (scheduler) de         |
|          |     fréquence globale : intervalle simple + support       |
|          |     d\'une expression cron, avec minimum 3 minutes imposé |
+----------+-----------------------------------------------------------+
| **J15**  | -   Tests de bout en bout sur plusieurs cycles            |
|          |     d\'exécution successifs (apparition, persistance,     |
|          |     résolution d\'une alerte)                             |
|          |                                                           |
|          | -   Tests de charge légers sur quelques milliers de       |
|          |     lignes (temps d\'exécution du cycle complet)          |
+----------+-----------------------------------------------------------+

> **Objectif de fin de semaine :** un cycle complet fonctionne seul : à
> intervalle régulier, les règles s\'exécutent, les anomalies génèrent
> des alertes groupées, et les alertes résolues disparaissent
> automatiquement.

**SEMAINE 4 --- API REST --- Endpoints & sécurité applicative**

+----------+-----------------------------------------------------------+
| *        | **Tâches**                                                |
| *Jours** |                                                           |
+==========+===========================================================+
| **J16**  | -   Mise en place de l\'authentification (POST            |
|          |     /auth/login, /auth/logout, /auth/me) avec token de    |
|          |     session                                               |
|          |                                                           |
|          | -   Gestion de session avec expiration après 15 minutes   |
|          |     d\'inactivité                                         |
+----------+-----------------------------------------------------------+
| **J17**  | -   Mise en place du blocage temporaire après 5           |
|          |     tentatives échouées                                   |
|          |                                                           |
|          | -   Endpoints de schéma et périmètre (GET /schema/tables, |
|          |     /schema/tables/{table}/columns, GET/PUT /scope)       |
+----------+-----------------------------------------------------------+
| **J18**  | -   Endpoints des règles (GET/POST/PUT/DELETE /rules)     |
|          |     avec pagination (limit=25 par défaut)                 |
|          |                                                           |
|          | -   Endpoint POST /rules/validate pour la validation en   |
|          |     direct sans sauvegarde                                |
+----------+-----------------------------------------------------------+
| **J19**  | -   Endpoints des alertes (GET /alerts avec filtre status |
|          |     et pagination, GET /alerts/{id})                      |
|          |                                                           |
|          | -   Endpoints de configuration de fréquence (GET/PUT      |
|          |     /config/frequency)                                    |
+----------+-----------------------------------------------------------+
| **J20**  | -   Standardisation du format d\'erreur JSON sur toutes   |
|          |     les routes                                            |
|          |                                                           |
|          | -   Documentation de l\'API (Swagger/OpenAPI) livrée au   |
|          |     collègue en charge du frontend                        |
+----------+-----------------------------------------------------------+

> **Objectif de fin de semaine :** l\'ensemble des endpoints REST est
> fonctionnel, documenté et transmis au collègue frontend, qui peut
> commencer à intégrer contre une API réelle (et non plus un mock).

**SEMAINE 5 --- Consolidation, tests globaux & finitions**

+----------+-----------------------------------------------------------+
| *        | **Tâches**                                                |
| *Jours** |                                                           |
+==========+===========================================================+
| **J21**  | -   Rejeu de scénarios de test complets : règles          |
|          |     ligne-à-ligne, règles agrégat multi-tables, cas du    |
|          |     stock/commandes                                       |
|          |                                                           |
|          | -   Correction des bugs identifiés                        |
+----------+-----------------------------------------------------------+
| **J22**  | -   Tests d\'API de bout en bout (Postman/tests           |
|          |     automatisés) : cas limites, erreurs, codes HTTP,      |
|          |     format des réponses                                   |
+----------+-----------------------------------------------------------+
| **J23**  | -   Revue de sécurité : vérification qu\'aucun            |
|          |     identifiant n\'est en dur, que le compte SQL est bien |
|          |     lecture seule, que les mots de passe sont bien hashés |
+----------+-----------------------------------------------------------+
| **J24**  | -   Rédaction d\'une documentation technique courte       |
|          |     (comment lancer le projet, variables d\'environnement |
|          |     nécessaires, structure du DSL avec exemples)          |
|          |                                                           |
|          | -   Nettoyage du code et des tests                        |
+----------+-----------------------------------------------------------+
| **J25**  | -   Démonstration complète de bout en bout via l\'API     |
|          |     (scénario réaliste avec plusieurs règles et anomalies |
|          |     volontaires)                                          |
|          |                                                           |
|          | -   Bilan et liste des pistes d\'amélioration (v2) :      |
|          |     sécurité avancée, performance, multi-admin            |
+----------+-----------------------------------------------------------+

> **Objectif de fin de semaine :** un backend stable, testé de bout en
> bout côté API, documenté, avec une démonstration complète
> reproductible --- prêt à être branché au frontend du collègue ou
> étendu en v2.

**Vue synthétique des 5 semaines**

  ----------------------------------------------------------------------------
  **Sem.**   **Thème**           **Livrable clé**
  ---------- ------------------- ---------------------------------------------
  **1**      **Connexion &       Connexion read-only fonctionnelle + détection
             introspection**     du schéma + périmètre défini

  **2**      **Moteur DSL →      Règles (ligne et agrégat) traduites et
             SQL**               exécutées correctement

  **3**      **Détection,        Cycle automatique complet : détection →
             alertes &           alerte → auto-résolution
             scheduler**         

  **4**      **API REST &        Endpoints complets, documentés
             sécurité            (Swagger/OpenAPI), transmis au collègue
             applicative**       frontend

  **5**      **Consolidation &   Backend stable, testé via l\'API, documenté,
             tests**             démonstrable de bout en bout
  ----------------------------------------------------------------------------

*Cette roadmap suppose un rythme de travail régulier sur 5 jours par
semaine ; elle peut être ajustée si des imprévus décalent certaines
tâches --- dans ce cas, prioriser les objectifs de fin de semaine 1 à 3
(le moteur), la semaine 4 (interface) pouvant être simplifiée si
besoin.*

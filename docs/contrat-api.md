**CONTRAT D\'API --- BACKEND ↔ FRONTEND**

# **Principe général**

-   Toute la logique SQL (traduction du DSL, exécution des requêtes sur
    la bd_bank) reste strictement interne au backend.

-   Le frontend ne communique avec le backend qu\'au travers de cette
    API REST, en JSON, jamais en SQL direct.

-   Toutes les routes (sauf /auth/login) nécessitent un token de session
    valide, transmis via l\'en-tête Authorization.

-   Format d\'erreur standardisé sur toutes les routes : un code HTTP
    cohérent (400, 401, 404, 409, 500...) accompagné d\'un corps JSON {
    \"error\": \"message explicite\" }.

-   Pagination : les listes (/rules, /alerts) acceptent les paramètres
    page et limit (limite par défaut : 25, modifiable).

## **1. Authentification**

L'endpoint d'initialisation `POST /auth/setup` est réservé au premier
démarrage. Il exige l'en-tête serveur `X-Setup-Key` correspondant à
`BDBANK_SETUP_KEY` et est refusé dès qu'un utilisateur existe. Il ne fait pas
partie du parcours de navigation normal.

  -----------------------------------------------------------------------------
  **Méthode**   **Endpoint**           **Description**
  ------------- ---------------------- ----------------------------------------
  **POST**      /auth/login            Connexion (identifiant + mot de passe) →
                                       retourne un token de session

  **POST**      /auth/logout           Déconnexion / invalidation du token

  **GET**       /auth/me               Vérifie si la session est valide (utile
                                       au chargement du frontend)
  -----------------------------------------------------------------------------

## **2. Schéma & périmètre**

  --------------------------------------------------------------------------------------
  **Méthode**   **Endpoint**                     **Description**
  ------------- -------------------------------- ---------------------------------------
  **GET**       /schema/tables                   Liste des tables détectées par
                                                 introspection de la bd_bank

  **GET**       /schema/tables/{table}/columns   Colonnes et types d\'une table donnée

  **GET**       /scope                           Périmètre actuel (tables sélectionnées
                                                 pour surveillance)

  **PUT**       /scope                           Modifie le périmètre (ajout/retrait de
                                                tables), avec validation du schéma réel
  --------------------------------------------------------------------------------------

## **3. Règles**

  -----------------------------------------------------------------------------
  **Méthode**   **Endpoint**           **Description**
  ------------- ---------------------- ----------------------------------------
  **GET**       /rules                 Liste des règles (paginée, limit=25 par
                                       défaut)

  **POST**      /rules                 Crée une règle (texte DSL + niveau de
                                       gravité)

  **GET**       /rules/{id}            Détail d\'une règle

  **PUT**       /rules/{id}            Modifie une règle

  **DELETE**    /rules/{id}            Supprime une règle

  **POST**      /rules/validate        Valide la syntaxe d\'une règle sans la
                                       sauvegarder (vérification en direct
                                       pendant la frappe)
  -----------------------------------------------------------------------------

## **4. Alertes**

  -----------------------------------------------------------------------------
  **Méthode**   **Endpoint**           **Description**
  ------------- ---------------------- ----------------------------------------
  **GET**       /alerts                Liste des alertes (paginée, filtre
                                       ?status=active\|resolved)

  **GET**       /alerts/{id}           Détail d\'une alerte (règle liée, date
                                       de détection, lignes concernées)
  -----------------------------------------------------------------------------

## **5. Configuration**

  -----------------------------------------------------------------------------
  **Méthode**   **Endpoint**           **Description**
  ------------- ---------------------- ----------------------------------------
  **GET**       /config/frequency      Fréquence d\'analyse actuelle

  **PUT**       /config/frequency      Modifie la fréquence (intervalle simple
                                       ou expression cron, minimum 3 minutes)
  -----------------------------------------------------------------------------

# **Notes importantes pour le frontend**

-   Les données retournées pour une ligne en anomalie contiennent
    uniquement son identifiant et les colonnes impliquées dans la règle
    --- jamais l\'intégralité de la ligne, ni de valeurs hors règle
    (confidentialité des données bancaires).

-   Une session expire après 15 minutes d\'inactivité : le frontend doit
    gérer une redirection propre vers l\'écran de connexion en cas de
    réponse 401.

-   Après 5 tentatives de connexion échouées, /auth/login retournera une
    erreur de blocage temporaire (à afficher clairement à
    l\'utilisateur, avec le temps d\'attente restant si disponible).

-   La documentation technique complète (schémas JSON détaillés de
    chaque requête/réponse) sera livrée via Swagger/OpenAPI en semaine 4
    du projet.

*Ce contrat peut évoluer légèrement d\'ici l\'implémentation complète
(semaine 4) ; toute modification sera communiquée avant intégration
finale.*

**FONCTIONNEMENT**

*Outil de vérification de conformité des données --- Bridge bd_bank*

# **1. Vue d\'ensemble du produit**

Le produit est un pont entre la base de données d\'une banque (bd_bank)
et son administrateur. Il permet de vérifier automatiquement que les
lignes de la base respectent des règles métier définies par
l\'administrateur, et de l\'alerter en cas d\'anomalie.

Le produit est conçu comme un outil « plug-in » : il doit pouvoir se
connecter à différentes bases de données relationnelles (compatibilité
MySQL et PostgreSQL prévue pour la v1), sans dépendre d\'une structure
de base spécifique.

# **2. Workflow global**

**Étape 1 --- Déclaration du périmètre**

> L\'administrateur sélectionne, parmi les tables détectées par la
> bd_bank (lecture seule), celles qui seront surveillées par l\'outil.

**Étape 2 --- Définition des règles**

> L\'administrateur écrit ses règles de conformité en texte (DSL), avec
> autocomplétion des tables/colonnes et validation syntaxique immédiate.
> Une règle peut porter sur une seule ligne ou sur un agrégat (somme,
> comptage\...) impliquant plusieurs tables.

**Étape 3 --- Interprétation des règles**

> Le moteur (développé en Java) traduit chaque règle du DSL en requête
> SQL équivalente.

**Étape 4 --- Exécution périodique**

> Selon la fréquence globale définie par l\'administrateur (intervalle
> simple ou expression cron, minimum 3 minutes), l\'outil se connecte à
> la bd_bank et exécute séquentiellement chaque requête SQL générée, en
> lecture seule, sur les données réelles et actuelles.

**Étape 5 --- Détection des anomalies**

> Chaque requête retourne directement les lignes qui violent la règle
> correspondante. L\'absence de résultat signifie une conformité totale
> pour cette règle.

**Étape 6 --- Génération de l\'alerte**

> Si des anomalies sont détectées, une alerte groupée est créée pour la
> règle concernée, avec un statut « active ».

**Étape 7 --- Notification & historique**

> L\'alerte apparaît immédiatement dans l\'espace admin et reste
> consultable dans l\'historique jusqu\'à ce que l\'anomalie ne soit
> plus détectée (auto-résolution au cycle suivant).

# **3. Schéma du flux de données**

Admin (règles + périmètre) → Moteur DSL→SQL (Java) → bd_bank (lecture
seule, MySQL/PostgreSQL) → Résultats (lignes en anomalie) → Génération
d\'alerte → Espace admin (notification + historique)

# **4. Informations supplémentaires**

  -----------------------------------------------------------------------
  **\#**   **Sujet**           **Décision**
  -------- ------------------- ------------------------------------------
  1        **Définition des    DSL texte avec autocomplétion et
           règles**            validation syntaxique immédiate. Règles
                               ligne-à-ligne et règles agrégat
                               multi-tables. Relations entre tables
                               écrites explicitement par l\'admin.
                               Fonctions : SUM, COUNT, AVG, MAX, MIN.

  2        **Tables/colonnes à Un seul administrateur utilise l\'outil.
           inspecter**         Il déclare au préalable le périmètre de
                               tables surveillées. Le schéma est
                               découvert par lecture de la bd_bank.

  3        **Interprétation    Traduction du DSL en requêtes SQL,
           des règles**        exécutées en lecture seule. Backend
                               développé en Java. v1 limitée au SQL

  4        **Connexion à la    Compatibilité multi-SGBD (MySQL et
           bd_bank**           PostgreSQL en v1). Outil et bd_bank sur le
                               même réseau. Accès à la demande (pas de
                               réplication locale des données).

  5        **Comparaison       Chaque requête SQL retourne directement
           règles / données**  les lignes en anomalie. Exécution
                               séquentielle des règles. Pour les
                               agrégats, l\'alerte pointe vers l\'entité
                               globale concernée, pas vers une ligne
                               précise.

  6        **Envoi de          Notifications uniquement dans l\'espace
           l\'alerte**         admin. Détection immédiate + persistance
                               en historique. Auto-résolution si
                               l\'anomalie disparaît au cycle suivant.
                               Une alerte groupée par règle (pas une par
                               ligne).

  7        **Contenu de        Nom de la règle, date/heure de détection,
           l\'alerte**         nombre de lignes concernées, ID + colonnes
                               impliquées (sans valeurs réelles, par
                               confidentialité). Niveau de gravité choisi
                               manuellement par l\'admin à la création de
                               la règle.

  8        **Fréquence         Fréquence globale (identique pour toutes
           d\'analyse**        les règles). Configurable en intervalle
                               simple ou en expression cron. Minimum
                               autorisé : 3 minutes.

  9        **Relecture des     Analyse complète de toutes les lignes à
           lignes**            chaque cycle, aussi bien pour les règles
                               ligne-à-ligne que pour les règles agrégat.
                               Pas d\'analyse incrémentale en v1 (volume
                               estimé : quelques milliers de lignes).
  -----------------------------------------------------------------------

# **5. Éléments techniques retenus**

-   Langage backend : Java

-   Bases de données cibles (v1) : MySQL, PostgreSQL

-   Accès à la bd_bank : lecture seule (read-only), à la demande

-   Moteur de règles : DSL propriétaire → traduction en SQL

-   Utilisateur du produit : un seul administrateur (pas de gestion
    multi-utilisateurs en v1)

# **6. Volet sécurité** 

Ce volet est aussi très important du schéma fonctionnel : il regroupe
les mesures de protection retenues pour la version 1 du projet, dans un
contexte de développement personnel en local (localhost + base de test).

  ------------------------------------------------------------------------
  **Sujet**            **Mesure retenue**
  -------------------- ---------------------------------------------------
  **Identifiants       Stockés en variables d\'environnement (fichier
  bd_bank**            .env), jamais en dur dans le code. Le .env est
                       exclu du dépôt via .gitignore dès le premier
                       commit.

  **Compte SQL dédié(  Compte technique distinct du compte admin de la
  por l**              base, avec droits limités à la lecture (SELECT) sur
                       les tables du périmètre surveillé. Aucun droit
                       d\'écriture ou de suppression, même en cas
                       d\'erreur de code.

  **Contexte de        Environnement local (localhost), avec une base de
  déploiement (v1)**   données de test personnelle. Pas de cloud, pas de
                       déploiement bancaire réel à ce stade.

  **Authentification   Un seul administrateur, identifiant + mot de passe.
  admin**              

  **Stockage du mot de Hashage avec bcrypt ou argon2 (jamais de mot de
  passe**              passe en clair, salage automatique inclus).

  **Gestion de         Déconnexion automatique après 15 minutes
  session**            d\'inactivité.

  **Protection         Blocage temporaire de la connexion après 5
  anti-bruteforce**    tentatives échouées (ex : 10 minutes de blocage).
  ------------------------------------------------------------------------

# **7. Points à approfondir**

-   Cycle de vie détaillé de l\'alerte (statuts, horodatage de
    résolution, historique/rapport exportable)

-   Optimisation des performances si le volume de données venait à
    augmenter significativement

-   Interface d\'autocomplétion du DSL (ergonomie de l\'éditeur de
    règles)

-   Sécurité avancée (chiffrement, authentification renforcée,
    déploiement réel) à revoir si le projet évolue vers un déploiement
    chez une banque partenaire

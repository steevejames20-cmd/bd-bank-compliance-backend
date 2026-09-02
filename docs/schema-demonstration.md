# Scénario de démonstration Bridge bd_bank

Ce document sert de fil conducteur pour présenter l'outil sans improviser.
La démonstration dure environ 10 à 15 minutes.

## 1. Préparer la démonstration

Ouvrir deux terminaux PowerShell.

### Terminal backend

```powershell
cd C:\Users\SJ20\Desktop\Projets\bd-bank-compliance-backend
docker compose up -d
.\scripts\load-env.ps1
mvn spring-boot:run
```

Vérifier que les logs indiquent :

```text
Connexion bd_bank OK -> MySQL
Introspection : 3 table(s) trouvée(s)
Périmètre surveillé : 3 table(s)
```

### Terminal frontend

```powershell
cd C:\Users\SJ20\Desktop\Projets\bd-bank-compliance-backend\Frontend
npm run dev
```

Ouvrir `http://127.0.0.1:5173`.

Le compte local de démonstration est celui créé pendant l'initialisation du
projet. Ne jamais afficher un mot de passe réel dans une présentation publique.

## 2. Fil conducteur à dire

> Bridge bd_bank contrôle une base bancaire en lecture seule. L'administrateur
> choisit les tables surveillées, écrit des règles métier lisibles et consulte
> les anomalies sans exposer les valeurs bancaires complètes.

## 3. Étape 1 : ouvrir une session

1. Afficher l'écran de connexion.
2. Entrer l'identifiant administrateur.
3. Entrer le mot de passe.
4. Cliquer sur **Ouvrir la session**.
5. Montrer la page d'accueil et les indicateurs.

À expliquer :

- Le frontend appelle `POST /auth/login`.
- Le token est envoyé ensuite dans l'en-tête Bearer.
- Une session inactive expire après 15 minutes.
- Cinq tentatives échouées déclenchent un verrouillage temporaire.

## 4. Étape 2 : examiner le schéma

1. Ouvrir **Schéma & périmètre**.
2. Montrer les tables détectées : `clients`, `comptes`, `transactions`.
3. Sélectionner `comptes`.
4. Montrer les colonnes `solde`, `client_id`, `iban`, `statut`.
5. Insister sur le fait que l'interface ne demande jamais de SQL à l'utilisateur.

À expliquer :

- Les tables viennent de `GET /schema/tables`.
- Les colonnes viennent de `GET /schema/tables/{table}/columns`.
- La connexion à la base bancaire utilise un compte SQL en lecture seule.

## 5. Étape 3 : vérifier le périmètre

1. Afficher les cases actives pour `clients`, `comptes` et `transactions`.
2. Désélectionner temporairement `transactions`.
3. Cliquer sur **Enregistrer le périmètre**.
4. Montrer le message de confirmation.
5. Réactiver `transactions` et enregistrer à nouveau.

À expliquer :

- Le frontend envoie un tableau de noms à `PUT /scope`.
- Le backend vérifie que chaque table existe réellement.
- Une table inconnue est refusée avec une erreur explicite.
- Le périmètre actif est conservé dans la base interne H2.

## 6. Étape 4 : créer une règle d'anomalie

1. Ouvrir **Règles DSL**.
2. Cliquer sur **Créer une règle**.
3. Saisir :

```text
solde < 0
```

4. Choisir la table `comptes`.
5. Choisir la gravité **Haute** ou **Critique**.
6. Valider la syntaxe.
7. Créer la règle.

À expliquer :

- La règle décrit directement l'anomalie recherchée.
- ANTLR analyse le texte.
- Le backend vérifie la table et les colonnes.
- Le traducteur produit une requête SQL paramétrée, sans concaténer de valeur
  utilisateur dans le SQL.

## 7. Étape 5 : déclencher une anomalie

Pour une démonstration contrôlée, utiliser une règle qui correspond aux données
seedées, par exemple `solde < 0` sur `comptes`.

1. Vérifier que la règle est active.
2. Ouvrir la configuration et vérifier une fréquence de `5m`.
3. Déclencher un cycle selon le mécanisme d'exécution disponible ou attendre le
   prochain cycle.
4. Revenir à la synthèse.
5. Montrer l'alerte active.

À expliquer :

- Le scheduler exécute les règles actives séquentiellement.
- Une alerte est groupée par règle et par entité en anomalie.
- L'alerte contient l'identifiant et les colonnes impliquées, jamais la ligne
  bancaire complète.

## 8. Étape 6 : consulter une alerte

1. Ouvrir **Alertes**.
2. Filtrer sur **Actives**.
3. Ouvrir une alerte, par exemple `ALR-...`.
4. Montrer : règle, statut, identifiant concerné, colonnes impliquées et nombre
   de détections consécutives.
5. Montrer le rappel de confidentialité.

Phrase utile :

> L'outil donne assez d'information pour agir, mais évite de recopier des
> données bancaires sensibles dans l'interface d'administration.

## 9. Étape 7 : corriger une anomalie et montrer l'auto-résolution

Dans l'environnement de test uniquement :

1. Identifier le compte en anomalie via son identifiant.
2. Corriger la donnée dans la base de test avec un compte de test autorisé, ou
   restaurer le seed prévu pour la démonstration.
3. Ne jamais faire cette modification avec `bdbank_readonly`.
4. Relancer un cycle.
5. Revenir dans **Alertes**.
6. Montrer que l'alerte passe de **Active** à **Résolue**.
7. Montrer la date de résolution et les détections précédentes.

À préciser :

- Le compte utilisé par l'application ne peut pas écrire dans `bd_bank`.
- La correction est donc une opération externe de préparation de test.
- L'outil constate la disparition de l'anomalie au cycle suivant et résout
  automatiquement l'alerte.

## 10. Étape 8 : modifier la fréquence

1. Ouvrir **Configuration**.
2. Afficher le mode **Intervalle simple**.
3. Choisir `5m`.
4. Enregistrer.
5. Montrer l'état du service et l'heure du prochain cycle.
6. Montrer ensuite le mode **Expression cron** sans forcément le sauvegarder :

```text
0 */5 * * * *
```

À expliquer :

- La fréquence est globale pour toutes les règles.
- L'intervalle minimum accepté est de 3 minutes.
- La configuration est envoyée à `PUT /config/frequency`.

## 11. Étape 9 : montrer les règles agrégées

Dans l'éditeur de règles, présenter sans nécessairement l'exécuter :

```text
SUM(transactions.montant) < 0 GROUP BY compte_id
```

Puis expliquer :

- Une règle agrégée raisonne sur un groupe, pas sur une ligne unique.
- Le résultat pointe vers la clé du groupe.
- Les fonctions disponibles incluent `SUM`, `COUNT`, `AVG`, `MAX` et `MIN`.
- Les relations entre tables sont explicites dans le DSL.

## 12. Parcours de secours si l'API n'est pas disponible

Pour une présentation visuelle sans backend :

1. Dans `Frontend/.env`, passer `VITE_DEMO_MODE=true`.
2. Redémarrer Vite.
3. Parcourir les vues avec les données de démonstration intégrées.
4. Dire clairement qu'il s'agit du mode démo et non d'une exécution sur MySQL.

## 13. Checklist avant présentation

- [ ] Docker est démarré et MySQL est `healthy`.
- [ ] `.env` backend est chargé dans le terminal Maven.
- [ ] Spring Boot écoute sur `8080`.
- [ ] Frontend écoute sur `5173`.
- [ ] Le compte administrateur existe.
- [ ] Le token et les mots de passe ne sont pas visibles dans les captures.
- [ ] Le périmètre contient les trois tables de démonstration.
- [ ] Au moins une règle active est disponible.
- [ ] La fréquence est configurée à `5m`.
- [ ] Une alerte active et une alerte résolue sont disponibles, ou le scénario de
      seed est prêt à les produire.
- [ ] Les logs backend sont masqués ou nettoyés avant projection.

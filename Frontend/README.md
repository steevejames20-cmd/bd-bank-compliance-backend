# Frontend Bridge Control

Cette application fournit l'interface d'administration de `bd_bank` : suivi
des alertes, gestion des règles DSL, choix des tables surveillées et réglage
de la fréquence d'analyse.

## Démarrer en local

```powershell
cd Frontend
npm install
npm run dev
```

Vite ouvre l'interface sur `http://127.0.0.1:5173`.

Le fichier `.env` utilise l'API Spring Boot sur `http://localhost:8080` avec
`VITE_DEMO_MODE=false`. Pour parcourir l'interface sans lancer le backend,
passer cette valeur à `true`.

## Connexion à l'API

Toutes les requêtes protégées reprennent le token retourné par `/auth/login`
dans l'en-tête `Authorization: Bearer ...`. Une réponse `401` efface la
session et renvoie vers l'écran de connexion.

Les listes utilisent `page` et `size`, comme les contrôleurs Spring Data du
backend.

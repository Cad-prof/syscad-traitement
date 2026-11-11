# ADR 001 — Modèle de données et états métier du prototype

- Date : 2026-11-11
- Statut : accepté

## Contexte

Le prototype SysCad doit séparer strictement les lots importés des données validées, tout en journalisant les décisions et les changements. La spécification impose une chaîne de traitement explicite placé sous les schémas `preparation`, `publie` et `journal`.

## Décision

1. La lifecycle de l'import est représentée par un enum métier, avec un état unique et des transitions contrôlées par le domaine.
2. Les données importées restent dans le schéma `preparation` jusqu'à la publication, ce qui évite toute exposition directe des lots non validés.
3. Les parcelles publiées sont stockées dans `publie.parcelle` avec une géométrie `MultiPolygon` et un CRS dédié `EPSG:32628`.
4. Les décisions de validation/rejet sont enregistrées comme des opérations journalisées, avec la raison de la décision.
5. Le prototype ajoute explicitement l'état `echec` pour couvrir les erreurs de traitement techniques sans ambiguïté.

## Conséquences

- L'implémentation reste compatible avec la phase 1 et les phases suivantes.
- Les règles de transition peuvent être testées unitairement dans le domaine sans dépendre du SGBD.
- L'ajout de nouvelles règles métier est centralisé dans le modèle de domaine.

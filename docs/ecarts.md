# Écarts et hypothèses

## Phase 1

- Le prototype adopte une stratégie de modélisation centrée sur le lot d'import et la parcelle validée.
- Les droits et l'API REST ne sont pas encore implémentés ; la phase 1 se limite au domaine, au schéma de données et au profil de démonstration.
- L'état `echec` est ajouté explicitement pour couvrir les erreurs techniques tout en conservant un état final explicite.

## Phase 2

- Le service de lecture sert uniquement les parcelles publiées et repose sur une liste en mémoire pour le prototype.
- Les filtres de rôle sont simplifiés à la validation du header `X-User-Role` et ne couvrent pas encore les cas d'OpenAPI ni de filtrage de champs complet.
- Les réponses de liste sont structurées comme un objet paginé minimal, conformément au contrat de prototype.

## Phase 3

- La détection de format est limitée à la signature ZIP et SQLite, sans validation complète du contenu métier de l'archive ou du GeoPackage.
- La sécurisation de l’archive vérifie les chemins absolus, les `..` et le nombre de fichiers, mais ne gère pas encore les limites de taille décompressée ni le ratio de compression complet.
- Les imports sont acceptés uniquement en réponse de service ; aucun stockage persistant des lots n’est encore branché sur la base PostgreSQL.

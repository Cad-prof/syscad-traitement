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

## Phase 4

- La validation et le rejet d’un import sont gérés au niveau du service applicatif, sans persistance de décision dans la base de données PostgreSQL.
- La publication est simulée via une transition de lifecycle métier et ne couvre pas encore la copie effective vers un schéma `publie` ou un historique de remplacement détaillé.
- Les journaux métier ne sont pas encore intégrés à un système de persistance, seulement à la logique de traitement.

## Phase 5

- L’export est actuellement un contrat minimal de métadonnées et de validation de format ; il ne produit pas encore de fichier binaire réel (GeoJSON, GPKG, SHP).
- La zone de filtrage est uniquement contrôlée par présence de paramètre et rôle ; aucune requête PostGIS ni filtrage d’emprise réel n’est encore branché.
- Les contrôles de volumétrie, de sensibilité et de journalisation downloadable ne sont pas encore implementés.

## Phase 6

- Le durcissement couvre la règle d’architecture et la documentation de démarrage, mais pas encore la totalité du contrôle de sécurité, de l’OpenAPI, ni des tests d’intégration PostGIS.
- Le README décrit le prototype et les points d’entrée, mais la procédure de déploiement complet n’est pas encore documentée pour tous les environnements.
- Les écarts restent bien identifiés et documentés pour éviter toute confusion de périmètre.

## Hypothèses de prototype restantes

- Les données sont factices, donc les sources, périmètres et références sont simplifiés.
- La gateway injecte les headers `X-User-Id` et `X-User-Role` ; le prototype valide uniquement leur présence et valeur minimale.
- L’API est fonctionnelle dans son périmètre de démonstration, sans prétendre au niveau d’un système de production complet.

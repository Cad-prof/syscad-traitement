# SysCad Traitement

Prototype Java de traitement et d’interopérabilité des données cadastrales.

## Prérequis

- Java 17
- Maven 3.9+
- Docker Desktop ou Docker Engine pour Postgres/PostGIS si vous lancez la base locale

## Démarrage rapide

1. Cloner le projet.
2. Vérifier l’environnement : `java -version`
3. Lancer la base locale : `docker compose up -d`
4. Démarrer le service : `mvn spring-boot:run`
5. Consulter la santé : `http://localhost:8080/actuator/health`

## Points d’entrée

- Recherche parcelles : `GET /api/v1/parcelles`
- Détail parcelle : `GET /api/v1/parcelles/{id}`
- Import : `POST /api/v1/imports`
- Décision d’import : `POST /api/v1/imports/{id}/validation`
- Export : `GET /api/v1/exports/parcelles?zone=...&format=geojson`

## Headers requis dans le prototype

- `X-User-Id`
- `X-User-Role`

## Profil de démonstration

Le projet contient un jeu de parcelles factices dans le profil `demo`.

## Notes

- Les données brutes restent séparées des données validées.
- Le prototype vise le flux import → préparation → validation → publication.
- Les écarts sont documentés dans [docs/ecarts.md](docs/ecarts.md).

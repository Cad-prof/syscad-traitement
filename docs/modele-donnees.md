# Modèle de données — phase 1

## Objet

Le service SysCad traite les parcelles en deux zones distinctes : la préparation, qui conserve les lots importés et leurs anomalies, et la publication, qui expose uniquement les données validées. L'historique est conservé dans le schéma `journal`.

## Schémas

### `preparation`

- `import_donnees` : identifiant UUID, source, périmètre, millésime, CRS source, format, état métier, compte d'entités, marqueur de publication, dates de création et de mise à jour.
- `anomalie_import` : anomalies bloquantes ou d'avertissement associées à un import.

### `publie`

- `parcelle` : parcelle validée avec identifiant, référence cadastrale, attribution géographique, statut, surfaces, géométrie PostGIS et provenance.
- `refererence_administrative` : références de titre foncier ou autres références administratives liées à une parcelle.

### `journal`

- `historique_operation` : journal des imports, validations, publications et changements de valeurs avec auteur et justification.

## États métier

Le lot suit le cycle suivant :

recu -> en_preparation -> normalise -> controle -> en_attente_validation -> valide -> publie -> archive

Un rejet court-circuite la séquence avec `rejete`, puis `archive`.
Une erreur technique termine le lot en `echec`.

## Décisions de conception

- Les sources injectées restent isolées du schéma publié.
- La géométrie est stockée en `geometry(MultiPolygon, 32628)` afin d'assurer un référentiel projeté cohérent pour les surfaces.
- Les identifiants utilisent `UUID` et les dates sont au format `timestamptz` en UTC.
- La référence cadastrale est devenue une clé d'unicité sur les parcelles actives.

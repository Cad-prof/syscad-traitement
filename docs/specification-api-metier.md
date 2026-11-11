# Spécification de l'API métier — Service de traitement des données cadastrales (prototype)

Projet SysCad · Version 0.1 · 11 novembre 2026 · Statut : proposition à valider

## 1. Objet et périmètre

Ce document spécifie l'API métier du **service de traitement des données et d'interopérabilité** décrit dans le dump du projet. Ce service gère les données cadastrales **en gros** : lots importés, contrôles, validation, publication, couches et exports. Le détail **par parcelle et par dossier** relève d'un autre microservice. Le service est construit autour de la bibliothèque **Apache SIS**.

C'est un **prototype alimenté par des données factices** : aucune donnée réelle, aucune connexion à une source officielle. Les endpoints sont ceux déjà définis dans le dump, sans ajout de route métier.

| Endpoint défini dans le dump | Dans ce service | Remarque |
| --- | --- | --- |
| `GET /api/v1/parcelles` | Oui | Recherche (données publiées) |
| `GET /api/v1/parcelles/{id}` | Oui | Fiche cadastrale validée |
| `GET /api/v1/parcelles/{id}/historique` | Oui | Historique des opérations sur les données |
| `POST /api/v1/imports` | Oui | ZIP contenant un shapefile, ou GeoPackage |
| `GET /api/v1/imports/{id}` | Oui | Suivi, anomalies, décisions |
| `POST /api/v1/imports/{id}/validation` | Oui | Validation ou rejet motivé |
| `GET /api/v1/couches/parcelles/...` | Oui | Couches validées (voir 4.7) |
| `GET /api/v1/exports/parcelles` | Oui | Téléchargements filtrés |
| `GET /api/v1/parcelles/{id}/dossier` | **Non** | Microservice des dossiers |
| `POST /api/v1/parcelles/{id}/demandes/extrait-plan` | **Non** | Microservice des dossiers |
| `POST /api/v1/parcelles/{id}/demandes/reattribution` | **Non** | Microservice des dossiers |

**Frontière entre les deux microservices.** Ce service est la source des données cadastrales validées (parcelle, géométrie, statut publié, provenance, historique d'import). L'autre service gère les dossiers et démarches d'une parcelle. La clé de liaison est l'identifiant de parcelle (`id`, UUID) accompagné de la `referenceCadastrale`.

## 2. Rôle d'Apache SIS et contraintes constatées

**Version visée :** Apache SIS 1.7 (publiée le 13 octobre 2026, Java 11 minimum, jeu EPSG 13.101). Le jeu EPSG est optionnel pour des raisons de licence mais recommandé ; il se fournit par le module `sis-embedded-data` (migré vers HSQLDB dans la 1.7) ou par une base PostgreSQL.

| Besoin du service | Apport d'Apache SIS |
| --- | --- |
| Lire le système de coordonnées d'un lot (fichier `.prj` en WKT, code EPSG d'un GeoPackage) | Module de référencement : création de CRS depuis un code ou du WKT |
| Reprojeter les géométries vers le CRS de stockage | Opérations de coordonnées entre deux CRS |
| Représenter les entités lues | Modèle `FeatureSet` / `Feature` |
| Filtrer les entités (emprise, attributs) | Filtres au sens ISO 19143 |
| Décrire lots et couches (source, millésime, emprise, CRS) | Métadonnées ISO 19115 |

**Constat à prendre en compte.** D'après les pages de documentation SIS consultées, les formats pris en charge comprennent notamment CSV, GeoTIFF, GPX, NetCDF, SQL et plusieurs formats raster ; **ni Shapefile, ni GeoPackage, ni GeoJSON n'y figurent**. Le module historique `sis-shapefile` n'a pas été republié depuis 2015. Le pont vers GDAL (`sis-gdal`) est présenté pour les rasters 2D et suppose GDAL installé ; son support des formats vectoriels n'est pas confirmé. À re-vérifier dans les notes de version 1.7 avant de figer.

**Conséquence de conception.** La lecture Shapefile/GeoPackage et l'écriture GeoJSON/GeoPackage/Shapefile sont des **adaptateurs** du service. Ils produisent ou consomment des `FeatureSet` SIS, et SIS assure ensuite CRS, reprojection, filtres et métadonnées. Trois options :

- **A** — bibliothèque tierce servant de codec (par exemple GeoTools ou JTS), derrière des interfaces `LecteurFormat` et `EcrivainFormat` ;
- **B** — `DataStoreProvider` SIS personnalisé, enregistré dans SIS ;
- **C** — `sis-gdal`, si le support vectoriel est confirmé.

**Recommandation pour le prototype : option A**, car les interfaces permettent de changer plus tard sans modifier l'API (décision D1).

## 3. Conventions générales

- **Base :** `/api/v1`. Format JSON UTF-8, sauf import (multipart) et export (fichier). Noms de champs en camelCase ASCII.
- **Identifiants :** UUID. Dates en ISO 8601 UTC.
- **Pagination :** `page` (à partir de 1, défaut 1) et `taille` (défaut 20, maximum 100). Enveloppe : `contenu`, `page`, `taille`, `total`.
- **Erreurs :** `application/problem+json` (RFC 9457) avec `type`, `title`, `status`, `detail`, `code`. Codes HTTP : 400 paramètre invalide, 401 non authentifié, 403 rôle insuffisant, 404 introuvable, 409 état incompatible, 413 fichier trop gros, 415 format non supporté, 422 contenu invalide, 500 erreur interne.
- **Traçabilité :** en-tête `X-Request-Id` repris dans les réponses et les journaux.
- **Identité (prototype) :** le service lit `X-User-Id` et `X-User-Role`, que l'API Gateway injectera après validation du jeton. Pour le prototype, ces en-têtes sont fournis à la main. Le service ne doit pas être joignable sans passer par la gateway en dehors du développement.
- **Rôles :** `ADMIN`, `AGENT_CONTROLE`, `AUTORITE_VALIDATION`, `UTILISATEUR_AUTORISE`. Le gestionnaire de dossiers n'a pas d'action dans ce service.
- **Documentation :** contrat OpenAPI 3.1 généré et versionné.

| Endpoint | ADMIN | AGENT\_CONTROLE | AUTORITE\_VALIDATION | UTILISATEUR\_AUTORISE |
| --- | --- | --- | --- | --- |
| Recherche, fiche, couches, exports | Oui | Oui | Oui | Oui (champs et formats limités) |
| Historique d'une parcelle | Oui | Oui | Oui | Non |
| Créer un import | Oui | Oui | Non | Non |
| Consulter un import | Oui | Oui | Oui | Non |
| Valider ou rejeter | Non | Non | Oui | Non |

## 4. Endpoints

### 4.1 Recherche — `GET /api/v1/parcelles`

| Élément | Spécification |
| --- | --- |
| Paramètres | `commune` (code ou nom exact), `statut`, `reference` (référence cadastrale, insensible à la casse), `page`, `taille` |
| Données servies | Uniquement les parcelles **publiées** |
| Réponse 200 | Enveloppe paginée de résumés : `id`, `referenceCadastrale`, `commune`, `statut`, `superficieCalculee`, `etatPublication` |
| Erreurs | 400 (statut inconnu, taille > 100), 401, 403 |
| Règle | Filtres combinés en ET. Aucun champ sensible dans les résumés. |

### 4.2 Fiche parcelle — `GET /api/v1/parcelles/{id}`

Retourne les informations cadastrales **validées** de la parcelle. Ce n'est pas le dossier : aucun propriétaire, aucune pièce, aucune démarche.

| Champ | UTILISATEUR\_AUTORISE | AGENT\_CONTROLE | AUTORITE / ADMIN |
| --- | --- | --- | --- |
| `id`, `referenceCadastrale`, `section`, `numero`, `commune`, `statut` | Oui | Oui | Oui |
| `geometrie` (GeoJSON, WGS 84) | Oui | Oui | Oui |
| `superficieDeclaree`, `superficieCalculee` | Oui | Oui | Oui |
| `ecartSurface` | Non | Oui | Oui |
| `provenance` (source, millésime, date d'import) | Source et millésime | Oui | Oui |
| `referencesAdministratives` (titre foncier si disponible) | Masqué | Oui | Oui |
| `limites` (ex. référence absente, mise à jour ancienne) | Oui | Oui | Oui |

Erreurs : 404 si la parcelle n'existe pas ou n'est pas publiée. Cette matrice est une hypothèse de prototype (décision D6).

### 4.3 Historique — `GET /api/v1/parcelles/{id}/historique`

| Élément | Spécification |
| --- | --- |
| Rôles | ADMIN, AGENT\_CONTROLE, AUTORITE\_VALIDATION |
| Paramètres | `page`, `taille` |
| Contenu | Opérations sur la parcelle, de la plus récente à la plus ancienne : import d'origine, contrôles, décision, publication, changement de valeur. Chaque entrée : `date`, `type`, `auteur`, `importId`, `justification`, `valeursAvant`, `valeursApres` (si pertinent). |
| Erreurs | 403 pour UTILISATEUR\_AUTORISE, 404 |

### 4.4 Création d'un import — `POST /api/v1/imports`

Requête `multipart/form-data` :

| Champ | Obligatoire | Description |
| --- | --- | --- |
| `fichier` | Oui | Archive `.zip` (shapefile) ou fichier `.gpkg` |
| `source` | Oui | Libellé de la source (texte, 200 caractères maximum) |
| `perimetre` | Oui | Commune ou zone couverte par le lot |
| `millesime` | Non | Année ou date d'effet des données |
| `crs` | Conditionnel | Code EPSG (ex. `EPSG:32628`). **Obligatoire** si le ZIP ne contient pas de `.prj`. |
| `couche` | Conditionnel | Nom de la couche GeoPackage. Obligatoire si le fichier contient plusieurs couches de type entités. |

**Format ZIP / shapefile.**

- Fichiers obligatoires : `.shp`, `.shx`, `.dbf`, de même nom de base. Recommandés : `.prj` (CRS) et `.cpg` (encodage ; UTF-8 par défaut s'il est absent). Les autres fichiers (`.sbn`, `.xml`…) sont ignorés.
- Un seul jeu de fichiers par archive ; sinon 422.
- Géométries admises : Polygon et MultiPolygon.
- Protections de l'archive : refus des chemins avec `..` ou absolus, plafond du nombre de fichiers, de la taille décompressée et du ratio de compression, liste blanche d'extensions.

**Format GeoPackage.** Fichier `.gpkg` reconnu à son contenu (en-tête SQLite et identifiant d'application GeoPackage), pas seulement à l'extension. Seules les couches d'entités sont lues ; le CRS vient de la table des systèmes de référence du fichier.

**Détection.** Le format est déterminé à partir du contenu (signature ZIP ou SQLite/GeoPackage). Une extension qui contredit le contenu donne un 415.

**Réponse 202 Accepted**, avec l'en-tête `Location: /api/v1/imports/{id}` :

```json
{
  "id": "0b7e5a1c-2f4d-4a6e-9c3b-5d8f1e2a7b90",
  "etat": "recu",
  "format": "SHAPEFILE_ZIP",
  "source": "Lot factice commune A",
  "perimetre": "Commune Test A",
  "creeLe": "2026-11-11T09:30:00Z"
}
```

| Erreurs | Cause |
| --- | --- |
| 400 | Champ obligatoire absent, `crs` inconnu |
| 413 | Fichier au-delà de la taille maximale (prototype : 100 Mo, configurable) |
| 415 | Format non supporté ou signature incohérente |
| 422 | Archive corrompue, fichiers shapefile manquants, `.prj` absent sans `crs`, couche GeoPackage ambiguë |

Le lot n'est **jamais publié directement** : il entre en zone de préparation.

### 4.5 Suivi d'un import — `GET /api/v1/imports/{id}`

| Élément | Spécification |
| --- | --- |
| Rôles | ADMIN, AGENT\_CONTROLE, AUTORITE\_VALIDATION |
| Réponse 200 | `id`, `etat`, `format`, `source`, `perimetre`, `millesime`, `crsSource`, `nbEntites`, `bilanControle`, `anomalies`, `decision`, `creeLe`, `misAJourLe` |
| `bilanControle` | `publiable` (booléen), nombre d'anomalies bloquantes et d'avertissements |
| `anomalies` | Liste paginable : `code`, `severite`, `entiteRef`, `message` |
| Erreurs | 403, 404 |

C'est l'endpoint d'examen de l'agent de contrôle et de l'autorité. Une correction passe par un **nouvel import**, car aucun endpoint d'édition n'est défini.

### 4.6 Décision — `POST /api/v1/imports/{id}/validation`

```json
{
  "decision": "REJETER",
  "motif": "Chevauchements anormaux sur le secteur nord"
}
```

| Élément | Spécification |
| --- | --- |
| Rôle | AUTORITE\_VALIDATION uniquement |
| Corps | `decision` : `VALIDER` ou `REJETER` ; `motif` obligatoire pour un rejet (10 caractères minimum), recommandé pour une validation |
| Précondition | Import à l'état `en_attente_validation`, sinon 409 |
| `VALIDER` | Refusé en 422 si `bilanControle.publiable` vaut faux. Sinon l'import passe à `valide`, puis la publication est lancée en tâche asynchrone (`publie`). |
| `REJETER` | L'import passe à `rejete`. Aucune donnée du lot ne rejoint les couches publiées. |
| Réponse 200 | L'import à jour avec l'objet `decision` : `decision`, `motif`, `auteur`, `date` |
| Journalisation | Toute décision crée une entrée d'historique. Elle n'a pas valeur de décision juridique sur la propriété. |

La décision porte sur le lot entier (pas de validation partielle dans le prototype).

### 4.7 Couches — `GET /api/v1/couches/parcelles/...`

Le dump laisse la fin du chemin ouverte. Proposition (décision D7) :

| Chemin | Rôle | Prototype |
| --- | --- | --- |
| `/api/v1/couches/parcelles` | Descripteur de la couche (nom, CRS, emprise, millésime, nombre d'entités, métadonnées ISO 19115 résumées) | Oui |
| `/api/v1/couches/parcelles/geojson` | FeatureCollection GeoJSON des parcelles publiées. Paramètres : `bbox` (minX,minY,maxX,maxY en WGS 84), `commune`, `page`, `taille` | Oui |
| `/api/v1/couches/parcelles/tuiles/{z}/{x}/{y}` | Tuiles vectorielles (PMTiles ou équivalent) | Réservé, non implémenté |

Règles : uniquement des données validées et publiées ; propriétés filtrées par rôle comme à la section 4.2 ; sortie en EPSG:4326 pour le web.

### 4.8 Exports — `GET /api/v1/exports/parcelles`

| Paramètre | Obligatoire | Description |
| --- | --- | --- |
| `zone` | Oui | Code de commune ou de zone du référentiel (décision D9) |
| `format` | Oui | `geojson`, `gpkg` ou `shp` (archive ZIP) |

- Réponse 200 : fichier avec `Content-Disposition`. Le fichier porte un manifeste (source, millésime, date d'export, CRS).
- Contrôle avant génération : rôle, périmètre, format et sensibilité des champs. Un refus renvoie 403 et est journalisé, tout comme un téléchargement autorisé.
- Volumétrie : génération synchrone jusqu'à un plafond configurable (prototype : 10 000 entités). Au-delà : 422 avec invitation à restreindre la zone. Les exports volumineux pré-générés sont une évolution.
- Erreurs : 400 (`format` inconnu), 403, 404 (zone sans donnée publiée), 422.

## 5. Cycle de vie d'un import

| État | Déclencheur | Suite |
| --- | --- | --- |
| `recu` | `POST /imports` accepté | `en_preparation` |
| `en_preparation` | Début du job : dézippage ou ouverture, lecture | `normalise` |
| `normalise` | CRS, champs et géométries harmonisés | `controle` |
| `controle` | Contrôles automatiques terminés | `en_attente_validation` |
| `en_attente_validation` | Résultats consultables | `valide` ou `rejete` |
| `valide` | Décision de l'autorité | `publie` |
| `rejete` | Décision de l'autorité | État final (conservé, historisé) |
| `publie` | Données copiées dans les couches publiées | `archive` |
| `archive` | Lot remplacé ou clos | État final |
| `echec` (proposé) | Erreur technique pendant un job | État final, nouvel import nécessaire |

L'état `echec` s'ajoute aux états du dump (décision D4).

## 6. Pipeline de traitement

1. **Réception :** validation des paramètres, détection du format, stockage du fichier source, création de l'`ImportDonnees` à `recu`.
2. **Préparation :** extraction sécurisée dans la zone de préparation, ouverture via l'adaptateur de format, obtention d'un `FeatureSet`.
3. **Normalisation (SIS) :** résolution du CRS source, reprojection vers le CRS de stockage, conversion en MultiPolygon, harmonisation des attributs, calcul de la superficie dans un CRS projeté métrique.
4. **Chargement :** écriture dans le schéma de préparation de PostgreSQL/PostGIS. Ce schéma n'est jamais exposé par l'API.
5. **Contrôles automatiques :** section 7, résultats enregistrés comme anomalies.
6. **Attente de décision :** import à `en_attente_validation`.
7. **Publication (après `VALIDER`) :** copie vers le schéma publié, historisation des changements, mise à jour de la couche, journalisation.

Pour le prototype, les jobs s'exécutent dans le processus du service (exécuteur asynchrone) ; une file de tâches externe pourra suivre (décision D10).

**Attributs sources attendus** (noms de 10 caractères maximum, pour rester compatibles avec le format DBF) :

| Attribut source | Champ cible | Obligatoire |
| --- | --- | --- |
| `REF_CAD` | `referenceCadastrale` | Oui |
| `SECTION` | `section` | Non |
| `NUMERO` | `numero` | Non |
| `COMMUNE` | `commune` | Oui |
| `SUPERF_DEC` | `superficieDeclaree` (m²) | Non |
| `STATUT` | `statut` | Non |
| `REF_TF` | référence administrative | Non |

Ce gabarit est un choix de prototype, à remplacer quand la source réelle sera connue (décision D3).

## 7. Contrôles automatiques

| Code | Règle | Sévérité |
| --- | --- | --- |
| `CRS_RECONNU` | Le CRS source est identifié et transformable | Bloquante |
| `CHAMPS_OBLIGATOIRES` | Référence cadastrale, commune et géométrie présentes | Bloquante |
| `UNICITE_LOT` | Pas de référence en double dans le lot | Bloquante |
| `GEOMETRIE_VALIDE` | Géométrie valide, de type Polygon ou MultiPolygon | Bloquante |
| `MISE_A_JOUR` | La référence existe déjà parmi les parcelles publiées (mise à jour, pas doublon) | Avertissement |
| `ECART_SURFACE` | Écart relatif entre surface déclarée et calculée au-delà du seuil (hypothèse : 5 %) | Avertissement |
| `CHEVAUCHEMENT` | Intersection anormale avec une autre parcelle active (hypothèse : plus de 1 % de la plus petite) | Avertissement |
| `PROVENANCE` | Source, date et millésime renseignés | Avertissement |

Les seuils sont configurables (décision D5). Le contrôle des documents n'est pas applicable : le prototype ne manipule pas de pièces.

## 8. Données factices

Un générateur produit un jeu de démonstration d'environ 300 parcelles réparties sur trois communes fictives (« Commune Test A, B, C »), dans une emprise plausible mais sans lien avec de vraies parcelles. Le champ `source` vaut `FACTICE`. **Aucune donnée cadastrale française n'est utilisée** (data.gouv.fr reste une inspiration d'architecture, jamais une source). Le jeu est chargé au démarrage par un profil `demo`, sans route supplémentaire.

| Fichier de test | Cas couvert | Résultat attendu |
| --- | --- | --- |
| `lot_valide.zip` | Shapefile complet (`.shp`, `.shx`, `.dbf`, `.prj`, `.cpg`) | Import jusqu'à `en_attente_validation`, publiable |
| `lot_sans_prj.zip` | Pas de `.prj` | 422 sans `crs` ; accepté avec `crs` |
| `lot_anomalies.zip` | Doublon, géométrie invalide, écart de surface, chevauchement | Non publiable (anomalies bloquantes) |
| `lot_valide.gpkg` | GeoPackage à une couche | Publiable |
| `lot_multicouches.gpkg` | Plusieurs couches | 422 sans `couche` |
| `archive_corrompue.zip` | ZIP illisible | 422 |
| `fausse_extension.gpkg` | Fichier texte renommé | 415 |
| `zip_slip.zip` | Chemins avec `..` | 422 |

## 9. Sécurité et exigences non fonctionnelles

- **Moindre privilège :** rôle vérifié à chaque endpoint, champs filtrés avant la réponse, y compris pour couches et exports.
- **Entrées :** validation côté serveur, taille maximale, contrôle du contenu réel des fichiers, protection contre ZIP bomb et zip-slip, nettoyage des fichiers temporaires.
- **Isolation :** schémas préparation et publié séparés ; l'API n'expose ni PostGIS, ni PostgREST, ni les tables de préparation.
- **Journalisation :** auteur, date, type, objet et justification pour imports, décisions, publications et exports.
- **Performance (indicatif) :** traitement asynchrone des imports, index spatiaux sur les géométries publiées, pagination obligatoire sur les listes.
- **Tests :** contrat OpenAPI vérifié, tests de bout en bout sur les fichiers de la section 8.

## 10. Intégration à l'API Gateway

Les deux microservices partagent le préfixe `/api/v1/parcelles`. La gateway doit donc router **par règle de priorité** :

| Priorité | Motif de chemin | Destination |
| --- | --- | --- |
| 1 | `/api/v1/parcelles/*/dossier**` et `/api/v1/parcelles/*/demandes/**` | Microservice des dossiers |
| 2 | `/api/v1/parcelles**`, `/api/v1/imports**`, `/api/v1/couches/**`, `/api/v1/exports/**` | Ce service |

- La gateway valide le jeton, puis injecte `X-User-Id`, `X-User-Role` et `X-Request-Id` ; le limitage de débit et le CORS se règlent à ce niveau.
- Chaque service publie son propre contrat OpenAPI, que la gateway peut agréger.
- Le préfixe `/api/v1` est conservé pour que les chemins restent identiques avant et après l'introduction de la gateway.

## 11. Décisions ouvertes

| N° | Décision |
| --- | --- |
| D1 | Codec Shapefile/GeoPackage/GeoJSON : option A, B ou C (section 2) |
| D2 | CRS de stockage et de sortie (hypothèse : EPSG:32628 pour les calculs, EPSG:4326 pour le GeoJSON) |
| D3 | Gabarit d'attributs de la source réelle et mécanisme de correspondance |
| D4 | États officiels d'un import, dont `echec` et le déclencheur d'`archive` |
| D5 | Seuils et sévérités des contrôles (écart de surface, chevauchement) |
| D6 | Politique de visibilité par rôle, dont une vue publique éventuelle |
| D7 | Fin du chemin de `/couches/parcelles/...` et tuiles |
| D8 | Import sur une parcelle déjà publiée : remplacement historisé ou rejet |
| D9 | Définition de `zone`, formats d'export autorisés par rôle, seuil synchrone |
| D10 | Framework backend et mécanisme de jobs du prototype |
| D11 | Contrat avec le microservice des dossiers (vérifier l'existence d'une parcelle) |
| D12 | Qui peut créer un import : ADMIN et AGENT\_CONTROLE (hypothèse retenue ici) |

## 12. Étapes suivantes

1. Valider les décisions D1 à D12 prioritaires (D1, D2, D6, D7).
2. Produire le contrat OpenAPI 3.1 des huit endpoints.
3. Générer le jeu de données factices et les fichiers de test.
4. Réaliser un squelette de service avec le pipeline d'import.
5. Réaliser la maquette de routage de la gateway.

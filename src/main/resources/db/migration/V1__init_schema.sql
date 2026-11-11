CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE SCHEMA IF NOT EXISTS preparation;
CREATE SCHEMA IF NOT EXISTS publie;
CREATE SCHEMA IF NOT EXISTS journal;

CREATE TABLE IF NOT EXISTS preparation.import_donnees (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source TEXT NOT NULL,
    perimetre TEXT NOT NULL,
    millesime TEXT,
    crs_source TEXT,
    format_import TEXT NOT NULL,
    etat TEXT NOT NULL CHECK (etat IN (
        'recu','en_preparation','normalise','controle','en_attente_validation','valide','rejete','publie','archive','echec'
    )),
    nb_entites INTEGER NOT NULL DEFAULT 0,
    publie BOOLEAN NOT NULL DEFAULT false,
    cree_le TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    mis_a_jour_le TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS preparation.anomalie_import (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    import_id UUID NOT NULL REFERENCES preparation.import_donnees(id) ON DELETE CASCADE,
    code TEXT NOT NULL,
    severite TEXT NOT NULL CHECK (severite IN ('bloquante','avertissement')),
    entite_ref TEXT,
    message TEXT NOT NULL,
    cree_le TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS publie.parcelle (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reference_cadastrale TEXT NOT NULL,
    section TEXT,
    numero TEXT,
    commune TEXT NOT NULL,
    statut TEXT NOT NULL DEFAULT 'actif',
    superficie_declaree NUMERIC(18,2),
    superficie_calculée NUMERIC(18,2),
    geom GEOMETRY(MultiPolygon, 32628) NOT NULL,
    source TEXT NOT NULL,
    millesime TEXT,
    import_id UUID,
    date_import TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    cree_le TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT parcelle_reference_unique UNIQUE (reference_cadastrale)
);

CREATE INDEX IF NOT EXISTS parcelle_geom_gix
    ON publie.parcelle USING GIST (geom);

CREATE TABLE IF NOT EXISTS journal.historique_operation (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entite_type TEXT NOT NULL,
    entite_id UUID NOT NULL,
    type_operation TEXT NOT NULL,
    auteur TEXT,
    import_id UUID,
    justification TEXT,
    valeurs_avant JSONB,
    valeurs_apres JSONB,
    cree_le TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS publie.refererence_administrative (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    parcelle_id UUID NOT NULL REFERENCES publie.parcelle(id) ON DELETE CASCADE,
    type_reference TEXT NOT NULL,
    valeur TEXT NOT NULL,
    cree_le TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

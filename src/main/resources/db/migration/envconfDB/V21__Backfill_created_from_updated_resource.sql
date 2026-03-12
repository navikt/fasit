-- resource_table_aud is missing the created/updated columns that were added to resource_table in V1_4.
-- Hibernate Envers reconstructs the entity from this audit table, so created is always null when
-- fetching a historic revision, causing NPE in ToPayloadTransformer.
ALTER TABLE resource_table_aud ADD
(
    created   TIMESTAMP(6),
    updated   TIMESTAMP(6),
    updatedby VARCHAR2(255)
);

UPDATE resource_table_aud aud
SET (created, updated) = (
    SELECT COALESCE(rt.created, TIMESTAMP '2013-02-01 13:37:00'),
           COALESCE(rt.updated, TIMESTAMP '2013-02-01 13:37:00')
    FROM resource_table rt
    WHERE rt.entid = aud.entid
)

WHERE aud.created IS NULL;

UPDATE resource_table_aud
SET created = TIMESTAMP '2013-02-01 13:37:00',
    updated = TIMESTAMP '2013-02-01 13:37:00'
WHERE created IS NULL;

-- application_aud
ALTER TABLE application_aud ADD
(
    created   TIMESTAMP(6),
    updated   TIMESTAMP(6),
    updatedby VARCHAR2(255)
);

UPDATE application_aud aud
SET (created, updated) = (
    SELECT COALESCE(a.created, TIMESTAMP '2013-02-01 13:37:00'),
           COALESCE(a.updated, TIMESTAMP '2013-02-01 13:37:00')
    FROM application a
    WHERE a.entid = aud.entid
)
WHERE aud.created IS NULL;

UPDATE application_aud
SET created = TIMESTAMP '2013-02-01 13:37:00',
    updated = TIMESTAMP '2013-02-01 13:37:00'
WHERE created IS NULL;

-- clusters_aud
ALTER TABLE clusters_aud ADD
(
    created   TIMESTAMP(6),
    updated   TIMESTAMP(6),
    updatedby VARCHAR2(255)
);

UPDATE clusters_aud aud
SET (created, updated) = (
    SELECT COALESCE(c.created, TIMESTAMP '2013-02-01 13:37:00'),
           COALESCE(c.updated, TIMESTAMP '2013-02-01 13:37:00')
    FROM clusters c
    WHERE c.entid = aud.entid
)
WHERE aud.created IS NULL;

UPDATE clusters_aud
SET created = TIMESTAMP '2013-02-01 13:37:00',
    updated = TIMESTAMP '2013-02-01 13:37:00'
WHERE created IS NULL;

-- node_aud
ALTER TABLE node_aud ADD
(
    created   TIMESTAMP(6),
    updated   TIMESTAMP(6),
    updatedby VARCHAR2(255)
);

UPDATE node_aud aud
SET (created, updated) = (
    SELECT COALESCE(n.created, TIMESTAMP '2013-02-01 13:37:00'),
           COALESCE(n.updated, TIMESTAMP '2013-02-01 13:37:00')
    FROM node n
    WHERE n.entid = aud.entid
)
WHERE aud.created IS NULL;

UPDATE node_aud
SET created = TIMESTAMP '2013-02-01 13:37:00',
    updated = TIMESTAMP '2013-02-01 13:37:00'
WHERE created IS NULL;

-- environment_aud
ALTER TABLE environment_aud ADD
(
    created   TIMESTAMP(6),
    updated   TIMESTAMP(6),
    updatedby VARCHAR2(255)
);

UPDATE environment_aud aud
SET (created, updated) = (
    SELECT COALESCE(e.created, TIMESTAMP '2013-02-01 13:37:00'),
           COALESCE(e.updated, TIMESTAMP '2013-02-01 13:37:00')
    FROM environment e
    WHERE e.entid = aud.entid
)
WHERE aud.created IS NULL;

UPDATE environment_aud
SET created = TIMESTAMP '2013-02-01 13:37:00',
    updated = TIMESTAMP '2013-02-01 13:37:00'
WHERE created IS NULL;

-- resource_table_aud is missing the created/updated columns that were added to resource_table in V1_4.
-- Hibernate Envers reconstructs the entity from this audit table, so created is always null when
-- fetching a historic revision, causing NPE in ToPayloadTransformer.

ALTER TABLE resource_table_aud ADD
(
    created   TIMESTAMP(6),
    updated   TIMESTAMP(6),
    updatedby VARCHAR2(255)
);

-- Backfill from the live table where possible, fall back to fixed timestamp when both are NULL
UPDATE resource_table_aud aud
SET (created, updated) = (
    SELECT COALESCE(rt.created, TIMESTAMP '2013-02-01 13:37:00'),
           COALESCE(rt.updated, TIMESTAMP '2013-02-01 13:37:00')
    FROM resource_table rt
    WHERE rt.entid = aud.entid
)
WHERE aud.created IS NULL;
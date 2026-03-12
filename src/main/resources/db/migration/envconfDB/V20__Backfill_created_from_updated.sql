-- Backfill CREATED and UPDATED columns:
--   1. Use UPDATED when CREATED is NULL but UPDATED is not NULL
--   2. Fall back to 2013-02-01 13:37:00 for both CREATED and UPDATED when both are NULL

UPDATE APPLICATION
SET created = COALESCE(updated, TIMESTAMP '2013-02-01 13:37:00'),
    updated = COALESCE(updated, TIMESTAMP '2013-02-01 13:37:00')
WHERE created IS NULL;

UPDATE ENVIRONMENT
SET created = COALESCE(updated, TIMESTAMP '2013-02-01 13:37:00'),
    updated = COALESCE(updated, TIMESTAMP '2013-02-01 13:37:00')
WHERE created IS NULL;
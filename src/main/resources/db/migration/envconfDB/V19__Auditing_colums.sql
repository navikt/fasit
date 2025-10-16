/*
Add env_id column to aud tables that need it because of bidirectional relationships
*/
ALTER TABLE node_aud ADD (env_id VARCHAR2(255));
UPDATE node_aud na
SET na.env_id = (
    SELECT n.env_id
    FROM node n
    WHERE n.entid = na.entid
)
WHERE EXISTS (
    SELECT 1
    FROM node n
    WHERE n.entid = na.entid
);

ALTER TABLE clusters_aud ADD (env_id VARCHAR2(255));
UPDATE clusters_aud ca
SET ca.env_id = (
    SELECT c.env_id
    FROM clusters c
    WHERE c.entid = ca.entid
)
WHERE EXISTS (
    SELECT 1
    FROM clusters c
    WHERE c.entid = ca.entid
);

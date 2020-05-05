-- add new column "application"
ALTER TABLE resource_table ADD application_entid NUMBER(19);
ALTER TABLE resource_table_aud ADD application_entid NUMBER(19);

-- move data from one-to-many table to new column
UPDATE resource_table rt
SET rt.application_entid = (SELECT rta.applications_entid FROM resource_table_application rta
WHERE rta.resource_table_entid = rt.entid);

-- delete one-to-many table
DROP TABLE resource_table_application;
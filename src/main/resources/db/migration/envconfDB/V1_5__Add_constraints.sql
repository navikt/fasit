ALTER TABLE application MODIFY(app_name NOT NULL);
UPDATE application SET dataorigin='EnvConfig' WHERE dataorigin IS NULL;
ALTER TABLE application MODIFY(dataorigin NOT NULL); 
ALTER TABLE application MODIFY(dataorigin DEFAULT 'EnvConfig');

ALTER TABLE applicationinstance MODIFY(application_entid NOT NULL);
DELETE FROM applicationinstance WHERE cluster_entid IS NULL;
ALTER TABLE applicationinstance MODIFY(cluster_entid NOT NULL);
ALTER TABLE applicationinstance ADD CONSTRAINT app_cluster_uix UNIQUE (application_entid, cluster_entid);
UPDATE applicationinstance SET dataorigin='EnvConfig' WHERE dataorigin IS NULL;
ALTER TABLE applicationinstance MODIFY(dataorigin NOT NULL); 
ALTER TABLE applicationinstance MODIFY(dataorigin DEFAULT 'EnvConfig');

UPDATE clusters SET clustertype='jboss' WHERE clustertype IS NULL;
ALTER TABLE clusters MODIFY(clustertype DEFAULT 'jboss');
ALTER TABLE clusters MODIFY(cluster_name NOT NULL);
UPDATE clusters SET dataorigin='EnvConfig' WHERE dataorigin IS NULL;
ALTER TABLE clusters MODIFY(dataorigin NOT NULL); 
ALTER TABLE clusters MODIFY(dataorigin DEFAULT 'EnvConfig');

ALTER TABLE environmentpart MODIFY(env_name NOT NULL);
ALTER TABLE environmentpart MODIFY(envclass NOT NULL);
ALTER TABLE environmentpart MODIFY(env_domain NOT NULL);
UPDATE environmentpart SET dataorigin='EnvConfig' WHERE dataorigin IS NULL;
ALTER TABLE environmentpart MODIFY(dataorigin NOT NULL); 
ALTER TABLE environmentpart MODIFY(dataorigin DEFAULT 'EnvConfig');

UPDATE fileentity SET dataorigin='EnvConfig' WHERE dataorigin IS NULL;
ALTER TABLE fileentity MODIFY(dataorigin NOT NULL); 
ALTER TABLE fileentity MODIFY(dataorigin DEFAULT 'EnvConfig');

ALTER TABLE login MODIFY(cred_type NOT NULL);
UPDATE login SET dataorigin='EnvConfig' WHERE dataorigin IS NULL;
ALTER TABLE login MODIFY(dataorigin NOT NULL); 
ALTER TABLE login MODIFY(dataorigin DEFAULT 'EnvConfig');

UPDATE node SET dataorigin='EnvConfig' WHERE dataorigin IS NULL;
ALTER TABLE node MODIFY(dataorigin NOT NULL); 
ALTER TABLE node MODIFY(dataorigin DEFAULT 'EnvConfig');

ALTER TABLE portfolio MODIFY(portfolio_name NOT NULL);
UPDATE portfolio SET dataorigin='EnvConfig' WHERE dataorigin IS NULL;
ALTER TABLE portfolio MODIFY(dataorigin NOT NULL); 
ALTER TABLE portfolio MODIFY(dataorigin DEFAULT 'EnvConfig');

ALTER TABLE resource_table MODIFY(resource_type NOT NULL);
ALTER TABLE resource_table MODIFY(resource_alias NOT NULL);
UPDATE resource_table SET dataorigin='EnvConfig' WHERE dataorigin IS NULL;
ALTER TABLE resource_table MODIFY(dataorigin NOT NULL); 
ALTER TABLE resource_table MODIFY(dataorigin DEFAULT 'EnvConfig');

ALTER TABLE resource_properties MODIFY(property_value NOT NULL);

ALTER TABLE resource_secrets MODIFY(iv NOT NULL);
ALTER TABLE resource_secrets MODIFY(keyid NOT NULL);
ALTER TABLE resource_secrets MODIFY(secret NOT NULL);
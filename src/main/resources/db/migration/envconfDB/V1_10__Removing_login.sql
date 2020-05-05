ALTER TABLE resource_secrets DROP PRIMARY KEY DROP INDEX;

ALTER TABLE resource_secrets ADD CONSTRAINT pk_res_secret_id_key PRIMARY KEY (propertyresource_entid, secret_key);

CREATE TABLE secret (
	entid NUMBER(19) NOT NULL
	, iv BLOB
	, keyid BLOB
	, content BLOB
	, created TIMESTAMP (6)
	, createdby  VARCHAR2(255 CHAR)
	, dataorigin VARCHAR2(255 CHAR) DEFAULT 'EnvConfig' NOT NULL
	, updated TIMESTAMP (6)
	, updatedby VARCHAR2(255 CHAR)
	, tmplogin_entid NUMBER(19)
	, tmpresource_endid NUMBER(19)
	, tmpresourcekey VARCHAR2(255 CHAR)
);

-- Refactor nodes

ALTER TABLE node ADD (
	username VARCHAR2(255)
	, password_entid NUMBER(19)
);

INSERT INTO secret (entid, iv, keyid, content, created, createdby, dataorigin, updated, updatedby, tmplogin_entid)
SELECT hibernate_sequence.nextval, login.iv, login.keyid, login.secret, login.created, login.createdby, login.dataorigin, login.updated, login.updatedby, login.entid 
FROM node 
JOIN login ON node.deployuser_entid = login.entid; 

UPDATE node
SET password_entid = (SELECT entid FROM secret WHERE secret.tmplogin_entid = node.deployuser_entid)
WHERE EXISTS (SELECT entid FROM secret WHERE secret.tmplogin_entid = node.deployuser_entid);

UPDATE node 
SET username = (SELECT username FROM login WHERE login.entid = node.deployuser_entid)
WHERE EXISTS (SELECT username FROM login WHERE login.entid = node.deployuser_entid);

-- Refactor property resource secrets

INSERT INTO secret (entid, iv, keyid, content, created, createdby, updated, updatedby, tmplogin_entid, tmpresource_endid, tmpresourcekey)
SELECT hibernate_sequence.nextval, iv, keyid, secret, null, null, null, null, null, propertyresource_entid, secret_key
FROM resource_secrets;

ALTER TABLE resource_secrets ADD (
	secrets_entid NUMBER(19)
);

CREATE UNIQUE INDEX RES_SECRETS_ID_UIX ON resource_secrets(SECRETS_ENTID);

UPDATE resource_secrets
SET secrets_entid = (SELECT secret.entid FROM secret WHERE secret.tmpresource_endid = resource_secrets.propertyresource_entid and secret.tmpresourcekey = resource_secrets.secret_key)
WHERE EXISTS (SELECT secret.entid FROM secret WHERE secret.tmpresource_endid = resource_secrets.propertyresource_entid and secret.tmpresourcekey = resource_secrets.secret_key);

ALTER TABLE resource_secrets RENAME COLUMN propertyresource_entid TO resource_table_entid;



-- Refactor property resource datasource logins

ALTER TABLE resource_secrets MODIFY (
	iv NULL
	, keyid NULL
	, secret NULL
);

ALTER TABLE resource_properties MODIFY (
	property_value NULL
	);

INSERT INTO secret (entid, iv, keyid, content, created, createdby, dataorigin, updated, updatedby, tmplogin_entid, tmpresource_endid)
SELECT hibernate_sequence.nextval, login.iv, login.keyid, login.secret, login.created, login.createdby, login.dataorigin, login.updated, login.updatedby, login.entid , resource_logins.resource_table_entid
FROM resource_logins 
JOIN login ON resource_logins.logins_entid = login.entid
WHERE resource_logins.login_key = 'schemauser'; 

INSERT INTO resource_secrets (resource_table_entid, secret_key, secrets_entid)
SELECT resource_logins.resource_table_entid, 'password', secret.entid
FROM resource_logins 
JOIN login ON resource_logins.logins_entid = login.entid
JOIN secret ON resource_logins.resource_table_entid = secret.tmpresource_endid
WHERE resource_logins.login_key = 'schemauser'; 

INSERT INTO resource_properties (propertyresource_entid, property_value, property_key)
SELECT resource_logins.resource_table_entid, login.username, 'username' 
FROM resource_logins 
JOIN login ON resource_logins.logins_entid = login.entid
WHERE resource_logins.login_key = 'schemauser'; 

-- Refactor property resource ldap logins

INSERT INTO secret (entid, iv, keyid, content, created, createdby, dataorigin, updated, updatedby, tmplogin_entid, tmpresource_endid)
SELECT hibernate_sequence.nextval, login.iv, login.keyid, login.secret, login.created, login.createdby, login.dataorigin, login.updated, login.updatedby, login.entid, resource_logins.resource_table_entid 
FROM resource_logins 
JOIN login ON resource_logins.logins_entid = login.entid
WHERE resource_logins.login_key = 'bind'; 

INSERT INTO resource_secrets (resource_table_entid, secret_key, secrets_entid)
SELECT resource_logins.resource_table_entid, 'password', secret.entid
FROM resource_logins 
JOIN login ON resource_logins.logins_entid = login.entid
JOIN secret ON resource_logins.resource_table_entid = secret.tmpresource_endid
WHERE resource_logins.login_key = 'bind'; 

INSERT INTO resource_properties (propertyresource_entid, property_value, property_key)
SELECT resource_logins.resource_table_entid, login.username, 'username' 
FROM resource_logins 
JOIN login ON resource_logins.logins_entid = login.entid
WHERE resource_logins.login_key = 'bind'; 

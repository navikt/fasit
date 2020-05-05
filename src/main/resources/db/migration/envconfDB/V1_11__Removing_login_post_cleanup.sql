ALTER TABLE node MODIFY (
	password_entid NOT NULL
);

ALTER TABLE resource_secrets MODIFY (
	secrets_entid NOT NULL
);

ALTER TABLE node DROP COLUMN deployuser_entid;

ALTER TABLE resource_secrets DROP COLUMN iv;
ALTER TABLE resource_secrets DROP COLUMN keyid;
ALTER TABLE resource_secrets DROP COLUMN secret;

ALTER TABLE secret DROP COLUMN tmplogin_entid;
ALTER TABLE secret DROP COLUMN tmpresource_endid;
ALTER TABLE secret DROP COLUMN tmpresourcekey;

DROP TABLE resource_logins;
DROP TABLE login_application;
DROP TABLE login;

CREATE TABLE applicationcertificate
(
appl_user VARCHAR2(40) NOT NULL, 
appl_password BLOB,
cert_data BLOB NOT NULL, 
cert_name VARCHAR2(80), 
cert_private_key BLOB NOT NULL, 
cert_public_key BLOB NOT NULL,
cert_passphrase BLOB,
cert_expiry DATE,
application_entid NUMBER(19),
CONSTRAINT pk_ac_appl_user PRIMARY KEY (appl_user),
CONSTRAINT fk_appcert_app_entid FOREIGN KEY (application_entid) REFERENCES application(entid) ENABLE
);
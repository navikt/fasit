DROP TABLE applicationcertificate;
DROP TABLE certificate;

CREATE TABLE resource_secrets
(
	propertyresource_entid NUMBER(19) NOT NULL, 
	IV BLOB,
	KEYID BLOB,
	SECRET BLOB,
	secret_key VARCHAR2(255) NOT NULL, 
	CONSTRAINT pk_res_secret_id_key PRIMARY KEY (propertyresource_entid, secret_key),
	CONSTRAINT fk_res_secret_res_table_entid FOREIGN KEY (propertyresource_entid) REFERENCES resource_table(entid)
);

CREATE TABLE FILEENTITY
  (
    ENTID NUMBER(19,0) NOT NULL ENABLE,
    FILEDATA BLOB,
    FILE_NAME VARCHAR2(255 CHAR),
   CONSTRAINT pk_fileentity_entid PRIMARY KEY (entid)
  );
  
  CREATE TABLE RESOURCE_FILES
  (
    RESOURCE_TABLE_ENTID NUMBER(19,0) NOT NULL ENABLE,
    FILEENTITIES_ENTID   NUMBER(19,0) NOT NULL ENABLE,
    FILE_KEY             VARCHAR2(255 CHAR) NOT NULL ENABLE,
  CONSTRAINT pk_res_files_id_key PRIMARY KEY (resource_table_entid, file_key),
	CONSTRAINT fk_res_files_res_table_entid FOREIGN KEY (resource_table_entid) REFERENCES resource_table(entid) 
  );
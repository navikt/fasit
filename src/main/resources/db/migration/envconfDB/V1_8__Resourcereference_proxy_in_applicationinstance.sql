CREATE TABLE app_instance_res_refs 
(	
entid NUMBER(19) NOT NULL, 
alias VARCHAR2(255), 
applicationinstance_entid NUMBER(19),
resource_entid NUMBER(19),
created TIMESTAMP (6),
createdby  VARCHAR2(255 CHAR),
dataorigin VARCHAR2(255 CHAR) DEFAULT 'EnvConfig' NOT NULL,
updated TIMESTAMP (6),
updatedby VARCHAR2(255 CHAR),
CONSTRAINT pk_airr_entid PRIMARY KEY (entid),
CONSTRAINT fk_airr_res_entid FOREIGN KEY (resource_entid) REFERENCES resource_table (entid) ON DELETE SET NULL,
CONSTRAINT fk_airr_appinst_entid FOREIGN KEY (applicationinstance_entid) REFERENCES applicationinstance (entid)
);

INSERT INTO app_instance_res_refs (entid, alias, applicationinstance_entid, resource_entid)
SELECT hibernate_sequence.nextval, resource_alias, applicationinstance_entid, resources_entid
FROM application_instance_resources JOIN resource_table ON resource_table.entid = resources_entid;

DROP TABLE application_instance_resources;

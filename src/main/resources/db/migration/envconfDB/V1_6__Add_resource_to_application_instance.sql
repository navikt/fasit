CREATE TABLE application_instance_resources
(
	applicationinstance_entid NUMBER(19) NOT NULL,
	resources_entid NUMBER(19) NOT NULL,
	CONSTRAINT pk_air_ids PRIMARY KEY (applicationinstance_entid, resources_entid),
	CONSTRAINT fk_air_app_inst_entid FOREIGN KEY (applicationinstance_entid) REFERENCES applicationinstance (entid), 
	CONSTRAINT fk_air_res_entid FOREIGN KEY (resources_entid) REFERENCES resource_table (entid)
);

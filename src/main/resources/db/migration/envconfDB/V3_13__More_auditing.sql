/*
DROP INDEX ix_app_inst_res_refs_aud;
DROP INDEX ix_appinst_aud;
DROP INDEX ix_clusters_aud;
DROP INDEX ix_node_aud;
DROP INDEX ix_environment_aud;
DROP TABLE app_inst_res_refs_aud;
DROP TABLE appinst_resref_aud;
DROP TABLE appinst_expservices_aud;
DROP TABLE applicationinstance_aud;
DROP TABLE clusters_aud;
DROP TABLE cluster_appinst_aud;
DROP TABLE node_aud;
DROP TABLE clusters_node_aud;
DROP TABLE secret_aud;
DROP TABLE environment_aud;
DROP TABLE environment_cluster_aud;
DROP TABLE environment_node_aud;
*/

CREATE TABLE app_inst_res_refs_aud (
    entid           NUMBER(19) NOT NULL,
    rev             NUMBER(19) NOT NULL,
    revtype         NUMBER(19),
    alias			VARCHAR2(255),
    applicationinstance_entid NUMBER(19),
    resource_entid 	NUMBER(19),
    resource_type 	VARCHAR2(255),
    future			NUMBER(1, 0)
);

CREATE UNIQUE INDEX ix_app_inst_res_refs_aud ON app_inst_res_refs_aud(entid, rev);


CREATE TABLE appinst_resref_aud (
    entid           NUMBER(19) NOT NULL,
    rev             NUMBER(19) NOT NULL,
    revtype         NUMBER(19),
    applicationinstance_entid NUMBER(19)
);


CREATE TABLE appinst_expservices_aud (
    rev             NUMBER(19) NOT NULL,
    revtype         NUMBER(19),
    resources_entid NUMBER(19),
    applicationinstance_entid NUMBER(19)
);


CREATE TABLE applicationinstance_aud (
    entid           NUMBER(19) NOT NULL,
    rev             NUMBER(19) NOT NULL,
    revtype         NUMBER(19),
    application_entid NUMBER(19),
    cluster_entid NUMBER(19),
    version 		VARCHAR2(50),
    selftestpagepath VARCHAR2(255)
);

CREATE UNIQUE INDEX ix_appinst_aud ON applicationinstance_aud(entid, rev);


CREATE TABLE clusters_aud (
	clustertype		VARCHAR2(31),
    entid           NUMBER(19) NOT NULL,
    rev             NUMBER(19) NOT NULL,
    revtype         NUMBER(19),
	cluster_name	VARCHAR2(255),
	loadbalancerurl	VARCHAR2(255),
	domain			VARCHAR2(255)
);

CREATE UNIQUE INDEX ix_clusters_aud ON clusters_aud(entid, rev);


CREATE TABLE cluster_appinst_aud (
    entid           NUMBER(19) NOT NULL,
    rev           	NUMBER(19) NOT NULL,
    revtype         NUMBER(19),
    cluster_entid 	NUMBER(19)
);


CREATE TABLE node_aud (
    entid           NUMBER(19) NOT NULL,
    rev             NUMBER(19) NOT NULL,
    revtype         NUMBER(19),
	adminurl		VARCHAR2(255),
	hostname		VARCHAR2(255),
	username		VARCHAR2(255),
	password_entid	NUMBER(19)
); 

CREATE UNIQUE INDEX ix_node_aud ON node_aud(entid, rev);


CREATE TABLE clusters_node_aud (
    rev             NUMBER(19) NOT NULL,
    revtype         NUMBER(19),
    nodes_entid		NUMBER(19),
    clusters_entid 	NUMBER(19)
);

CREATE TABLE secret_aud (
    entid           NUMBER(19) NOT NULL,
    rev             NUMBER(19) NOT NULL,
    revtype         NUMBER(19),
	iv				BLOB,
	keyid			BLOB,
	content			BLOB
);

CREATE UNIQUE INDEX ix_secret_aud ON secret_aud(entid, rev);

	
CREATE TABLE environment_aud (
    entid           NUMBER(19) NOT NULL,
    rev             NUMBER(19) NOT NULL,
    revtype         NUMBER(19),
    envclass		VARCHAR(255),
    name			VARCHAR(255)
);

CREATE UNIQUE INDEX ix_environment_aud ON environment_aud(entid, rev);


CREATE TABLE environment_cluster_aud (
    entid           NUMBER(19) NOT NULL,
    rev             NUMBER(19) NOT NULL,
    revtype         NUMBER(19),
	env_id			NUMBER(19)
);

CREATE TABLE environment_node_aud (
    entid           NUMBER(19) NOT NULL,
    rev             NUMBER(19) NOT NULL,
    revtype         NUMBER(19),
	env_id			NUMBER(19)
);


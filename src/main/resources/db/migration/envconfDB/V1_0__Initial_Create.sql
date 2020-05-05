CREATE TABLE portfolio 
(
entid NUMBER(19) NOT NULL, 
portfolio_name VARCHAR2(255), 		
CONSTRAINT pk_portfolio_entid PRIMARY KEY (entid)
);

CREATE UNIQUE INDEX porfolio_p_name_uix ON portfolio(portfolio_name);

CREATE TABLE application 
(	
entid NUMBER(19) NOT NULL, 
artifactid VARCHAR2(255), 
groupid VARCHAR2(255), 
app_name VARCHAR2(255), 
portfolio_entid NUMBER(19),
CONSTRAINT pk_app_entid PRIMARY KEY (entid),
CONSTRAINT fk_app_portfolio_entid FOREIGN KEY (portfolio_entid) REFERENCES portfolio(entid)
);

CREATE UNIQUE INDEX application_app_name_uix ON application(app_name);

CREATE TABLE environmentpart 
(
entid NUMBER(19) NOT NULL, 
env_domain VARCHAR2(255), 
envclass VARCHAR2(255), 
env_name VARCHAR2(255), 
CONSTRAINT pk_envpart_entid PRIMARY KEY (entid)
);

CREATE TABLE login
(
entid NUMBER(19) NOT NULL, 
description VARCHAR2(255), 
iv BLOB, 
keyid BLOB, 
secret BLOB, 
env_domain VARCHAR2(255), 
envclass VARCHAR2(255), 
environmentname	VARCHAR2(255), 
cred_type VARCHAR2(255), 
username VARCHAR2(255),
CONSTRAINT pk_login_entid PRIMARY KEY (entid)
);

CREATE TABLE node
(
entid NUMBER(19) NOT NULL, 
adminurl VARCHAR2(255), 
cpucount NUMBER(10) NOT NULL, 
hostname VARCHAR2(255), 
ip VARCHAR2(255), 
memorymb NUMBER(10) NOT NULL, 
deployuser_entid NUMBER(19), 
env_id	NUMBER(19), 
CONSTRAINT pk_node_entid PRIMARY KEY (entid),
CONSTRAINT fk_node_env_id FOREIGN KEY (env_id) REFERENCES environmentpart(entid), 
CONSTRAINT fk_node_login_entid FOREIGN KEY (deployuser_entid) REFERENCES login(entid)
);
	  
CREATE UNIQUE INDEX node_hostname_uix ON node(hostname);

CREATE TABLE clusters
(
clustertype VARCHAR2(31) NOT NULL, 
entid NUMBER(19) NOT NULL, 
cluster_name VARCHAR2(255), 
env_id NUMBER(19), 
CONSTRAINT pk_clust_entid PRIMARY KEY (entid),
CONSTRAINT fk_clust_envpart_entid FOREIGN KEY (env_id) REFERENCES environmentpart(entid)
);

CREATE TABLE clusters_node
(
clusters_entid NUMBER(19) NOT NULL, 
nodes_entid NUMBER(19) NOT NULL, 
CONSTRAINT pk_clust_node_ids PRIMARY KEY (clusters_entid, nodes_entid),
CONSTRAINT fk_clust_node_node_entid FOREIGN KEY (nodes_entid) REFERENCES node (entid), 
CONSTRAINT fk_clust_node_clusters_entid FOREIGN KEY (clusters_entid) REFERENCES clusters(entid)
);

CREATE TABLE applicationinstance
(
entid NUMBER(19) NOT NULL, 
application_entid NUMBER(19), 
cluster_entid NUMBER(19), 
CONSTRAINT pk_appinst_entid PRIMARY KEY (entid),
CONSTRAINT fk_appinst_app_entid FOREIGN KEY (application_entid) REFERENCES application (entid), 
CONSTRAINT fk_appinst_clusters_entid FOREIGN KEY (cluster_entid) REFERENCES clusters (entid)
);

CREATE TABLE certificate 
(
entid NUMBER(19) NOT NULL, 
certificatedata BLOB, 
iv BLOB, 
keyid BLOB, 
secret BLOB, 
cert_name VARCHAR2(255), 
privatekey BLOB, 
CONSTRAINT pk_certificate_entid PRIMARY KEY (entid)
);

CREATE TABLE resource_table
(
resourcetype VARCHAR2(31) NOT NULL, 
entid NUMBER(19) NOT NULL, 
resource_alias VARCHAR2(255),
env_domain VARCHAR2(255), 
envclass VARCHAR2(255), 
environmentname VARCHAR2(255), 
resource_type VARCHAR2(255), 
linkedto_entid	NUMBER(19), 
CONSTRAINT pk_res_table_entid PRIMARY KEY (entid)
);

CREATE INDEX res_table_res_alias_ix ON resource_table(resource_alias);

CREATE TABLE resource_properties
(
propertyresource_entid NUMBER(19) NOT NULL, 
property_value VARCHAR2(255), 
property_key VARCHAR2(255) NOT NULL, 
CONSTRAINT pk_res_prop_id_key PRIMARY KEY (propertyresource_entid, property_key),
CONSTRAINT fk_res_prop_res_table_entid FOREIGN KEY (propertyresource_entid) REFERENCES resource_table(entid)
);

CREATE TABLE resource_logins
(
resource_table_entid NUMBER(19) NOT NULL, 
logins_entid NUMBER(19) NOT NULL, 
login_key VARCHAR2(255) NOT NULL, 
CONSTRAINT pk_res_logins_id_key PRIMARY KEY (resource_table_entid, login_key),
CONSTRAINT fk_res_logins_res_table_entid FOREIGN KEY (resource_table_entid) REFERENCES resource_table(entid), 
CONSTRAINT fk_res_logins_login_entid FOREIGN KEY (logins_entid) REFERENCES login(entid)
);

CREATE SEQUENCE hibernate_sequence START WITH 1 INCREMENT BY 1;
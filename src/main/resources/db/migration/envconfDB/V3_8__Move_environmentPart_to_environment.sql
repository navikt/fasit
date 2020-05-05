-----------------------------------------------------------------
-- Non destructive refactoring environmentpart --> environment
------------------------------------------------------------------


-- Add new table Environment
CREATE TABLE environment
(
	entid NUMBER(19), 
	envclass VARCHAR2(255) , 
	name VARCHAR2(255) unique,
	CREATED TIMESTAMP (6),
    CREATEDBY  VARCHAR2(255 CHAR),
    DATAORIGIN VARCHAR2(255 CHAR),
    UPDATED TIMESTAMP (6),
    UPDATEDBY VARCHAR2(255 CHAR)
);

-- Update node
ALTER TABLE node ADD env_id2 NUMBER(19);
alter table node drop column ip;

-- Update clusters
ALTER TABLE clusters ADD env_id2 NUMBER(19);
alter table clusters add domain VARCHAR2(255) ;

--recreate Environments
insert into environment ( name)  
  select distinct env_name from environmentpart; 

UPDATE Environment e
SET
    envclass =(select unique (envclass) from ENVIRONMENTPART ep where ep.env_name = e.name ),
    entid =(hibernate_sequence.nextval );

-- Update nodes reference
update Node n
SET
  Env_id2=(select e.entid from environment e , ENVIRONMENTPART ep  where e.name=ep.env_name AND ep.entid=n.env_id);
  
  -- Update cluster reference
update Clusters c
SET
  Env_id2=(select e.entid from environment e , ENVIRONMENTPART ep  where e.name=ep.env_name AND ep.entid=c.env_id),
  domain=(select ep.env_domain from ENVIRONMENTPART ep  where ep.entid=c.env_id)
  ;
  
 -- Create constraints
  alter table environment MODIFY(
  	entid NOT NULL,
  	name not null,
  	envclass not null
  );
  alter table environment ADD CONSTRAINT pk_environment PRIMARY KEY (entid);
  
  -- clean up
  alter table node drop constraint fk_node_env_id;
  alter table node rename column env_id to env_id_old;
  alter table node rename column env_id2 to env_id;
  alter table node ADD CONSTRAINT fk_node_env_id  FOREIGN KEY (env_id) REFERENCES environment(entid);

  alter table clusters modify (domain not null);
  alter table clusters drop	constraint fk_clust_envpart_entid;
  alter table clusters rename column env_id to env_id_old;
  alter table clusters rename column env_id2 to env_id;
  alter table clusters ADD CONSTRAINT fk_cluster_env_id  FOREIGN KEY (env_id) REFERENCES environment(entid)
 

  
  	
  
  
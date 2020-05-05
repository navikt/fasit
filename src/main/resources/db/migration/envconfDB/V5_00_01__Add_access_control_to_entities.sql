-- Embedding access control to entities because of hibnernate envvers fuckup with relations
-- Ressurs
alter table resource_table ADD (
   access_envclass VARCHAR(255),
   access_groups VARCHAR(4000)
);

alter table resource_table_aud ADD (
   access_envclass VARCHAR(255),
   access_groups VARCHAR(4000)
);

update resource_table set (access_envclass) = envclass; 
update resource_table_aud set (access_envclass) = envclass; 


--nodes
alter table node ADD (
   access_envclass VARCHAR(255),
   access_groups VARCHAR(4000)
);

alter table node_aud ADD (
   access_envclass VARCHAR(255),
   access_groups VARCHAR(4000)
);
update node n set (access_envclass) = (select  e.envClass from environment e  where n.env_id = e.entid);
update node_aud na set (access_envclass) = (select  n.access_envclass from node n  where na.entid = n.entid); 

-- application
alter table application ADD (
   access_envclass VARCHAR(255),
   access_groups VARCHAR(4000)
);

alter table application_aud ADD (
   access_envclass VARCHAR(255),
   access_groups VARCHAR(4000)
);

update application a set (access_envclass) = 't';
update application_aud a set (access_envclass) = 't'; 

-- application_group
alter table application_group ADD (
   access_envclass VARCHAR(255),
   access_groups VARCHAR(4000)
);

alter table application_group_aud ADD (
   access_envclass VARCHAR(255),
   access_groups VARCHAR(4000)
);

update application_group set (access_envclass) = 't';
update application_group_aud  set (access_envclass) = 't';


-- environment
alter table environment ADD (
   access_envclass VARCHAR(255),
   access_groups VARCHAR(4000)
);

alter table environment_aud ADD (
   access_envclass VARCHAR(255),
   access_groups VARCHAR(4000)
);

update environment set (access_envclass) = envclass;
update environment_aud set (access_envclass) = envclass;

-- clusters
alter table clusters ADD (
   access_envclass VARCHAR(255),
   access_groups VARCHAR(4000)
);

alter table clusters_aud ADD (
   access_envclass VARCHAR(255),
   access_groups VARCHAR(4000)
);
update clusters c set (access_envclass) = (select  e.envClass from environment e  where c.env_id = e.entid);
update clusters_aud ca set (access_envclass) = (select  c.access_envclass from clusters c  where ca.entid = c.entid); 

-- secret (Fra noder og ressurser)
alter table secret ADD (
   access_envclass VARCHAR(255),
   access_groups VARCHAR(4000)
);

alter table secret_aud ADD (
   access_envclass VARCHAR(255),
   access_groups VARCHAR(4000)
);
update secret s set (s.access_envclass) = (select n.access_envclass from node n where n.password_entid = s.entid) where exists(select n.access_envclass from node n where n.password_entid = s.entid ) ;
update secret s set (access_envclass) = (select r.access_envclass from resource_table r, resource_secrets rs where r.entid=rs.resource_table_entid AND rs.secrets_entid= s.entid ) where s.access_envclass is null;

update secret_aud sa set (access_envclass) = (select  s.access_envclass from secret s  where sa.entid = s.entid); 

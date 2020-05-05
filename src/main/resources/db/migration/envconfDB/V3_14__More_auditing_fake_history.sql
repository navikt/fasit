/*
DELETE FROM app_inst_res_refs_aud where rev between 6 and 10;
DELETE FROM appinst_resref_aud where rev between 6 and 10;
DELETE FROM appinst_expservices_aud where rev between 6 and 10;
DELETE FROM applicationinstance_aud where rev between 6 and 10;
DELETE FROM clusters_aud where rev between 6 and 10;
DELETE FROM cluster_appinst_aud where rev between 6 and 10;
DELETE FROM node_aud where rev between 6 and 10;
DELETE FROM clusters_node_aud where rev between 6 and 10;
DELETE FROM secret_aud where rev between 6 and 10;
DELETE FROM environment_aud where rev between 6 and 10;
DELETE FROM environment_cluster_aud where rev between 6 and 10;
DELETE FROM environment_node_aud where rev between 6 and 10;

DELETE FROM additionalrevisioninfo where revision between 6 and 10;
*/


insert into app_inst_res_refs_aud
(entid, rev, revtype, alias, applicationinstance_entid, resource_entid, resource_type, future)
select e.entid, 10, 0, e.alias, e.applicationinstance_entid, e.resource_entid, e.resource_type, e.future
from app_instance_res_refs e
where not exists(select o.entid from app_inst_res_refs_aud o where o.entid = e.entid and o.revtype = 0);


insert into appinst_resref_aud
(entid, rev, revtype, applicationinstance_entid)
select e.entid, 10, 0, e.applicationinstance_entid
from app_instance_res_refs e
where not exists(select o.entid from appinst_resref_aud o where o.entid = e.entid and o.revtype = 0);


insert into appinst_expservices_aud
(rev, revtype, resources_entid, applicationinstance_entid)
select 10, 0, e.resources_entid, e.applicationinstance_entid
from appinstance_exposedservices e
where not exists(select o.resources_entid from appinst_expservices_aud o where o.resources_entid = e.resources_entid and o.applicationinstance_entid = e.applicationinstance_entid and o.revtype = 0);


insert into applicationinstance_aud
(entid, rev, revtype, application_entid, cluster_entid, version, selftestpagepath)
select e.entid, 10, 0, e.application_entid, e.cluster_entid, e.version, e.selftestpagepath
from applicationinstance e
where not exists(select o.entid from applicationinstance_aud o where o.entid = e.entid and o.revtype = 0);

insert into additionalrevisioninfo
(revision, author, dataorigin, message, modifiedentitytype, timestamp)
values
(10, 'Script', 'Script', 'Initial revision', 'no.nav.aura.envconfig.model.infrastructure.ApplicationInstance', to_timestamp('2012-12-01 00:00:00', 'YYYY-MM-DD HH24:MI:SS'));


insert into clusters_aud
(clustertype, entid, rev, revtype, cluster_name, loadbalancerurl, domain)
select e.clustertype, e.entid, 9, 0, e.cluster_name, e.loadbalancerurl, e.domain
from clusters e
where not exists(select o.entid from clusters_aud o where o.entid = e.entid and o.revtype = 0);

insert into additionalrevisioninfo
(revision, author, dataorigin, message, modifiedentitytype, timestamp)
values
(9, 'Script', 'Script', 'Initial revision', 'no.nav.aura.envconfig.model.infrastructure.JbossCluster', to_timestamp('2012-12-01 00:00:00', 'YYYY-MM-DD HH24:MI:SS'));


insert into cluster_appinst_aud
(entid, rev, revtype, cluster_entid)
select e.entid, 10, 0, e.cluster_entid
from applicationinstance e
where not exists(select o.entid from applicationinstance_aud o where o.entid = e.entid and o.cluster_entid = e.cluster_entid and o.revtype = 0);


insert into node_aud
(entid, rev, revtype, adminurl, hostname, username, password_entid)
select e.entid, 8, 0, e.adminurl, e.hostname, e.username, e.password_entid
from node e
where not exists(select o.entid from node_aud o where o.entid = e.entid and o.revtype = 0);

insert into additionalrevisioninfo
(revision, author, dataorigin, message, modifiedentitytype, timestamp)
values
(8, 'Script', 'Script', 'Initial revision', 'no.nav.aura.envconfig.model.infrastructure.Node', to_timestamp('2012-12-01 00:00:00', 'YYYY-MM-DD HH24:MI:SS'));


insert into clusters_node_aud
(rev, revtype, nodes_entid, clusters_entid)
select 9, 0, e.nodes_entid, e.clusters_entid
from clusters_node e
where not exists(select o.clusters_entid from clusters_node_aud o where o.nodes_entid = e.nodes_entid and o.clusters_entid = e.clusters_entid and o.revtype = 0);


insert into secret_aud
(entid, rev, revtype, iv, keyid, content)
select e.entid, 6, 0, e.iv, e.keyid, e.content
from secret e
where not exists(select o.entid from secret_aud o where o.entid = e.entid and o.revtype = 0);


insert into environment_aud
(entid, rev, revtype, envclass, name)
select e.entid, 7, 0, e.envclass, e.name
from environment e
where not exists(select o.entid from environment_aud o where o.entid = e.entid and o.revtype = 0);

insert into additionalrevisioninfo
(revision, author, dataorigin, message, modifiedentitytype, timestamp)
values
(7, 'Script', 'Script', 'Initial revision', 'no.nav.aura.envconfig.model.infrastructure.Environment', to_timestamp('2012-12-01 00:00:00', 'YYYY-MM-DD HH24:MI:SS'));


insert into environment_cluster_aud
(entid, rev, revtype, env_id)
select e.entid, 9, 0, e.env_id
from clusters e
where not exists(select o.entid from environment_cluster_aud o where o.entid = e.entid and o.env_id = e.env_id and o.revtype = 0);


insert into environment_node_aud
(entid, rev, revtype, env_id)
select e.entid, 8, 0, e.env_id
from node e
where not exists(select o.entid from environment_node_aud o where o.entid = e.entid and o.env_id = e.env_id and o.revtype = 0);

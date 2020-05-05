alter table clusters drop column clustertype;

alter table clusters_aud drop column clustertype;

update additionalrevisioninfo
set modifiedentitytype = 'no.nav.aura.envconfig.model.infrastructure.Cluster'
where modifiedentitytype = 'no.nav.aura.envconfig.model.infrastructure.JbossCluster';

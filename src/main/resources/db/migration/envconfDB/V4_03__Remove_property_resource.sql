alter table resource_table drop column resourcetype;
alter table resource_table_aud drop column resourcetype;

alter table resource_properties rename column propertyresource_entid to resource_entid; 
alter table resource_properties_aud rename column propertyresource_entid to resource_entid; 

update additionalrevisioninfo
set modifiedentitytype = 'no.nav.aura.envconfig.model.resource.PropertyResource'
where modifiedentitytype = 'no.nav.aura.envconfig.model.resource.Resource';

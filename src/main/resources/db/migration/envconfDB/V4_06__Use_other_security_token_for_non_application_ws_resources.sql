update resource_properties rp
set rp.property_value = 'OTHER'
where rp.property_key = 'securityToken'
and exists (select * from resource_table rt where rp.resource_entid = rt.entid and rt.dataorigin != 'Application' and rt.resource_type = 'WebserviceEndpoint'); 

update resource_properties_aud rp
set rp.property_value = 'OTHER'
where rp.property_key = 'securityToken'
and exists (select * from resource_table rt where rp.resource_entid = rt.entid and rt.dataorigin != 'Application' and rt.resource_type = 'WebserviceEndpoint'); 

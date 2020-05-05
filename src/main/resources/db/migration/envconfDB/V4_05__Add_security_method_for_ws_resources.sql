insert into resource_properties
(resource_entid, property_key, property_value)
select rt.entid, 'securityToken', 'SAML'
from resource_table rt
where rt.resource_type = 'WebserviceEndpoint'
and not exists(select rp.* from resource_properties rp where rt.entid = rp.resource_entid and rp.property_key = 'securityToken');

insert into resource_properties_aud
(rev, resource_entid, property_key, property_value, revtype)
select min(rt.rev), rt.entid, 'securityToken', 'SAML', 0
from resource_table_aud rt
where rt.resource_type = 'WebserviceEndpoint'
and not exists(select rp.* from resource_properties_aud rp where rt.entid = rp.resource_entid and rp.property_key = 'securityToken')
group by rt.entid;

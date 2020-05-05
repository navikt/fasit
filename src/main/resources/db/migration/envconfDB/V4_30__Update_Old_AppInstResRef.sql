
delete from app_instance_res_refs ap
where ap.resource_type is null
and ap.resource_entid is null;


update app_instance_res_refs a
set a.resource_type = (select r.resource_type
                      from resource_table r
                      where a.resource_entid = r.entid)
where a.resource_type is null;

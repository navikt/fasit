-- Create fake history since every entity needs a creation entry or we will get EntityNotFoundException on references 
-- from an audited to a non-audited one.   

-- These reverts the fake history
--alter table additionalrevisioninfo rename column timestamp to timestamp_new;
--alter table additionalrevisioninfo rename column timestamp_old to timestamp;
--alter table additionalrevisioninfo modify(timestamp not null);
--alter table additionalrevisioninfo drop column timestamp_new;
--delete from application_aud where rev <= 5;
--delete from additionalrevisioninfo where revision <= 5;
--delete from resource_properties_aud where rev <= 5;
--delete from resource_table_aud where rev <= 5;
--alter table additionalrevisioninfo modify(timestamp_old not null);

alter table additionalrevisioninfo add (timestamp_new timestamp (6));
update additionalrevisioninfo 
set timestamp_new = to_timestamp('1970-01-01 00:00:00', 'YYYY-MM-DD HH24:MI:SS') + numtodsinterval(timestamp/1000,'second');
alter table additionalrevisioninfo rename column timestamp to timestamp_old;
alter table additionalrevisioninfo modify(timestamp_old null);
alter table additionalrevisioninfo rename column timestamp_new to timestamp;
alter table additionalrevisioninfo modify(timestamp not null);

insert into application_aud
(entid, rev, revtype, artifactid, groupid, app_name)
select a.entid, 3, 0, a.artifactid, a.groupid, a.app_name
from application a
where not exists(select aa.entid from application_aud aa where aa.entid = a.entid and aa.revtype = 0);

insert into resource_table_aud
(resourcetype, entid, rev, revtype, resource_alias, env_domain, envclass, environmentname, resource_type, application_entid)
select rt.resourcetype, rt.entid, 5, 0, rt.resource_alias, rt.env_domain, rt.envclass, rt.environmentname, rt.resource_type, application_entid
from resource_table rt
where not exists(select 1 from resource_table_aud rta where rta.entid = rt.entid and rta.revtype = 0);

insert into resource_properties_aud
(rev, propertyresource_entid, property_key, property_value, revtype)
select 4, rp.propertyresource_entid, rp.property_key, rp.property_value, 0
from resource_properties rp
where not exists(select 1 from  resource_properties_aud rpa where rpa.propertyresource_entid =  rp.propertyresource_entid and rpa.revtype = 0);

insert into additionalrevisioninfo
(revision, author, dataorigin, message, modifiedentitytype, timestamp)
values
(3, 'Script', 'Script', 'Initial revision', 'no.nav.aura.envconfig.model.application.Application', to_timestamp('2012-12-01 00:00:00', 'YYYY-MM-DD HH24:MI:SS'));

insert into additionalrevisioninfo
(revision, author, dataorigin, message, modifiedentitytype, timestamp)
values
(5, 'Script', 'Script', 'Initial revision', 'no.nav.aura.envconfig.model.resource.PropertyResource', to_timestamp('2012-12-01 00:00:00', 'YYYY-MM-DD HH24:MI:SS'));

-- Increase sequence in case of clean schema
select hibernate_sequence.nextval from dual;
select hibernate_sequence.nextval from dual;
select hibernate_sequence.nextval from dual;
select hibernate_sequence.nextval from dual;
select hibernate_sequence.nextval from dual;
select hibernate_sequence.nextval from dual;
select hibernate_sequence.nextval from dual;
select hibernate_sequence.nextval from dual;
select hibernate_sequence.nextval from dual;
select hibernate_sequence.nextval from dual;

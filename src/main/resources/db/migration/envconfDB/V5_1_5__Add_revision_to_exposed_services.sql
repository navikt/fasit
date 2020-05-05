alter table appinstance_exposedservices drop primary key drop index;

alter table appinstance_exposedservices add (
  entid number(19) null,
  resource_alias varchar2(255) null,
  created timestamp(6) null,
  updated timestamp(6) null,
  updatedby varchar2(255) null,
  revision number(19) null
);
alter table appinstance_exposedservices rename column resources_entid to resource_entid;

create index aies_resentid_aientid_ix on appinstance_exposedservices(resource_entid, applicationinstance_entid);

update appinstance_exposedservices set entid=hibernate_sequence.nextval;
alter table appinstance_exposedservices add constraint pk_aies_entid primary key (entid);

update appinstance_exposedservices
set
resource_alias=(select resource_alias from resource_table where entid=appinstance_exposedservices.resource_entid),
revision=(select max(rev) from resource_table_aud group by resource_table_aud.entid having appinstance_exposedservices.resource_entid=resource_table_aud.entid),
created=sysdate,
updated=sysdate, updatedby='Script'
;

create table appinst_exposedservices_aud (
  entid number(19) not null,
  rev number(19) not null,
  revtype number(19) not null,
  applicationinstance_entid number(19) null,
  resource_entid number(19) null,
  resource_alias varchar2(255) null,
  created timestamp(6) null,
  updated timestamp(6) null,
  updatedby varchar2(255) null,
  revision number(19) null
);

alter table appinst_expservices_aud add ( entid number(19) );
alter table appinst_expservices_aud modify revtype not null;
rename appinst_expservices_aud to appinst_expref_aud;

alter table appinstance_exposedservices modify applicationinstance_entid null;
alter table appinstance_exposedservices modify resource_entid null;



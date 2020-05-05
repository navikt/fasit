alter table resource_table ADD CONSTRAINT fk_resource_to_app_entid FOREIGN KEY (application_entid) REFERENCES application(entid);

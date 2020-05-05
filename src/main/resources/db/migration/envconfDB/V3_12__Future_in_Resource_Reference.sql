alter table app_instance_res_refs add 
(
	resource_type varchar2(255), 
	future number(1,0)
);

update app_instance_res_refs set future = 0;

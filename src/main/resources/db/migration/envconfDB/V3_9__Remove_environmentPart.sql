-- destructive cleanup after environmentpart refactoring
 alter table node drop column env_id_old;
 alter table clusters drop column env_id_old; 
 drop table environmentpart;
  
  	
  
  
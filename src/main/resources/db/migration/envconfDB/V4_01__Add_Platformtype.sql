-- add type on node
 alter table node add(
 	 "PLATFORMTYPE"  VARCHAR2(255)
 ) ;

 update node set platformtype='JBOSS' where platformtype is null;
 
 alter table node_aud add(
 	 "PLATFORMTYPE"  VARCHAR2(255)
 ) ;
 update node_aud set platformtype='JBOSS' where platformtype is null;
  
  	
  
  
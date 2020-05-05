-- The applicationinstance and cluster are part of an unhealthy two-way binding that means that 
-- foreign key must be nulled before deleting instance. This constraint stops that.  
ALTER TABLE applicationinstance MODIFY(cluster_entid NULL);

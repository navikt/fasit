-- Remove constraint to allow hibernate to update the applicationinstance.cluster_entid to null 
-- Hibernate 2 - 0 TPR 
alter table applicationinstance drop constraint app_cluster_uix;

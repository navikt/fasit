-- Remove old unwanted resourcereferences without parent
DELETE FROM app_instance_res_refs WHERE applicationinstance_entid IS NULL;

-- Add some missing indexes
CREATE INDEX airr_resentid_aientid_ix on app_instance_res_refs("RESOURCE_ENTID", "APPLICATIONINSTANCE_ENTID");
ALTER TABLE secret ADD CONSTRAINT pk_secret_entid PRIMARY KEY (entid);
CREATE INDEX ai_clusterentid_ix ON applicationinstance("CLUSTER_ENTID");
CREATE INDEX node_env_id_ix ON node("ENV_ID");

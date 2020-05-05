package no.nav.aura.fasit.client.model;

import no.nav.aura.envconfig.client.ResourceTypeDO;

public class MissingResource {
    private String alias;
    private ResourceTypeDO type;

    public MissingResource() {
    }

    public MissingResource(String alias, ResourceTypeDO type) {
        this.alias = alias;
        this.type = type;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public ResourceTypeDO getType() {
        return type;
    }

    public void setType(ResourceTypeDO type) {
        this.type = type;
    }








}

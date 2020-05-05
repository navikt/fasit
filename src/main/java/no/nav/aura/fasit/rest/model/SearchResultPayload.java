package no.nav.aura.fasit.rest.model;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.net.URI;
import java.util.Map;

import static org.apache.commons.lang3.builder.ToStringStyle.NO_CLASS_NAME_STYLE;

public class SearchResultPayload {
    public Long id;
    public String name;
    public URI link;
    public String type;
    public String info;
    public Long lastChange;
    public LifecyclePayload lifecycle;
    public Map<String, Object> detailedInfo;


    @Override
    public String toString(){
        return ToStringBuilder.reflectionToString(this, NO_CLASS_NAME_STYLE);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(id).append(type).build();
    }


    @Override
    public boolean equals(Object other) {
        if (!(other instanceof SearchResultPayload)) {
            return false;
        }

        SearchResultPayload o = (SearchResultPayload) other;


        return this.id.compareTo(o.id) == 0 && this.type.compareTo(o.type) == 0;
    }
}
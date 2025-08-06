package no.nav.aura.envconfig.model;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import no.nav.aura.envconfig.model.infrastructure.EnvironmentClass;

import org.apache.commons.lang3.StringUtils;

@SuppressWarnings("serial")
@Embeddable
public class AccessControl implements Serializable {


    @Enumerated(EnumType.STRING)
    @Column(name = "access_envclass")
    private EnvironmentClass environmentClass;


    @Column(name = "access_groups")
    private String adGroups; // GA-001-tullball;v137023;trulsern

    public AccessControl() {
    }

    public AccessControl(EnvironmentClass environmentClass) {
        this.setEnvironmentClass(environmentClass);
    }

    public List<String> getAdGroupsAsList() {
        if (StringUtils.isEmpty(adGroups)) {
            return Collections.emptyList();
        }
        return Arrays.asList(adGroups.split("\\s*,\\s*"));
    }

    public void setAdGroups(String adGroups) {
        this.adGroups = adGroups;
    }

    public String getAdGroups() {
        return adGroups;
    }

    public EnvironmentClass getEnvironmentClass() {
        return environmentClass;
    }

    @Override
    public String toString() {
        return String.format("Access envclass: %s , adgroups: %s", environmentClass, adGroups);
    }

    public boolean hasOverriddenSecurity() {
        return !StringUtils.isEmpty(adGroups);
    }

    public void setEnvironmentClass(EnvironmentClass environmentClass) {
        this.environmentClass = environmentClass;
    }

    public void setAdGroups(List<String> adGroups) {
      this.adGroups=adGroups.stream().collect(Collectors.joining(","));
    }

}

package no.nav.aura.envconfig.model.application;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.hibernate.envers.AuditJoinTable;
import org.hibernate.envers.Audited;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import no.nav.aura.envconfig.model.AccessControl;
import no.nav.aura.envconfig.model.AccessControlled;
import no.nav.aura.envconfig.model.ModelEntity;
import no.nav.aura.envconfig.model.infrastructure.EnvironmentClass;

@SuppressWarnings("serial")
@Entity
@Table(name = "application_group")
@Audited
public class ApplicationGroup extends ModelEntity implements AccessControlled {

    @Column(name = "group_name", unique = true)
    private String name;

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "app_group_id")
    @AuditJoinTable(name = "APP_GROUP_APPLICATION_AUD")
    private Set<Application> applications = new HashSet<>();

    @Embedded
    private AccessControl accessControl;

    // Must be public because of ModelEntityIdentifier reflection
    public ApplicationGroup() {
//        this("");
    }

    public ApplicationGroup(String name) {
        this(name, new ArrayList<Application>());
    }

    public ApplicationGroup(String name, Application applicationToAdd) {
        this(name, new ArrayList<Application>(Arrays.asList(applicationToAdd)));
    }

    public ApplicationGroup(String name, List<Application> applicationsToAdd) {
        this.name = name;
        for (Application application : applicationsToAdd) {
            addApplication(application);
        }
        // hardcoding access to t for edit
        this.accessControl = new AccessControl(EnvironmentClass.t);
    }

    public String getName() {
        return name;
    }

    public void addApplication(Application application) {
        application.setPortOffset(calculatePortOffset());
        applications.add(application);
    }

    private int calculatePortOffset() {
        int smallestAvailablePortOffset = 0;

        for (Application application : getApplicationsByPortOffset()) {
            if (application.getPortOffset() == smallestAvailablePortOffset) {
                smallestAvailablePortOffset++;
            }
        }
        return smallestAvailablePortOffset;
    }

    public List<Application> getApplicationsByPortOffset() {
    	return applications.stream()
    			.sorted(Comparator.comparingInt(Application::getPortOffset))
				.collect(Collectors.toList());
    }

    public Set<Application> getApplications() {
        return applications;
    }

    public void removeApplication(Application application) {
        if (applications.contains(application)) {
            applications.remove(application);
        }
    }

    @Override
    public int hashCode() {
        return createHashCodeBuilder().append(name).build();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ApplicationGroup)) {
            return false;
        }
        ApplicationGroup other = (ApplicationGroup) obj;
        return createEqualsBuilder(other).append(name, other.name).build();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("name", name).append("applications", applications.size()).toString();
    }

    public void setName(String name) {
        this.name = name;
    }

    public void removeApplicationByApplicationId(final Long id) {
    	boolean  removed = applications.removeIf(application -> application.getID().equals(id));
    	
        if (!removed) {
            throw new RuntimeException("Unable to remove application " + id + " from cluster " + toString());
        }
    }

    @Override
    public AccessControl getAccessControl() {
        return accessControl;
    }
}

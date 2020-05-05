package no.nav.aura.envconfig.model.application;

import java.util.List;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.hibernate.envers.AuditJoinTable;
import org.hibernate.envers.Audited;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import com.google.common.primitives.Ints;

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

    @OneToMany
    @JoinColumn(name = "app_group_id")
    @AuditJoinTable(name = "APP_GROUP_APPLICATION_AUD")
    private Set<Application> applications = Sets.newHashSet();

    @Embedded
    private AccessControl accessControl;

    // Must be public because of ModelEntityIdentifier reflection
    public ApplicationGroup() {
        this("");
    }

    public ApplicationGroup(String name) {
        this(name, Lists.<Application> newArrayList());
    }

    public ApplicationGroup(String name, Application applicationToAdd) {
        this(name, Lists.newArrayList(applicationToAdd));
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

    public ImmutableList<Application> getApplicationsByPortOffset() {
        return orderByPortOffset().immutableSortedCopy(applications);
    }

    private Ordering<Application> orderByPortOffset() {
        return new Ordering<Application>() {
            @Override
            public int compare(Application a1, Application a2) {
                return Ints.compare(a1.getPortOffset(), a2.getPortOffset());
            }
        };
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
        boolean removed = Iterables.removeIf(applications, new Predicate<Application>() {
            public boolean apply(Application application) {
                return application.getID().equals(id);
            }
        });
        if (!removed) {
            throw new RuntimeException("Unable to remove application " + id + " from cluster " + toString());
        }
    }

    @Override
    public AccessControl getAccessControl() {
        return accessControl;
    }
}

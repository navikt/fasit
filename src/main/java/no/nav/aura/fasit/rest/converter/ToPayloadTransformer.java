package no.nav.aura.fasit.rest.converter;

import no.nav.aura.envconfig.model.AccessControl;
import no.nav.aura.envconfig.model.AccessControlled;
import no.nav.aura.envconfig.model.deletion.DeleteableEntity;
import no.nav.aura.fasit.rest.model.EntityPayload;
import org.joda.time.DateTime;

import java.time.LocalDateTime;
import java.util.function.Function;

abstract class ToPayloadTransformer<T extends DeleteableEntity, R extends EntityPayload> implements Function<T, R> {
    protected Long revision = null;

    protected abstract R transform(T from);

    @Override
    public R apply(T from) {
        R to = transform(from);
        to.created = toJava8Time(from.getCreated());
        to.updated = toJava8Time(from.getUpdated());
        to.id = from.getID();
        to.lifecycle.status = from.getLifeCycleStatus();

        if (from instanceof AccessControlled) {
            AccessControlled accessControlled = (AccessControlled) from;
            AccessControl accessControl = accessControlled.getAccessControl();
            to.accessControl.environmentClass = accessControl.getEnvironmentClass();
            to.accessControl.adGroups = accessControl.getAdGroupsAsList();
        }
        return to;
    }

    private LocalDateTime toJava8Time(DateTime joda) {
        if (joda == null) {
            return null;
        }
        return joda.toGregorianCalendar().toZonedDateTime().toLocalDateTime();
    }

}

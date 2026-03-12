package no.nav.aura.fasit.rest.converter;

import no.nav.aura.envconfig.model.AccessControl;
import no.nav.aura.envconfig.model.AccessControlled;
import no.nav.aura.envconfig.model.deletion.DeleteableEntity;
import no.nav.aura.fasit.rest.model.EntityPayload;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.function.Function;

abstract class ToPayloadTransformer<T extends DeleteableEntity, R extends EntityPayload> implements Function<T, R> {
    protected Long revision = null;

    protected abstract R transform(T from);

    @Override
    public R apply(T from) {
        R to = transform(from);
        to.created = toLocalDateTime(from.getCreated());
        to.updated = toLocalDateTime(from.getUpdated());
        to.id = from.getID();
        to.lifecycle.status = from.getLifeCycleStatus();

        if (from instanceof AccessControlled accessControlled) {
            AccessControl accessControl = accessControlled.getAccessControl();
            to.accessControl.environmentClass = accessControl.getEnvironmentClass();
            to.accessControl.adGroups = accessControl.getAdGroupsAsList();
        }
        return to;
    }

    private static LocalDateTime toLocalDateTime(ZonedDateTime zdt) {
        return zdt != null ? zdt.toLocalDateTime() : LocalDateTime.now();
    }

}

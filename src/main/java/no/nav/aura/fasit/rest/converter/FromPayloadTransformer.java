package no.nav.aura.fasit.rest.converter;

import no.nav.aura.envconfig.model.AccessControlled;
import no.nav.aura.envconfig.model.deletion.DeleteableEntity;
import no.nav.aura.fasit.rest.model.EntityPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public abstract class FromPayloadTransformer<T extends EntityPayload, R extends DeleteableEntity> implements Function<T, R> {
    
    private final Logger log = LoggerFactory.getLogger(FromPayloadTransformer.class);

    protected abstract R transform(T from);

    @Override
    public R apply(T from) {
        R to = transform(from);
        
        if (to instanceof AccessControlled) {
            AccessControlled accessControlled = (AccessControlled) to;
            List<String> adGroups = from.accessControl.adGroups;
            if (adGroups != null) {
                accessControlled.getAccessControl().setAdGroups(adGroups);
            }
        }

        return to;
    }

    protected <P> Optional<P> optional(P property){
         return Optional.ofNullable(property);
    }

}

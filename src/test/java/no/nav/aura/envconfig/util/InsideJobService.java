package no.nav.aura.envconfig.util;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class InsideJobService {

    public void perform(Effect effect) {
        effect.perform();
    }

    public <T> T produce(Producer<T> producer) {
        return producer.apply(null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void performInNewTransaction(Effect effect) {
        effect.perform();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public <T> T performInNewTransaction(Producer<T> producer) {
        return producer.get();
    }

}

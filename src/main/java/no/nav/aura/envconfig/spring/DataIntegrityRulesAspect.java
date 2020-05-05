package no.nav.aura.envconfig.spring;

import javax.inject.Inject;

import no.nav.aura.envconfig.DataIntegrityRulesEvaluator;
import no.nav.aura.envconfig.model.ModelEntity;
import no.nav.aura.envconfig.util.Consumer;
import no.nav.aura.envconfig.util.ReflectionUtil;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;

@Aspect
public class DataIntegrityRulesAspect {

    @Inject
    private DataIntegrityRulesEvaluator dataIntegrity;

    @SuppressWarnings("serial")
    @Before(value = "execution(* no.nav.aura.envconfig.FasitRepository.store(..)) && args(entity)")
    public void checkIntegrity(ModelEntity entity) {
        ReflectionUtil.doRecursively(entity, new Consumer<ModelEntity>() {
            @Override
            public void perform(ModelEntity entity) {
                dataIntegrity.checkConsistency(entity);
            }
        });
    }
}

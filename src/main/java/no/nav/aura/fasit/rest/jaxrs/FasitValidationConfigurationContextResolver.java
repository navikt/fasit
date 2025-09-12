package no.nav.aura.fasit.rest.jaxrs;

import javax.validation.BootstrapConfiguration;
import javax.validation.Configuration;
import javax.validation.Validation;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

import org.jboss.resteasy.plugins.validation.GeneralValidatorImpl;
import org.jboss.resteasy.spi.validation.GeneralValidator;
import org.springframework.stereotype.Component;

/** ikke tatt i bruk enda, da spring/ resteasy krangler om hvordan dette gjøres 

http://stackoverflow.com/questions/23940911/getting-spring-4-dependency-injection-working-with-resteasy-3-validator
*/
//@Provider
//@Component
public class FasitValidationConfigurationContextResolver implements ContextResolver<GeneralValidator> {

    @Override
    public GeneralValidator getContext(Class<?> type) {
        Configuration<?> config = Validation.byDefaultProvider().configure();
        BootstrapConfiguration bootstrapConfiguration = config.getBootstrapConfiguration();

        config.parameterNameProvider(new RestAnnotationParamterNameProvider());

        return new GeneralValidatorImpl(config.buildValidatorFactory(),
                bootstrapConfiguration.isExecutableValidationEnabled(),
                bootstrapConfiguration.getDefaultValidatedExecutableTypes());
    }

}

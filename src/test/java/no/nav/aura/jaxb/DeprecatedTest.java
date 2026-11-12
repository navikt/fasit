package no.nav.aura.jaxb;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import no.nav.aura.appconfig.Application;
import no.nav.aura.appconfig.deprecated.Deprecations;

public class DeprecatedTest {

    @Test
    public void findDeprecated() {
        Application application = Application.instance(getClass().getResourceAsStream("/app-config-deprecated.xml"));
        Deprecations deprecations = application.deprecations();

        assertThat(deprecations.get().toString(), containsString("tag=contextRoot"));
    }

    @Test
    public void noDeprecatedInAppConfigMax() {
        Application application = Application.instance(getClass().getResourceAsStream("/app-config-max.xml"));
        assertThat(application.deprecations().get(), Matchers.hasSize(0));

    }

}

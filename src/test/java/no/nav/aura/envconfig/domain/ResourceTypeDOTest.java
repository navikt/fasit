package no.nav.aura.envconfig.domain;

import static no.nav.aura.envconfig.client.ResourceTypeDO.findTypeFor;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Set;

import no.nav.aura.appconfig.resource.Resource;
import no.nav.aura.envconfig.client.ResourceTypeDO;
import no.nav.aura.envconfig.model.resource.ResourceType;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;

public class ResourceTypeDOTest {

    @Test
    public void validateResouceTypeEnumsMatch() {
        for (ResourceTypeDO rdo : ResourceTypeDO.values()) {
            assertNotNull(ResourceType.valueOf(rdo.name()));
        }
    }

    @Test
    public void findTypeByClass() throws Exception {
        ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(true);
        provider.addIncludeFilter(new AssignableTypeFilter(no.nav.aura.appconfig.resource.Resource.class));
        Set<BeanDefinition> definitions = provider.findCandidateComponents("no/nav/aura/appconfig");
        assertThat(definitions.size(), greaterThan(0));
        for (BeanDefinition beanDefinition : definitions) {
            Resource resource = create(beanDefinition);
            if (ResourceTypeDO.isDefinedFor(resource)) {
                assertNotNull(ResourceTypeDO.findTypeFor(resource));
            }
        }

    }

    private Resource create(BeanDefinition beanDefinition) throws Exception {
        @SuppressWarnings("unchecked")
        Class<? extends no.nav.aura.appconfig.resource.Resource> clazz = (Class<? extends no.nav.aura.appconfig.resource.Resource>) Class.forName(beanDefinition.getBeanClassName());
        return clazz.newInstance();
    }

    @Test
    public void notFound() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            findTypeFor(new Resource() {
            });
        });
    }

}

package no.nav.aura.fasit.repository;

import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import no.nav.aura.envconfig.model.infrastructure.EnvironmentClass;
import no.nav.aura.envconfig.model.resource.Resource;
import no.nav.aura.envconfig.model.resource.ResourceType;
import no.nav.aura.envconfig.model.resource.Scope;
import no.nav.aura.envconfig.spring.SpringUnitTestConfig;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { SpringUnitTestConfig.class })
@Transactional
public class ResourceRepositoryTest  {

    @Inject
    private ResourceRepository repo;
  
    private Resource saved;
    
    @BeforeEach
    public void setup() {
        Resource entity = new Resource("alias", ResourceType.BaseUrl, new Scope(EnvironmentClass.u));
        entity.putPropertyAndValidate("url", "http://someresource");
        saved = repo.save(entity);
    }

    @Test
    public void testStreamAll() {
        List<Resource> findAll = repo.findAll();
        System.out.println(findAll);
    }
    
//    @Test
//    public void testgetOptional() {
//        Optional<Resource> findOne = repo.getOne(100L);
//        System.out.println(findOne);
//    }

}

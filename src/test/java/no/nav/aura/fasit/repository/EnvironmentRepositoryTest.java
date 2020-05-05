package no.nav.aura.fasit.repository;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import no.nav.aura.envconfig.model.infrastructure.Environment;
import no.nav.aura.envconfig.model.infrastructure.EnvironmentClass;
import no.nav.aura.envconfig.spring.SpringUnitTestConfig;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { SpringUnitTestConfig.class })
@Transactional
public class EnvironmentRepositoryTest {

    @Inject
    private EnvironmentRepository repository;
    private Environment t1;


    @BeforeEach
    public void setup() throws Exception {
        
        repository.save(new Environment("u1", EnvironmentClass.u));
        repository.save(new Environment("u2", EnvironmentClass.u));
        repository.save(new Environment("u3", EnvironmentClass.u));
    
        t1 = repository.save(new Environment("t1", EnvironmentClass.t));
        repository.save(new Environment("t2", EnvironmentClass.t));
        repository.save(new Environment("t3", EnvironmentClass.t));
        
        repository.save(new Environment("q1", EnvironmentClass.q));
        
        repository.save(new Environment("p", EnvironmentClass.p));
  
    }
    
    @Test 
    public void getById(){
        assertThat(repository.findById(t1.getID()).get().getName(), equalTo("t1") );
    }
    
    @Test 
    public void findByEnvironmentClass(){
        assertThat(repository.findByEnvClass(EnvironmentClass.u).size(), equalTo(3) );
    }
    
    @Test 
    public void findByName(){
        assertThat(repository.findByNameIgnoreCase("q1").getName(), equalTo("q1") );
    }

   

}

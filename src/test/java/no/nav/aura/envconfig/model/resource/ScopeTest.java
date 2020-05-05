package no.nav.aura.envconfig.model.resource;

import no.nav.aura.envconfig.model.application.Application;
import no.nav.aura.envconfig.model.infrastructure.Domain;
import no.nav.aura.envconfig.model.infrastructure.EnvironmentClass;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ScopeTest {

    private Scope resourceScope_1;
    private Scope resourceScope_2;
    private Scope resourceScope_3;
    private Scope resourceScope_4;
    private Scope resourceScope_5;
    private Scope resourceScope_6;
    private Scope resourceScope_7;
    private Scope resourceScope_8;
    private Scope resourceScope_9;
    private Scope resourceScope_10;
    private Application app;

    @BeforeEach
    public void setup() {
        app = new Application("pen");

        resourceScope_1 = new Scope(EnvironmentClass.t).domain(Domain.TestLocal).envName("t1").application(app);
        resourceScope_2 = new Scope(EnvironmentClass.t).envName("t1").application(app);
        resourceScope_3 = new Scope(EnvironmentClass.t).domain(Domain.TestLocal).application(app);
        resourceScope_4 = new Scope(EnvironmentClass.t).application(app);
        resourceScope_5 = new Scope().application(app);
        resourceScope_6 = new Scope(EnvironmentClass.t).domain(Domain.TestLocal).envName("t1");
        resourceScope_7 = new Scope(EnvironmentClass.t).envName("t1");
        resourceScope_8 = new Scope(EnvironmentClass.t).domain(Domain.TestLocal);
        resourceScope_9 = new Scope().domain(Domain.TestLocal);
        resourceScope_10 = new Scope(EnvironmentClass.t);
    }

    @Test
    public void testScopeRating() {
        assertTrue(resourceScope_1.calculateScopeWeight() > resourceScope_2.calculateScopeWeight());
        assertTrue(resourceScope_2.calculateScopeWeight() > resourceScope_3.calculateScopeWeight());
        assertTrue(resourceScope_3.calculateScopeWeight() > resourceScope_4.calculateScopeWeight());
        assertTrue(resourceScope_4.calculateScopeWeight() > resourceScope_5.calculateScopeWeight());
        assertTrue(resourceScope_5.calculateScopeWeight() > resourceScope_6.calculateScopeWeight());
        assertTrue(resourceScope_6.calculateScopeWeight() > resourceScope_7.calculateScopeWeight());
        assertTrue(resourceScope_7.calculateScopeWeight() > resourceScope_8.calculateScopeWeight());
        assertTrue(resourceScope_8.calculateScopeWeight() > resourceScope_9.calculateScopeWeight());
        assertTrue(resourceScope_9.calculateScopeWeight() > resourceScope_10.calculateScopeWeight());
    }

    @Test
    public void testScopeSubset() {
        assertTrue(resourceScope_7.isSubsetOf(resourceScope_10));
        assertFalse(resourceScope_10.isSubsetOf(resourceScope_7));
        assertTrue(resourceScope_1.isSubsetOf(resourceScope_10));
        assertTrue(resourceScope_1.isSubsetOf(resourceScope_7));
        assertTrue(resourceScope_1.isSubsetOf(resourceScope_3));
        assertTrue(resourceScope_1.isSubsetOf(resourceScope_1));
        assertFalse(resourceScope_2.isSubsetOf(resourceScope_1));
    }

    @Test
    public void testScopeEquals() {
        assertEquals(resourceScope_1, new Scope(EnvironmentClass.t).domain(Domain.TestLocal).envName("t1").application(app));
        assertNotEquals(resourceScope_1, resourceScope_2);
        assertNotEquals(resourceScope_1, resourceScope_3);
        assertNotEquals(resourceScope_1, resourceScope_4);
        assertNotEquals(resourceScope_1, resourceScope_5);
        assertNotEquals(resourceScope_1, resourceScope_6);
        assertNotEquals(resourceScope_1, resourceScope_7);
        assertNotEquals(resourceScope_1, resourceScope_8);
        assertNotEquals(resourceScope_1, resourceScope_9);
        assertNotEquals(resourceScope_1, resourceScope_10);
    }
}

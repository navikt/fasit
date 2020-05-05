package no.nav.aura.envconfig.filter;

import no.nav.aura.envconfig.auditing.NavUser;

public class DummyUserLookup implements UserLookup {

    @Override
    public NavUser lookup(String id) {
        NavUser navUser = new NavUser(id);
        navUser.setExists(true);
        navUser.setName("Dummy");
        return navUser;
    }

}

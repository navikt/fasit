package no.nav.aura.envconfig.filter;

import no.nav.aura.envconfig.auditing.NavUser;

public interface UserLookup {

    NavUser lookup(String id);

}
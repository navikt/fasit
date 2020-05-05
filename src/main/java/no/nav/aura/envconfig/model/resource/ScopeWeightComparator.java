package no.nav.aura.envconfig.model.resource;

import java.util.Comparator;

import no.nav.aura.envconfig.model.Scopeable;

public class ScopeWeightComparator<T extends Scopeable> implements Comparator<T> {
    @Override
    public int compare(T o1, T o2) {
        return compareScopeWeight(o1.getScope(), o2.getScope());
    }
    
    private int compareScopeWeight(Scope scope1 , Scope scope2) {
        int thisRating = scope1.calculateScopeWeight();
        int thatRating = scope2.calculateScopeWeight();

        if (thisRating == thatRating) {
            return 0;
        }

        return thisRating > thatRating ? 1 : -1;
    }

    
}

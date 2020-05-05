package no.nav.aura.appconfig.artifact;

import java.util.Comparator;

public class ArtifactStartupOrderComparator implements Comparator<Ear> {

    @Override
    public int compare(Ear o1, Ear o2) {
        return o1.getStartUpOrder() - o2.getStartUpOrder();
    }

}

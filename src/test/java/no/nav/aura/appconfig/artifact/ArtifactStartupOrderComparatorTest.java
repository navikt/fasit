package no.nav.aura.appconfig.artifact;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;

import org.junit.Test;

public class ArtifactStartupOrderComparatorTest {

    @Test
    public void testOrder() {
        Ear ear1 = new Ear();
        ear1.setStartUpOrder(5);
        Ear ear2 = new Ear();
        ear2.setStartUpOrder(10);
        Ear ear3 = new Ear();
        ear3.setStartUpOrder(1);

        ArrayList<Ear> ears = new ArrayList<>();
        ears.add(ear1);
        ears.add(ear2);
        ears.add(ear3);

        Collections.sort(ears, new ArtifactStartupOrderComparator());
        assertEquals(ear3, ears.get(0));
        assertEquals(ear1, ears.get(1));
        assertEquals(ear2, ears.get(2));
    }

    @Test
    public void testDefault() {
        Ear ear1 = new Ear();
        Ear ear2 = new Ear();
        ear2.setStartUpOrder(11);
        Ear ear3 = new Ear();
        ear3.setStartUpOrder(9);

        ArrayList<Ear> ears = new ArrayList<>();
        ears.add(ear1);
        ears.add(ear2);
        ears.add(ear3);

        Collections.sort(ears, new ArtifactStartupOrderComparator());
        assertEquals(ear3, ears.get(0));
        assertEquals(ear1, ears.get(1));
        assertEquals(ear2, ears.get(2));
    }
}

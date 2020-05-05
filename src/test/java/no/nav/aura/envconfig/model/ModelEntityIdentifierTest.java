package no.nav.aura.envconfig.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import no.nav.aura.envconfig.FasitRepository;
import no.nav.aura.envconfig.model.application.Application;
import no.nav.aura.envconfig.util.Tuple;

import org.hibernate.envers.RevisionType;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

public class ModelEntityIdentifierTest {

    private FasitRepository repository = Mockito.mock(FasitRepository.class);
    private Optional<Long> absent = Optional.absent();

    @Test
    public void newIsHead() {
        assertTrue(new ModelEntityIdentifier<>(Application.class, absent, absent).isHead(repository));
    }

    @Test
    public void noRevisionIsHead() {
        Mockito.when(repository.getRevisionsFor(Application.class, 1l)).thenReturn(ImmutableList.of(Tuple.of(5l, RevisionType.MOD), Tuple.of(3l, RevisionType.MOD), Tuple.of(1l, RevisionType.ADD)));
        assertTrue(new ModelEntityIdentifier<>(Application.class, Optional.of(1l), absent).isHead(repository));
    }

    @Test
    public void nonLastRevisionIsNOTHead() {
        Mockito.when(repository.getRevisionsFor(Application.class, 1l)).thenReturn(ImmutableList.of(Tuple.of(5l, RevisionType.MOD), Tuple.of(3l, RevisionType.MOD), Tuple.of(1l, RevisionType.ADD)));
        assertFalse(new ModelEntityIdentifier<>(Application.class, Optional.of(1l), Optional.of(3l)).isHead(repository));
    }

    @Test
    public void lastRevisionIsHead() {
        Mockito.when(repository.getRevisionsFor(Application.class, 1l)).thenReturn(ImmutableList.of(Tuple.of(5l, RevisionType.MOD), Tuple.of(3l, RevisionType.MOD), Tuple.of(1l, RevisionType.ADD)));
        assertTrue(new ModelEntityIdentifier<>(Application.class, Optional.of(1l), Optional.of(5l)).isHead(repository));
    }

    @Test
    public void deletedRevisionIsNOTHead() {
        Mockito.when(repository.getRevisionsFor(Application.class, 1l)).thenReturn(ImmutableList.of(Tuple.of(5l, RevisionType.DEL), Tuple.of(3l, RevisionType.MOD), Tuple.of(1l, RevisionType.ADD)));
        assertFalse(new ModelEntityIdentifier<>(Application.class, Optional.of(1l), Optional.of(5l)).isHead(repository));
    }

}

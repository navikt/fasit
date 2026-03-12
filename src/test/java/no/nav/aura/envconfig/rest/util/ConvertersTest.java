package no.nav.aura.envconfig.rest.util;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

import no.nav.aura.envconfig.client.PlatformTypeDO;
import no.nav.aura.envconfig.model.infrastructure.PlatformType;
import no.nav.aura.envconfig.util.SerializableFunction;

import org.junit.jupiter.api.Test;

public class ConvertersTest {

    @SuppressWarnings("serial")
    @Test
    public void toPlatformType() {
        checkEnumConversion(PlatformTypeDO.values(), new SerializableFunction<PlatformTypeDO, PlatformType>() {
            public PlatformType process(PlatformTypeDO input) {
                return Converters.toPlatformType(input);
            }
        });
    }

    @SuppressWarnings("serial")
    @Test
    public void toPlatformTypeDO() {
        checkEnumConversion(PlatformType.values(), new SerializableFunction<PlatformType, PlatformTypeDO>() {
            public PlatformTypeDO process(PlatformType input) {
                return Converters.toPlatformTypeDO(input);
            }
        });
    }

    private <T, F> void checkEnumConversion(F[] values, Function<F, T> f) {
        Set<T> set = new HashSet<>();
        for (F environmentClass : values) {
            set.add(f.apply(environmentClass));
        }
        assertThat(set.size(), equalTo(values.length));
    }

}

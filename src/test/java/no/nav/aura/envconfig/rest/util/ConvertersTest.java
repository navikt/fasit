package no.nav.aura.envconfig.rest.util;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Set;

import no.nav.aura.envconfig.client.PlatformTypeDO;
import no.nav.aura.envconfig.model.infrastructure.PlatformType;
import no.nav.aura.envconfig.util.SerializableFunction;

import org.junit.jupiter.api.Test;

import com.google.common.base.Function;
import com.google.common.collect.Sets;

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
        Set<T> set = Sets.newHashSet();
        for (F environmentClass : values) {
            set.add(f.apply(environmentClass));
        }
        assertThat(set.size(), equalTo(values.length));
    }

}

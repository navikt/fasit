package no.nav.aura.envconfig.rest.util;

import no.nav.aura.envconfig.client.PlatformTypeDO;
import no.nav.aura.envconfig.model.infrastructure.PlatformType;

public abstract class Converters {

    private Converters() {
    }

    public static PlatformType toPlatformType(PlatformTypeDO platformTypeDO) {
        return PlatformType.valueOf(platformTypeDO.name());
    }

    public static PlatformTypeDO toPlatformTypeDO(PlatformType platformType) {
        return PlatformTypeDO.valueOf(platformType.name());
    }

    public static <T extends Enum<T>> T toEnumOrNull(Class<T> enumType, String name) {
        try {
            return Enum.valueOf(enumType, name);
        } catch (Exception e) {
            return null;
        }
    }

}

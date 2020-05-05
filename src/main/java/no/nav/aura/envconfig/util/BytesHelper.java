package no.nav.aura.envconfig.util;

public abstract class BytesHelper {

    private BytesHelper() {
    }

    public static byte[] cloneOrNull(byte[] bs) {
        return bs == null ? null : bs.clone();
    }

}

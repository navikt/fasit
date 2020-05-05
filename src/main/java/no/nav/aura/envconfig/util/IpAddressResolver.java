package no.nav.aura.envconfig.util;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.fromNullable;

import java.net.InetAddress;
import java.net.UnknownHostException;

import com.google.common.base.Optional;

public class IpAddressResolver {

    public static Optional<String> resolveIpFrom(String hostname) {
        if (hostname != null && !hostname.isEmpty()) {
            try {
                return fromNullable(InetAddress.getByName(hostname).getHostAddress());
            } catch (UnknownHostException e) {
                return absent();
            }
        }
        return absent();
    }
}

package no.nav.aura.envconfig.util;


import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;


public class IpAddressResolver {

    public static Optional<String> resolveIpFrom(String hostname) {
        if (hostname != null && !hostname.isEmpty()) {
            try {
                return Optional.ofNullable(InetAddress.getByName(hostname).getHostAddress());
            } catch (UnknownHostException e) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }
}

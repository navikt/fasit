package no.nav.aura.fasit.rest.converter;

import java.util.Optional;

import no.nav.aura.envconfig.model.infrastructure.Node;
import no.nav.aura.envconfig.model.secrets.Secret;
import no.nav.aura.fasit.rest.model.NodePayload;

public class Payload2NodeTransformer extends FromPayloadTransformer<NodePayload, Node> {

    private final Optional<Node> defaultValue;

    public Payload2NodeTransformer() {
        this(null);
    }

    public Payload2NodeTransformer(Node defaultValue) {
        this.defaultValue = Optional.ofNullable(defaultValue);
    }

    @Override
    protected Node transform(NodePayload from) {
       
        Node node = defaultValue.orElse(new Node(from.hostname, from.username, "dummy", from.environmentClass, from.type));
        node.setHostname(from.hostname);

        optional(from.username).ifPresent(u -> node.setUserName(u));
        optional(from.type).ifPresent(t ->node.setPlatformType(t));
        optional(from.password).ifPresent(p -> node.setPassword(Secret.withValueAndAuthLevel(p.value, from.environmentClass)));
        return node;
    }

}

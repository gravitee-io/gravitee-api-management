package io.gravitee.gateway.jupiter.reactor.v4.reactor;

import java.util.List;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ReactorFactoryManager {

    private static final List<ReactorFactory> factories = List.of();

    public List<ReactorFactory> getFactories() {
        return factories;
    }
}

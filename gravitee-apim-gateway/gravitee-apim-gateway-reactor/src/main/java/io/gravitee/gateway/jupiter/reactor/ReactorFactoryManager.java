package io.gravitee.gateway.jupiter.reactor;

import io.gravitee.gateway.jupiter.reactor.subscription.SubscriptionReactorFactory;

import java.util.Collections;
import java.util.List;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ReactorFactoryManager {

    private final static List<ReactorFactory> factories = Collections.singletonList(
            new SubscriptionReactorFactory()
    );

    public List<ReactorFactory> getFactories() {
        return factories;
    }
}

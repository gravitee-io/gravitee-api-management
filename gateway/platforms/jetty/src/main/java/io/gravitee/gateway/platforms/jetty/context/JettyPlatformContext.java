package io.gravitee.gateway.platforms.jetty.context;

import io.gravitee.gateway.core.PlatformContext;
import io.gravitee.gateway.core.Reactor;
import io.gravitee.gateway.core.Registry;
import io.gravitee.gateway.core.impl.DefaultReactor;
import io.gravitee.gateway.core.impl.FileRegistry;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class JettyPlatformContext implements PlatformContext {

    private final Reactor reactor = new DefaultReactor();
    private final Registry registry = new FileRegistry();

    @Override
    public Reactor getReactor() {
        return reactor;
    }

    @Override
    public Registry getRegistry() {
        return registry;
    }
}

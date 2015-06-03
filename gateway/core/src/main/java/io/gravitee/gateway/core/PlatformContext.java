package io.gravitee.gateway.core;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public interface PlatformContext {

    Reactor getReactor();

    Registry getRegistry();
}

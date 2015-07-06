package io.gravitee.gateway.api;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public interface ConfigurablePolicy<T extends PolicyConfiguration> extends Policy {

    T getConfiguration();
}

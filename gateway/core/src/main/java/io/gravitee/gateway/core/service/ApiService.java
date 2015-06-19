package io.gravitee.gateway.core.service;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public interface ApiService {

    boolean start(String name);
    boolean stop(String name);
    boolean reload(String name);
}

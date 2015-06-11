package io.gravitee.gateway.api;

import java.util.Map;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public interface Request {

    String id();

    String requestURI();

    Map<String, String> queryParameters();

    String method();

    boolean hasContent();
}

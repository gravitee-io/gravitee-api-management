package io.gravitee.gateway.api;

import java.util.Map;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public interface Response {

    int status();

    Map<String, String> headers();

    byte[] content();
}

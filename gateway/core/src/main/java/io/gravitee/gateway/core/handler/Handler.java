package io.gravitee.gateway.core.handler;

import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public interface Handler {

    void handle(Request request, Response response);
}

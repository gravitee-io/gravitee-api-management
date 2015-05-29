package io.gravitee.gateway.core;

import io.gravitee.gateway.http.Request;
import io.gravitee.gateway.http.Response;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public interface Handler {

	void handle(Request request, Response response);
}

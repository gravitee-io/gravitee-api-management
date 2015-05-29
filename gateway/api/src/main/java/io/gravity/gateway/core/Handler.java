package io.gravity.gateway.core;

import io.gravity.gateway.http.Request;
import io.gravity.gateway.http.Response;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public interface Handler {

	void handle(Request request, Response response);
}

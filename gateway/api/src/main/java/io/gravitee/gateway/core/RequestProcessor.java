package io.gravitee.gateway.core;

import io.gravitee.gateway.http.Request;

/**
 *
 * @author David BRASSELY (brasseld at gmail.com)
 */
public interface RequestProcessor {

	void process(Request request);
}

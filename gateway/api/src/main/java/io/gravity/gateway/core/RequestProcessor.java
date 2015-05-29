package io.gravity.gateway.core;

import io.gravity.gateway.http.Request;

/**
 *
 * @author David BRASSELY (brasseld at gmail.com)
 */
public interface RequestProcessor {

	void process(Request request);
}

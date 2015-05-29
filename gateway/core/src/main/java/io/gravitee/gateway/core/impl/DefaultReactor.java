package io.gravitee.gateway.core.impl;

import io.gravitee.gateway.core.Processor;
import io.gravitee.gateway.http.Request;
import io.gravitee.gateway.core.AsyncHandler;
import io.gravitee.gateway.core.Reactor;
import io.gravitee.gateway.http.Response;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class DefaultReactor implements Reactor {

	@Override public Processor handle(Request request, AsyncHandler<Response> responseHandler) {
		return new RequestProcessor(request, responseHandler);
	}
}

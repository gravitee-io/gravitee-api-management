package io.gravity.gateway.core.impl;

import io.gravity.gateway.core.AsyncHandler;
import io.gravity.gateway.core.Processor;
import io.gravity.gateway.core.Reactor;
import io.gravity.gateway.http.Request;
import io.gravity.gateway.http.Response;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class DefaultReactor implements Reactor {

	@Override public Processor handle(Request request, AsyncHandler<Response> responseHandler) {
		return new RequestProcessor(request, responseHandler);
	}
}

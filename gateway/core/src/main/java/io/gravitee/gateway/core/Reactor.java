package io.gravitee.gateway.core;

import io.gravitee.gateway.http.Request;
import io.gravitee.gateway.http.Response;

/**
 * Created by david on 28/05/2015.
 */
public interface Reactor {

	Processor handle(Request request, AsyncHandler<Response> responseHandler);
}

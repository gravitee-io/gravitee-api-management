package io.gravity.gateway.core;

import io.gravity.gateway.http.Request;
import io.gravity.gateway.http.Response;

/**
 * Created by david on 28/05/2015.
 */
public interface Reactor {

	Processor handle(Request request, AsyncHandler<Response> responseHandler);
}

package io.gravitee.gateway.reactive.reactor.handler.context;

import io.gravitee.gateway.core.component.ComponentProvider;
import io.gravitee.gateway.reactive.api.context.async.AsyncRequest;
import io.gravitee.gateway.reactive.api.context.async.AsyncResponse;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class AsyncExecutionContext extends AbstractExecutionContext<AsyncRequest, AsyncResponse> {
    public AsyncExecutionContext(AsyncRequest request, AsyncResponse response, ComponentProvider componentProvider) {
        super(request, response, componentProvider);
    }
}

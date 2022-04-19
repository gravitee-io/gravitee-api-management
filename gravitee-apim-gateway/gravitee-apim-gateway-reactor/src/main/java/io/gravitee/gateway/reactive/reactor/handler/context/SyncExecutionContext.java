package io.gravitee.gateway.reactive.reactor.handler.context;

import io.gravitee.gateway.core.component.ComponentProvider;
import io.gravitee.gateway.reactive.api.context.sync.SyncRequest;
import io.gravitee.gateway.reactive.api.context.sync.SyncResponse;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SyncExecutionContext extends AbstractExecutionContext<SyncRequest, SyncResponse> implements io.gravitee.gateway.reactive.api.context.sync.SyncExecutionContext {

    public SyncExecutionContext(SyncRequest request, SyncResponse response, ComponentProvider componentProvider) {
        super(request, response, componentProvider);
    }
}

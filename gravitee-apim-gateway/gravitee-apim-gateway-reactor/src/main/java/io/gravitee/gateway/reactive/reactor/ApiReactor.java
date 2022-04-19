package io.gravitee.gateway.reactive.reactor;

import io.gravitee.gateway.reactive.api.context.Request;
import io.gravitee.gateway.reactive.api.context.Response;
import io.reactivex.Completable;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface ApiReactor<RQ extends Request<?>, RS extends Response<?>> {
    Completable handle(RQ request, RS response);
}

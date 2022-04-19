package io.gravitee.gateway.reactive.reactor;

import io.gravitee.common.service.Service;
import io.reactivex.Completable;
import io.vertx.reactivex.core.http.HttpServerRequest;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface HttpRequestDispatcher extends Service<HttpRequestDispatcher> {
    Completable dispatch(HttpServerRequest httpServerRequest);
}

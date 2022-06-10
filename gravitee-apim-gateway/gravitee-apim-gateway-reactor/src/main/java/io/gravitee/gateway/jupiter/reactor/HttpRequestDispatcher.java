/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.jupiter.reactor;

import io.gravitee.common.service.Service;
import io.reactivex.Completable;
import io.vertx.reactivex.core.http.HttpServerRequest;

/**
 * Request dispatcher responsible to dispatch any HTTP request to the appropriate {@link io.gravitee.gateway.reactor.handler.ReactorHandler}.
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface HttpRequestDispatcher extends Service<HttpRequestDispatcher> {
    /**
     * Dispatches the incoming request to the right {@link io.gravitee.gateway.reactor.handler.ReactorHandler}.
     *
     * @param httpServerRequest the vertx http request.
     *
     * @return a {@link Completable} that completes when the request has been fully dispatched and completed.
     */
    Completable dispatch(HttpServerRequest httpServerRequest);
}

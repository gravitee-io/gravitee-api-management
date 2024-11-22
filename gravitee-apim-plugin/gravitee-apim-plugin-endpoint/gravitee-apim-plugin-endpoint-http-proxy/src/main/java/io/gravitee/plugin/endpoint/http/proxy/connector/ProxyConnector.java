/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.plugin.endpoint.http.proxy.connector;

import io.gravitee.gateway.reactive.api.context.http.HttpExecutionContext;
import io.reactivex.rxjava3.core.Completable;

/**
 * Define a proxy connection
 *
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface ProxyConnector {
    /**
     * Effectively connect to the underlying connection
     *
     * @param ctx
     * @return a {@link Completable} that completes once the connection has been opened
     */
    Completable connect(final HttpExecutionContext ctx);
}

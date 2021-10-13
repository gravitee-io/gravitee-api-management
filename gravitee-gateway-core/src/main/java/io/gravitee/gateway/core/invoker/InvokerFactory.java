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
package io.gravitee.gateway.core.invoker;

import io.gravitee.definition.model.Api;
import io.gravitee.gateway.api.Invoker;
import io.gravitee.gateway.api.endpoint.resolver.EndpointResolver;
import io.gravitee.gateway.core.failover.FailoverInvoker;
import io.gravitee.gateway.core.failover.FailoverOptions;
import io.vertx.core.Vertx;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class InvokerFactory {

    private final Api api;

    private final Vertx vertx;

    private final EndpointResolver endpointResolver;

    public InvokerFactory(final Api api, final Vertx vertx, final EndpointResolver endpointResolver) {
        this.api = api;
        this.vertx = vertx;
        this.endpointResolver = endpointResolver;
    }

    public Invoker create() {
        if (api.getProxy().failoverEnabled()) {
            return new FailoverInvoker(
                vertx,
                endpointResolver,
                new FailoverOptions()
                    .setMaxAttempts(api.getProxy().getFailover().getMaxAttempts())
                    .setRetryTimeout(api.getProxy().getFailover().getRetryTimeout())
            );
        }

        return new EndpointInvoker(endpointResolver);
    }
}

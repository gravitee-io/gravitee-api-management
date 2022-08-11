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
package io.gravitee.gateway.jupiter.core.v4.invoker;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.definition.model.v4.Api;
import io.gravitee.gateway.jupiter.api.ExecutionFailure;
import io.gravitee.gateway.jupiter.api.context.HttpExecutionContext;
import io.gravitee.gateway.jupiter.api.context.MessageExecutionContext;
import io.gravitee.gateway.jupiter.api.context.RequestExecutionContext;
import io.gravitee.gateway.jupiter.api.endpoint.EndpointConnector;
import io.gravitee.gateway.jupiter.api.invoker.Invoker;
import io.gravitee.gateway.jupiter.core.v4.endpoint.DefaultEndpointConnectorResolver;
import io.reactivex.Completable;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class EndpointInvoker implements Invoker {

    private final Api api;
    private final DefaultEndpointConnectorResolver endpointResolver;

    public EndpointInvoker(final Api api, final DefaultEndpointConnectorResolver endpointResolver) {
        this.api = api;
        this.endpointResolver = endpointResolver;
    }

    @Override
    public String getId() {
        return "endpoint-invoker";
    }

    @Override
    public Completable invoke(RequestExecutionContext ctx) {
        return invoke((HttpExecutionContext) ctx);
    }

    @Override
    public Completable invoke(MessageExecutionContext ctx) {
        return invoke((HttpExecutionContext) ctx);
    }

    private Completable invoke(HttpExecutionContext ctx) {
        final EndpointConnector<HttpExecutionContext> endpointConnector = endpointResolver.resolve(ctx);

        if (endpointConnector == null) {
            return ctx.interruptWith(new ExecutionFailure(HttpStatusCode.NOT_FOUND_404).message("No endpoint available"));
        }

        return endpointConnector.connect(ctx);
    }
}

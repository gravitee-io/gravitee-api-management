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
package io.gravitee.gateway.jupiter.core.v4.invoker;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gateway.jupiter.api.ExecutionFailure;
import io.gravitee.gateway.jupiter.api.connector.endpoint.EndpointConnector;
import io.gravitee.gateway.jupiter.api.context.ExecutionContext;
import io.gravitee.gateway.jupiter.api.invoker.Invoker;
import io.gravitee.gateway.jupiter.core.v4.endpoint.DefaultEndpointConnectorResolver;
import io.reactivex.Completable;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class EndpointInvoker implements Invoker {

    public static final String NO_ENDPOINT_FOUND_KEY = "NO_ENDPOINT_FOUND";

    private final DefaultEndpointConnectorResolver endpointResolver;

    public EndpointInvoker(final DefaultEndpointConnectorResolver endpointResolver) {
        this.endpointResolver = endpointResolver;
    }

    @Override
    public String getId() {
        return "endpoint-invoker";
    }

    public Completable invoke(ExecutionContext ctx) {
        final EndpointConnector endpointConnector = endpointResolver.resolve(ctx);

        if (endpointConnector == null) {
            return ctx.interruptWith(
                new ExecutionFailure(HttpStatusCode.NOT_FOUND_404).key(NO_ENDPOINT_FOUND_KEY).message("No endpoint available")
            );
        }

        return endpointConnector.connect(ctx);
    }
}

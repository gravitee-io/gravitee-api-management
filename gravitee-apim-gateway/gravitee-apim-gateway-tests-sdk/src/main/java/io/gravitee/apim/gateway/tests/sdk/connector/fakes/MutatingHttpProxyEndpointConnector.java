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
package io.gravitee.apim.gateway.tests.sdk.connector.fakes;

import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.reactive.api.context.http.HttpExecutionContext;
import io.gravitee.gateway.reactive.core.context.MutableRequest;
import io.gravitee.plugin.endpoint.http.proxy.HttpProxyEndpointConnector;
import io.gravitee.plugin.endpoint.http.proxy.configuration.HttpProxyEndpointConnectorConfiguration;
import io.gravitee.plugin.endpoint.http.proxy.configuration.HttpProxyEndpointConnectorSharedConfiguration;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;

/**
 * An endpoint connector that mutates the request (body, headers, pathInfo) before forwarding to the backend.
 * This simulates what connectors like LLM proxy do (transforming the request for a specific provider),
 * allowing to test that failover correctly restores the original request state on each retry.
 */
public class MutatingHttpProxyEndpointConnector extends HttpProxyEndpointConnector {

    public MutatingHttpProxyEndpointConnector(
        HttpProxyEndpointConnectorConfiguration configuration,
        HttpProxyEndpointConnectorSharedConfiguration sharedConfiguration
    ) {
        super(configuration, sharedConfiguration);
    }

    @Override
    public Completable connect(HttpExecutionContext ctx) {
        return ctx
            .request()
            .onBody(body ->
                body
                    .defaultIfEmpty(Buffer.buffer())
                    .flatMapMaybe(b -> {
                        Buffer transformed = Buffer.buffer("mutated-" + b.toString());
                        ((MutableRequest) ctx.request()).pathInfo("/mutated-path");
                        ctx.request().headers().set("X-Mutated", "true");
                        ctx.request().contentLength(transformed.length());
                        return Maybe.just(transformed);
                    })
            )
            .andThen(super.connect(ctx));
    }
}

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

import io.gravitee.apim.gateway.tests.sdk.connector.fakes.ConnectionErrorHttpProxyEndpointConnectorConfiguration.Failure;
import io.gravitee.gateway.reactive.api.ExecutionFailure;
import io.gravitee.gateway.reactive.api.context.http.HttpExecutionContext;
import io.gravitee.plugin.endpoint.http.proxy.HttpProxyEndpointConnector;
import io.gravitee.plugin.endpoint.http.proxy.configuration.HttpProxyEndpointConnectorSharedConfiguration;
import io.reactivex.rxjava3.core.Completable;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Getter;
import lombok.Setter;

/**
 * This endpoint simulates failure to interrupt the flow during the connection to the endpoint
 * For repeatability, different error can be provided through a Deque in the configuration: connectionErrors. It's a Deque of {@link Failure}.
 * The connector will apply a delay taking the first element of the Deque, or 0 if no element.
 * Configuration does not support EL in contrary to original HTTP-PROXY connector
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Getter
@Setter
public class ConnectionErrorHttpProxyEndpointConnector extends HttpProxyEndpointConnector {

    public static final String MOCK_CONNECTION_ATTEMPTS = "Mock-Connection-Attempts";
    final ConnectionErrorHttpProxyEndpointConnectorConfiguration failureConfiguration;
    final AtomicInteger connectAttempts = new AtomicInteger(0);

    public ConnectionErrorHttpProxyEndpointConnector(
        ConnectionErrorHttpProxyEndpointConnectorConfiguration failureConfiguration,
        HttpProxyEndpointConnectorSharedConfiguration sharedConfiguration
    ) {
        super(failureConfiguration, sharedConfiguration);
        this.failureConfiguration = failureConfiguration;
    }

    @Override
    public Completable connect(HttpExecutionContext ctx) {
        Failure failure = Optional.ofNullable(failureConfiguration.connectionErrors.pollFirst()).orElse(new Failure(200, "ok"));
        // Return the connection attempts counter as a header to simplify the assertions
        ctx.response().headers().set(MOCK_CONNECTION_ATTEMPTS, Integer.toString(connectAttempts.incrementAndGet()));
        if (failure.status() == 200) {
            return super.connect(ctx);
        }
        return ctx.interruptWith(new ExecutionFailure(failure.status()).message(failure.message()));
    }
}

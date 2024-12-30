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

import io.gravitee.gateway.reactive.api.context.ExecutionContext;
import io.gravitee.gateway.reactive.api.context.http.HttpExecutionContext;
import io.gravitee.plugin.endpoint.mock.MockEndpointConnector;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Getter;
import lombok.Setter;

/**
 * This endpoint allows to apply some latency during the connection to the endpoint
 * For repeatability, different latencies can be provided through a Deque in the configuration: connectionLatencies.
 * The connector will apply a delay taking the first element of the Deque, or 0 if no element.
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Getter
@Setter
public class ConnectionLatencyMockEndpointConnector extends MockEndpointConnector {

    public static final String MOCK_CONNECTION_ATTEMPTS = "Mock-Connection-Attempts";
    final ConnectionLatencyMockEndpointConnectorConfiguration latencyConfiguration;
    final AtomicInteger connectAttempts = new AtomicInteger(0);

    public ConnectionLatencyMockEndpointConnector(ConnectionLatencyMockEndpointConnectorConfiguration latencyConfiguration) {
        super(latencyConfiguration);
        this.latencyConfiguration = latencyConfiguration;
    }

    @Override
    public Completable connect(HttpExecutionContext ctx) {
        Long latency = Optional.ofNullable(latencyConfiguration.connectionLatencies.pollFirst()).orElse(0L);
        // Return the connection attempts counter as a header to simplify the assertions
        ctx.response().headers().set(MOCK_CONNECTION_ATTEMPTS, Integer.toString(connectAttempts.incrementAndGet()));
        return Observable.just(new Object()).delay(latency, TimeUnit.MILLISECONDS).ignoreElements().andThen(super.connect(ctx));
    }
}

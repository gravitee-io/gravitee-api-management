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
package io.gravitee.plugin.endpoint.mock;

import io.gravitee.gateway.jupiter.api.ApiType;
import io.gravitee.gateway.jupiter.api.ConnectorMode;
import io.gravitee.gateway.jupiter.api.context.MessageExecutionContext;
import io.gravitee.gateway.jupiter.api.endpoint.async.EndpointAsyncConnector;
import io.gravitee.gateway.jupiter.api.message.Message;
import io.gravitee.plugin.endpoint.mock.configuration.MockEndpointConnectorConfiguration;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import lombok.AllArgsConstructor;

/**
 * @author GraviteeSource Team
 */
@AllArgsConstructor
public class MockEndpointConnector implements EndpointAsyncConnector {

    static final Set<ConnectorMode> SUPPORTED_MODES = Set.of(ConnectorMode.PUBLISH, ConnectorMode.SUBSCRIBE);
    static final ApiType SUPPORTED_API = ApiType.ASYNC;

    private final MockEndpointConnectorConfiguration configuration;

    @Override
    public ApiType supportedApi() {
        return SUPPORTED_API;
    }

    @Override
    public Set<ConnectorMode> supportedModes() {
        return SUPPORTED_MODES;
    }

    @Override
    public Completable connect(MessageExecutionContext ctx) {
        return Completable.fromRunnable(() -> ctx.response().messages(generateMessageFlow()));
    }

    private Flowable<Message> generateMessageFlow() {
        return Flowable
            .interval(configuration.getMessagesInterval(), TimeUnit.SECONDS)
            .map(value -> new MockMessage(configuration.getMessagesContent() + " " + value));
    }
}

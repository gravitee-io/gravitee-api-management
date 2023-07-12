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
package io.gravitee.plugin.endpoint.mock;

import io.gravitee.gateway.jupiter.api.ConnectorMode;
import io.gravitee.gateway.jupiter.api.connector.endpoint.async.EndpointAsyncConnector;
import io.gravitee.gateway.jupiter.api.context.ExecutionContext;
import io.gravitee.gateway.jupiter.api.context.MessageExecutionContext;
import io.gravitee.gateway.jupiter.api.message.DefaultMessage;
import io.gravitee.gateway.jupiter.api.message.Message;
import io.gravitee.plugin.endpoint.mock.configuration.MockEndpointConnectorConfiguration;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author GraviteeSource Team
 */
@AllArgsConstructor
@Slf4j
public class MockEndpointConnector implements EndpointAsyncConnector {

    static final Set<ConnectorMode> SUPPORTED_MODES = Set.of(ConnectorMode.PUBLISH, ConnectorMode.SUBSCRIBE);

    protected final MockEndpointConnectorConfiguration configuration;

    @Override
    public Set<ConnectorMode> supportedModes() {
        return SUPPORTED_MODES;
    }

    @Override
    public Completable connect(ExecutionContext ctx) {
        return Completable.defer(() ->
            ctx
                .request()
                .messages()
                .doOnNext(message -> log.info("Received message:\n" + message.content().toString()))
                .ignoreElements()
                .andThen(Completable.fromRunnable(() -> ctx.response().messages(generateMessageFlow())))
        );
    }

    private Flowable<Message> generateMessageFlow() {
        Flowable<Message> messageFlow = Flowable
            .interval(configuration.getMessageInterval(), TimeUnit.MILLISECONDS)
            .map(value -> configuration.getMessageContent() + " " + value)
            .map(DefaultMessage::new);
        if (configuration.getMessageCount() != null) {
            return messageFlow.limit(configuration.getMessageCount());
        }
        return messageFlow;
    }
}

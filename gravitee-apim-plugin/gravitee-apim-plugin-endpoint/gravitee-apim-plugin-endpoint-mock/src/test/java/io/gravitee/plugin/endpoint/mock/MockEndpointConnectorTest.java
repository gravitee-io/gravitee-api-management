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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.gateway.jupiter.api.context.ExecutionContext;
import io.gravitee.gateway.jupiter.api.context.Request;
import io.gravitee.gateway.jupiter.api.context.Response;
import io.gravitee.gateway.jupiter.api.message.DefaultMessage;
import io.gravitee.gateway.jupiter.api.message.Message;
import io.gravitee.plugin.endpoint.mock.configuration.MockEndpointConnectorConfiguration;
import io.reactivex.Flowable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

/**
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class MockEndpointConnectorTest {

    private static final String MESSAGE_TO_LOG = "message to log";
    private final MockEndpointConnectorConfiguration configuration = new MockEndpointConnectorConfiguration();

    @InjectMocks
    private MockEndpointConnector mockEndpointConnector;

    @Mock
    private ExecutionContext ctx;

    @Mock
    private Request request;

    @Mock
    private Response response;

    @Mock
    Logger logger;

    @BeforeEach
    public void setup() {
        configuration.setMessageInterval(100L);
        configuration.setMessageContent("test mock endpoint");
        mockEndpointConnector = new MockEndpointConnector(configuration);

        when(request.messages()).thenReturn(Flowable.just(new DefaultMessage(MESSAGE_TO_LOG)));

        when(ctx.request()).thenReturn(request);
        when(ctx.response()).thenReturn(response);
    }

    @Test
    @DisplayName("Should generate messages flow")
    void shouldGenerateMessagesFlow() {
        mockEndpointConnector.connect(ctx).test().assertComplete();

        ArgumentCaptor<Flowable<Message>> messagesCaptor = ArgumentCaptor.forClass(Flowable.class);
        verify(response).messages(messagesCaptor.capture());

        messagesCaptor
            .getValue()
            .test()
            .awaitCount(3)
            .assertValueCount(3)
            .assertValueAt(0, message -> message.content().toString().equals("test mock endpoint 0"))
            .assertValueAt(1, message -> message.content().toString().equals("test mock endpoint 1"))
            .assertValueAt(2, message -> message.content().toString().equals("test mock endpoint 2"));

        verify(logger).info("Received message:\n" + MESSAGE_TO_LOG);
    }

    @Test
    @DisplayName("Should generate messages flow with a limited count of messages")
    void shouldGenerateLimitedMessagesFlow() throws InterruptedException {
        configuration.setMessageCount(5L);

        mockEndpointConnector.connect(ctx).test().assertComplete();

        ArgumentCaptor<Flowable<Message>> messagesCaptor = ArgumentCaptor.forClass(Flowable.class);
        verify(response).messages(messagesCaptor.capture());

        messagesCaptor.getValue().test().await().assertValueCount(5);

        verify(logger).info("Received message:\n" + MESSAGE_TO_LOG);
    }
}

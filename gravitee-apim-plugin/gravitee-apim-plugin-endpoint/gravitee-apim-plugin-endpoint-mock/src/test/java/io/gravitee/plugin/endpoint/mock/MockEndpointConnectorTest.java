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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.gateway.jupiter.api.context.MessageExecutionContext;
import io.gravitee.gateway.jupiter.api.context.MessageResponse;
import io.gravitee.gateway.jupiter.api.message.Message;
import io.gravitee.plugin.endpoint.mock.configuration.MockEndpointConnectorConfiguration;
import io.reactivex.Flowable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class MockEndpointConnectorTest {

    private MockEndpointConnector mockEndpointConnector;

    @Mock
    private MessageExecutionContext messageExecutionContext;

    @Mock
    private MessageResponse messageResponse;

    @BeforeEach
    public void setup() {
        MockEndpointConnectorConfiguration configuration = new MockEndpointConnectorConfiguration();
        configuration.setMessagesInterval(1L);
        configuration.setMessagesContent("test mock endpoint");
        mockEndpointConnector = new MockEndpointConnector(configuration);
    }

    @Test
    public void shouldSendResponseMessages() {
        when(messageExecutionContext.response()).thenReturn(messageResponse);

        mockEndpointConnector.connect(messageExecutionContext).test().assertComplete();

        ArgumentCaptor<Flowable<Message>> messagesCaptor = ArgumentCaptor.forClass(Flowable.class);
        verify(messageResponse).messages(messagesCaptor.capture());

        messagesCaptor
            .getValue()
            .test()
            .awaitCount(3)
            .assertValueCount(3)
            .assertValueAt(0, message -> message.content().toString().equals("test mock endpoint 0"))
            .assertValueAt(1, message -> message.content().toString().equals("test mock endpoint 1"))
            .assertValueAt(2, message -> message.content().toString().equals("test mock endpoint 2"));
    }
}

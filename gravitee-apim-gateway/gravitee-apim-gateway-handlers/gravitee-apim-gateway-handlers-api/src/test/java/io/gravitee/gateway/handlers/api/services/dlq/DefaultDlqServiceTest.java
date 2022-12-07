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
package io.gravitee.gateway.handlers.api.services.dlq;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.gateway.jupiter.api.connector.endpoint.async.EndpointAsyncConnector;
import io.gravitee.gateway.jupiter.api.message.DefaultMessage;
import io.gravitee.gateway.jupiter.api.message.Message;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class DefaultDlqServiceTest {

    public static final int MESSAGES_COUNT = 100;

    @Mock
    private EndpointAsyncConnector endpointConnector;

    private DefaultDlqService cut;

    @BeforeEach
    void init() {
        cut = new DefaultDlqService(endpointConnector);
    }

    @Test
    void shouldPublishErrorMessagesToEndpoint() {
        final List<Message> messages = new ArrayList<>();

        for (int i = 0; i < MESSAGES_COUNT; i++) {
            messages.add(DefaultMessage.builder().error(i % 2 == 0).id("message-" + i).build());
        }

        when(endpointConnector.publish(any(DlqExecutionContext.class))).thenReturn(Completable.complete());
        final TestSubscriber<Message> obs = cut.apply(Flowable.fromIterable(messages)).test();

        obs.assertComplete();
        obs.assertValueCount(MESSAGES_COUNT);
        obs.assertValueSequence(messages);

        verify(endpointConnector).publish(any(DlqExecutionContext.class));
    }
}

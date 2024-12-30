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
package io.gravitee.gateway.reactive.core;

import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.reactive.api.message.DefaultMessage;
import io.gravitee.gateway.reactive.api.message.Message;
import io.gravitee.gateway.reactive.core.context.OnMessagesInterceptor;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.FlowableTransformer;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
class MessageFlowTest {

    @Test
    void shouldApplyMessageInterceptor() {
        final MessageFlow<Message> cut = new MessageFlow<>();
        final Function<FlowableTransformer<Message, Message>, FlowableTransformer<Message, Message>> interceptor = onMessages ->
            upstream -> upstream.doOnNext(message -> message.attribute("intercepted", true)).compose(onMessages);

        cut.messages(Flowable.just(DefaultMessage.builder().content("test").build()));
        OnMessagesInterceptor.MessagesInterceptor<Message> messagesInterceptor = OnMessagesInterceptor.MessagesInterceptor
            .<Message>builder()
            .id("id")
            .transformersFunction(interceptor)
            .build();
        cut.registerMessagesInterceptor(messagesInterceptor);

        cut.onMessages(upstream -> upstream.map(message -> message.content(Buffer.buffer("Transformed test"))));

        final TestSubscriber<Message> obs = cut.messages().test();

        obs.assertComplete();
        obs.assertValue(message -> message.<Boolean>attribute("intercepted") && message.content().toString().equals("Transformed test"));
    }

    @Test
    void shouldApplyOnMessagesWhenNoInterceptor() {
        final MessageFlow<Message> cut = new MessageFlow<>();

        cut.messages(Flowable.just(DefaultMessage.builder().content("test").build()));
        cut.onMessages(upstream -> upstream.map(message -> message.content(Buffer.buffer("Transformed test"))));

        final TestSubscriber<Message> obs = cut.messages().test();

        obs.assertComplete();
        obs.assertValue(message -> message.content().toString().equals("Transformed test"));
    }

    @Test
    void shouldUnsetOnMessagesInterceptor() {
        final MessageFlow<Message> cut = new MessageFlow<>();
        final Function<FlowableTransformer<Message, Message>, FlowableTransformer<Message, Message>> interceptor = onMessages ->
            upstream -> upstream.doOnNext(message -> message.attribute("intercepted", true)).compose(onMessages);

        cut.messages(Flowable.just(DefaultMessage.builder().content("test").build()));
        OnMessagesInterceptor.MessagesInterceptor<Message> messagesInterceptor = OnMessagesInterceptor.MessagesInterceptor
            .<Message>builder()
            .id("id")
            .transformersFunction(interceptor)
            .build();
        cut.registerMessagesInterceptor(messagesInterceptor);
        cut.unregisterMessagesInterceptor("id");
        cut.onMessages(upstream -> upstream.map(message -> message.content(Buffer.buffer("Transformed test"))));

        final TestSubscriber<Message> obs = cut.messages().test();

        obs.assertComplete();
        obs.assertValue(message -> message.attribute("intercepted") == null && message.content().toString().equals("Transformed test"));
    }
}

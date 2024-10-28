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

import io.gravitee.gateway.reactive.api.message.Message;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.FlowableTransformer;
import java.util.function.Function;
import lombok.NoArgsConstructor;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@NoArgsConstructor
public class MessageFlow<T extends Message> {

    private Function<FlowableTransformer<T, T>, FlowableTransformer<T, T>> onMessagesInterceptor;

    protected Flowable<T> messages = Flowable.empty();

    public MessageFlow(Flowable<T> messages) {
        this.messages = messages;
    }

    public Flowable<T> messages() {
        return messages;
    }

    public void messages(final Flowable<T> messages) {
        this.messages = messages;
    }

    public void onMessages(final FlowableTransformer<T, T> onMessages) {
        if (onMessagesInterceptor != null) {
            this.messages = this.messages.compose(onMessagesInterceptor.apply(onMessages));
        } else {
            this.messages = this.messages.compose(onMessages);
        }
    }

    public void setOnMessagesInterceptor(Function<FlowableTransformer<T, T>, FlowableTransformer<T, T>> interceptor) {
        this.onMessagesInterceptor = interceptor;
    }

    public void unsetOnMessagesInterceptor() {
        this.onMessagesInterceptor = null;
    }
}

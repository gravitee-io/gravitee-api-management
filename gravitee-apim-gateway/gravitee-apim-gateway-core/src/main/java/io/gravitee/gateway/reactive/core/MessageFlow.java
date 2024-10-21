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
import io.gravitee.gateway.reactive.core.context.OnMessagesInterceptor;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.FlowableTransformer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class MessageFlow<T extends Message> implements OnMessagesInterceptor<T> {

    private Map<String, MessagesInterceptor<T>> onMessagesInterceptorsMapping;
    private List<MessagesInterceptor<T>> messagesInterceptorsPrioritized;

    @Getter
    @Setter
    @Accessors(fluent = true)
    private Flowable<T> messages = Flowable.empty();

    public void onMessages(final FlowableTransformer<T, T> onMessages) {
        if (messagesInterceptorsPrioritized != null) {
            FlowableTransformer<T, T> composedTransformers = onMessages;
            for (MessagesInterceptor<T> messagesInterceptor : messagesInterceptorsPrioritized) {
                composedTransformers = messagesInterceptor.transformersFunction().apply(composedTransformers);
            }
            this.messages = this.messages.compose(composedTransformers);
        } else {
            this.messages = this.messages.compose(onMessages);
        }
    }

    @Override
    public void registerMessagesInterceptor(final MessagesInterceptor<T> messagesInterceptor) {
        if (this.messagesInterceptorsPrioritized == null) {
            this.messagesInterceptorsPrioritized = new ArrayList<>();
        }
        if (this.onMessagesInterceptorsMapping == null) {
            this.onMessagesInterceptorsMapping = new TreeMap<>();
        }
        this.onMessagesInterceptorsMapping.put(messagesInterceptor.id(), messagesInterceptor);

        this.messagesInterceptorsPrioritized.add(messagesInterceptor);
        this.messagesInterceptorsPrioritized.sort(Collections.reverseOrder(Comparator.comparingInt(MessagesInterceptor::priority)));
    }

    @Override
    public void unregisterMessagesInterceptor(final String id) {
        if (this.onMessagesInterceptorsMapping != null) {
            var removed = this.onMessagesInterceptorsMapping.remove(id);
            if (removed != null) {
                this.messagesInterceptorsPrioritized.remove(removed);
            }
        }
    }
}

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
package io.gravitee.gateway.reactive.core.context;

import io.gravitee.gateway.reactive.api.message.Message;
import io.reactivex.rxjava3.core.FlowableTransformer;
import java.util.function.Function;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface OnMessagesInterceptor {
    /**
     * Set an interceptor allowing to capture any call to {@link io.gravitee.gateway.reactive.api.context.base.BaseMessageRequest#onMessages(FlowableTransformer)} or {@link io.gravitee.gateway.reactive.api.context.base.BaseMessageResponse#onMessages(FlowableTransformer)}.
     * The captured {@link FlowableTransformer} will then be provided to the specified function which will be able to provide its own {@link FlowableTransformer}.
     * <p/>
     * This can be particularly useful to avoid, replace or compose the transformation set by a policy on the incoming messages.
     * <p/>
     * Ex: calculate the message content size before and after a policy is applied on a message.
     * <p/>
     * <pre>
     *      request.setOnMessagesInterceptor(onMessages ->
     *             messages -> {
     *                 final AtomicLong initialTotalSize = new AtomicLong(0);
     *                 final AtomicLong finalTotalSize = new AtomicLong(0);
     *                 return messages
     *                     .doOnNext(message -> initialTotalSize.addAndGet(message.content().length()))
     *                     .compose(onMessages)
     *                     .doOnNext(message -> finalTotalSize.addAndGet(message.content().length()));
     *             }
     *         );
     * </pre>
     *
     * @param interceptor the function taking the {@link FlowableTransformer} provided by the call to {@link io.gravitee.gateway.reactive.api.context.base.BaseMessageRequest#onMessages(FlowableTransformer)} or {@link io.gravitee.gateway.reactive.api.context.base.BaseMessageResponse#onMessages(FlowableTransformer)} and returns another {@link FlowableTransformer} as a replacement.
     */
    void setMessagesInterceptor(Function<FlowableTransformer<Message, Message>, FlowableTransformer<Message, Message>> interceptor);

    /**
     * Unset the <code>onMessages</code> interceptor.
     * Removing the interceptor has only effect on future calls to {@link io.gravitee.gateway.reactive.api.context.base.BaseMessageRequest#onMessages(FlowableTransformer)} or {@link io.gravitee.gateway.reactive.api.context.base.BaseMessageResponse#onMessages(FlowableTransformer)}.
     *
     * <b>WARN: </b> it is the responsibility of the caller to make sure the interceptor is set and unset at the right time to avoid any undesired side effects.
     */
    void unsetMessagesInterceptor();
}

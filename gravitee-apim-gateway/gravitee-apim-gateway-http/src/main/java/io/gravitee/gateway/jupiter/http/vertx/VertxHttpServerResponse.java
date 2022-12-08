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
package io.gravitee.gateway.jupiter.http.vertx;

import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.jupiter.api.context.GenericExecutionContext;
import io.gravitee.gateway.jupiter.api.message.Message;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import java.util.concurrent.atomic.AtomicReference;
import org.reactivestreams.Subscription;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class VertxHttpServerResponse extends AbstractVertxServerResponse {

    public VertxHttpServerResponse(final VertxHttpServerRequest vertxHttpServerRequest) {
        super(vertxHttpServerRequest);
    }

    @Override
    public Completable end(final GenericExecutionContext ctx) {
        return Completable.defer(
            () -> {
                if (((VertxHttpServerRequest) serverRequest).isWebSocketUpgraded()) {
                    return chunks().ignoreElements();
                }

                if (!opened()) {
                    return Completable.error(new IllegalStateException("The response is already ended"));
                }
                prepareHeaders();

                final AtomicReference<Subscription> subscriptionRef = new AtomicReference<>();

                if (lazyBufferFlow().hasChunks()) {
                    return nativeResponse
                        .rxSend(
                            chunks()
                                .doOnSubscribe(subscriptionRef::set)
                                .map(buffer -> io.vertx.rxjava3.core.buffer.Buffer.buffer(buffer.getNativeBuffer()))
                                .doOnNext(
                                    buffer ->
                                        ctx.metrics().setResponseContentLength(ctx.metrics().getResponseContentLength() + buffer.length())
                                )
                        )
                        .doOnDispose(() -> subscriptionRef.get().cancel());
                }

                return nativeResponse.rxEnd();
            }
        );
    }

    @Override
    public void messages(final Flowable<Message> messages) {
        super.messages(messages);

        // If message flow is set up, make sure any access to chunk buffers will not be possible anymore and returns empty.
        chunks(Flowable.empty());
    }
}

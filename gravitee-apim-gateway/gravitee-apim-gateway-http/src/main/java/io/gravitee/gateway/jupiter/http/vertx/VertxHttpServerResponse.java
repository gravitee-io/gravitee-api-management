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
import io.gravitee.gateway.jupiter.core.context.MutableResponse;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.FlowableTransformer;
import io.reactivex.Maybe;
import io.reactivex.MaybeTransformer;
import io.reactivex.Single;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class VertxHttpServerResponse extends AbstractVertxServerResponse implements MutableResponse {

    private final BodyChunksFlowable bodyChunksFlowable;

    public VertxHttpServerResponse(final VertxHttpServerRequest vertxHttpServerRequest) {
        super(vertxHttpServerRequest);
        bodyChunksFlowable = new BodyChunksFlowable();
    }

    @Override
    public Maybe<Buffer> body() {
        return bodyChunksFlowable.body();
    }

    @Override
    public Single<Buffer> bodyOrEmpty() {
        return bodyChunksFlowable.bodyOrEmpty();
    }

    @Override
    public void body(final Buffer buffer) {
        bodyChunksFlowable.body(buffer);
    }

    @Override
    public Completable onBody(final MaybeTransformer<Buffer, Buffer> onBody) {
        return bodyChunksFlowable.onBody(onBody);
    }

    @Override
    public Flowable<Buffer> chunks() {
        return bodyChunksFlowable.chunks();
    }

    @Override
    public void chunks(final Flowable<Buffer> chunks) {
        bodyChunksFlowable.chunks(chunks);
    }

    @Override
    public Completable onChunks(final FlowableTransformer<Buffer, Buffer> onChunks) {
        return bodyChunksFlowable.onChunks(onChunks);
    }

    @Override
    public Completable end() {
        return Completable.defer(
            () -> {
                if (((VertxHttpServerRequest) serverRequest).isWebSocketUpgraded()) {
                    return Completable.complete();
                }

                if (!opened()) {
                    return Completable.error(new IllegalStateException("The response is already ended"));
                }
                prepareHeaders();

                if (bodyChunksFlowable.chunks != null) {
                    return nativeResponse.rxSend(
                        chunks()
                            .map(buffer -> io.vertx.reactivex.core.buffer.Buffer.buffer(buffer.getNativeBuffer()))
                            .doOnNext(
                                buffer ->
                                    serverRequest
                                        .metrics()
                                        .setResponseContentLength(serverRequest.metrics().getResponseContentLength() + buffer.length())
                            )
                    );
                }

                return nativeResponse.rxEnd();
            }
        );
    }
}

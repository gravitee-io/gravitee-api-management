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
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.FlowableTransformer;
import io.reactivex.Maybe;
import io.reactivex.MaybeTransformer;
import io.reactivex.Single;
import java.util.function.Supplier;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class BodyChunksFlowable {

    protected Flowable<Buffer> chunks;
    protected Maybe<Buffer> cachedBuffer;

    public Maybe<Buffer> body() {
        this.chunks = chunksFromCache(() -> chunksOrEmpty().compose(chunksToCache()));

        return chunks.firstElement();
    }

    public Single<Buffer> bodyOrEmpty() {
        return body()
            .switchIfEmpty(
                Single.fromCallable(
                    () -> {
                        final Buffer emptyBuffer = Buffer.buffer();
                        cachedBuffer = Maybe.empty();
                        return emptyBuffer;
                    }
                )
            );
    }

    public void body(Buffer buffer) {
        cachedBuffer = Maybe.just(buffer);
        this.chunks = cachedBuffer.toFlowable();
    }

    public Completable onBody(MaybeTransformer<Buffer, Buffer> onBody) {
        this.chunks = bodyFromCache(this::reduceBody).compose(onBody).compose(bodyToCache()).toFlowable();
        return this.chunks.ignoreElements();
    }

    public Flowable<Buffer> chunks() {
        chunks = chunksFromCache(this::chunksOrEmpty);
        return chunks;
    }

    public void chunks(final Flowable<Buffer> chunks) {
        cachedBuffer = null;
        this.chunks = chunks;
    }

    public Completable onChunks(final FlowableTransformer<Buffer, Buffer> onChunks) {
        this.chunks = this.chunksFromCache(this::chunks).compose(onChunks).compose(chunksToCache());

        return this.chunks.ignoreElements();
    }

    private Flowable<Buffer> chunksOrEmpty() {
        if (this.chunks == null) {
            return Flowable.empty();
        }

        return chunks;
    }

    private Maybe<Buffer> reduceBody() {
        return chunksOrEmpty().reduce(Buffer::appendBuffer);
    }

    private MaybeTransformer<Buffer, Buffer> bodyToCache() {
        return upstream ->
            upstream
                .doOnComplete(() -> cachedBuffer = Maybe.empty())
                .doOnError(t -> cachedBuffer = Maybe.just(Buffer.buffer(t.getMessage())))
                .doOnSuccess(buffer -> cachedBuffer = Maybe.just(buffer));
    }

    /**
     * Tries to return the current body from the cached buffer if it has already been evaluated to avoid multiple subscriptions to the source observable.
     * Multiple subscriptions can occur during the request / response execution (EL expression accessing the body, track body for logging, track body for debug mode, ...).
     *
     * If no cache body exists yet, the <code>orElse</code> supplier will be used to retrieve the alternative body observable to use.
     *
     * @param orElse the {@link Supplier} with the body to return in case there is no cached body yet.
     * @return the current body.
     */
    private Maybe<Buffer> bodyFromCache(Supplier<Maybe<Buffer>> orElse) {
        Maybe<Buffer> body;

        if (cachedBuffer != null) {
            body = cachedBuffer;
            cachedBuffer = null;
        } else {
            body = orElse.get();
        }

        return body;
    }

    private Flowable<Buffer> chunksFromCache(Supplier<Flowable<Buffer>> orElse) {
        Flowable<Buffer> chunks;

        if (cachedBuffer != null) {
            chunks = cachedBuffer.toFlowable();
            cachedBuffer = null;
        } else {
            chunks = orElse.get();
        }

        return chunks;
    }

    private FlowableTransformer<Buffer, Buffer> chunksToCache() {
        return upstream -> upstream.reduce(Buffer::appendBuffer).compose(bodyToCache()).toFlowable();
    }
}

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
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class AbstractHttpChunks {

    private final AtomicBoolean requiredBodyCache = new AtomicBoolean(true);
    protected Flowable<Buffer> chunks;

    public Maybe<Buffer> body() {
        if (requiredBodyCache.compareAndSet(true, false)) {
            // Reduce all the chunks to create a unique buffer containing all the content.
            this.chunks = reduceBody().toFlowable().cache();
        }
        return chunks().firstElement();
    }

    public Single<Buffer> bodyOrEmpty() {
        return body().switchIfEmpty(Single.just(Buffer.buffer()));
    }

    public void body(Buffer buffer) {
        this.requiredBodyCache.set(true);
        this.chunks = chunks().compose(upstream -> upstream.ignoreElements().andThen(Flowable.just(buffer)));
    }

    public Completable onBody(MaybeTransformer<Buffer, Buffer> onBody) {
        this.chunks = reduceBody().compose(onBody).toFlowable().cache();
        return this.chunks.ignoreElements();
    }

    public Flowable<Buffer> chunks() {
        if (this.chunks == null) {
            this.chunks = Flowable.empty();
        }
        return chunks;
    }

    public void chunks(final Flowable<Buffer> chunks) {
        this.requiredBodyCache.set(true);
        this.chunks = chunks;
    }

    public Completable onChunk(final FlowableTransformer<Buffer, Buffer> chunkTransformer) {
        this.chunks = chunks().compose(chunkTransformer).cache();
        return this.chunks.ignoreElements();
    }

    private Maybe<Buffer> reduceBody() {
        return chunks().reduce(Buffer::appendBuffer);
    }
}

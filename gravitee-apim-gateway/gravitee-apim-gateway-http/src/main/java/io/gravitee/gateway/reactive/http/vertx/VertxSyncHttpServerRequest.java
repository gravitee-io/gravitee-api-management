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
package io.gravitee.gateway.reactive.http.vertx;

import io.gravitee.common.http.IdGenerator;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.reactive.api.context.sync.SyncRequest;
import io.reactivex.*;
import io.vertx.reactivex.core.http.HttpServerRequest;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class VertxSyncHttpServerRequest extends AbstractVertxHttpServerRequest<Buffer> implements SyncRequest {

    public VertxSyncHttpServerRequest(HttpServerRequest httpServerRequest, String contextPath, IdGenerator idGenerator) {
        super(httpServerRequest, contextPath, idGenerator);
        // Make sure that any subscription to the request body will be cached to avoid multiple consumptions.
        this.content =
            nativeRequest
                .toFlowable()
                .doOnNext(buffer -> metrics.setRequestContentLength(metrics.getRequestContentLength() + buffer.length()))
                .map(Buffer::buffer)
                .cache();
    }

    @Override
    public Maybe<Buffer> getBody() {
        return content.reduce(Buffer::appendBuffer);
    }

    @Override
    public Single<Buffer> getBodyOrEmpty() {
        // Reduce all the chunks to create a unique buffer containing all the content.
        return getBody().switchIfEmpty(Single.just(Buffer.buffer()));
    }

    @Override
    public Flowable<Buffer> getChunkedBody() {
        return content;
    }

    @Override
    public Completable onChunk(FlowableTransformer<Buffer, Buffer> chunkTransformer) {
        content = content.compose(chunkTransformer);

        return Completable.complete();
    }

    @Override
    public Completable onBody(MaybeTransformer<Buffer, Buffer> bodyTransformer) {
        return setBody(getBody().compose(bodyTransformer));
    }

    @Override
    public Completable setBody(Maybe<Buffer> buffer) {
        return setChunks(buffer.toFlowable());
    }

    @Override
    public Completable setBody(Buffer buffer) {
        return setChunks(Flowable.just(buffer));
    }

    @Override
    public synchronized Completable setChunkedBody(final Flowable<Buffer> chunks) {
        return setChunks(chunks);
    }

    @Override
    public Flowable<Buffer> content() {
        return content;
    }

    @Override
    public Completable content(Flowable<Buffer> content) {
        return setChunks(content);
    }

    private synchronized Completable setChunks(Flowable<Buffer> chunks) {
        return onChunk(upstream -> chunks);
    }
}

package io.gravitee.gateway.reactive.http.vertx;

import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.reactive.api.context.sync.SyncResponse;
import io.reactivex.*;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class VertxSyncHttpServerResponse extends AbstractVertxHttpServerResponse<Buffer> implements SyncResponse {

    public VertxSyncHttpServerResponse(AbstractVertxHttpServerRequest<Buffer> serverRequest) {
        super(serverRequest);
    }

    @Override
    public Flowable<Buffer> content() {
        if (this.content == null) {
            this.content = Flowable.empty();
        }

        return this.content;
    }

    @Override
    public Completable content(Flowable<Buffer> content) {
        return setChunks(content);
    }

    @Override
    public Completable onBuffer(FlowableTransformer<Buffer, Buffer> bufferTransformer) {
        return null;
    }

    @Override
    public Completable onBody(FlowableTransformer<Buffer, Buffer> bodyTransformer) {
        return null;
    }

    @Override
    public synchronized Maybe<Buffer> getBody() {
        // Reduce all the chunks to create a unique buffer containing all the content.
        final Maybe<Buffer> buffer = content().reduce(Buffer::appendBuffer);
        this.content = buffer.toFlowable();
        return buffer;
    }

    @Override
    public Single<Buffer> getBodyOrEmpty() {
        return getBody().switchIfEmpty(Single.just(Buffer.buffer()));
    }

    @Override
    public Flowable<Buffer> getChunkedBody() {
        return content();
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
    public Completable setChunkedBody(final Flowable<Buffer> chunks) {
        return setChunks(chunks);
    }

    @Override
    public Completable end() {
        if (!valid()) {
            return Completable.error(new IllegalStateException("The response is already ended"));
        }

        if (!nativeResponse.headWritten()) {
            writeHeaders();
        }

        if (content != null) {
            return nativeResponse.rxSend(
                content
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

    private synchronized Completable setChunks(Flowable<Buffer> chunks) {
        if (chunks != null) {
            // Current chunks need to be drained before being replaced.
            this.content = content().ignoreElements().andThen(chunks);
        }

        return Completable.complete();
    }
}

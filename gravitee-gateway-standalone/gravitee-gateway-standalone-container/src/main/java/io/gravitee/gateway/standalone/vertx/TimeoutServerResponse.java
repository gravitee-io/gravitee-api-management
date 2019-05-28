package io.gravitee.gateway.standalone.vertx;

import io.gravitee.common.http.HttpHeaders;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.stream.WriteStream;
import io.vertx.core.Vertx;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class TimeoutServerResponse implements Response {

    private final Vertx vertx;

    private final Response response;

    private final long timerId;

    public TimeoutServerResponse(final Vertx vertx, final Response response, final long timerId) {
        this.vertx = vertx;
        this.response = response;
        this.timerId = timerId;
    }

    @Override
    public Response status(int i) {
        return response.status(i);
    }

    @Override
    public int status() {
        return response.status();
    }

    @Override
    public HttpHeaders headers() {
        return response.headers();
    }

    @Override
    public boolean ended() {
        return response.ended();
    }

    @Override
    public WriteStream<Buffer> write(Buffer buffer) {
        return response.write(buffer);
    }

    @Override
    public void end() {
        response.end();

        release();
    }

    @Override
    public void end(Buffer buffer) {
        response.end(buffer);

        release();
    }

    private void release() {
        vertx.cancelTimer(timerId);
    }

    @Override
    public WriteStream<Buffer> drainHandler(Handler<Void> drainHandler) {
        return response.drainHandler(drainHandler);
    }

    @Override
    public boolean writeQueueFull() {
        return response.writeQueueFull();
    }
}

package io.gravitee.gateway.reactive.handlers.api.adapter.context;

import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.api.stream.WriteStream;
import io.gravitee.gateway.reactive.api.context.Response;
import io.reactivex.Completable;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ResponseAdapter implements io.gravitee.gateway.api.Response {

    private final Response<?> response;

    public ResponseAdapter(Response<?> response) {
        this.response = response;
    }

    @Override
    public io.gravitee.gateway.api.Response status(int statusCode) {
        response.status(statusCode);
        return this;
    }

    @Override
    public int status() {
        return response.status();
    }

    @Override
    public String reason() {
        return response.reason();
    }

    @Override
    public io.gravitee.gateway.api.Response reason(String message) {
        response.reason(message);
        return this;
    }

    @Override
    public HttpHeaders headers() {
        return response.headers();
    }

    @Override
    public HttpHeaders trailers() {
        return response.trailers();
    }

    @Override
    public boolean ended() {
        return response.ended();
    }

    @Override
    public WriteStream<Buffer> write(Buffer content) {
        return this;
    }
}

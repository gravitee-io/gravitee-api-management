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
package io.gravitee.gateway.standalone.vertx;

import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.api.http2.HttpFrame;
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
    public String reason() {
        return response.reason();
    }

    @Override
    public Response reason(String reason) {
        return response.reason(reason);
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
    public HttpHeaders trailers() {
        return response.trailers();
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

    @Override
    public Response endHandler(Handler<Void> endHandler) {
        return response.endHandler(endHandler);
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

    @Override
    public Response writeCustomFrame(HttpFrame frame) {
        return response.writeCustomFrame(frame);
    }
}

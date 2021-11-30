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
package io.gravitee.gateway.http.vertx;

import io.gravitee.common.http.HttpVersion;
import io.gravitee.common.http.IdGenerator;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.http2.HttpFrame;
import io.vertx.core.http.HttpServerRequest;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class VertxHttp2ServerRequest extends VertxHttpServerRequest {

    public VertxHttp2ServerRequest(HttpServerRequest httpServerRequest, IdGenerator idGenerator) {
        super(httpServerRequest, idGenerator);
    }

    @Override
    public HttpVersion version() {
        return HttpVersion.HTTP_2;
    }

    @Override
    public Request customFrameHandler(Handler<HttpFrame> frameHandler) {
        getNativeServerRequest()
            .customFrameHandler(
                frame -> frameHandler.handle(HttpFrame.create(frame.type(), frame.flags(), Buffer.buffer(frame.payload().getBytes())))
            );

        return this;
    }

    @Override
    public Response create() {
        return new VertxHttp2ServerResponse(this);
    }
}

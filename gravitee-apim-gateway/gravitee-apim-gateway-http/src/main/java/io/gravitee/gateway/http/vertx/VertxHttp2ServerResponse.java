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

import io.gravitee.common.http.HttpHeadersValues;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.http2.HttpFrame;
import io.vertx.core.http.HttpHeaders;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class VertxHttp2ServerResponse extends VertxHttpServerResponse {

    public VertxHttp2ServerResponse(final VertxHttp2ServerRequest serverRequest) {
        super(serverRequest);
    }

    @Override
    public Response writeCustomFrame(HttpFrame frame) {
        serverResponse.writeCustomFrame(frame.type(), frame.flags(), io.vertx.core.buffer.Buffer.buffer(frame.payload().getBytes()));

        return this;
    }

    protected void writeHeaders() {
        if (
            headers.contains(HttpHeaders.CONNECTION) &&
            headers.getAll(HttpHeaders.CONNECTION).contains(HttpHeadersValues.CONNECTION_GO_AWAY)
        ) {
            // 'Connection: goAway' is a special header indicating the native connection should be shutdown because of the node itself will shutdown.
            this.getNativeConnection().shutdown();
        }

        // As per https://tools.ietf.org/html/rfc7540#section-8.1.2.2
        // connection-specific header fields must be remove from response headers
        headers.remove(HttpHeaders.CONNECTION).remove(HttpHeaders.KEEP_ALIVE).remove(HttpHeaders.TRANSFER_ENCODING);
    }
}

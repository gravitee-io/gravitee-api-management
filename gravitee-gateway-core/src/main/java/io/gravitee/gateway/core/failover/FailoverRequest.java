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
package io.gravitee.gateway.core.failover;

import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.RequestWrapper;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.stream.ReadStream;

/**
 * The failover request is defined to store the incoming request body into a buffer which would be reusable in case
 * of retry / failover to an other endpoint.
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
class FailoverRequest extends RequestWrapper {

    private Buffer buffer;
    private boolean resumed = false;
    private Handler<Buffer> bodyHandler;
    private Handler<Void> endHandler;

    FailoverRequest(Request request) {
        super(request);
    }

    @Override
    public ReadStream<Buffer> bodyHandler(Handler<Buffer> bodyHandler) {
        this.bodyHandler = bodyHandler;

        request.bodyHandler(
            result -> {
                if (buffer == null) {
                    buffer = Buffer.buffer();
                }
                buffer.appendBuffer(result);
                bodyHandler.handle(result);
            }
        );

        return this;
    }

    @Override
    public ReadStream<Buffer> endHandler(Handler<Void> endHandler) {
        this.endHandler = endHandler;

        request.endHandler(endHandler);
        return this;
    }

    /**
     * <code>resume</code> method may be called multiple times depending on the failover max-retry configuration.
     * At the very first call, the underlying / wrapped request is normally resumed. For the next calls, we are simply
     * pushing the buffer content and then call end to signal the end of the stream.
     */
    @Override
    public ReadStream<Buffer> resume() {
        if (!resumed) {
            request.resume();
            resumed = true;
        } else {
            if (bodyHandler != null && buffer != null) {
                bodyHandler.handle(buffer);
            }
            endHandler.handle(null);
        }

        return this;
    }
}

/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.debug.vertx;

import io.gravitee.common.http.IdGenerator;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.http.vertx.VertxHttpServerRequest;
import io.gravitee.gateway.http.vertx.VertxHttpServerRequestOptions;

public class VertxHttpServerRequestDebugDecorator extends VertxHttpServerRequest {

    private final VertxHttpServerRequest delegate;

    /**
     * Rebuilds itself from the native request, so anything the dispatcher decided about the delegate
     * has to be carried across explicitly — otherwise this decorator silently reverts it.
     *
     * <p>That is not hypothetical: the path is resolved before the listener is chosen, and until the
     * options below were passed on, a debug session running in V3 execution mode reported the path
     * as received while the request it described had been routed on the resolved one.
     */
    public VertxHttpServerRequestDebugDecorator(VertxHttpServerRequest delegate, IdGenerator idGenerator) {
        super(
            delegate.getNativeServerRequest(),
            idGenerator,
            VertxHttpServerRequestOptions.builder().path(delegate.path()).timestamp(delegate.timestamp()).build()
        );
        this.delegate = delegate;
    }

    @Override
    public Response createResponse() {
        return new VertxHttpServerResponseDebugDecorator(delegate);
    }
}

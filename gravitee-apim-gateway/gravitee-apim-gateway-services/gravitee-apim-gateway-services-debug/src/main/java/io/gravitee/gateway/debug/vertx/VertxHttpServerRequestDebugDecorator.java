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
package io.gravitee.gateway.debug.vertx;

import io.gravitee.common.http.IdGenerator;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.http.vertx.VertxHttpServerRequest;

public class VertxHttpServerRequestDebugDecorator extends VertxHttpServerRequest {

    private final VertxHttpServerRequest delegate;

    public VertxHttpServerRequestDebugDecorator(VertxHttpServerRequest delegate, IdGenerator idGenerator) {
        super(delegate.getNativeServerRequest(), idGenerator);
        this.delegate = delegate;
    }

    @Override
    public Response create() {
        return new VertxHttpServerResponseDebugDecorator(delegate);
    }
}

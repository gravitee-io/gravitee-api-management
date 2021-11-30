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
package io.gravitee.gateway.core.processor;

import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.stream.ReadStream;
import io.gravitee.gateway.api.stream.WriteStream;

public class EmptyStreamableProcessor<T, S> extends AbstractStreamableProcessor<T, S> {

    private Handler<S> bodyHandler;
    private Handler<Void> endHandler;

    @Override
    public void handle(T t) {
        // Do nothing and move to next processor
        next.handle(t);
    }

    @Override
    public ReadStream<S> bodyHandler(Handler<S> handler) {
        this.bodyHandler = handler;
        return this;
    }

    @Override
    public ReadStream<S> endHandler(Handler<Void> handler) {
        this.endHandler = handler;
        return this;
    }

    @Override
    public WriteStream<S> write(S data) {
        bodyHandler.handle(data);
        return this;
    }

    @Override
    public void end() {
        endHandler.handle(null);
    }
}

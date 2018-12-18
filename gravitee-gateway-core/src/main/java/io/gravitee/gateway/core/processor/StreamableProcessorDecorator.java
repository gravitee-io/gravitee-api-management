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

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class StreamableProcessorDecorator<T, S> extends AbstractStreamableProcessor<T, S> {

    private final Processor<T> processor;
    private Handler<Void> endHandler;
    private Handler<S> bodyHandler;

    public StreamableProcessorDecorator(Processor<T> processor) {
        this.processor = processor;
    }

    @Override
    public void handle(T data) {
        this.processor.handle(data);
    }

    @Override
    public StreamableProcessor<T, S> handler(Handler<T> handler) {
        this.processor.handler(handler);
        return this;
    }

    @Override
    public StreamableProcessor<T, S> errorHandler(Handler<ProcessorFailure> errorHandler) {
        this.processor.errorHandler(errorHandler);
        return this;
    }

    @Override
    public StreamableProcessor<T, S> exitHandler(Handler<Void> exitHandler) {
        this.processor.exitHandler(exitHandler);
        return this;
    }

    @Override
    public ReadStream<S> bodyHandler(Handler<S> bodyHandler) {
        this.bodyHandler = bodyHandler;
        return this;
    }

    @Override
    public ReadStream<S> endHandler(Handler<Void> endHandler) {
        this.endHandler = endHandler;
        return this;
    }

    @Override
    public WriteStream<S> write(S content) {
        bodyHandler.handle(content);
        return this;
    }

    @Override
    public void end() {
        endHandler.handle(null);
    }
}

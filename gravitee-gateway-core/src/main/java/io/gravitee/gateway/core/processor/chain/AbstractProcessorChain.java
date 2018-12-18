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
package io.gravitee.gateway.core.processor.chain;

import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.core.processor.Processor;
import io.gravitee.gateway.core.processor.ProcessorFailure;

import java.util.Iterator;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractProcessorChain<T, P extends Processor<T>> implements ProcessorChain<T, P>, Iterator<P> {

    P last;
    protected Handler<T> resultHandler;
    protected Handler<Void> exitHandler;
    protected Handler<ProcessorFailure> errorHandler;

    @Override
    public void handle(T data) {
        if (hasNext()) {
            P processor = next(data);
            last = processor;
            processor
                    .handler(__ -> handle(data))
                    .errorHandler(failure -> errorHandler.handle(failure))
                    .exitHandler(stream -> exitHandler.handle(null))
                    .handle(data);
        } else {
            resultHandler.handle(data);
        }
    }

    @Override
    public ProcessorChain<T, P> handler(Handler<T> handler) {
        this.resultHandler = handler;
        return this;
    }

    @Override
    public ProcessorChain<T, P> errorHandler(Handler<ProcessorFailure> errorHandler) {
        this.errorHandler = errorHandler;
        return this;
    }

    @Override
    public ProcessorChain<T, P> exitHandler(Handler<Void> exitHandler) {
        this.exitHandler = exitHandler;
        return this;
    }

    protected abstract P next(T data);
}

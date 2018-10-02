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

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractStreamableProcessor<T> extends AbstractProcessor<T> implements StreamableProcessor<T> {

    protected Handler<ProcessorFailure> streamErrorHandler;

    @Override
    public StreamableProcessor<T> handler(Handler<T> handler) {
        this.handler = handler;
        return this;
    }

    @Override
    public StreamableProcessor<T> errorHandler(Handler<ProcessorFailure> errorHandler) {
        this.errorHandler = errorHandler;
        return this;
    }

    @Override
    public StreamableProcessor<T> streamErrorHandler(Handler<ProcessorFailure> streamErrorHandler) {
        this.streamErrorHandler = streamErrorHandler;
        return this;
    }

    @Override
    public StreamableProcessor<T> exitHandler(Handler<T> exitHandler) {
        this.exitHandler = exitHandler;
        return this;
    }
}

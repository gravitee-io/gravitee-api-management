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
public abstract class AbstractProcessor<T> implements Processor<T> {

    protected Handler<T> next;
    protected Handler<Void> exitHandler;
    protected Handler<ProcessorFailure> errorHandler;

    @Override
    public Processor<T> handler(Handler<T> handler) {
        this.next = handler;
        return this;
    }

    @Override
    public Processor<T> errorHandler(Handler<ProcessorFailure> errorHandler) {
        this.errorHandler = errorHandler;
        return this;
    }

    @Override
    public Processor<T> exitHandler(Handler<Void> exitHandler) {
        this.exitHandler = exitHandler;
        return this;
    }
}

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
import io.gravitee.gateway.api.stream.ReadWriteStream;
import io.gravitee.gateway.core.processor.ProcessorFailure;
import io.gravitee.gateway.core.processor.StreamableProcessor;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface StreamableProcessorChain<T, S, P extends StreamableProcessor<T, S>>
        extends ProcessorChain<T, P>, ReadWriteStream<S>, StreamableProcessor<T, S> {

    @Override
    StreamableProcessorChain<T, S, P> handler(Handler<T> handler);

    @Override
    StreamableProcessorChain<T, S, P> errorHandler(Handler<ProcessorFailure> handler);

    StreamableProcessorChain<T, S, P> streamErrorHandler(Handler<ProcessorFailure> handler);

    @Override
    StreamableProcessorChain<T, S, P> exitHandler(Handler<Void> handler);
}

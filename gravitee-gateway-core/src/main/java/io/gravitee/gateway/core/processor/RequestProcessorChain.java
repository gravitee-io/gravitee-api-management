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

import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.stream.ReadStream;
import io.gravitee.gateway.api.stream.WriteStream;

import java.util.Iterator;
import java.util.List;

/**
 * A {@link RequestProcessorChain} container used to run multiples {@link Processor}
 * while handling a request.
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class RequestProcessorChain extends AbstractStreamableProcessor<Buffer> {

    private final Iterator<StreamableProcessor<Buffer>> iterator;
    private StreamableProcessor<Buffer> lastProcessor;

    public RequestProcessorChain(List<StreamableProcessor<Buffer>> processors) {
        this.iterator = processors.iterator();
    }

    @Override
    public ReadStream<Buffer> bodyHandler(Handler<Buffer> handler) {
        return lastProcessor.bodyHandler(handler);
    }

    @Override
    public ReadStream<Buffer> endHandler(Handler<Void> handler) {
        return lastProcessor.endHandler(handler);
    }

    @Override
    public WriteStream<Buffer> write(Buffer buffer) {
        return lastProcessor.write(buffer);
    }

    @Override
    public void end() {
        lastProcessor.end();
    }

    @Override
    public void process(ProcessorContext context) {
        if (iterator.hasNext()) {
            StreamableProcessor<Buffer> next = iterator.next();
            lastProcessor = next;
            next
                    .handler(buffer -> process(context))
                    .errorHandler(failure -> errorHandler.handle(failure))
                    .streamErrorHandler(failure -> streamErrorHandler.handle(failure))
                    .process(context);
        } else {
            handler.handle(null);
        }
    }
}

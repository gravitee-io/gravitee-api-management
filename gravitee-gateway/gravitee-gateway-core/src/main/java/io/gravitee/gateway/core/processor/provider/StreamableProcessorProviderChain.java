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
package io.gravitee.gateway.core.processor.provider;

import io.gravitee.gateway.core.processor.StreamableProcessor;
import io.gravitee.gateway.core.processor.chain.AbstractStreamableProcessorChain;
import java.util.Iterator;
import java.util.List;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class StreamableProcessorProviderChain<T, S> extends AbstractStreamableProcessorChain<T, S, StreamableProcessor<T, S>> {

    private final Iterator<ProcessorProvider<T, StreamableProcessor<T, S>>> ite;

    public StreamableProcessorProviderChain(List<ProcessorProvider<T, StreamableProcessor<T, S>>> providers) {
        this.ite = providers.iterator();
    }

    @Override
    protected StreamableProcessor<T, S> next(T data) {
        return ite.next().provide(data);
    }

    @Override
    public boolean hasNext() {
        return ite.hasNext();
    }

    @Override
    public StreamableProcessor<T, S> next() {
        throw new IllegalStateException();
    }
}

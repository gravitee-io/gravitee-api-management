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

import io.gravitee.gateway.core.processor.Processor;
import io.gravitee.gateway.core.processor.chain.AbstractProcessorChain;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ProcessorProviderChain<T> extends AbstractProcessorChain<T, Processor<T>> {

    private final Iterator<ProcessorProvider<T, Processor<T>>> ite;
    private Iterator<Processor<T>> iteProcessor;
    private final List<Processor<T>> processors;

    public ProcessorProviderChain(List<ProcessorProvider<T, Processor<T>>> providers) {
        this.ite = providers.iterator();
        this.processors = new ArrayList<>(providers.size());
    }

    @Override
    protected Processor<T> next(T data) {
        Processor<T> processor = ite.next().provide(data);
        processors.add(processor);
        return processor;
    }

    @Override
    public boolean hasNext() {
        boolean hasNext = ite.hasNext();
        if (! hasNext && iteProcessor == null) {
            iteProcessor = processors.iterator();
            return hasNext;
        } else if (!hasNext) {
            return iteProcessor.hasNext();
        }

        return hasNext;
    }

    @Override
    public Processor<T> next() {
        return this.iteProcessor.next();
    }
}

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
package io.gravitee.gateway.reactive.reactor.processor;

import io.gravitee.gateway.reactive.api.context.ExecutionContext;
import io.gravitee.gateway.reactive.api.context.RequestExecutionContext;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ProcessorChain {

    protected final String id;
    private final Logger log = LoggerFactory.getLogger(ProcessorChain.class);
    protected Flowable<Processor> processors;

    public ProcessorChain(String id, List<Processor> processors) {
        this.id = id;
        this.processors = Flowable.fromIterable(processors);
    }

    public Completable execute(RequestExecutionContext ctx) {
        log.debug("Executing processor chain {}", id);

        return processors.flatMapCompletable(processor -> next(ctx, processor), false, 1);
    }

    private Completable next(RequestExecutionContext ctx, Processor processor) {
        if (ctx.isInterrupted()) {
            return Completable.complete();
        }

        log.debug("Executing processor {}", processor.getId());

        return processor.execute(ctx);
    }

    public String getId() {
        return id;
    }
}

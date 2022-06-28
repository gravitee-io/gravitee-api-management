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
package io.gravitee.gateway.jupiter.core.processor;

import io.gravitee.gateway.jupiter.api.ExecutionPhase;
import io.gravitee.gateway.jupiter.api.context.RequestExecutionContext;
import io.gravitee.gateway.jupiter.api.hook.Hookable;
import io.gravitee.gateway.jupiter.api.hook.ProcessorHook;
import io.gravitee.gateway.jupiter.core.hook.HookHelper;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ProcessorChain implements Hookable<ProcessorHook> {

    private final Logger log = LoggerFactory.getLogger(ProcessorChain.class);
    private final String id;
    private final Flowable<Processor> processors;
    private List<ProcessorHook> processorHooks;

    public ProcessorChain(final String id, final List<Processor> processors) {
        this.id = id;
        this.processors = processors != null ? Flowable.fromIterable(processors) : Flowable.empty();
    }

    @Override
    public void addHooks(List<ProcessorHook> hooks) {
        if (this.processorHooks == null) {
            this.processorHooks = new ArrayList<>();
        }
        processorHooks.addAll(hooks);
    }

    public Completable execute(final RequestExecutionContext ctx, final ExecutionPhase phase) {
        return processors
            .doOnSubscribe(subscription -> log.debug("Executing processor chain {}", id))
            .flatMapCompletable(processor -> executeNext(ctx, processor, phase), false, 1);
    }

    private Completable executeNext(final RequestExecutionContext ctx, final Processor processor, final ExecutionPhase phase) {
        log.debug("Executing processor {}", processor.getId());
        return HookHelper.hook(() -> processor.execute(ctx), processor.getId(), processorHooks, ctx, phase);
    }

    public String getId() {
        return id;
    }
}

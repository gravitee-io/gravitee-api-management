/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.reactive.core.processor;

import io.gravitee.gateway.reactive.api.ExecutionPhase;
import io.gravitee.gateway.reactive.api.hook.Hookable;
import io.gravitee.gateway.reactive.api.hook.ProcessorHook;
import io.gravitee.gateway.reactive.core.context.HttpExecutionContextInternal;
import io.gravitee.gateway.reactive.core.hook.HookHelper;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public class ProcessorChain implements Hookable<ProcessorHook> {

    private final String id;
    private final Flowable<Processor> processors;
    private List<ProcessorHook> processorHooks;

    public ProcessorChain(final String id, final List<Processor> processors) {
        this.id = id;
        this.processors = processors != null ? Flowable.fromIterable(processors) : Flowable.empty();
    }

    public ProcessorChain(final String id, final List<Processor> processors, final List<ProcessorHook> hooks) {
        this.id = id;
        this.processors = processors != null ? Flowable.fromIterable(processors) : Flowable.empty();
        this.addHooks(hooks);
    }

    @Override
    public void addHooks(List<ProcessorHook> hooks) {
        if (this.processorHooks == null) {
            this.processorHooks = new ArrayList<>();
        }
        processorHooks.addAll(hooks);
    }

    public Completable execute(final HttpExecutionContextInternal ctx, final ExecutionPhase phase) {
        return processors.concatMapCompletable(processor -> executeNext(ctx, processor, phase));
    }

    private Completable executeNext(final HttpExecutionContextInternal ctx, final Processor processor, final ExecutionPhase phase) {
        log.debug("Executing processor {} in processor chain {}", processor.getId(), id);
        return HookHelper.hook(() -> processor.execute(ctx), processor.getId(), processorHooks, ctx, phase);
    }

    public String getId() {
        return id;
    }
}

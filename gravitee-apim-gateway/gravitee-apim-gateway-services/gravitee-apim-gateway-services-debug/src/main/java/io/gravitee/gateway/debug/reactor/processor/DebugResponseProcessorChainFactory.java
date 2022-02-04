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
package io.gravitee.gateway.debug.reactor.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.core.processor.Processor;
import io.gravitee.gateway.reactor.processor.ResponseProcessorChainFactory;
import io.gravitee.repository.management.api.EventRepository;
import java.util.List;

public class DebugResponseProcessorChainFactory extends ResponseProcessorChainFactory {

    private final EventRepository eventRepository;
    private final ObjectMapper objectMapper;

    public DebugResponseProcessorChainFactory(EventRepository eventRepository, ObjectMapper objectMapper) {
        this.eventRepository = eventRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    protected List<Processor<ExecutionContext>> getProcessors() {
        final List<Processor<ExecutionContext>> processors = super.getProcessors();
        processors.add(new DebugEventCompletionProcessor(eventRepository, objectMapper));
        return processors;
    }
}

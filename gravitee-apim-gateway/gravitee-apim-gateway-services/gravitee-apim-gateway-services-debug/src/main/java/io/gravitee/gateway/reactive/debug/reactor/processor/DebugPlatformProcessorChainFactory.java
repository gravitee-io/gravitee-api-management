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
package io.gravitee.gateway.reactive.debug.reactor.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.reactive.core.connection.ConnectionDrainManager;
import io.gravitee.gateway.reactive.core.processor.Processor;
import io.gravitee.gateway.reactive.reactor.processor.DefaultPlatformProcessorChainFactory;
import io.gravitee.gateway.reactive.reactor.processor.transaction.TransactionPreProcessorFactory;
import io.gravitee.gateway.report.ReporterService;
import io.gravitee.node.api.Node;
import io.gravitee.plugin.alert.AlertEventProducer;
import io.gravitee.repository.management.api.EventRepository;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DebugPlatformProcessorChainFactory extends DefaultPlatformProcessorChainFactory {

    private final EventRepository eventRepository;
    private final ObjectMapper objectMapper;

    public DebugPlatformProcessorChainFactory(
        final TransactionPreProcessorFactory transactionHandlerFactory,
        final boolean traceContext,
        final boolean xForwardProcessor,
        final ReporterService reporterService,
        final AlertEventProducer eventProducer,
        final Node node,
        final String port,
        final boolean tracing,
        final GatewayConfiguration gatewayConfiguration,
        final EventRepository eventRepository,
        final ObjectMapper objectMapper,
        final ConnectionDrainManager connectionDrainManager,
        final MeterRegistry meterRegistry
    ) {
        super(
            transactionHandlerFactory,
            traceContext,
            xForwardProcessor,
            reporterService,
            eventProducer,
            node,
            port,
            gatewayConfiguration,
            connectionDrainManager,
            meterRegistry
        );
        this.eventRepository = eventRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    protected List<Processor> buildPostProcessorList() {
        List<Processor> processorList = super.buildPostProcessorList();
        processorList.add(new DebugCompletionProcessor(eventRepository, objectMapper));
        return processorList;
    }
}

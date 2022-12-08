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
package io.gravitee.gateway.jupiter.reactor.processor;

import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.jupiter.core.processor.Processor;
import io.gravitee.gateway.jupiter.reactor.processor.forward.XForwardForProcessor;
import io.gravitee.gateway.jupiter.reactor.processor.metrics.MetricsProcessor;
import io.gravitee.gateway.jupiter.reactor.processor.tracing.TraceContextProcessor;
import io.gravitee.gateway.jupiter.reactor.processor.transaction.TransactionProcessorFactory;
import io.gravitee.gateway.report.ReporterService;
import io.gravitee.node.api.Node;
import io.gravitee.plugin.alert.AlertEventProducer;
import java.util.ArrayList;
import java.util.List;

/**
 * The default {@link AbstractPlatformProcessorChainFactory} used with default http dispatcher
 *
 * @author GraviteeSource Team
 */
public class DefaultPlatformProcessorChainFactory extends AbstractPlatformProcessorChainFactory {

    private final boolean traceContext;
    private final GatewayConfiguration gatewayConfiguration;

    public DefaultPlatformProcessorChainFactory(
        TransactionProcessorFactory transactionHandlerFactory,
        boolean traceContext,
        ReporterService reporterService,
        AlertEventProducer eventProducer,
        Node node,
        String port,
        boolean tracing,
        GatewayConfiguration gatewayConfiguration
    ) {
        super(transactionHandlerFactory, reporterService, eventProducer, node, port, tracing);
        this.traceContext = traceContext;
        this.gatewayConfiguration = gatewayConfiguration;
    }

    @Override
    protected List<Processor> buildPreProcessorList() {
        List<Processor> preProcessorList = new ArrayList<>();
        preProcessorList.add(transactionHandlerFactory.create());
        preProcessorList.add(new MetricsProcessor(gatewayConfiguration));

        preProcessorList.add(new XForwardForProcessor());

        // Trace context is executed before the transaction to ensure that we can use the traceparent span value as the
        // transaction ID
        if (traceContext) {
            preProcessorList.add(new TraceContextProcessor());
        }

        return preProcessorList;
    }
}

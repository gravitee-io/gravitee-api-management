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

import io.gravitee.gateway.reactive.core.processor.Processor;
import io.gravitee.gateway.reactive.core.processor.ProcessorChain;
import io.gravitee.gateway.reactive.reactor.processor.alert.AlertProcessor;
import io.gravitee.gateway.reactive.reactor.processor.forward.XForwardForProcessor;
import io.gravitee.gateway.reactive.reactor.processor.reporter.ReporterProcessor;
import io.gravitee.gateway.reactive.reactor.processor.responsetime.ResponseTimeProcessor;
import io.gravitee.gateway.reactive.reactor.processor.shutdown.ShutdownProcessor;
import io.gravitee.gateway.reactive.reactor.processor.transaction.TraceContextProcessor;
import io.gravitee.gateway.reactive.reactor.processor.transaction.TransactionProcessorFactory;
import io.gravitee.gateway.report.ReporterService;
import io.gravitee.node.api.Node;
import io.gravitee.plugin.alert.AlertEventProducer;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PlatformProcessorChainFactory {

    private final TransactionProcessorFactory transactionHandlerFactory;
    private final boolean traceContext;
    private final ReporterService reporterService;
    private final AlertEventProducer eventProducer;
    private final Node node;
    private final String port;
    private ProcessorChain preProcessorChain;
    private ProcessorChain postProcessorChain;

    public PlatformProcessorChainFactory(
        TransactionProcessorFactory transactionHandlerFactory,
        @Value("${handlers.request.trace-context.enabled:false}") boolean traceContext,
        ReporterService reporterService,
        AlertEventProducer eventProducer,
        Node node,
        @Value("${http.port:8082}") String port
    ) {
        this.transactionHandlerFactory = transactionHandlerFactory;
        this.traceContext = traceContext;
        this.reporterService = reporterService;
        this.eventProducer = eventProducer;
        this.node = node;
        this.port = port;
    }

    public ProcessorChain preProcessorChain() {
        if (preProcessorChain == null) {
            initPreProcessorChain();
        }
        return preProcessorChain;
    }

    private void initPreProcessorChain() {
        List<Processor> preProcessorList = new ArrayList<>();

        preProcessorList.add(new XForwardForProcessor());

        // Trace context is executed before the transaction to ensure that we can use the traceparent span value as the
        // transaction ID
        if (traceContext) {
            preProcessorList.add(new TraceContextProcessor());
        }

        preProcessorList.add(transactionHandlerFactory.create());
        preProcessorChain = new ProcessorChain("pre-platform-processor-chain", preProcessorList);
    }

    public ProcessorChain postProcessorChain() {
        if (postProcessorChain == null) {
            initPostProcessorChain();
        }
        return postProcessorChain;
    }

    private void initPostProcessorChain() {
        List<Processor> postProcessorList = new ArrayList<>();
        postProcessorList.add(new ShutdownProcessor(node));
        postProcessorList.add(new ResponseTimeProcessor());
        postProcessorList.add(new ReporterProcessor(reporterService));

        if (!eventProducer.isEmpty()) {
            postProcessorList.add(new AlertProcessor(eventProducer, node, port));
        }

        postProcessorChain = new ProcessorChain("post-platform-processor-chain", postProcessorList);
    }
}

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
package io.gravitee.gateway.reactive.reactor.processor;

import io.gravitee.gateway.reactive.api.hook.ProcessorHook;
import io.gravitee.gateway.reactive.core.processor.Processor;
import io.gravitee.gateway.reactive.core.processor.ProcessorChain;
import io.gravitee.gateway.reactive.core.tracing.TracingHook;
import io.gravitee.gateway.reactive.reactor.processor.alert.AlertProcessor;
import io.gravitee.gateway.reactive.reactor.processor.reporter.ReporterProcessor;
import io.gravitee.gateway.reactive.reactor.processor.responsetime.ResponseTimeProcessor;
import io.gravitee.gateway.reactive.reactor.processor.transaction.TransactionPreProcessorFactory;
import io.gravitee.gateway.report.ReporterService;
import io.gravitee.node.api.Node;
import io.gravitee.plugin.alert.AlertEventProducer;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractPlatformProcessorChainFactory {

    protected final TransactionPreProcessorFactory transactionHandlerFactory;
    private final ReporterService reporterService;
    private final AlertEventProducer eventProducer;
    private final Node node;
    private final String port;
    private final List<ProcessorHook> processorHooks = new ArrayList<>();
    private ProcessorChain preProcessorChain;
    private ProcessorChain postProcessorChain;

    protected abstract List<Processor> buildPreProcessorList();

    public AbstractPlatformProcessorChainFactory(
        TransactionPreProcessorFactory transactionHandlerFactory,
        ReporterService reporterService,
        AlertEventProducer eventProducer,
        Node node,
        String port,
        boolean tracing
    ) {
        this.transactionHandlerFactory = transactionHandlerFactory;
        this.reporterService = reporterService;
        this.eventProducer = eventProducer;
        this.node = node;
        this.port = port;

        if (tracing) {
            processorHooks.add(new TracingHook("Processor"));
        }
    }

    public ProcessorChain preProcessorChain() {
        if (preProcessorChain == null) {
            initPreProcessorChain();
        }
        return preProcessorChain;
    }

    private void initPreProcessorChain() {
        List<Processor> preProcessorList = buildPreProcessorList();
        preProcessorChain = new ProcessorChain("pre-platform", preProcessorList);
        preProcessorChain.addHooks(processorHooks);
    }

    public ProcessorChain postProcessorChain() {
        if (postProcessorChain == null) {
            initPostProcessorChain();
        }
        return postProcessorChain;
    }

    private void initPostProcessorChain() {
        List<Processor> postProcessorList = buildPostProcessorList();

        postProcessorChain = new ProcessorChain("post-platform", postProcessorList);
        postProcessorChain.addHooks(processorHooks);
    }

    protected List<Processor> buildPostProcessorList() {
        List<Processor> postProcessorList = new ArrayList<>();
        postProcessorList.add(new ResponseTimeProcessor());
        postProcessorList.add(new ReporterProcessor(reporterService));

        if (!eventProducer.isEmpty()) {
            postProcessorList.add(new AlertProcessor(eventProducer, node, port));
        }
        return postProcessorList;
    }
}

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

import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.reactive.api.hook.ProcessorHook;
import io.gravitee.gateway.reactive.core.processor.Processor;
import io.gravitee.gateway.reactive.core.processor.ProcessorChain;
import io.gravitee.gateway.reactive.core.tracing.TracingHook;
import io.gravitee.gateway.reactive.reactor.processor.metrics.MetricsProcessor;
import io.gravitee.gateway.reactive.reactor.processor.notfound.NotFoundProcessor;
import io.gravitee.gateway.reactive.reactor.processor.reporter.ReporterProcessor;
import io.gravitee.gateway.reactive.reactor.processor.responsetime.ResponseTimeProcessor;
import io.gravitee.gateway.reactive.reactor.processor.transaction.TransactionPreProcessorFactory;
import io.gravitee.gateway.report.ReporterService;
import java.util.ArrayList;
import java.util.List;
import org.springframework.core.env.Environment;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class NotFoundProcessorChainFactory {

    private final TransactionPreProcessorFactory transactionHandlerFactory;
    private final Environment environment;
    private final ReporterService reporterService;
    private final boolean notFoundAnalyticsEnabled;
    private final GatewayConfiguration gatewayConfiguration;
    private final List<ProcessorHook> processorHooks = new ArrayList<>();
    private ProcessorChain processorChain;

    public NotFoundProcessorChainFactory(
        final TransactionPreProcessorFactory transactionHandlerFactory,
        final Environment environment,
        final ReporterService reporterService,
        boolean notFoundAnalyticsEnabled,
        boolean tracing,
        GatewayConfiguration gatewayConfiguration
    ) {
        this.transactionHandlerFactory = transactionHandlerFactory;
        this.environment = environment;
        this.reporterService = reporterService;
        this.notFoundAnalyticsEnabled = notFoundAnalyticsEnabled;
        this.gatewayConfiguration = gatewayConfiguration;
        if (tracing) {
            processorHooks.add(new TracingHook("processor"));
        }
    }

    public ProcessorChain processorChain() {
        if (processorChain == null) {
            initProcessorChain();
        }
        return processorChain;
    }

    void initProcessorChain() {
        List<Processor> processorList = buildProcessorChain();
        processorChain = new ProcessorChain("not-found", processorList);
        processorChain.addHooks(processorHooks);
    }

    protected List<Processor> buildProcessorChain() {
        List<Processor> processorList = new ArrayList<>();
        processorList.add(transactionHandlerFactory.create());
        processorList.add(new MetricsProcessor(gatewayConfiguration, notFoundAnalyticsEnabled));
        processorList.add(new NotFoundProcessor(environment));
        processorList.add(new ResponseTimeProcessor());
        processorList.add(new ReporterProcessor(reporterService));
        return processorList;
    }
}

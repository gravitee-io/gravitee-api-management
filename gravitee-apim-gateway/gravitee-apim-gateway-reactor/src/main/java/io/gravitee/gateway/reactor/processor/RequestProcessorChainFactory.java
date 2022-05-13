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
package io.gravitee.gateway.reactor.processor;

import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.core.processor.Processor;
import io.gravitee.gateway.core.processor.provider.ProcessorProvider;
import io.gravitee.gateway.core.processor.provider.ProcessorProviderChain;
import io.gravitee.gateway.core.processor.provider.ProcessorSupplier;
import io.gravitee.gateway.reactor.processor.forward.XForwardForProcessor;
import io.gravitee.gateway.reactor.processor.transaction.TraceContextProcessorFactory;
import io.gravitee.gateway.reactor.processor.transaction.TransactionProcessorFactory;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class RequestProcessorChainFactory implements InitializingBean {

    private final List<ProcessorProvider<ExecutionContext, Processor<ExecutionContext>>> providers = new ArrayList<>();

    @Autowired
    @Qualifier("v3TransactionHandlerFactory")
    private TransactionProcessorFactory transactionHandlerFactory;

    @Autowired
    @Qualifier("v3TraceContextProcessorFactory")
    private TraceContextProcessorFactory traceContextHandlerFactory;

    @Value("${handlers.request.trace-context.enabled:false}")
    private boolean traceContext;

    @Override
    public void afterPropertiesSet() throws Exception {
        providers.add(new ProcessorSupplier<>(XForwardForProcessor::new));

        // Trace context is executed before the transaction to ensure that we can use the traceparent span value as the
        // transaction ID
        if (traceContext) {
            providers.add(new ProcessorSupplier<>(() -> traceContextHandlerFactory.create()));
        }

        providers.add(new ProcessorSupplier<>(() -> transactionHandlerFactory.create()));
    }

    public Processor<ExecutionContext> create() {
        return new ProcessorProviderChain<>(providers);
    }
}

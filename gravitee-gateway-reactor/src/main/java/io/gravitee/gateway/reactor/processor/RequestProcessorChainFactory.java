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
import io.gravitee.gateway.core.processor.chain.DefaultProcessorChain;
import io.gravitee.gateway.reactor.processor.forward.XForwardForProcessor;
import io.gravitee.gateway.reactor.processor.transaction.TraceContextProcessorFactory;
import io.gravitee.gateway.reactor.processor.transaction.TransactionProcessorFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.ArrayList;
import java.util.List;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class RequestProcessorChainFactory implements InitializingBean {

    @Autowired
    private TransactionProcessorFactory transactionHandlerFactory;

    @Autowired
    private TraceContextProcessorFactory traceContextHandlerFactory;

    @Value("${handlers.request.trace-context.enabled:false}")
    private boolean traceContext;

    private final List<Processor<ExecutionContext>> processors = new ArrayList<>();

    @Override
    public void afterPropertiesSet() throws Exception {
        processors.add(new XForwardForProcessor());
        processors.add(transactionHandlerFactory.create());

        if (traceContext) processors.add(traceContextHandlerFactory.create());
    }

    public Processor<ExecutionContext> create() {
        return new DefaultProcessorChain<>(processors);
    }
}

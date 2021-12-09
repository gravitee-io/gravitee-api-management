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
import io.gravitee.gateway.reactor.processor.alert.AlertProcessor;
import io.gravitee.gateway.reactor.processor.reporter.ReporterProcessor;
import io.gravitee.gateway.reactor.processor.responsetime.ResponseTimeProcessor;
import io.gravitee.gateway.report.ReporterService;
import io.gravitee.node.api.Node;
import io.gravitee.plugin.alert.AlertEventProducer;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ResponseProcessorChainFactory {

    @Autowired
    private ReporterService reporterService;

    @Autowired
    private AlertEventProducer eventProducer;

    @Autowired
    private Node node;

    @Value("${http.port:8082}")
    private String port;

    public Processor<ExecutionContext> create() {
        List<Processor<ExecutionContext>> processors = Arrays.asList(new ResponseTimeProcessor(), new ReporterProcessor(reporterService));

        if (!eventProducer.isEmpty()) {
            processors.add(new AlertProcessor(eventProducer, node, port));
        }

        return new DefaultProcessorChain<>(processors);
    }
}

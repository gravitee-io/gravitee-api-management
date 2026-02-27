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
package io.gravitee.gateway.reactor.processor;

import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.core.processor.Processor;
import io.gravitee.gateway.core.processor.chain.DefaultProcessorChain;
import io.gravitee.gateway.reactor.processor.notfound.NotFoundProcessor;
import io.gravitee.gateway.reactor.processor.notfound.NotFoundReporter;
import io.gravitee.gateway.reactor.processor.responsetime.ResponseTimeProcessor;
import io.gravitee.gateway.report.ReporterService;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Arrays;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;

public class NotFoundProcessorChainFactory {

    @Autowired
    private ReporterService reporterService;

    @Autowired
    private Environment environment;

    @Autowired(required = false)
    private MeterRegistry meterRegistry;

    @Value("${handlers.notfound.log.enabled:false}")
    private boolean logEnabled;

    public Processor<ExecutionContext> create() {
        return new DefaultProcessorChain<>(
            Arrays.asList(
                new NotFoundProcessor(environment),
                new ResponseTimeProcessor(meterRegistry),
                new NotFoundReporter(reporterService, logEnabled)
            )
        );
    }
}

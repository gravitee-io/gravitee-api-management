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
package io.gravitee.gateway.jupiter.handlers.api.v4.processor.reporter;

import io.gravitee.gateway.jupiter.core.context.MutableExecutionContext;
import io.gravitee.gateway.jupiter.core.processor.Processor;
import io.gravitee.gateway.report.ReporterService;
import io.gravitee.reporter.api.v4.metric.Metrics;
import io.reactivex.rxjava3.core.Completable;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public class EventNativeReporterProcessor implements Processor {

    private final ReporterService reporterService;

    public EventNativeReporterProcessor(final ReporterService reporterService) {
        this.reporterService = reporterService;
    }

    @Override
    public String getId() {
        return "processor-event-native-reporter";
    }

    @Override
    public Completable execute(final MutableExecutionContext ctx) {
        return Completable
            .fromRunnable(
                () -> {
                    Metrics metrics = ctx.metrics();
                    if (metrics != null && metrics.isEnabled()) {
                        reporterService.report(metrics);
                        if (metrics.getLog() != null) {
                            metrics.getLog().setApiId(metrics.getApiId());
                            reporterService.report(metrics.getLog());
                        }
                    }
                }
            )
            .doOnError(throwable -> log.error("An error occurs while reporting metrics", throwable))
            .onErrorComplete();
    }
}

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
package io.gravitee.gateway.reactive.reactor.processor.reporter;

import io.gravitee.gateway.reactive.api.context.RequestExecutionContext;
import io.gravitee.gateway.reactive.core.processor.Processor;
import io.gravitee.gateway.report.ReporterService;
import io.gravitee.reporter.api.http.Metrics;
import io.reactivex.Completable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ReporterProcessor implements Processor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReporterProcessor.class);

    private final ReporterService reporterService;

    public ReporterProcessor(final ReporterService reporterService) {
        this.reporterService = reporterService;
    }

    @Override
    public String getId() {
        return "reporter-processor";
    }

    @Override
    public Completable execute(final RequestExecutionContext ctx) {
        return Completable
            .fromRunnable(
                () -> {
                    Metrics metrics = ctx.request().metrics();
                    reporterService.report(metrics);
                    if (metrics.getLog() != null) {
                        metrics.getLog().setApi(metrics.getApi());
                        reporterService.report(metrics.getLog());
                    }
                }
            )
            .doOnError(throwable -> LOGGER.error("An error occurs while reporting metrics", throwable))
            .onErrorComplete();
    }
}

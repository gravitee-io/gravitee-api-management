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
package io.gravitee.gateway.jupiter.reactor.processor.reporter;

import io.gravitee.definition.model.v4.Api;
import io.gravitee.gateway.jupiter.core.context.MutableExecutionContext;
import io.gravitee.gateway.jupiter.core.processor.Processor;
import io.gravitee.gateway.report.ReporterService;
import io.gravitee.reporter.api.v4.log.Log;
import io.gravitee.reporter.api.v4.metric.Metrics;
import io.reactivex.rxjava3.core.Completable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

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
        return "processor-reporter";
    }

    @Override
    public Completable execute(final MutableExecutionContext ctx) {
        return Completable
            .fromRunnable(
                () -> {
                    Metrics metrics = ctx.metrics();
                    if (metrics != null && metrics.isEnabled()) {
                        Api api = getApiV4(ctx);
                        if (api != null) {
                            metrics.setRequestEnded(true);
                            reporterService.report(metrics);
                            Log log = metrics.getLog();
                            if (log != null) {
                                log.setApiId(metrics.getApiId());
                                log.setRequestEnded(true);
                                reporterService.report(log);
                            }
                        } else {
                            // As api is null, this means we are executing a v2 api with jupiter engine; metrics are always enabled
                            io.gravitee.reporter.api.http.Metrics metricsV2 = metrics.toV2();
                            reporterService.report(metricsV2);
                            if (metricsV2.getLog() != null) {
                                metricsV2.getLog().setApi(metricsV2.getApi());
                                reporterService.report(metricsV2.getLog());
                            }
                        }
                    }
                }
            )
            .doOnError(throwable -> LOGGER.error("An error occurs while reporting metrics", throwable))
            .onErrorComplete();
    }

    private Api getApiV4(final MutableExecutionContext ctx) {
        // Temporary work around to check if current api is V4 or V2
        try {
            return ctx.getComponent(Api.class);
        } catch (NoSuchBeanDefinitionException e) {
            // api V4 not found, we are running an api V2.
            return null;
        }
    }
}

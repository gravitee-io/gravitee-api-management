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
package io.gravitee.gateway.reactive.reactor.processor.reporter;

import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.gateway.reactive.api.connector.Connector;
import io.gravitee.gateway.reactive.api.context.InternalContextAttributes;
import io.gravitee.gateway.reactive.core.context.MutableExecutionContext;
import io.gravitee.gateway.reactive.core.processor.Processor;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.gateway.report.ReporterService;
import io.gravitee.reporter.api.common.Request;
import io.gravitee.reporter.api.common.Response;
import io.gravitee.reporter.api.v4.log.Log;
import io.gravitee.reporter.api.v4.metric.Metrics;
import io.reactivex.rxjava3.core.Completable;
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
        return "processor-reporter";
    }

    @Override
    public Completable execute(final MutableExecutionContext ctx) {
        return Completable
            .fromRunnable(() -> {
                Metrics metrics = ctx.metrics();
                if (metrics != null && metrics.isEnabled()) {
                    metrics.setRequestEnded(true);
                    setEntrypointId(ctx, metrics);

                    executeReportActions(metrics);

                    ReactableApi<?> reactableApi = ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_REACTABLE_API);
                    if (reactableApi != null) {
                        DefinitionVersion definitionVersion = reactableApi.getDefinitionVersion();
                        if (definitionVersion == DefinitionVersion.V2) { // We are executing a v2 api with v4 emulation engine
                            io.gravitee.reporter.api.http.Metrics metricsV2 = metrics.toV2();
                            reporterService.report(metricsV2);
                            if (metricsV2.getLog() != null) {
                                metricsV2.getLog().setApi(metricsV2.getApi());
                                metricsV2.getLog().setApiName(metricsV2.getApiName());
                                reporterService.report(metricsV2.getLog());
                            }
                        } else if (definitionVersion == DefinitionVersion.V4) {
                            reporterService.report(metrics);
                            Log log = metrics.getLog();
                            if (log != null) {
                                log.setApiId(metrics.getApiId());
                                log.setRequestEnded(metrics.isRequestEnded());
                                reporterService.report(log);
                            }
                        } else {
                            // Version unsupported only report metrics
                            reporterService.report(metrics);
                        }
                    } else {
                        // No api found report only metrics
                        reporterService.report(metrics);
                    }
                }
            })
            .doOnError(throwable -> LOGGER.error("An error occurs while reporting metrics", throwable))
            .onErrorComplete();
    }

    private static void setEntrypointId(MutableExecutionContext ctx, Metrics metrics) {
        final Connector connector = ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_ENTRYPOINT_CONNECTOR);
        if (connector != null) {
            metrics.setEntrypointId(connector.id());
        }
    }

    private void executeReportActions(Metrics metrics) {
        final Log log = metrics.getLog();

        if (log != null) {
            executeReportActions(log.getEntrypointRequest());
            executeReportActions(log.getEntrypointResponse());
            executeReportActions(log.getEndpointRequest());
            executeReportActions(log.getEndpointResponse());
        }
    }

    private void executeReportActions(Request request) {
        if (request != null && request.getOnReportActions() != null) {
            request.getOnReportActions().forEach(action -> action.accept(request));
        }
    }

    private void executeReportActions(Response response) {
        if (response != null && response.getOnReportActions() != null) {
            response.getOnReportActions().forEach(action -> action.accept(response));
        }
    }
}

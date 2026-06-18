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
import io.gravitee.gateway.reactive.api.ComponentType;
import io.gravitee.gateway.reactive.api.connector.Connector;
import io.gravitee.gateway.reactive.api.context.ContextAttributes;
import io.gravitee.gateway.reactive.api.context.InternalContextAttributes;
import io.gravitee.gateway.reactive.core.context.HttpExecutionContextInternal;
import io.gravitee.gateway.reactive.core.processor.Processor;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.gateway.report.ReporterService;
import io.gravitee.reporter.api.common.Request;
import io.gravitee.reporter.api.common.Response;
import io.gravitee.reporter.api.v4.log.Log;
import io.gravitee.reporter.api.v4.metric.Diagnostic;
import io.gravitee.reporter.api.v4.metric.Metrics;
import io.reactivex.rxjava3.core.Completable;
import lombok.CustomLog;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
public class ReporterProcessor implements Processor {

    private static final String UNKNOWN_COMPONENT = "Unknown component";

    /**
     * Key family recorded directly on metrics when the gateway, acting as an HTTP client, fails to reach the backend
     * or to stream its response (see the http-proxy endpoint connector's ConnectionFailureClassifier, which this
     * module cannot depend on). Only these keys may be attributed to the ENDPOINT component: other errorKey-only
     * failures sharing the same metrics shape — typically CLIENT_ABORTED_* — are not the backend's fault.
     */
    private static final String GATEWAY_CLIENT_ERROR_KEY_PREFIX = "GATEWAY_CLIENT_";

    private final ReporterService reporterService;

    public ReporterProcessor(final ReporterService reporterService) {
        this.reporterService = reporterService;
    }

    @Override
    public String getId() {
        return "processor-reporter";
    }

    @Override
    public Completable execute(final HttpExecutionContextInternal ctx) {
        return Completable.fromRunnable(() -> {
            Metrics metrics = ctx.metrics();
            if (metrics != null && metrics.isEnabled()) {
                metrics.setRequestEnded(true);
                setEntrypointId(ctx, metrics);
                setQuota(ctx, metrics);

                executeReportActions(metrics);

                // Translate error key and error message to Diagnostic failure if needed
                translateErrorToDiagnosticFailure(metrics);

                ReactableApi<?> reactableApi = ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_REACTABLE_API);
                if (reactableApi != null) {
                    DefinitionVersion definitionVersion = reactableApi.getDefinitionVersion();
                    if (definitionVersion == DefinitionVersion.V2) {
                        // We are executing a v2 api with v4 emulation engine
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
                            log.setApiName(metrics.getApiName());
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
            .doOnError(throwable -> ctx.withLogger(log).error("An error occurs while reporting metrics", throwable))
            .onErrorComplete();
    }

    private static void setEntrypointId(HttpExecutionContextInternal ctx, Metrics metrics) {
        final Connector connector = ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_ENTRYPOINT_CONNECTOR);
        if (connector != null) {
            metrics.setEntrypointId(connector.id());
        }
    }

    private static void addLongMetric(Metrics metrics, String key, HttpExecutionContextInternal ctx) {
        Long value = ctx.getAttribute(key);
        if (value != null) {
            metrics.putAdditionalMetric("long_" + key.replace(ContextAttributes.ATTR_PREFIX, ""), value);
        }
    }

    private static void setQuota(HttpExecutionContextInternal ctx, Metrics metrics) {
        addLongMetric(metrics, ContextAttributes.ATTR_QUOTA_COUNT, ctx);
        addLongMetric(metrics, ContextAttributes.ATTR_QUOTA_LIMIT, ctx);
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

    /**
     * Translates error key and error message to Diagnostic failure if failure is null and error information exists.
     *
     * @param metrics the metrics object to process
     */
    private void translateErrorToDiagnosticFailure(Metrics metrics) {
        if (metrics != null && metrics.getFailure() == null) {
            String errorKey = metrics.getErrorKey();
            String errorMessage = metrics.getErrorMessage();

            if (errorMessage != null && !errorMessage.isBlank()) {
                // A backend connection failure recorded directly on metrics is attributed to the ENDPOINT component
                // instead of the generic "Unknown component" placeholder. Restricted to the GATEWAY_CLIENT_* family:
                // metrics.endpoint is set as soon as the connector attempts the request, so its presence alone does
                // not mean the backend caused the failure (e.g. a client abort mid-response).
                String endpoint = metrics.getEndpoint();
                boolean endpointFailure =
                    errorKey != null && errorKey.startsWith(GATEWAY_CLIENT_ERROR_KEY_PREFIX) && endpoint != null && !endpoint.isBlank();
                String componentType = endpointFailure ? ComponentType.ENDPOINT.name() : UNKNOWN_COMPONENT;
                String componentName = endpointFailure ? endpoint : UNKNOWN_COMPONENT;

                Diagnostic failure = new Diagnostic(
                    errorKey != null ? errorKey : "internal_error",
                    errorMessage,
                    componentType,
                    componentName
                );

                metrics.setFailure(failure);
            }
        }
    }
}

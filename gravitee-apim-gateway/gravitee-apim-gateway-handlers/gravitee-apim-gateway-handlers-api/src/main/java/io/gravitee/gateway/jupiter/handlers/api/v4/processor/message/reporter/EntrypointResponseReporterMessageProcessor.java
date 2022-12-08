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
package io.gravitee.gateway.jupiter.handlers.api.v4.processor.message.reporter;

import static io.gravitee.gateway.jupiter.api.context.ContextAttributes.ATTR_API;
import static io.gravitee.gateway.jupiter.api.context.InternalContextAttributes.ATTR_INTERNAL_ENTRYPOINT_CONNECTOR;
import static io.gravitee.gateway.jupiter.api.context.InternalContextAttributes.ATTR_INTERNAL_MESSAGE_RECORDABLE;
import static io.gravitee.gateway.jupiter.api.context.InternalContextAttributes.ATTR_INTERNAL_MESSAGE_RECORDABLE_WITH_LOGGING;

import io.gravitee.gateway.jupiter.api.connector.entrypoint.EntrypointConnector;
import io.gravitee.gateway.jupiter.api.context.InternalContextAttributes;
import io.gravitee.gateway.jupiter.core.condition.ExpressionLanguageMessageConditionFilter;
import io.gravitee.gateway.jupiter.core.context.MutableExecutionContext;
import io.gravitee.gateway.jupiter.core.processor.MessageProcessor;
import io.gravitee.gateway.jupiter.core.v4.analytics.AnalyticsContext;
import io.gravitee.gateway.jupiter.core.v4.analytics.LoggingContext;
import io.gravitee.gateway.jupiter.handlers.api.v4.analytics.logging.response.message.MessageLogEntrypointResponse;
import io.gravitee.gateway.report.ReporterService;
import io.gravitee.reporter.api.v4.common.MessageConnectorType;
import io.gravitee.reporter.api.v4.common.MessageOperation;
import io.gravitee.reporter.api.v4.log.MessageLog;
import io.gravitee.reporter.api.v4.metric.MessageMetrics;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class EntrypointResponseReporterMessageProcessor implements MessageProcessor {

    public static final String ID = "message-processor-entrypoint-request-reporter";

    private final ReporterService reporterService;

    public EntrypointResponseReporterMessageProcessor(final ReporterService reporterService) {
        this.reporterService = reporterService;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Completable execute(final MutableExecutionContext ctx) {
        if (!ctx.metrics().isEnabled()) {
            return Completable.complete();
        }
        return Completable.defer(
            () -> {
                final AnalyticsContext analyticsContext = ctx.getInternalAttribute(
                    InternalContextAttributes.ATTR_INTERNAL_ANALYTICS_CONTEXT
                );

                if (analyticsContext == null || !analyticsContext.isEnabled()) {
                    return Completable.complete();
                }

                AtomicInteger messagesErrorCount = new AtomicInteger(0);
                AtomicInteger messagesCount = new AtomicInteger(0);
                return ctx
                    .response()
                    .onMessage(
                        message -> {
                            Boolean isRecordable = message.attribute(ATTR_INTERNAL_MESSAGE_RECORDABLE);
                            if (Boolean.TRUE.equals(isRecordable)) {
                                String requestId = ctx.request().id();
                                String clientIdentifier = ctx.request().clientIdentifier();
                                String apiId = ctx.getAttribute(ATTR_API);
                                MessageOperation operation = MessageOperation.SUBSCRIBE;
                                MessageConnectorType connectorType = MessageConnectorType.ENTRYPOINT;
                                EntrypointConnector entrypointConnector = ctx.getInternalAttribute(ATTR_INTERNAL_ENTRYPOINT_CONNECTOR);
                                String connectorId = entrypointConnector.id();
                                MessageMetrics.MessageMetricsBuilder<?, ?> metricsBuilder = MessageMetrics
                                    .builder()
                                    .requestId(requestId)
                                    .clientIdentifier(clientIdentifier)
                                    .apiId(apiId)
                                    .correlationId(message.correlationId())
                                    .parentCorrelationId(message.parentCorrelationId())
                                    .timestamp(message.timestamp())
                                    .count(messagesCount.incrementAndGet())
                                    .operation(operation)
                                    .connectorType(connectorType)
                                    .connectorId(connectorId)
                                    .gatewayLatencyMs(System.currentTimeMillis() - message.timestamp());
                                if (message.content() != null) {
                                    metricsBuilder.contentLength(message.content().length());
                                }
                                if (message.error()) {
                                    metricsBuilder.errorsCount(messagesErrorCount.incrementAndGet());
                                }
                                reporterService.report(metricsBuilder.build());

                                final LoggingContext loggingContext = analyticsContext.getLoggingContext();
                                Boolean messageLoggingFiltered = message.attribute(ATTR_INTERNAL_MESSAGE_RECORDABLE_WITH_LOGGING);
                                if (
                                    loggingContext != null &&
                                    loggingContext.entrypointResponse() &&
                                    Boolean.TRUE.equals(messageLoggingFiltered)
                                ) {
                                    MessageLog messageLog = MessageLog
                                        .builder()
                                        .timestamp(ctx.request().timestamp())
                                        .requestId(requestId)
                                        .clientIdentifier(clientIdentifier)
                                        .correlationId(message.correlationId())
                                        .parentCorrelationId(message.parentCorrelationId())
                                        .apiId(apiId)
                                        .operation(operation)
                                        .connectorType(connectorType)
                                        .connectorId(connectorId)
                                        .message(new MessageLogEntrypointResponse(loggingContext, message))
                                        .build();
                                    reporterService.report(messageLog);
                                }
                            }
                            return Maybe.just(message);
                        }
                    );
            }
        );
    }
}

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
import io.gravitee.gateway.jupiter.core.v4.analytics.sampling.MessageSamplingStrategy;
import io.gravitee.gateway.jupiter.handlers.api.v4.analytics.logging.request.message.MessageLogEntrypointRequest;
import io.gravitee.gateway.report.ReporterService;
import io.gravitee.reporter.api.v4.common.MessageConnectorType;
import io.gravitee.reporter.api.v4.common.MessageOperation;
import io.gravitee.reporter.api.v4.log.MessageLog;
import io.gravitee.reporter.api.v4.metric.MessageMetrics;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class EntrypointRequestReporterMessageProcessor implements MessageProcessor {

    public static final String ID = "message-processor-entrypoint-request-reporter";

    private final ReporterService reporterService;
    private static final ExpressionLanguageMessageConditionFilter<LoggingContext> MESSAGE_CONDITION_FILTER = new ExpressionLanguageMessageConditionFilter<>();

    public EntrypointRequestReporterMessageProcessor(final ReporterService reporterService) {
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

                MessageSamplingStrategy messageSamplingStrategy = analyticsContext.getMessageSamplingStrategy();
                AtomicInteger messagesCount = new AtomicInteger(0);
                AtomicInteger messagesErrorCount = new AtomicInteger(0);
                AtomicLong lastMessageTimestamp = new AtomicLong(-1);
                return ctx
                    .request()
                    .onMessage(
                        message -> {
                            int count = messagesCount.incrementAndGet();
                            boolean recordable = messageSamplingStrategy.isRecordable(message, count, lastMessageTimestamp.get());
                            message.attribute(ATTR_INTERNAL_MESSAGE_RECORDABLE, recordable);

                            if (recordable) {
                                lastMessageTimestamp.set(message.timestamp());
                                String requestId = ctx.request().id();
                                String clientIdentifier = ctx.request().clientIdentifier();
                                String apiId = ctx.getAttribute(ATTR_API);
                                MessageOperation operation = MessageOperation.PUBLISH;
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
                                    .count(count)
                                    .operation(operation)
                                    .connectorType(connectorType)
                                    .connectorId(connectorId);
                                if (message.content() != null) {
                                    metricsBuilder.contentLength(message.content().length());
                                }
                                if (message.error()) {
                                    metricsBuilder.errorsCount(messagesErrorCount.incrementAndGet());
                                }
                                reporterService.report(metricsBuilder.build());

                                final LoggingContext loggingContext = analyticsContext.getLoggingContext();
                                if (loggingContext != null && loggingContext.entrypointRequest()) {
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
                                        .message(new MessageLogEntrypointRequest(loggingContext, message))
                                        .build();
                                    return MESSAGE_CONDITION_FILTER
                                        .filter(ctx, analyticsContext.getLoggingContext(), message)
                                        .doOnSuccess(
                                            activeLoggingContext -> {
                                                message.attribute(ATTR_INTERNAL_MESSAGE_RECORDABLE_WITH_LOGGING, true);
                                                reporterService.report(messageLog);
                                            }
                                        )
                                        .ignoreElement()
                                        .andThen(Maybe.just(message));
                                }
                            }
                            return Maybe.just(message);
                        }
                    );
            }
        );
    }
}

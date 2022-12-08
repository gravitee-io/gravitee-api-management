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
package io.gravitee.gateway.jupiter.handlers.api.v4.analytics;

import static io.gravitee.gateway.jupiter.api.context.ContextAttributes.ATTR_API;
import static io.gravitee.gateway.jupiter.api.context.InternalContextAttributes.ATTR_INTERNAL_ENDPOINT_CONNECTOR_ID;
import static io.gravitee.gateway.jupiter.api.context.InternalContextAttributes.ATTR_INTERNAL_ENTRYPOINT_CONNECTOR;
import static io.gravitee.gateway.jupiter.api.context.InternalContextAttributes.ATTR_INTERNAL_MESSAGE_RECORDABLE_WITH_LOGGING;

import io.gravitee.gateway.jupiter.api.connector.entrypoint.EntrypointConnector;
import io.gravitee.gateway.jupiter.api.context.ExecutionContext;
import io.gravitee.gateway.jupiter.api.message.Message;
import io.gravitee.gateway.jupiter.core.condition.ExpressionLanguageMessageConditionFilter;
import io.gravitee.gateway.jupiter.core.v4.analytics.LoggingContext;
import io.gravitee.gateway.report.ReporterService;
import io.gravitee.reporter.api.v4.common.MessageConnectorType;
import io.gravitee.reporter.api.v4.common.MessageOperation;
import io.gravitee.reporter.api.v4.log.MessageLog;
import io.gravitee.reporter.api.v4.metric.MessageMetrics;
import io.reactivex.rxjava3.core.Maybe;
import lombok.RequiredArgsConstructor;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
public class MessageReporter {

    private static final ExpressionLanguageMessageConditionFilter<LoggingContext> MESSAGE_CONDITION_FILTER = new ExpressionLanguageMessageConditionFilter<>();

    private final ReporterService reporterService;

    public MessageMetrics reportMessageMetricsWithLatency(
        final ExecutionContext ctx,
        final MessageOperation operation,
        final MessageConnectorType connectorType,
        final Message message,
        final MessageCounters.Counters counters
    ) {
        return reportMessageMetrics(ctx, operation, connectorType, message, counters, System.currentTimeMillis() - message.timestamp());
    }

    public MessageMetrics reportMessageMetrics(
        final ExecutionContext ctx,
        final MessageOperation operation,
        final MessageConnectorType connectorType,
        final Message message,
        final MessageCounters.Counters counters
    ) {
        return reportMessageMetrics(ctx, operation, connectorType, message, counters, -1);
    }

    private MessageMetrics reportMessageMetrics(
        final ExecutionContext ctx,
        final MessageOperation operation,
        final MessageConnectorType connectorType,
        final Message message,
        final MessageCounters.Counters counters,
        final long gatewayLatency
    ) {
        String requestId = ctx.request().id();
        String clientIdentifier = ctx.request().clientIdentifier();
        String apiId = ctx.getAttribute(ATTR_API);
        String connectorId = null;
        if (connectorType == MessageConnectorType.ENTRYPOINT) {
            EntrypointConnector entrypointConnector = ctx.getInternalAttribute(ATTR_INTERNAL_ENTRYPOINT_CONNECTOR);
            connectorId = entrypointConnector.id();
        } else if (connectorType == MessageConnectorType.ENDPOINT) {
            connectorId = ctx.getInternalAttribute(ATTR_INTERNAL_ENDPOINT_CONNECTOR_ID);
        }
        MessageMetrics.MessageMetricsBuilder<?, ?> metricsBuilder = MessageMetrics
            .builder()
            .requestId(requestId)
            .clientIdentifier(clientIdentifier)
            .apiId(apiId)
            .correlationId(message.correlationId())
            .parentCorrelationId(message.parentCorrelationId())
            .timestamp(message.timestamp())
            .count(counters.messageCount())
            .errorCount(counters.errorCount())
            .error(message.error())
            .operation(operation)
            .connectorType(connectorType)
            .connectorId(connectorId)
            .gatewayLatencyMs(gatewayLatency);
        if (message.content() != null) {
            metricsBuilder.contentLength(message.content().length());
        }
        MessageMetrics metrics = metricsBuilder.build();
        reporterService.report(metrics);
        return metrics;
    }

    public Maybe<Message> reportMessageLog(
        final MessageMetrics messageMetrics,
        final Message message,
        final io.gravitee.gateway.jupiter.handlers.api.v4.analytics.logging.MessageLog messageLog
    ) {
        MessageLog reportableMessageLog = MessageLog
            .builder()
            .timestamp(message.timestamp())
            .requestId(messageMetrics.getRequestId())
            .clientIdentifier(messageMetrics.getClientIdentifier())
            .correlationId(message.correlationId())
            .parentCorrelationId(message.parentCorrelationId())
            .apiId(messageMetrics.getApiId())
            .operation(messageMetrics.getOperation())
            .connectorType(messageMetrics.getConnectorType())
            .connectorId(messageMetrics.getConnectorId())
            .message(messageLog)
            .build();
        reporterService.report(reportableMessageLog);
        return Maybe.just(message);
    }

    public Maybe<Message> reportConditionalMessageLog(
        final ExecutionContext ctx,
        final MessageMetrics messageMetrics,
        final LoggingContext loggingContext,
        final Message message,
        final io.gravitee.gateway.jupiter.handlers.api.v4.analytics.logging.MessageLog messageLog
    ) {
        MessageLog reportableMessageLog = MessageLog
            .builder()
            .timestamp(message.timestamp())
            .requestId(messageMetrics.getRequestId())
            .clientIdentifier(messageMetrics.getClientIdentifier())
            .correlationId(message.correlationId())
            .parentCorrelationId(message.parentCorrelationId())
            .apiId(messageMetrics.getApiId())
            .operation(messageMetrics.getOperation())
            .connectorType(messageMetrics.getConnectorType())
            .connectorId(messageMetrics.getConnectorId())
            .message(messageLog)
            .build();
        if (message.error()) {
            message.attribute(ATTR_INTERNAL_MESSAGE_RECORDABLE_WITH_LOGGING, true);
            reporterService.report(reportableMessageLog);
            return Maybe.just(message);
        } else {
            return MESSAGE_CONDITION_FILTER
                .filter(ctx, loggingContext, message)
                .doOnSuccess(
                    activeLoggingContext -> {
                        message.attribute(ATTR_INTERNAL_MESSAGE_RECORDABLE_WITH_LOGGING, true);
                        reporterService.report(reportableMessageLog);
                    }
                )
                .ignoreElement()
                .andThen(Maybe.just(message));
        }
    }
}

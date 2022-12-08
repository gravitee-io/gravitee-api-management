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
import static io.gravitee.gateway.jupiter.api.context.InternalContextAttributes.ATTR_INTERNAL_MESSAGE_RECORDABLE;
import static io.gravitee.gateway.jupiter.api.context.InternalContextAttributes.ATTR_INTERNAL_MESSAGE_RECORDABLE_WITH_LOGGING;

import io.gravitee.gateway.jupiter.api.ExecutionFailure;
import io.gravitee.gateway.jupiter.api.ExecutionPhase;
import io.gravitee.gateway.jupiter.api.context.ExecutionContext;
import io.gravitee.gateway.jupiter.api.context.InternalContextAttributes;
import io.gravitee.gateway.jupiter.api.hook.InvokerHook;
import io.gravitee.gateway.jupiter.api.message.Message;
import io.gravitee.gateway.jupiter.core.condition.ExpressionLanguageMessageConditionFilter;
import io.gravitee.gateway.jupiter.core.v4.analytics.AnalyticsContext;
import io.gravitee.gateway.jupiter.core.v4.analytics.LoggingContext;
import io.gravitee.gateway.jupiter.core.v4.analytics.sampling.MessageSamplingStrategy;
import io.gravitee.gateway.jupiter.handlers.api.v4.analytics.logging.request.message.MessageLogEndpointRequest;
import io.gravitee.gateway.jupiter.handlers.api.v4.analytics.logging.response.message.MessageLogEndpointResponse;
import io.gravitee.gateway.report.ReporterService;
import io.gravitee.reporter.api.v4.common.MessageConnectorType;
import io.gravitee.reporter.api.v4.common.MessageOperation;
import io.gravitee.reporter.api.v4.log.MessageLog;
import io.gravitee.reporter.api.v4.metric.MessageMetrics;
import io.reactivex.rxjava3.annotations.Nullable;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class AnalyticsMessageHook implements InvokerHook {

    private static final ExpressionLanguageMessageConditionFilter<LoggingContext> MESSAGE_CONDITION_FILTER = new ExpressionLanguageMessageConditionFilter<>();
    private final ReporterService reporterService;

    public AnalyticsMessageHook(final ReporterService reporterService) {
        this.reporterService = reporterService;
    }

    @Override
    public String id() {
        return "hook-message-analytics";
    }

    @Override
    public Completable pre(final String id, final ExecutionContext ctx, final ExecutionPhase executionPhase) {
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

                AtomicInteger messagesCount = new AtomicInteger(0);
                AtomicInteger messagesErrorCount = new AtomicInteger(0);
                return ctx
                    .request()
                    .onMessage(
                        message -> {
                            Boolean recorded = message.attribute(ATTR_INTERNAL_MESSAGE_RECORDABLE);
                            if (Boolean.TRUE.equals(recorded)) {
                                String requestId = ctx.request().id();
                                String clientIdentifier = ctx.request().clientIdentifier();
                                String apiId = ctx.getAttribute(ATTR_API);
                                MessageOperation operation = MessageOperation.PUBLISH;
                                MessageConnectorType connectorType = MessageConnectorType.ENDPOINT;
                                String connectorId = ctx.getInternalAttribute(ATTR_INTERNAL_ENDPOINT_CONNECTOR_ID);
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
                                        .message(new MessageLogEndpointRequest(loggingContext, message))
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

    @Override
    public Completable post(String id, ExecutionContext ctx, @Nullable ExecutionPhase executionPhase) {
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
                    .response()
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
                                MessageOperation operation = MessageOperation.SUBSCRIBE;
                                MessageConnectorType connectorType = MessageConnectorType.ENDPOINT;
                                String connectorId = ctx.getInternalAttribute(ATTR_INTERNAL_ENDPOINT_CONNECTOR_ID);
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
                                if (loggingContext != null && loggingContext.endpointResponse()) {
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
                                        .message(new MessageLogEndpointResponse(loggingContext, message))
                                        .build();
                                    if (loggingContext.getMessageCondition() != null) {
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
                                    } else {
                                        reporterService.report(messageLog);
                                    }
                                }
                            }
                            return Maybe.just(message);
                        }
                    );
            }
        );
    }

    @Override
    public Completable interrupt(String id, ExecutionContext ctx, @Nullable ExecutionPhase executionPhase) {
        return post(id, ctx, executionPhase);
    }

    @Override
    public Completable interruptWith(String id, ExecutionContext ctx, @Nullable ExecutionPhase executionPhase, ExecutionFailure failure) {
        return post(id, ctx, executionPhase);
    }
}

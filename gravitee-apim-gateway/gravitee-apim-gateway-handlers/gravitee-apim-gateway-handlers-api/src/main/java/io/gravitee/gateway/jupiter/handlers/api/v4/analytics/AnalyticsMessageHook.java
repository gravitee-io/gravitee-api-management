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

import io.gravitee.gateway.jupiter.api.ExecutionFailure;
import io.gravitee.gateway.jupiter.api.ExecutionPhase;
import io.gravitee.gateway.jupiter.api.context.ExecutionContext;
import io.gravitee.gateway.jupiter.api.context.InternalContextAttributes;
import io.gravitee.gateway.jupiter.api.hook.InvokerHook;
import io.gravitee.gateway.jupiter.core.v4.analytics.AnalyticsContext;
import io.gravitee.gateway.jupiter.core.v4.analytics.LoggingContext;
import io.gravitee.gateway.jupiter.core.v4.analytics.sampling.MessageSamplingStrategy;
import io.gravitee.gateway.jupiter.handlers.api.v4.analytics.logging.request.message.MessageLogEndpointRequest;
import io.gravitee.gateway.jupiter.handlers.api.v4.analytics.logging.request.message.MessageLogEntrypointRequest;
import io.gravitee.gateway.jupiter.handlers.api.v4.analytics.logging.response.message.MessageLogEndpointResponse;
import io.gravitee.gateway.report.ReporterService;
import io.gravitee.reporter.api.v4.common.MessageConnectorType;
import io.gravitee.reporter.api.v4.common.MessageOperation;
import io.gravitee.reporter.api.v4.metric.MessageMetrics;
import io.reactivex.rxjava3.annotations.Nullable;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class AnalyticsMessageHook implements InvokerHook {

    private final MessageReporter messageReporter;

    public AnalyticsMessageHook(final ReporterService reporterService) {
        this.messageReporter = new MessageReporter(reporterService);
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

                MessageCounters atomicCounters = new MessageCounters();
                return ctx
                    .request()
                    .onMessage(
                        message -> {
                            MessageCounters.Counters counters = atomicCounters.increment(message);
                            if (MessageAnalyticsHelper.isRecordable(message)) {
                                MessageMetrics messageMetrics = messageReporter.reportMessageMetricsWithLatency(
                                    ctx,
                                    MessageOperation.PUBLISH,
                                    MessageConnectorType.ENDPOINT,
                                    message,
                                    counters
                                );

                                final LoggingContext loggingContext = analyticsContext.getLoggingContext();
                                if (
                                    loggingContext != null &&
                                    loggingContext.endpointRequest() &&
                                    MessageAnalyticsHelper.isRecordableWithLogging(message)
                                ) {
                                    return messageReporter.reportMessageLog(
                                        messageMetrics,
                                        message,
                                        new MessageLogEndpointRequest(loggingContext, message)
                                    );
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

                MessageCounters atomicCounters = new MessageCounters();
                AtomicLong lastMessageTimestamp = new AtomicLong(-1);
                return ctx
                    .response()
                    .onMessage(
                        message -> {
                            MessageCounters.Counters counters = atomicCounters.increment(message);
                            if (
                                MessageAnalyticsHelper.computeRecordable(
                                    message,
                                    messageSamplingStrategy,
                                    counters.messageCount(),
                                    lastMessageTimestamp.get()
                                )
                            ) {
                                lastMessageTimestamp.set(message.timestamp());
                                MessageMetrics messageMetrics = messageReporter.reportMessageMetrics(
                                    ctx,
                                    MessageOperation.SUBSCRIBE,
                                    MessageConnectorType.ENDPOINT,
                                    message,
                                    counters
                                );

                                final LoggingContext loggingContext = analyticsContext.getLoggingContext();
                                if (loggingContext != null && loggingContext.endpointResponse()) {
                                    return messageReporter.reportConditionalMessageLog(
                                        ctx,
                                        messageMetrics,
                                        loggingContext,
                                        message,
                                        new MessageLogEndpointResponse(loggingContext, message)
                                    );
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

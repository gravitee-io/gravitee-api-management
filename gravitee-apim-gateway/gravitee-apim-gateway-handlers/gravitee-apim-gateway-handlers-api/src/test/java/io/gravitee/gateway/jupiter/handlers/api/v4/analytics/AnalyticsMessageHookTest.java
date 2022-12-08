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

import static io.gravitee.gateway.jupiter.api.context.InternalContextAttributes.ATTR_INTERNAL_MESSAGE_RECORDABLE;
import static io.gravitee.gateway.jupiter.api.context.InternalContextAttributes.ATTR_INTERNAL_MESSAGE_RECORDABLE_WITH_LOGGING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.definition.model.v4.analytics.Analytics;
import io.gravitee.definition.model.v4.analytics.logging.Logging;
import io.gravitee.definition.model.v4.analytics.logging.LoggingMode;
import io.gravitee.definition.model.v4.analytics.logging.LoggingPhase;
import io.gravitee.definition.model.v4.analytics.sampling.Sampling;
import io.gravitee.definition.model.v4.analytics.sampling.SamplingType;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.jupiter.api.ExecutionPhase;
import io.gravitee.gateway.jupiter.api.context.InternalContextAttributes;
import io.gravitee.gateway.jupiter.api.message.DefaultMessage;
import io.gravitee.gateway.jupiter.api.message.Message;
import io.gravitee.gateway.jupiter.core.context.DefaultExecutionContext;
import io.gravitee.gateway.jupiter.core.context.MutableExecutionContext;
import io.gravitee.gateway.jupiter.core.context.MutableRequest;
import io.gravitee.gateway.jupiter.core.context.MutableResponse;
import io.gravitee.gateway.jupiter.core.v4.analytics.AnalyticsContext;
import io.gravitee.gateway.jupiter.handlers.api.v4.analytics.logging.request.message.MessageLogEndpointRequest;
import io.gravitee.gateway.jupiter.handlers.api.v4.analytics.logging.response.message.MessageLogEndpointResponse;
import io.gravitee.gateway.report.ReporterService;
import io.gravitee.reporter.api.Reportable;
import io.gravitee.reporter.api.v4.common.MessageConnectorType;
import io.gravitee.reporter.api.v4.common.MessageOperation;
import io.gravitee.reporter.api.v4.log.MessageLog;
import io.gravitee.reporter.api.v4.metric.MessageMetrics;
import io.gravitee.reporter.api.v4.metric.Metrics;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AnalyticsMessageHookTest {

    @Mock
    private ReporterService reporterService;

    @Mock
    private MutableRequest mockRequest;

    @Mock
    private MutableResponse mockResponse;

    @Mock
    protected Metrics mockMetrics;

    private MutableExecutionContext ctx;
    private AnalyticsMessageHook cut;
    private Analytics analytics;

    @BeforeEach
    void init() {
        lenient().when(mockRequest.id()).thenReturn("requestId");
        lenient().when(mockRequest.clientIdentifier()).thenReturn("clientIdentifier");
        lenient().when(mockRequest.onMessage(any())).thenReturn(Completable.complete());
        lenient().when(mockResponse.onMessage(any())).thenReturn(Completable.complete());

        when(mockMetrics.isEnabled()).thenReturn(true);
        ctx = new DefaultExecutionContext(mockRequest, mockResponse);
        ctx.metrics(mockMetrics);
        analytics = prepareAnalytics();
        AnalyticsContext analyticsContext = new AnalyticsContext(analytics, true, null, null);
        ctx.setInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_ANALYTICS_CONTEXT, analyticsContext);
        ctx.setInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_ENDPOINT_CONNECTOR_ID, "endpointId");
        cut = new AnalyticsMessageHook(reporterService);
    }

    @Nested
    class Pre {

        @Test
        void should_not_apply_transformer_when_metrics_disabled() {
            when(mockMetrics.isEnabled()).thenReturn(false);
            cut.pre("id", ctx, ExecutionPhase.MESSAGE_REQUEST).test().assertResult();
            verifyNoInteractions(mockRequest);
        }

        @Test
        void should_not_apply_transformer_when_analytics_is_null() {
            ctx.setInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_ANALYTICS_CONTEXT, null);
            cut.pre("id", ctx, ExecutionPhase.MESSAGE_REQUEST).test().assertResult();
            verifyNoInteractions(mockRequest);
        }

        @Test
        void should_not_report_metrics_when_message_is_not_recordable() {
            cut.pre("id", ctx, ExecutionPhase.MESSAGE_REQUEST).test().assertResult();

            ArgumentCaptor<Function<Message, Maybe<Message>>> requestMessagesCaptor = ArgumentCaptor.forClass(Function.class);
            verify(mockRequest).onMessage(requestMessagesCaptor.capture());

            Function<Message, Maybe<Message>> requestMessages = requestMessagesCaptor.getValue();
            DefaultMessage message = DefaultMessage
                .builder()
                .id("id")
                .content(Buffer.buffer())
                .attributes(Map.of(ATTR_INTERNAL_MESSAGE_RECORDABLE, false))
                .build();
            Message returnMessage = requestMessages.apply(message).blockingGet();
            assertThat(message).isSameAs(returnMessage);
            verifyNoInteractions(reporterService);
        }

        @Test
        void should_report_metrics_when_message_is_recordable() {
            cut.pre("id", ctx, ExecutionPhase.MESSAGE_REQUEST).test().assertResult();

            ArgumentCaptor<Function<Message, Maybe<Message>>> requestMessagesCaptor = ArgumentCaptor.forClass(Function.class);
            verify(mockRequest).onMessage(requestMessagesCaptor.capture());

            Function<Message, Maybe<Message>> requestMessages = requestMessagesCaptor.getValue();
            DefaultMessage message = DefaultMessage
                .builder()
                .id("id")
                .content(Buffer.buffer())
                .attributes(Map.of(ATTR_INTERNAL_MESSAGE_RECORDABLE, true))
                .build();
            Message returnMessage = requestMessages.apply(message).blockingGet();
            assertThat(message).isSameAs(returnMessage);
            ArgumentCaptor<MessageMetrics> metricsCaptor = ArgumentCaptor.forClass(MessageMetrics.class);
            verify(reporterService).report(metricsCaptor.capture());

            MessageMetrics metrics = metricsCaptor.getValue();
            assertThat(metrics.getRequestId()).isEqualTo("requestId");
            assertThat(metrics.getClientIdentifier()).isEqualTo("clientIdentifier");
            assertThat(metrics.getCorrelationId()).isNotNull();
            assertThat(metrics.getParentCorrelationId()).isNull();
            assertThat(metrics.getCount()).isEqualTo(1);
            assertThat(metrics.getOperation()).isEqualTo(MessageOperation.PUBLISH);
            assertThat(metrics.getConnectorType()).isEqualTo(MessageConnectorType.ENDPOINT);
            assertThat(metrics.getConnectorId()).isEqualTo("endpointId");
        }

        @Test
        void should_report_metrics_and_logs_when_message_is_recordable_with_log() {
            Logging logging = new Logging();
            LoggingMode mode = new LoggingMode();
            mode.setEndpoint(true);
            logging.setMode(mode);
            LoggingPhase loggingPhase = new LoggingPhase();
            loggingPhase.setRequest(true);
            logging.setPhase(loggingPhase);
            analytics.setLogging(logging);
            AnalyticsContext analyticsContext = new AnalyticsContext(analytics, true, null, null);
            ctx.setInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_ANALYTICS_CONTEXT, analyticsContext);

            cut.pre("id", ctx, ExecutionPhase.MESSAGE_REQUEST).test().assertResult();

            ArgumentCaptor<Function<Message, Maybe<Message>>> requestMessagesCaptor = ArgumentCaptor.forClass(Function.class);
            verify(mockRequest).onMessage(requestMessagesCaptor.capture());

            Function<Message, Maybe<Message>> requestMessages = requestMessagesCaptor.getValue();
            DefaultMessage message = DefaultMessage
                .builder()
                .id("id")
                .content(Buffer.buffer())
                .attributes(Map.of(ATTR_INTERNAL_MESSAGE_RECORDABLE, true, ATTR_INTERNAL_MESSAGE_RECORDABLE_WITH_LOGGING, true))
                .build();
            Message returnMessage = requestMessages.apply(message).blockingGet();
            assertThat(message).isSameAs(returnMessage);
            ArgumentCaptor<Reportable> reportableArgumentCaptor = ArgumentCaptor.forClass(Reportable.class);
            verify(reporterService, times(2)).report(reportableArgumentCaptor.capture());

            List<Reportable> reportables = reportableArgumentCaptor.getAllValues();
            MessageMetrics metrics = (MessageMetrics) reportables.get(0);
            assertThat(metrics.getRequestId()).isEqualTo("requestId");
            assertThat(metrics.getClientIdentifier()).isEqualTo("clientIdentifier");
            assertThat(metrics.getCorrelationId()).isNotNull();
            assertThat(metrics.getParentCorrelationId()).isNull();
            assertThat(metrics.getCount()).isEqualTo(1);
            assertThat(metrics.getOperation()).isEqualTo(MessageOperation.PUBLISH);
            assertThat(metrics.getConnectorType()).isEqualTo(MessageConnectorType.ENDPOINT);
            assertThat(metrics.getConnectorId()).isEqualTo("endpointId");

            MessageLog messageLog = (MessageLog) reportables.get(1);
            assertThat(messageLog.getRequestId()).isEqualTo("requestId");
            assertThat(messageLog.getClientIdentifier()).isEqualTo("clientIdentifier");
            assertThat(messageLog.getCorrelationId()).isNotNull();
            assertThat(messageLog.getParentCorrelationId()).isNull();
            assertThat(messageLog.getOperation()).isEqualTo(MessageOperation.PUBLISH);
            assertThat(messageLog.getConnectorType()).isEqualTo(MessageConnectorType.ENDPOINT);
            assertThat(messageLog.getConnectorId()).isEqualTo("endpointId");
            assertThat(messageLog.getMessage()).isInstanceOf(MessageLogEndpointRequest.class);
        }

        @Test
        void should_report_metrics_but_not_logs_when_message_is_recordable_without_log() {
            Logging logging = new Logging();
            LoggingMode mode = new LoggingMode();
            mode.setEntrypoint(true);
            logging.setMode(mode);
            LoggingPhase loggingPhase = new LoggingPhase();
            loggingPhase.setResponse(true);
            logging.setPhase(loggingPhase);
            logging.setMessageCondition("{#message.id == 'bad'}");
            analytics.setLogging(logging);
            AnalyticsContext analyticsContext = new AnalyticsContext(analytics, true, null, null);
            ctx.setInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_ANALYTICS_CONTEXT, analyticsContext);

            cut.pre("id", ctx, ExecutionPhase.MESSAGE_REQUEST).test().assertResult();

            ArgumentCaptor<Function<Message, Maybe<Message>>> requestMessagesCaptor = ArgumentCaptor.forClass(Function.class);
            verify(mockRequest).onMessage(requestMessagesCaptor.capture());

            Function<Message, Maybe<Message>> requestMessages = requestMessagesCaptor.getValue();
            DefaultMessage message = DefaultMessage
                .builder()
                .id("id")
                .content(Buffer.buffer())
                .attributes(Map.of(ATTR_INTERNAL_MESSAGE_RECORDABLE, true, ATTR_INTERNAL_MESSAGE_RECORDABLE_WITH_LOGGING, false))
                .build();
            Message returnMessage = requestMessages.apply(message).blockingGet();
            assertThat(message).isSameAs(returnMessage);
            assertThat(message.<Boolean>attribute(ATTR_INTERNAL_MESSAGE_RECORDABLE)).isTrue();
            ArgumentCaptor<MessageMetrics> argumentCaptor = ArgumentCaptor.forClass(MessageMetrics.class);
            verify(reporterService, times(1)).report(argumentCaptor.capture());

            MessageMetrics metrics = argumentCaptor.getValue();
            assertThat(metrics.getRequestId()).isEqualTo("requestId");
            assertThat(metrics.getClientIdentifier()).isEqualTo("clientIdentifier");
            assertThat(metrics.getCorrelationId()).isNotNull();
            assertThat(metrics.getParentCorrelationId()).isNull();
            assertThat(metrics.getCount()).isEqualTo(1);
            assertThat(metrics.getOperation()).isEqualTo(MessageOperation.PUBLISH);
            assertThat(metrics.getConnectorType()).isEqualTo(MessageConnectorType.ENDPOINT);
            assertThat(metrics.getConnectorId()).isEqualTo("endpointId");
        }
    }

    @Nested
    class Post {

        @Test
        void should_not_apply_transformer_when_metrics_disabled() {
            when(mockMetrics.isEnabled()).thenReturn(false);
            cut.post("id", ctx, ExecutionPhase.MESSAGE_RESPONSE).test().assertResult();
            verifyNoInteractions(mockRequest);
        }

        @Test
        void should_not_apply_transformer_when_analytics_is_null() {
            ctx.setInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_ANALYTICS_CONTEXT, null);
            cut.post("id", ctx, ExecutionPhase.MESSAGE_RESPONSE).test().assertResult();
            verifyNoInteractions(mockRequest);
        }

        @Test
        void should_report_metrics_when_analytics_is_enabled() {
            cut.post("id", ctx, ExecutionPhase.MESSAGE_RESPONSE).test().assertResult();

            ArgumentCaptor<Function<Message, Maybe<Message>>> requestMessagesCaptor = ArgumentCaptor.forClass(Function.class);
            verify(mockResponse).onMessage(requestMessagesCaptor.capture());

            Function<Message, Maybe<Message>> requestMessages = requestMessagesCaptor.getValue();
            DefaultMessage message = new DefaultMessage("1");
            Message returnMessage = requestMessages.apply(message).blockingGet();
            assertThat(message).isSameAs(returnMessage);
            assertThat(message.<Boolean>attribute(ATTR_INTERNAL_MESSAGE_RECORDABLE)).isTrue();
            ArgumentCaptor<MessageMetrics> metricsCaptor = ArgumentCaptor.forClass(MessageMetrics.class);
            verify(reporterService, times(1)).report(metricsCaptor.capture());

            MessageMetrics metrics = metricsCaptor.getValue();
            assertThat(metrics.getRequestId()).isEqualTo("requestId");
            assertThat(metrics.getClientIdentifier()).isEqualTo("clientIdentifier");
            assertThat(metrics.getCorrelationId()).isNotNull();
            assertThat(metrics.getParentCorrelationId()).isNull();
            assertThat(metrics.getCount()).isEqualTo(1);
            assertThat(metrics.getOperation()).isEqualTo(MessageOperation.SUBSCRIBE);
            assertThat(metrics.getConnectorType()).isEqualTo(MessageConnectorType.ENDPOINT);
            assertThat(metrics.getConnectorId()).isEqualTo("endpointId");
        }

        @Test
        void should_report_metrics_and_logs_when_logging_is_enabled() {
            Logging logging = new Logging();
            LoggingMode mode = new LoggingMode();
            mode.setEndpoint(true);
            logging.setMode(mode);
            LoggingPhase loggingPhase = new LoggingPhase();
            loggingPhase.setResponse(true);
            logging.setPhase(loggingPhase);
            analytics.setLogging(logging);
            AnalyticsContext analyticsContext = new AnalyticsContext(analytics, true, null, null);
            ctx.setInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_ANALYTICS_CONTEXT, analyticsContext);

            cut.post("id", ctx, ExecutionPhase.MESSAGE_RESPONSE).test().assertResult();

            ArgumentCaptor<Function<Message, Maybe<Message>>> requestMessagesCaptor = ArgumentCaptor.forClass(Function.class);
            verify(mockResponse).onMessage(requestMessagesCaptor.capture());

            Function<Message, Maybe<Message>> requestMessages = requestMessagesCaptor.getValue();
            DefaultMessage message = new DefaultMessage("1");
            Message returnMessage = requestMessages.apply(message).blockingGet();
            assertThat(message).isSameAs(returnMessage);
            assertThat(message.<Boolean>attribute(ATTR_INTERNAL_MESSAGE_RECORDABLE)).isTrue();
            ArgumentCaptor<Reportable> reportableArgumentCaptor = ArgumentCaptor.forClass(Reportable.class);
            verify(reporterService, times(2)).report(reportableArgumentCaptor.capture());

            List<Reportable> reportables = reportableArgumentCaptor.getAllValues();
            MessageMetrics metrics = (MessageMetrics) reportables.get(0);
            assertThat(metrics.getRequestId()).isEqualTo("requestId");
            assertThat(metrics.getClientIdentifier()).isEqualTo("clientIdentifier");
            assertThat(metrics.getCorrelationId()).isNotNull();
            assertThat(metrics.getParentCorrelationId()).isNull();
            assertThat(metrics.getCount()).isEqualTo(1);
            assertThat(metrics.getOperation()).isEqualTo(MessageOperation.SUBSCRIBE);
            assertThat(metrics.getConnectorType()).isEqualTo(MessageConnectorType.ENDPOINT);
            assertThat(metrics.getConnectorId()).isEqualTo("endpointId");

            MessageLog messageLog = (MessageLog) reportables.get(1);
            assertThat(messageLog.getRequestId()).isEqualTo("requestId");
            assertThat(messageLog.getClientIdentifier()).isEqualTo("clientIdentifier");
            assertThat(messageLog.getCorrelationId()).isNotNull();
            assertThat(messageLog.getParentCorrelationId()).isNull();
            assertThat(metrics.getOperation()).isEqualTo(MessageOperation.SUBSCRIBE);
            assertThat(metrics.getConnectorType()).isEqualTo(MessageConnectorType.ENDPOINT);
            assertThat(metrics.getConnectorId()).isEqualTo("endpointId");
            assertThat(messageLog.getMessage()).isInstanceOf(MessageLogEndpointResponse.class);
        }

        @Test
        void should_report_metrics_and_logs_when_message_condition_is_true() {
            Logging logging = new Logging();
            LoggingMode mode = new LoggingMode();
            mode.setEndpoint(true);
            logging.setMode(mode);
            LoggingPhase loggingPhase = new LoggingPhase();
            loggingPhase.setResponse(true);
            logging.setPhase(loggingPhase);
            logging.setMessageCondition("{#message.id == '1'}");
            analytics.setLogging(logging);
            AnalyticsContext analyticsContext = new AnalyticsContext(analytics, true, null, null);
            ctx.setInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_ANALYTICS_CONTEXT, analyticsContext);

            cut.post("id", ctx, ExecutionPhase.MESSAGE_RESPONSE).test().assertResult();

            ArgumentCaptor<Function<Message, Maybe<Message>>> requestMessagesCaptor = ArgumentCaptor.forClass(Function.class);
            verify(mockResponse).onMessage(requestMessagesCaptor.capture());

            Function<Message, Maybe<Message>> requestMessages = requestMessagesCaptor.getValue();
            DefaultMessage message = DefaultMessage.builder().id("1").content(Buffer.buffer("content")).build();
            Message returnMessage = requestMessages.apply(message).blockingGet();
            assertThat(message).isSameAs(returnMessage);
            assertThat(message.<Boolean>attribute(ATTR_INTERNAL_MESSAGE_RECORDABLE)).isTrue();
            ArgumentCaptor<Reportable> reportableArgumentCaptor = ArgumentCaptor.forClass(Reportable.class);
            verify(reporterService, times(2)).report(reportableArgumentCaptor.capture());

            List<Reportable> reportables = reportableArgumentCaptor.getAllValues();
            MessageMetrics metrics = (MessageMetrics) reportables.get(0);
            assertThat(metrics.getRequestId()).isEqualTo("requestId");
            assertThat(metrics.getClientIdentifier()).isEqualTo("clientIdentifier");
            assertThat(metrics.getCorrelationId()).isNotNull();
            assertThat(metrics.getParentCorrelationId()).isNull();
            assertThat(metrics.getCount()).isEqualTo(1);
            assertThat(metrics.getOperation()).isEqualTo(MessageOperation.SUBSCRIBE);
            assertThat(metrics.getConnectorType()).isEqualTo(MessageConnectorType.ENDPOINT);
            assertThat(metrics.getConnectorId()).isEqualTo("endpointId");

            MessageLog messageLog = (MessageLog) reportables.get(1);
            assertThat(messageLog.getRequestId()).isEqualTo("requestId");
            assertThat(messageLog.getClientIdentifier()).isEqualTo("clientIdentifier");
            assertThat(messageLog.getCorrelationId()).isNotNull();
            assertThat(messageLog.getParentCorrelationId()).isNull();
            assertThat(metrics.getOperation()).isEqualTo(MessageOperation.SUBSCRIBE);
            assertThat(metrics.getConnectorType()).isEqualTo(MessageConnectorType.ENDPOINT);
            assertThat(metrics.getConnectorId()).isEqualTo("endpointId");
            assertThat(messageLog.getMessage()).isInstanceOf(MessageLogEndpointResponse.class);
        }

        @Test
        void should_report_metrics_but_not_logs_when_message_condition_is_false() {
            Logging logging = new Logging();
            LoggingMode mode = new LoggingMode();
            mode.setEntrypoint(true);
            logging.setMode(mode);
            LoggingPhase loggingPhase = new LoggingPhase();
            loggingPhase.setRequest(true);
            logging.setPhase(loggingPhase);
            logging.setMessageCondition("{#message.id == 'bad'}");
            analytics.setLogging(logging);
            AnalyticsContext analyticsContext = new AnalyticsContext(analytics, true, null, null);
            ctx.setInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_ANALYTICS_CONTEXT, analyticsContext);

            cut.post("id", ctx, ExecutionPhase.MESSAGE_RESPONSE).test().assertResult();

            ArgumentCaptor<Function<Message, Maybe<Message>>> requestMessagesCaptor = ArgumentCaptor.forClass(Function.class);
            verify(mockResponse).onMessage(requestMessagesCaptor.capture());

            Function<Message, Maybe<Message>> requestMessages = requestMessagesCaptor.getValue();
            DefaultMessage message = DefaultMessage.builder().id("1").content(Buffer.buffer("content")).build();
            Message returnMessage = requestMessages.apply(message).blockingGet();
            assertThat(message).isSameAs(returnMessage);
            assertThat(message.<Boolean>attribute(ATTR_INTERNAL_MESSAGE_RECORDABLE)).isTrue();
            ArgumentCaptor<Reportable> reportableArgumentCaptor = ArgumentCaptor.forClass(Reportable.class);
            verify(reporterService, times(1)).report(reportableArgumentCaptor.capture());

            Reportable reportable = reportableArgumentCaptor.getValue();
            MessageMetrics metrics = (MessageMetrics) reportable;
            assertThat(metrics.getRequestId()).isEqualTo("requestId");
            assertThat(metrics.getClientIdentifier()).isEqualTo("clientIdentifier");
            assertThat(metrics.getCorrelationId()).isNotNull();
            assertThat(metrics.getParentCorrelationId()).isNull();
            assertThat(metrics.getCount()).isEqualTo(1);
            assertThat(metrics.getOperation()).isEqualTo(MessageOperation.SUBSCRIBE);
            assertThat(metrics.getConnectorType()).isEqualTo(MessageConnectorType.ENDPOINT);
            assertThat(metrics.getConnectorId()).isEqualTo("endpointId");
        }
    }

    private static Analytics prepareAnalytics() {
        Analytics analytics = new Analytics();
        Sampling messageSampling = new Sampling();
        messageSampling.setType(SamplingType.COUNT);
        analytics.setMessageSampling(messageSampling);
        return analytics;
    }
}

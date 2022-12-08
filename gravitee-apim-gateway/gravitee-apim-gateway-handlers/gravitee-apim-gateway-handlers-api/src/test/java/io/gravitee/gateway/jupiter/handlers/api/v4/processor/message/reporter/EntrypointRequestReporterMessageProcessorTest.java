package io.gravitee.gateway.jupiter.handlers.api.v4.processor.message.reporter;/**
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

import static io.gravitee.gateway.jupiter.api.context.InternalContextAttributes.ATTR_INTERNAL_MESSAGE_RECORDABLE;
import static org.assertj.core.api.Assertions.assertThat;
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
import io.gravitee.gateway.jupiter.api.connector.entrypoint.EntrypointConnector;
import io.gravitee.gateway.jupiter.api.context.InternalContextAttributes;
import io.gravitee.gateway.jupiter.api.message.DefaultMessage;
import io.gravitee.gateway.jupiter.api.message.Message;
import io.gravitee.gateway.jupiter.core.v4.analytics.AnalyticsContext;
import io.gravitee.gateway.jupiter.handlers.api.v4.analytics.logging.request.message.MessageLogEntrypointRequest;
import io.gravitee.gateway.jupiter.handlers.api.v4.processor.AbstractV4ProcessorTest;
import io.gravitee.gateway.report.ReporterService;
import io.gravitee.reporter.api.Reportable;
import io.gravitee.reporter.api.v4.common.MessageConnectorType;
import io.gravitee.reporter.api.v4.common.MessageOperation;
import io.gravitee.reporter.api.v4.log.MessageLog;
import io.gravitee.reporter.api.v4.metric.MessageMetrics;
import io.reactivex.rxjava3.core.Maybe;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
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
class EntrypointRequestReporterMessageProcessorTest extends AbstractV4ProcessorTest {

    @Mock
    private EntrypointConnector entrypointConnector;

    @Mock
    private ReporterService reporterService;

    private EntrypointRequestReporterMessageProcessor reporterMessageProcessor;
    private Analytics analytics;

    @BeforeEach
    public void beforeEach() {
        reporterMessageProcessor = new EntrypointRequestReporterMessageProcessor(reporterService);

        when(mockMetrics.isEnabled()).thenReturn(true);
        analytics = prepareAnalytics();
        AnalyticsContext analyticsContext = new AnalyticsContext(analytics, true, null, null);
        ctx.setInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_ANALYTICS_CONTEXT, analyticsContext);
        ctx.setInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_ENTRYPOINT_CONNECTOR, entrypointConnector);
        lenient().when(entrypointConnector.id()).thenReturn("entrypointId");
    }

    @Test
    void should_not_apply_transformer_when_metrics_disabled() {
        when(mockMetrics.isEnabled()).thenReturn(false);
        reporterMessageProcessor.execute(ctx).test().assertResult();
        verifyNoInteractions(mockRequest);
    }

    @Test
    void should_not_apply_transformer_when_analytics_is_null() {
        ctx.setInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_ANALYTICS_CONTEXT, null);
        reporterMessageProcessor.execute(ctx).test().assertResult();
        verifyNoInteractions(mockRequest);
    }

    @Test
    void should_report_metrics_when_analytics_is_enabled() {
        reporterMessageProcessor.execute(ctx).test().assertResult();

        ArgumentCaptor<Function<Message, Maybe<Message>>> requestMessagesCaptor = ArgumentCaptor.forClass(Function.class);
        verify(mockRequest).onMessage(requestMessagesCaptor.capture());

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
        assertThat(metrics.getOperation()).isEqualTo(MessageOperation.PUBLISH);
        assertThat(metrics.getConnectorType()).isEqualTo(MessageConnectorType.ENTRYPOINT);
        assertThat(metrics.getConnectorId()).isEqualTo("entrypointId");
    }

    @Test
    void should_report_metrics_and_logs_when_logging_is_enabled() {
        Logging logging = new Logging();
        LoggingMode mode = new LoggingMode();
        mode.setEntrypoint(true);
        logging.setMode(mode);
        LoggingPhase loggingPhase = new LoggingPhase();
        loggingPhase.setRequest(true);
        logging.setPhase(loggingPhase);
        analytics.setLogging(logging);
        AnalyticsContext analyticsContext = new AnalyticsContext(analytics, true, null, null);
        ctx.setInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_ANALYTICS_CONTEXT, analyticsContext);

        reporterMessageProcessor.execute(ctx).test().assertResult();

        ArgumentCaptor<Function<Message, Maybe<Message>>> requestMessagesCaptor = ArgumentCaptor.forClass(Function.class);
        verify(mockRequest).onMessage(requestMessagesCaptor.capture());

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
        assertThat(metrics.getOperation()).isEqualTo(MessageOperation.PUBLISH);
        assertThat(metrics.getConnectorType()).isEqualTo(MessageConnectorType.ENTRYPOINT);
        assertThat(metrics.getConnectorId()).isEqualTo("entrypointId");

        MessageLog messageLog = (MessageLog) reportables.get(1);
        assertThat(messageLog.getRequestId()).isEqualTo("requestId");
        assertThat(messageLog.getClientIdentifier()).isEqualTo("clientIdentifier");
        assertThat(messageLog.getCorrelationId()).isNotNull();
        assertThat(messageLog.getParentCorrelationId()).isNull();
        assertThat(messageLog.getOperation()).isEqualTo(MessageOperation.PUBLISH);
        assertThat(messageLog.getConnectorType()).isEqualTo(MessageConnectorType.ENTRYPOINT);
        assertThat(messageLog.getConnectorId()).isEqualTo("entrypointId");
        assertThat(messageLog.getMessage()).isInstanceOf(MessageLogEntrypointRequest.class);
    }

    @Test
    void should_report_metrics_and_logs_when_message_condition_is_true() {
        Logging logging = new Logging();
        LoggingMode mode = new LoggingMode();
        mode.setEntrypoint(true);
        logging.setMode(mode);
        LoggingPhase loggingPhase = new LoggingPhase();
        loggingPhase.setRequest(true);
        logging.setPhase(loggingPhase);
        logging.setMessageCondition("{#message.id == '1'}");
        analytics.setLogging(logging);
        AnalyticsContext analyticsContext = new AnalyticsContext(analytics, true, null, null);
        ctx.setInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_ANALYTICS_CONTEXT, analyticsContext);

        reporterMessageProcessor.execute(ctx).test().assertResult();

        ArgumentCaptor<Function<Message, Maybe<Message>>> requestMessagesCaptor = ArgumentCaptor.forClass(Function.class);
        verify(mockRequest).onMessage(requestMessagesCaptor.capture());

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
        assertThat(metrics.getOperation()).isEqualTo(MessageOperation.PUBLISH);
        assertThat(metrics.getConnectorType()).isEqualTo(MessageConnectorType.ENTRYPOINT);
        assertThat(metrics.getConnectorId()).isEqualTo("entrypointId");

        MessageLog messageLog = (MessageLog) reportables.get(1);
        assertThat(messageLog.getRequestId()).isEqualTo("requestId");
        assertThat(messageLog.getClientIdentifier()).isEqualTo("clientIdentifier");
        assertThat(messageLog.getCorrelationId()).isNotNull();
        assertThat(messageLog.getParentCorrelationId()).isNull();
        assertThat(messageLog.getOperation()).isEqualTo(MessageOperation.PUBLISH);
        assertThat(messageLog.getConnectorType()).isEqualTo(MessageConnectorType.ENTRYPOINT);
        assertThat(messageLog.getConnectorId()).isEqualTo("entrypointId");
        assertThat(messageLog.getMessage()).isInstanceOf(MessageLogEntrypointRequest.class);
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

        reporterMessageProcessor.execute(ctx).test().assertResult();

        ArgumentCaptor<Function<Message, Maybe<Message>>> requestMessagesCaptor = ArgumentCaptor.forClass(Function.class);
        verify(mockRequest).onMessage(requestMessagesCaptor.capture());

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
        assertThat(metrics.getOperation()).isEqualTo(MessageOperation.PUBLISH);
        assertThat(metrics.getConnectorType()).isEqualTo(MessageConnectorType.ENTRYPOINT);
        assertThat(metrics.getConnectorId()).isEqualTo("entrypointId");
    }

    private static Analytics prepareAnalytics() {
        Analytics analytics = new Analytics();
        Sampling messageSampling = new Sampling();
        messageSampling.setType(SamplingType.COUNT);
        analytics.setMessageSampling(messageSampling);
        return analytics;
    }
}

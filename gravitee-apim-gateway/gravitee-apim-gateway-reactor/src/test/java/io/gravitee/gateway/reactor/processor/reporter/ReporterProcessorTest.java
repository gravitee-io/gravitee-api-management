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
package io.gravitee.gateway.reactor.processor.reporter;

<<<<<<< HEAD
import static org.mockito.Mockito.*;

import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.core.processor.Processor;
import io.gravitee.gateway.report.ReporterService;
import io.gravitee.reporter.api.http.Metrics;
import io.gravitee.reporter.api.log.Log;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReporterProcessorTest {

    private ReporterService reporterService;
    private ReporterProcessor processor;
    private Processor<ExecutionContext> next;
    private ExecutionContext context;
    private Request request;
=======
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.core.processor.AbstractProcessor;
import io.gravitee.gateway.report.ReporterService;
import io.gravitee.reporter.api.http.Metrics;
import io.gravitee.reporter.api.v4.metric.Diagnostic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReporterProcessorTest {

    private ReporterProcessor cut;

    @Mock
    private ReporterService reporterService;

    @Mock
    private AbstractProcessor<ExecutionContext> processorNext;

    @Mock
    private ExecutionContext executionContext;

    @Mock
    private Request request;

>>>>>>> ff653a0deb (fix(gateway): restore error fields in legacy engine logs (APIM-12654))
    private Metrics metrics;

    @BeforeEach
    void setUp() {
<<<<<<< HEAD
        reporterService = mock(ReporterService.class);
        next = mock(Processor.class);

        processor = new ReporterProcessor(reporterService);
        processor.handler(next);

        context = mock(ExecutionContext.class);
        request = mock(Request.class);
        metrics = mock(Metrics.class);

        when(context.request()).thenReturn(request);
=======
        cut = new ReporterProcessor(reporterService);
        cut.handler(processorNext);
        metrics = Metrics.on(System.currentTimeMillis()).build();
        when(executionContext.request()).thenReturn(request);
>>>>>>> ff653a0deb (fix(gateway): restore error fields in legacy engine logs (APIM-12654))
        when(request.metrics()).thenReturn(metrics);
    }

    @Test
<<<<<<< HEAD
    void should_report_metrics() {
        processor.handle(context);

        verify(reporterService).report(metrics);
        verify(next).handle(context);
    }

    @Test
    void should_report_log_when_present() {
        Log log = mock(Log.class);

        when(metrics.getLog()).thenReturn(log);
        when(metrics.getApi()).thenReturn("api-id");
        when(metrics.getApiName()).thenReturn("api-name");

        processor.handle(context);

        // verify log enrichment
        verify(log).setApi("api-id");
        verify(log).setApiName("api-name");

        // verify log reported
        verify(reporterService).report(log);
    }

    @Test
    void should_not_report_log_when_absent() {
        when(metrics.getLog()).thenReturn(null);

        processor.handle(context);

        verify(reporterService, never()).report(isA(Log.class));
    }

    @Test
    void should_add_quota_metrics_when_present() {
        when(context.getAttribute(ExecutionContext.ATTR_QUOTA_COUNT)).thenReturn(5L);
        when(context.getAttribute(ExecutionContext.ATTR_QUOTA_LIMIT)).thenReturn(10L);

        processor.handle(context);

        verify(metrics).putAdditionalMetric("long_quota.count", 5L);
        verify(metrics).putAdditionalMetric("long_quota.limit", 10L);
    }

    @Test
    void should_not_add_quota_metrics_when_missing() {
        when(context.getAttribute(ExecutionContext.ATTR_QUOTA_COUNT)).thenReturn(null);
        when(context.getAttribute(ExecutionContext.ATTR_QUOTA_LIMIT)).thenReturn(null);

        processor.handle(context);

        verify(metrics, never()).putAdditionalMetric(eq("long_quota.count"), anyLong());
        verify(metrics, never()).putAdditionalMetric(eq("long_quota.limit"), anyLong());
    }

    @Test
    void should_handle_exception_and_continue() {
        doThrow(new RuntimeException("boom")).when(reporterService).report(metrics);

        processor.handle(context);

        verify(next).handle(context);
=======
    @DisplayName("Should create Diagnostic when errorKey and message are present")
    void shouldCreateDiagnosticWhenErrorKeyAndMessagePresent() {
        metrics.setErrorKey("GATEWAY_PLAN_UNRESOLVABLE");
        metrics.setMessage("Unauthorized");

        cut.handle(executionContext);

        assertThat(metrics.getFailure()).isNotNull();
        assertThat(metrics.getFailure().getKey()).isEqualTo("GATEWAY_PLAN_UNRESOLVABLE");
        assertThat(metrics.getFailure().getMessage()).isEqualTo("Unauthorized");
        assertThat(metrics.getFailure().getComponentType()).isNull();
        assertThat(metrics.getFailure().getComponentName()).isNull();
        verify(reporterService).report(metrics);
        verify(processorNext).handle(executionContext);
    }

    @Test
    @DisplayName("Should use internal_error as default key when errorKey is null")
    void shouldUseDefaultKeyWhenErrorKeyNull() {
        metrics.setMessage("Some error");

        cut.handle(executionContext);

        assertThat(metrics.getFailure()).isNotNull();
        assertThat(metrics.getFailure().getKey()).isEqualTo("internal_error");
        assertThat(metrics.getFailure().getMessage()).isEqualTo("Some error");
        verify(reporterService).report(metrics);
    }

    @Test
    @DisplayName("Should not create Diagnostic when message is null")
    void shouldNotCreateDiagnosticWhenMessageNull() {
        metrics.setErrorKey("GATEWAY_PLAN_UNRESOLVABLE");

        cut.handle(executionContext);

        assertThat(metrics.getFailure()).isNull();
        verify(reporterService).report(metrics);
    }

    @Test
    @DisplayName("Should not create Diagnostic when message is blank")
    void shouldNotCreateDiagnosticWhenMessageBlank() {
        metrics.setErrorKey("GATEWAY_PLAN_UNRESOLVABLE");
        metrics.setMessage("   ");

        cut.handle(executionContext);

        assertThat(metrics.getFailure()).isNull();
        verify(reporterService).report(metrics);
    }

    @Test
    @DisplayName("Should not override existing Diagnostic failure")
    void shouldNotOverrideExistingFailure() {
        Diagnostic existing = new Diagnostic("existing_key", "existing_message", "comp_type", "comp_name");
        metrics.setFailure(existing);
        metrics.setErrorKey("GATEWAY_PLAN_UNRESOLVABLE");
        metrics.setMessage("Unauthorized");

        cut.handle(executionContext);

        assertThat(metrics.getFailure()).isSameAs(existing);
        verify(reporterService).report(metrics);
    }

    @Test
    @DisplayName("Should report metrics even without error information")
    void shouldReportMetricsWithoutErrors() {
        cut.handle(executionContext);

        assertThat(metrics.getFailure()).isNull();
        verify(reporterService).report(metrics);
        verify(processorNext).handle(executionContext);
>>>>>>> ff653a0deb (fix(gateway): restore error fields in legacy engine logs (APIM-12654))
    }
}

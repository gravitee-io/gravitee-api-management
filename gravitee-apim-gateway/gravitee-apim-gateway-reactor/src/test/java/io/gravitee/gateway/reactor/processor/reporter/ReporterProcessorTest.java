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

import static org.mockito.Mockito.*;

import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.core.processor.Processor;
import io.gravitee.gateway.report.ReporterService;
import io.gravitee.reporter.api.http.Metrics;
import io.gravitee.reporter.api.log.Log;
import io.gravitee.reporter.api.v4.metric.Diagnostic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReporterProcessorTest {

    private ReporterService reporterService;
    private ReporterProcessor processor;
    private Processor<ExecutionContext> next;
    private ExecutionContext context;
    private Request request;
    private Metrics metrics;

    @BeforeEach
    void setUp() {
        reporterService = mock(ReporterService.class);
        next = mock(Processor.class);

        processor = new ReporterProcessor(reporterService);
        processor.handler(next);

        context = mock(ExecutionContext.class);
        request = mock(Request.class);
        metrics = mock(Metrics.class);

        when(context.request()).thenReturn(request);
        when(request.metrics()).thenReturn(metrics);
    }

    @Test
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
    }

    @Test
    void should_create_diagnostic_when_error_key_and_message_present() {
        when(metrics.getErrorKey()).thenReturn("GATEWAY_PLAN_UNRESOLVABLE");
        when(metrics.getMessage()).thenReturn("Unauthorized");

        processor.handle(context);

        verify(metrics).setFailure(new Diagnostic("GATEWAY_PLAN_UNRESOLVABLE", "Unauthorized", null, null));
    }

    @Test
    void should_use_internal_error_key_when_error_key_null() {
        when(metrics.getMessage()).thenReturn("Some error");

        processor.handle(context);

        verify(metrics).setFailure(new Diagnostic("internal_error", "Some error", null, null));
    }

    @Test
    void should_not_create_diagnostic_when_message_null() {
        when(metrics.getErrorKey()).thenReturn("GATEWAY_PLAN_UNRESOLVABLE");

        processor.handle(context);

        verify(metrics, never()).setFailure(any());
    }

    @Test
    void should_not_create_diagnostic_when_message_blank() {
        when(metrics.getErrorKey()).thenReturn("GATEWAY_PLAN_UNRESOLVABLE");
        when(metrics.getMessage()).thenReturn("   ");

        processor.handle(context);

        verify(metrics, never()).setFailure(any());
    }

    @Test
    void should_not_override_existing_diagnostic_failure() {
        Diagnostic existing = new Diagnostic("existing_key", "existing_message", "comp_type", "comp_name");
        when(metrics.getFailure()).thenReturn(existing);
        when(metrics.getErrorKey()).thenReturn("GATEWAY_PLAN_UNRESOLVABLE");
        when(metrics.getMessage()).thenReturn("Unauthorized");

        processor.handle(context);

        verify(metrics, never()).setFailure(any());
    }
}

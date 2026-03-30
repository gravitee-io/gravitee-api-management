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

    private Metrics metrics;

    @BeforeEach
    void setUp() {
        cut = new ReporterProcessor(reporterService);
        cut.handler(processorNext);
        metrics = Metrics.on(System.currentTimeMillis()).build();
        when(executionContext.request()).thenReturn(request);
        when(request.metrics()).thenReturn(metrics);
    }

    @Test
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
    }
}

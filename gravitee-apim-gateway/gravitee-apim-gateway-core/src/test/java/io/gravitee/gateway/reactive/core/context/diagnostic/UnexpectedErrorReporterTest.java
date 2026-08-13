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
package io.gravitee.gateway.reactive.core.context.diagnostic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.gateway.reactive.api.ExecutionWarn;
import io.gravitee.gateway.reactive.api.context.http.HttpPlainExecutionContext;
import io.gravitee.gateway.reactive.api.context.http.HttpPlainResponse;
import io.gravitee.reporter.api.v4.metric.Diagnostic;
import io.gravitee.reporter.api.v4.metric.Metrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class UnexpectedErrorReporterTest {

    private static final Logger LOG = LoggerFactory.getLogger(UnexpectedErrorReporterTest.class);

    @Mock
    private HttpPlainExecutionContext ctx;

    @Mock
    private HttpPlainResponse response;

    private Metrics metrics;

    @BeforeEach
    void init() {
        metrics = Metrics.builder().build();
        lenient().when(ctx.metrics()).thenReturn(metrics);
        lenient().when(ctx.response()).thenReturn(response);
        lenient().when(response.status()).thenReturn(502);
        lenient()
            .when(ctx.withLogger(any(Logger.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void should_attribute_a_key_of_its_own_when_nothing_was_attributed_to_the_request() {
        UnexpectedErrorReporter.recordAndAnswer(ctx, 500, "Internal Server Error", new IllegalStateException("boom"), LOG);

        // Without this, the request is reported with a 500 and no error key at all, which reads as a success in any
        // dashboard counting failures by key.
        assertThat(metrics.getErrorKey()).isEqualTo(UnexpectedErrorReporter.UNEXPECTED_ERROR_KEY);
        // The cause is what makes the entry actionable: "the gateway failed" alone sends operators to the pod logs.
        assertThat(metrics.getErrorMessage()).contains("boom");
        verify(ctx, never()).warnWith(any());
    }

    @Test
    void should_carry_the_unexpected_exception_into_analytics_when_a_key_is_already_attributed() {
        metrics.setErrorKey("GATEWAY_CLIENT_CONNECTION_CLOSED");
        metrics.setErrorMessage("Connection was closed");

        UnexpectedErrorReporter.recordAndAnswer(
            ctx,
            500,
            "Internal Server Error",
            new IllegalStateException("policy chain already stopped"),
            LOG
        );

        // The failure keeps naming the original cause, so without this warning the exception that actually broke the
        // error handling exists nowhere but the gateway's log file — invisible to whoever reads the analytics.
        final ArgumentCaptor<ExecutionWarn> warn = ArgumentCaptor.forClass(ExecutionWarn.class);
        verify(ctx).warnWith(warn.capture());
        assertThat(warn.getValue().key()).isEqualTo(UnexpectedErrorReporter.UNEXPECTED_ERROR_KEY);
        assertThat(warn.getValue().message()).contains("replacing status 502");
        assertThat(warn.getValue().cause()).hasMessage("policy chain already stopped");
    }

    @Test
    void should_describe_the_most_specific_cause_rather_than_the_wrapper() {
        final Throwable wrapped = new RuntimeException("handling failed", new IllegalStateException("endpoint manager stopped"));

        UnexpectedErrorReporter.recordAndAnswer(ctx, 500, "Internal Server Error", wrapped, LOG);

        // Wrapper messages are generic by nature; the innermost one is the only one naming the actual fault.
        assertThat(metrics.getErrorMessage()).contains("endpoint manager stopped").doesNotContain("handling failed");
    }

    @Test
    void should_preserve_an_error_key_already_recorded_on_the_metrics() {
        metrics.setErrorKey("GATEWAY_CLIENT_CONNECTION_CLOSED");
        metrics.setErrorMessage("Connection was closed");

        UnexpectedErrorReporter.recordAndAnswer(ctx, 500, "Internal Server Error", new IllegalStateException("boom"), LOG);

        // The recorded key names what actually went wrong; reaching this path only says the gateway could not handle
        // the consequence of it.
        assertThat(metrics.getErrorKey()).isEqualTo("GATEWAY_CLIENT_CONNECTION_CLOSED");
        assertThat(metrics.getErrorMessage()).isEqualTo("Connection was closed");
    }

    @Test
    void should_preserve_a_failure_already_attributed_through_an_interruption() {
        // interruptWith records a failure without touching errorKey: the diagnosis lives there, and overwriting it
        // would replace a 502-worth of context with a generic one.
        metrics.setFailure(new Diagnostic("GATEWAY_CLIENT_CONNECTION_CLOSED", "Connection was closed", "ENDPOINT", "https://backend"));

        UnexpectedErrorReporter.recordAndAnswer(ctx, 500, "Internal Server Error", new IllegalStateException("boom"), LOG);

        assertThat(metrics.getErrorKey()).isNull();
        assertThat(metrics.getFailure().getKey()).isEqualTo("GATEWAY_CLIENT_CONNECTION_CLOSED");
    }

    @Test
    void should_answer_with_the_status_it_reports() {
        UnexpectedErrorReporter.recordAndAnswer(ctx, 500, "Internal Server Error", new IllegalStateException("boom"), LOG);

        // Reporting and answering are done together so the two cannot drift: every message states the status the
        // request is answered with, and would describe a response that was never sent if the caller applied its own.
        final InOrder inOrder = inOrder(response);
        inOrder.verify(response).status(500);
        inOrder.verify(response).reason("Internal Server Error");
        assertThat(metrics.getErrorMessage()).contains("answered with a 500");
    }

    @Test
    void should_report_the_status_being_replaced_not_the_one_it_answers() {
        metrics.setErrorKey("GATEWAY_CLIENT_CONNECTION_CLOSED");
        metrics.setErrorMessage("Connection was closed");
        when(response.status()).thenReturn(502);

        UnexpectedErrorReporter.recordAndAnswer(ctx, 500, "Internal Server Error", new IllegalStateException("boom"), LOG);

        // The 502 is what the failure called for; the 500 is what the caller gets. Both matter, and confusing them is
        // exactly what makes this path unreadable in analytics.
        final ArgumentCaptor<ExecutionWarn> warn = ArgumentCaptor.forClass(ExecutionWarn.class);
        verify(ctx).warnWith(warn.capture());
        assertThat(warn.getValue().message()).contains("answered 500").contains("replacing status 502");
    }

    @Test
    void should_not_fail_when_the_request_carries_no_metrics() {
        when(ctx.metrics()).thenReturn(null);

        UnexpectedErrorReporter.recordAndAnswer(ctx, 500, "Internal Server Error", new IllegalStateException("boom"), LOG);
    }
}

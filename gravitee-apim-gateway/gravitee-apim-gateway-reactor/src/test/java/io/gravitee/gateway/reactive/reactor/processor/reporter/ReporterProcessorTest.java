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
package io.gravitee.gateway.reactive.reactor.processor.reporter;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.reactive.api.connector.Connector;
import io.gravitee.gateway.reactive.api.context.ContextAttributes;
import io.gravitee.gateway.reactive.api.context.InternalContextAttributes;
import io.gravitee.gateway.reactive.reactor.processor.AbstractProcessorTest;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.gateway.report.ReporterService;
import io.gravitee.reporter.api.common.Response;
import io.gravitee.reporter.api.v4.log.Log;
import io.gravitee.reporter.api.v4.metric.AdditionalMetric;
import io.gravitee.reporter.api.v4.metric.Metrics;
import io.gravitee.reporter.api.v4.metric.NoopMetrics;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ReporterProcessorTest extends AbstractProcessorTest {

    @Mock
    private ReactableApi reactableApi;

    @Mock
    private ReporterService reporterService;

    @Mock
    private Response response;

    private ReporterProcessor reporterProcessor;

    @BeforeEach
    public void beforeEach() {
        reporterProcessor = new ReporterProcessor(reporterService);
    }

    @Nested
    class ApiV4 {

        @Test
        void should_report_metrics_when_api_v4_found_and_analytics_is_enabled() {
            // Given
            when(reactableApi.getDefinitionVersion()).thenReturn(DefinitionVersion.V4);
            ctx.setInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_REACTABLE_API, reactableApi);
            final Connector mockConnector = mock(Connector.class);
            when(mockConnector.id()).thenReturn("fake-connector");
            ctx.setInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_ENTRYPOINT_CONNECTOR, mockConnector);
            ctx.setAttribute(ContextAttributes.ATTR_QUOTA_COUNT, 4L);
            ctx.setAttribute(ContextAttributes.ATTR_QUOTA_LIMIT, 10L);

            // When
            reporterProcessor.execute(ctx).test().assertResult();

            // Then
            verify(reporterService).report(ctx.metrics());
            assertNull(ctx.metrics().getLog());
            assertThat(ctx.metrics().getEntrypointId()).isEqualTo("fake-connector");

            assertThat(ctx.metrics().getAdditionalMetrics()).contains(new AdditionalMetric.LongMetric("long_quota.limit", 10L));
            assertThat(ctx.metrics().getAdditionalMetrics()).contains(new AdditionalMetric.LongMetric("long_quota.count", 4L));
            verify(reporterService, never()).report(ctx.metrics().getLog());
        }

        @Test
        void should_not_report_metrics_when_api_v4_found_and_analytics_is_disabled() {
            // Given
            ctx.metrics(new NoopMetrics());

            // When
            reporterProcessor.execute(ctx).test().assertResult();

            // Then
            assertNull(ctx.metrics().getLog());
            verifyNoInteractions(reporterService);
        }

        @Test
        void should_report_metrics_and_log_when_analytics_and_log_are_enabled() {
            // Given
            when(reactableApi.getDefinitionVersion()).thenReturn(DefinitionVersion.V4);
            ctx.setInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_REACTABLE_API, reactableApi);
            ctx.metrics().setLog(Log.builder().build());

            // When
            reporterProcessor.execute(ctx).test().assertResult();

            // Then
            assertNotNull(ctx.metrics().getLog());
            verify(reporterService).report(ctx.metrics());
            verify(reporterService).report(ctx.metrics().getLog());
        }

        @Test
        void should_execute_report_actions_when_analytics_and_log_are_enabled() {
            // Given
            when(reactableApi.getDefinitionVersion()).thenReturn(DefinitionVersion.V4);
            ctx.setInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_REACTABLE_API, reactableApi);
            Log log = Log.builder().entrypointResponse(response).build();
            HttpHeaders headers = HttpHeaders.create();
            when(response.getHeaders()).thenReturn(headers);
            when(response.getOnReportActions()).thenReturn(List.of(response -> response.getHeaders().set("X-Gravitee-Test", "test")));
            ctx.metrics().setLog(log);

            // When
            reporterProcessor.execute(ctx).test().assertResult();

            // Then
            assertNotNull(ctx.metrics().getLog());
            assertEquals("test", ctx.metrics().getLog().getEntrypointResponse().getHeaders().get("X-Gravitee-Test"));
        }
    }

    @Nested
    class ApiV2 {

        @Test
        void should_report_metrics_when_api_v2_found_and_analytics_is_enabled() {
            // Given
            when(reactableApi.getDefinitionVersion()).thenReturn(DefinitionVersion.V2);
            ctx.setInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_REACTABLE_API, reactableApi);
            ctx.setAttribute(ExecutionContext.ATTR_QUOTA_COUNT, 4L);
            ctx.setAttribute(ExecutionContext.ATTR_QUOTA_LIMIT, 10L);

            // When
            reporterProcessor.execute(ctx).test().assertResult();

            // Then
            verify(reporterService, never()).report(any(Metrics.class));
            verify(reporterService).report(any(io.gravitee.reporter.api.http.Metrics.class));
            assertNull(ctx.metrics().getLog());
            verify(reporterService, never()).report(any(io.gravitee.reporter.api.log.Log.class));
            verify(reporterService, never()).report(any(Log.class));
            assertThat(ctx.metrics().getAdditionalMetrics()).contains(new AdditionalMetric.LongMetric("long_quota.limit", 10L));
            assertThat(ctx.metrics().getAdditionalMetrics()).contains(new AdditionalMetric.LongMetric("long_quota.count", 4L));
        }

        @Test
        void should_not_report_metrics_when_api_v2_found_and_analytics_is_disabled() {
            // Given
            ctx.metrics(new NoopMetrics());

            // When
            reporterProcessor.execute(ctx).test().assertResult();

            // Then
            assertNull(ctx.metrics().getLog());
            verifyNoInteractions(reporterService);
        }

        @Test
        void should_report_metrics_and_log_when_analytics_and_log_are_enabled() {
            // Given
            when(reactableApi.getDefinitionVersion()).thenReturn(DefinitionVersion.V2);
            ctx.setInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_REACTABLE_API, reactableApi);
            ctx.metrics().setLog(Log.builder().build());

            // When
            reporterProcessor.execute(ctx).test().assertResult();

            // Then
            verify(reporterService, never()).report(any(Metrics.class));
            verify(reporterService).report(any(io.gravitee.reporter.api.http.Metrics.class));
            assertNotNull(ctx.metrics().getLog());
            verify(reporterService).report(any(io.gravitee.reporter.api.log.Log.class));
            verify(reporterService, never()).report(any(Log.class));
        }

        @Test
        void should_execute_report_actions_when_analytics_and_log_are_enabled() {
            // Given
            when(reactableApi.getDefinitionVersion()).thenReturn(DefinitionVersion.V2);
            ctx.setInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_REACTABLE_API, reactableApi);
            Log log = Log.builder().entrypointResponse(response).build();
            HttpHeaders headers = HttpHeaders.create();
            when(response.getHeaders()).thenReturn(headers);
            when(response.getOnReportActions()).thenReturn(List.of(response -> response.getHeaders().set("X-Gravitee-Test", "test")));
            ctx.metrics().setLog(log);

            // When
            reporterProcessor.execute(ctx).test().assertResult();

            // Then
            assertNotNull(ctx.metrics().getLog());
            assertEquals("test", ctx.metrics().getLog().getEntrypointResponse().getHeaders().get("X-Gravitee-Test"));
        }
    }

    @Nested
    class NotFound {

        @Test
        void should_report_metrics_when_no_api_found_and_analytics_is_enabled() {
            // When
            reporterProcessor.execute(ctx).test().assertResult();

            // Then
            verify(reporterService).report(ctx.metrics());
            verify(reporterService).report(ctx.metrics().toV2());
            assertNull(ctx.metrics().getLog());
            verify(reporterService, never()).report(ctx.metrics().getLog());
        }

        @Test
        void should_not_report_metrics_when_no_api_found_and_analytics_are_disabled() {
            // Given
            ctx.metrics(new NoopMetrics());

            // When
            reporterProcessor.execute(ctx).test().assertResult();

            // Then
            assertNull(ctx.metrics().getLog());
            verifyNoInteractions(reporterService);
        }
    }
}

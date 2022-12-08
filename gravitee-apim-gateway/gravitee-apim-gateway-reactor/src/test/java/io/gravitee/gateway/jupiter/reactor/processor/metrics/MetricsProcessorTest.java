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
package io.gravitee.gateway.jupiter.reactor.processor.metrics;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.when;

import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.analytics.Analytics;
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.jupiter.api.context.InternalContextAttributes;
import io.gravitee.gateway.jupiter.reactor.processor.AbstractProcessorTest;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.reporter.api.v4.metric.Metrics;
import io.gravitee.reporter.api.v4.metric.NoopMetrics;
import java.util.Optional;
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
class MetricsProcessorTest extends AbstractProcessorTest {

    @Mock
    private ReactableApi reactableApi;

    @Mock
    private GatewayConfiguration gatewayConfiguration;

    private MetricsProcessor metricsProcessor;

    @Nested
    class ApiV4 {

        @Test
        void should_enabled_metrics_when_api_v4_is_found_and_analytics_are_enabled() {
            // Given
            Api api = new Api();
            api.setAnalytics(new Analytics());
            when(reactableApi.getDefinitionVersion()).thenReturn(api.getDefinitionVersion());
            when(reactableApi.getDefinition()).thenReturn(api);
            ctx.setInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_REACTABLE_API, reactableApi);
            metricsProcessor = new MetricsProcessor(gatewayConfiguration, true);

            // When
            metricsProcessor.execute(ctx).test().assertComplete();

            // Then
            Metrics metrics = ctx.metrics();
            assertThat(metrics.isEnabled()).isTrue();
        }

        @Test
        void should_return_noop_metrics_when_api_v4_is_found_and_analytics_are_disabled() {
            // Given
            Api api = new Api();
            Analytics analytics = new Analytics();
            analytics.setEnabled(false);
            api.setAnalytics(analytics);
            when(reactableApi.getDefinitionVersion()).thenReturn(api.getDefinitionVersion());
            when(reactableApi.getDefinition()).thenReturn(api);
            ctx.setInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_REACTABLE_API, reactableApi);
            metricsProcessor = new MetricsProcessor(gatewayConfiguration, true);

            // When
            metricsProcessor.execute(ctx).test().assertComplete();

            // Then
            Metrics metrics = ctx.metrics();
            assertThat(metrics.isEnabled()).isFalse();
            assertThat(metrics).isInstanceOf(NoopMetrics.class);
        }

        @Test
        void should_set_tenant_and_zone() {
            // Given
            Api api = new Api();
            api.setAnalytics(new Analytics());
            when(reactableApi.getDefinitionVersion()).thenReturn(api.getDefinitionVersion());
            when(reactableApi.getDefinition()).thenReturn(api);
            ctx.setInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_REACTABLE_API, reactableApi);
            metricsProcessor = new MetricsProcessor(gatewayConfiguration, true);

            when(gatewayConfiguration.tenant()).thenReturn(Optional.of("TENANT"));
            when(gatewayConfiguration.zone()).thenReturn(Optional.of("ZONE"));

            // When
            metricsProcessor.execute(ctx).test().assertComplete();

            // Then
            Metrics metrics = ctx.metrics();
            assertThat(metrics.getTenant()).isEqualTo("TENANT");
            assertThat(metrics.getZone()).isEqualTo("ZONE");
        }

        @Test
        void should_not_set_tenant_and_zone_when_analytics_are_disabled() {
            // Given
            Api api = new Api();
            Analytics analytics = new Analytics();
            analytics.setEnabled(false);
            api.setAnalytics(analytics);
            when(reactableApi.getDefinitionVersion()).thenReturn(api.getDefinitionVersion());
            when(reactableApi.getDefinition()).thenReturn(api);
            ctx.setInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_REACTABLE_API, reactableApi);
            metricsProcessor = new MetricsProcessor(gatewayConfiguration, true);

            // When
            metricsProcessor.execute(ctx).test().assertComplete();

            // Then
            Metrics metrics = ctx.metrics();
            assertThat(metrics.getTenant()).isNull();
            assertThat(metrics.getZone()).isNull();
        }
    }

    @Nested
    class ApiV2 {

        @Test
        void should_enabled_metrics_when_api_v2_is_found_and_analytics_are_enabled() {
            // Given
            io.gravitee.definition.model.Api api = new io.gravitee.definition.model.Api();
            when(reactableApi.getDefinitionVersion()).thenReturn(api.getDefinitionVersion());
            ctx.setInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_REACTABLE_API, reactableApi);
            metricsProcessor = new MetricsProcessor(gatewayConfiguration, true);

            // When
            metricsProcessor.execute(ctx).test().assertComplete();

            // Then
            Metrics metrics = ctx.metrics();
            assertThat(metrics.isEnabled()).isTrue();
        }

        @Test
        void should_set_tenant_and_zone() {
            // Given
            io.gravitee.definition.model.Api api = new io.gravitee.definition.model.Api();
            when(reactableApi.getDefinitionVersion()).thenReturn(api.getDefinitionVersion());
            ctx.setInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_REACTABLE_API, reactableApi);
            metricsProcessor = new MetricsProcessor(gatewayConfiguration, true);

            when(gatewayConfiguration.tenant()).thenReturn(Optional.of("TENANT"));
            when(gatewayConfiguration.zone()).thenReturn(Optional.of("ZONE"));

            // When
            metricsProcessor.execute(ctx).test().assertComplete();

            // Then
            Metrics metrics = ctx.metrics();
            assertThat(metrics.getTenant()).isEqualTo("TENANT");
            assertThat(metrics.getZone()).isEqualTo("ZONE");
        }
    }

    @Nested
    class NotFoundApi {

        @Test
        void should_enabled_metrics_when_no_api_found_and_analytics_are_enabled() {
            // Given
            metricsProcessor = new MetricsProcessor(gatewayConfiguration, true);

            // When
            metricsProcessor.execute(ctx).test().assertComplete();

            // Then
            Metrics metrics = ctx.metrics();
            assertThat(metrics.isEnabled()).isTrue();
        }

        @Test
        void should_return_noop_metrics_when_no_api_found_and_analytics_are_disabled() {
            // Given
            metricsProcessor = new MetricsProcessor(gatewayConfiguration, false);

            // When
            metricsProcessor.execute(ctx).test().assertComplete();

            // Then
            Metrics metrics = ctx.metrics();
            assertThat(metrics.isEnabled()).isFalse();
            assertThat(metrics).isInstanceOf(NoopMetrics.class);
        }

        @Test
        void should_set_tenant_and_zone() {
            // Given
            metricsProcessor = new MetricsProcessor(gatewayConfiguration, true);

            when(gatewayConfiguration.tenant()).thenReturn(Optional.of("TENANT"));
            when(gatewayConfiguration.zone()).thenReturn(Optional.of("ZONE"));

            // When
            metricsProcessor.execute(ctx).test().assertComplete();

            // Then
            Metrics metrics = ctx.metrics();
            assertThat(metrics.getTenant()).isEqualTo("TENANT");
            assertThat(metrics.getZone()).isEqualTo("ZONE");
        }
    }
}

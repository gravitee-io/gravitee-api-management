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
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.analytics.Analytics;
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.jupiter.core.context.MutableExecutionContext;
import io.gravitee.gateway.jupiter.core.processor.ProcessorChain;
import io.gravitee.gateway.jupiter.reactor.processor.AbstractProcessorTest;
import io.gravitee.gateway.jupiter.reactor.processor.forward.XForwardForProcessor;
import io.gravitee.reporter.api.v4.metric.Metrics;
import java.util.List;
import java.util.Optional;
import org.checkerframework.checker.units.qual.A;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
class MetricsProcessorTest extends AbstractProcessorTest {

    @Mock
    private GatewayConfiguration gatewayConfiguration;

    private MetricsProcessor metricsProcessor;

    @Test
    void shouldSetTenantAndZoneWhenApiV4() {
        // Given
        Api api = new Api();
        api.setAnalytics(new Analytics());
        when(componentProvider.getComponent(Api.class)).thenReturn(api);
        metricsProcessor = new MetricsProcessor(gatewayConfiguration);

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
    void shouldSetTenantAndZoneWhenApiV2() {
        // Given
        Api api = new Api();
        api.setAnalytics(new Analytics());
        when(componentProvider.getComponent(Api.class)).thenThrow(new NoSuchBeanDefinitionException("wrong"));
        metricsProcessor = new MetricsProcessor(gatewayConfiguration);

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

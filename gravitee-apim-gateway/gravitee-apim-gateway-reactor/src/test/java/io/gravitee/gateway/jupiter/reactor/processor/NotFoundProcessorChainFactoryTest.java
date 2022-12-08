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
package io.gravitee.gateway.jupiter.reactor.processor;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.analytics.Analytics;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.core.component.ComponentProvider;
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.jupiter.api.ExecutionPhase;
import io.gravitee.gateway.jupiter.core.context.DefaultExecutionContext;
import io.gravitee.gateway.jupiter.core.context.MutableRequest;
import io.gravitee.gateway.jupiter.core.context.MutableResponse;
import io.gravitee.gateway.jupiter.core.processor.ProcessorChain;
import io.gravitee.gateway.report.ReporterService;
import io.gravitee.reporter.api.Reportable;
import io.gravitee.reporter.api.v4.metric.Metrics;
import io.reactivex.rxjava3.core.Completable;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.StandardEnvironment;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class NotFoundProcessorChainFactoryTest {

    @Mock
    private ReporterService reporterService;

    @Mock
    private MutableRequest request;

    @Mock
    private MutableResponse response;

    @Mock
    private GatewayConfiguration gatewayConfiguration;

    @Test
    void shouldExecuteNotFoundProcessorChain() {
        // Given
        DefaultExecutionContext notFoundRequestContext = new DefaultExecutionContext(request, response);
        ComponentProvider componentProvider = mock(ComponentProvider.class);
        Api api = new Api();
        api.setAnalytics(new Analytics());
        when(componentProvider.getComponent(Api.class)).thenReturn(api);
        notFoundRequestContext.componentProvider(componentProvider);

        NotFoundProcessorChainFactory notFoundProcessorChainFactory = new NotFoundProcessorChainFactory(
            new StandardEnvironment(),
            reporterService,
            false,
            false,
            gatewayConfiguration
        );
        ProcessorChain processorChain = notFoundProcessorChainFactory.processorChain();

        when(response.end(notFoundRequestContext)).thenReturn(Completable.complete());
        when(response.headers()).thenReturn(HttpHeaders.create());

        when(gatewayConfiguration.tenant()).thenReturn(Optional.of("TENANT"));
        when(gatewayConfiguration.zone()).thenReturn(Optional.of("ZONE"));

        // When
        processorChain.execute(notFoundRequestContext, ExecutionPhase.RESPONSE).test().assertResult();

        // Then
        verify(response).status(HttpStatusCode.NOT_FOUND_404);
        verify(response).end(notFoundRequestContext);
        verify(reporterService).report(any(Reportable.class));
        Metrics metrics = notFoundRequestContext.metrics();
        assertTrue(metrics.getGatewayResponseTimeMs() > 0);
        assertThat(metrics.getGatewayLatencyMs()).isEqualTo(-1L);
        assertThat(metrics.getApiId()).isEqualTo("1");
        assertThat(metrics.getApplicationId()).isEqualTo("1");
        assertThat(metrics.getTenant()).isEqualTo("TENANT");
        assertThat(metrics.getZone()).isEqualTo("ZONE");
    }
}

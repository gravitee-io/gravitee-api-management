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
package io.gravitee.gamma.rest.infra.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.analytics_engine.use_case.ComputeFacetsUseCase;
import io.gravitee.apim.core.analytics_engine.use_case.ComputeMeasuresUseCase;
import io.gravitee.apim.core.analytics_engine.use_case.ComputeTimeSeriesUseCase;
import io.gravitee.apim.core.user.domain_service.UserContextLoader;
import io.gravitee.gamma.rest.core.observability.analytics.port.service_provider.ObservabilityAnalyticsDataPort;
import io.gravitee.gamma.rest.infra.adapter.ObservabilityAnalyticsDataPortAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

/**
 * Spring wiring for the observability analytics domain. Picks up the Gamma analytics use cases and
 * the shared {@code AnalyticsRequestPipeline} via {@link ComponentScan}, and declares the data port
 * adapter as a bean that delegates to the APIM compute use cases (already in the parent context).
 *
 * @author GraviteeSource Team
 */
@Configuration
@ComponentScan(
    basePackages = "io.gravitee.gamma.rest.core.observability.analytics",
    includeFilters = {
        @ComponentScan.Filter(type = FilterType.ANNOTATION, value = UseCase.class),
        @ComponentScan.Filter(type = FilterType.ANNOTATION, value = DomainService.class),
    }
)
public class GammaAnalyticsConfiguration {

    @Bean
    public ObservabilityAnalyticsDataPort observabilityAnalyticsDataPort(
        ComputeMeasuresUseCase computeMeasuresUseCase,
        ComputeFacetsUseCase computeFacetsUseCase,
        ComputeTimeSeriesUseCase computeTimeSeriesUseCase,
        UserContextLoader userContextLoader,
        ObjectMapper objectMapper
    ) {
        return new ObservabilityAnalyticsDataPortAdapter(
            computeMeasuresUseCase,
            computeFacetsUseCase,
            computeTimeSeriesUseCase,
            userContextLoader,
            objectMapper
        );
    }
}

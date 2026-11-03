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
import io.gravitee.apim.core.analytics.domain_service.AiWorkspaceAnalyticsApisDomainService;
import io.gravitee.apim.core.analytics_engine.domain_service.AnalyticsQueryValidator;
import io.gravitee.apim.core.analytics_engine.domain_service.UnitEnrichmentPostProcessor;
import io.gravitee.apim.core.analytics_engine.service_provider.AnalyticsQueryContextProvider;
import io.gravitee.apim.core.analytics_engine.use_case.ComputeFacetsUseCase;
import io.gravitee.apim.core.analytics_engine.use_case.ComputeMeasuresUseCase;
import io.gravitee.apim.core.analytics_engine.use_case.ComputeTimeSeriesUseCase;
import io.gravitee.apim.core.analytics_engine.use_case.GetApiMetricSpecUseCase;
import io.gravitee.apim.core.analytics_engine.use_case.GetMetricFacetSpecUseCase;
import io.gravitee.apim.core.analytics_engine.use_case.GetMetricFilterSpecUseCase;
import io.gravitee.apim.core.user.domain_service.UserContextLoader;
import io.gravitee.gamma.rest.core.observability.analytics.port.service_provider.AnalyticsDefinitionPort;
import io.gravitee.gamma.rest.core.observability.analytics.port.service_provider.ObservabilityAnalyticsDataPort;
import io.gravitee.gamma.rest.core.observability.filter.port.service_provider.FilterRegistry;
import io.gravitee.gamma.rest.infra.adapter.AnalyticsDefinitionPortAdapter;
import io.gravitee.gamma.rest.infra.adapter.ObservabilityAnalyticsDataPortAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

/**
 * Spring wiring for the observability analytics domain. Picks up the Gamma analytics use cases and
 * the shared {@code AnalyticsRequestPipeline} via {@link ComponentScan}, and declares the data and definition
 * port adapters as beans that delegate to the APIM compute and definition use cases (already in the parent
 * context).
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
        AnalyticsQueryValidator analyticsQueryValidator,
        UnitEnrichmentPostProcessor unitEnrichmentPostProcessor,
        UserContextLoader userContextLoader,
        AiWorkspaceAnalyticsApisDomainService workspaceApis,
        AnalyticsQueryContextProvider queryContextProvider,
        ObjectMapper objectMapper
    ) {
        return new ObservabilityAnalyticsDataPortAdapter(
            computeMeasuresUseCase,
            computeFacetsUseCase,
            computeTimeSeriesUseCase,
            analyticsQueryValidator,
            unitEnrichmentPostProcessor,
            userContextLoader,
            workspaceApis,
            queryContextProvider,
            objectMapper
        );
    }

    @Bean
    public AnalyticsDefinitionPort analyticsDefinitionPort(
        GetApiMetricSpecUseCase getApiMetricSpecUseCase,
        GetMetricFacetSpecUseCase getMetricFacetSpecUseCase,
        GetMetricFilterSpecUseCase getMetricFilterSpecUseCase,
        FilterRegistry filterRegistry
    ) {
        return new AnalyticsDefinitionPortAdapter(
            getApiMetricSpecUseCase,
            getMetricFacetSpecUseCase,
            getMetricFilterSpecUseCase,
            filterRegistry
        );
    }
}

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

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.analytics_engine.query_service.BuiltInAdditionalMetricsQueryService;
import io.gravitee.gamma.rest.core.observability.analytics.port.service_provider.ObservabilityAnalyticsDataPort;
import io.gravitee.gamma.rest.core.observability.catalog.port.repository.AdditionalMetricDefinitionRepository;
import io.gravitee.gamma.rest.core.observability.catalog.port.service_provider.AdditionalMetricDiscoveryPort;
import io.gravitee.gamma.rest.core.observability.catalog.port.service_provider.AdditionalMetricGrammarPort;
import io.gravitee.gamma.rest.core.observability.catalog.port.service_provider.BuiltInAdditionalMetricsPort;
import io.gravitee.gamma.rest.core.observability.catalog.port.service_provider.TrafficReadPermissionPort;
import io.gravitee.gamma.rest.infra.adapter.AdditionalMetricGrammarPortAdapter;
import io.gravitee.gamma.rest.infra.adapter.BuiltInAdditionalMetricsPortAdapter;
import io.gravitee.gamma.rest.infra.adapter.EmptyAdditionalMetricDiscoveryPortAdapter;
import io.gravitee.gamma.rest.infra.adapter.GammaMetricDefinitionRepositoryAdapter;
import io.gravitee.gamma.rest.infra.adapter.TrafficReadPermissionPortAdapter;
import io.gravitee.repository.management.api.GammaMetricDefinitionRepository;
import io.gravitee.rest.api.service.PermissionService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Lazy;

/**
 * Spring wiring for the additional metric catalog: the environment's metric definition records and the list
 * merging them with the built-in and discovered fields.
 *
 * <p>{@code @Lazy} on the repository SPI for the reason {@link GammaDashboardsConfiguration} gives.
 *
 * @author GraviteeSource Team
 */
@Configuration
@ComponentScan(
    basePackages = "io.gravitee.gamma.rest.core.observability.catalog",
    includeFilters = {
        @ComponentScan.Filter(type = FilterType.ANNOTATION, value = UseCase.class),
        @ComponentScan.Filter(type = FilterType.ANNOTATION, value = DomainService.class),
    }
)
public class GammaMetricDefinitionsConfiguration {

    @Bean
    public AdditionalMetricDefinitionRepository additionalMetricDefinitionRepository(@Lazy GammaMetricDefinitionRepository repository) {
        return new GammaMetricDefinitionRepositoryAdapter(repository);
    }

    @Bean
    public AdditionalMetricGrammarPort additionalMetricGrammarPort() {
        return new AdditionalMetricGrammarPortAdapter();
    }

    @Bean
    public BuiltInAdditionalMetricsPort builtInAdditionalMetricsPort(BuiltInAdditionalMetricsQueryService queryService) {
        return new BuiltInAdditionalMetricsPortAdapter(queryService);
    }

    /** Replaced by the discovery pass of OBS-74. */
    @Bean
    public AdditionalMetricDiscoveryPort additionalMetricDiscoveryPort() {
        return new EmptyAdditionalMetricDiscoveryPortAdapter();
    }

    @Bean
    public TrafficReadPermissionPort trafficReadPermissionPort(
        PermissionService permissionService,
        ObservabilityAnalyticsDataPort analyticsDataPort
    ) {
        return new TrafficReadPermissionPortAdapter(permissionService, analyticsDataPort);
    }
}

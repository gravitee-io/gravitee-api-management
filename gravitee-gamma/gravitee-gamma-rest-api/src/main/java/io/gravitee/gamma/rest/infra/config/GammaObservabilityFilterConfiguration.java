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

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.analytics_engine.use_case.GetFilterValuesUseCase;
import io.gravitee.apim.core.analytics_engine.use_case.ResolveFilterLabelsUseCase;
import io.gravitee.gamma.rest.core.observability.filter.port.service_provider.FilterRegistry;
import io.gravitee.gamma.rest.core.observability.filter.port.service_provider.ObservabilityFilterDataPort;
import io.gravitee.gamma.rest.infra.adapter.ObservabilityFilterDataPortAdapter;
import io.gravitee.gamma.rest.infra.adapter.SpiFilterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

/**
 * Spring wiring for the shared observability filter engine and its discovery use cases. The registry
 * discovers
 * {@link io.gravitee.gamma.rest.core.observability.filter.port.service_provider.FilterContributor}
 * implementations via {@link java.util.ServiceLoader} at construction — runs once when the rest-api
 * context starts.
 *
 * <p>Use cases are picked up by {@link ComponentScan} filtered on {@link UseCase}: the apim-side
 * {@code UsecaseSpringConfiguration} only scans {@code io.gravitee.apim.core}, so this per-domain
 * config opts its own package in.
 *
 * @author GraviteeSource Team
 */
@Configuration
@ComponentScan(
    basePackages = "io.gravitee.gamma.rest.core.observability.filter",
    includeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION, value = UseCase.class)
)
public class GammaObservabilityFilterConfiguration {

    @Bean
    public FilterRegistry gammaFilterRegistry() {
        return new SpiFilterRegistry();
    }

    /**
     * Delegates KEYWORD value listing and id → label resolution to the platform's analytics use cases
     * (beans of the inherited parent context), reusing their Elasticsearch + management-DB machinery.
     */
    @Bean
    public ObservabilityFilterDataPort observabilityFilterDataPort(
        GetFilterValuesUseCase getFilterValuesUseCase,
        ResolveFilterLabelsUseCase resolveFilterLabelsUseCase
    ) {
        return new ObservabilityFilterDataPortAdapter(getFilterValuesUseCase, resolveFilterLabelsUseCase);
    }
}

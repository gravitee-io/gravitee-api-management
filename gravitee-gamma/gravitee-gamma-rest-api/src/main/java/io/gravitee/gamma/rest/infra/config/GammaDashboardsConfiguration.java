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
import io.gravitee.gamma.rest.core.observability.dashboard.port.repository.DashboardRepository;
import io.gravitee.gamma.rest.infra.adapter.GammaDashboardRepositoryAdapter;
import io.gravitee.repository.management.api.GammaDashboardRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Lazy;

/**
 * Spring wiring for the Gamma dashboards domain.
 *
 * <p>{@code @Lazy} on the SPI parameter is load-bearing: the repository plugin handler registers the
 * {@code GammaDashboardRepository} bean after the rest-api Spring context refresh completes, so eager
 * resolution at {@code @Bean} processing time throws {@code NoSuchBeanDefinitionException}. Mirrors
 * {@code GammaTracingConfiguration}.
 *
 * @author GraviteeSource Team
 */
@Configuration
@ComponentScan(
    basePackages = "io.gravitee.gamma.rest.core.observability.dashboard",
    includeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION, value = UseCase.class)
)
public class GammaDashboardsConfiguration {

    @Bean
    public DashboardRepository gammaDashboardRepositoryPort(@Lazy GammaDashboardRepository gammaDashboardRepository) {
        return new GammaDashboardRepositoryAdapter(gammaDashboardRepository);
    }
}

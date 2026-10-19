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
import io.gravitee.gamma.rest.core.observability.capabilities.port.service_provider.DashboardPermissionPort;
import io.gravitee.gamma.rest.infra.adapter.DashboardPermissionPortAdapter;
import io.gravitee.rest.api.service.PermissionService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

/**
 * Spring wiring for the observability capabilities. Picks up the use case via {@link ComponentScan} and declares
 * the dashboard permission port adapter as a bean; the readable APIs come from the analytics data port.
 *
 * @author GraviteeSource Team
 */
@Configuration
@ComponentScan(
    basePackages = "io.gravitee.gamma.rest.core.observability.capabilities",
    includeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION, value = UseCase.class)
)
public class GammaObservabilityCapabilitiesConfiguration {

    @Bean
    public DashboardPermissionPort dashboardPermissionPort(PermissionService permissionService) {
        return new DashboardPermissionPortAdapter(permissionService);
    }
}

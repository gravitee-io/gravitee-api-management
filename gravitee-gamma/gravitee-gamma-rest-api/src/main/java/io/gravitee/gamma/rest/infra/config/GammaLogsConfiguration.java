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
import io.gravitee.apim.core.api_product.query_service.ApiProductQueryService;
import io.gravitee.apim.core.application.crud_service.ApplicationCrudService;
import io.gravitee.apim.core.gateway.query_service.InstanceQueryService;
import io.gravitee.apim.core.log.crud_service.ConnectionLogsCrudService;
import io.gravitee.apim.core.plan.crud_service.PlanCrudService;
import io.gravitee.apim.core.user.domain_service.UserContextLoader;
import io.gravitee.gamma.rest.core.observability.logs.port.service_provider.ObservabilityLogsDataPort;
import io.gravitee.gamma.rest.infra.adapter.ObservabilityLogsDataPortAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

/**
 * Spring wiring for the observability logs search domain. Picks up the use case via
 * {@link ComponentScan} and declares the data port adapter as a bean.
 *
 * @author GraviteeSource Team
 */
@Configuration
@ComponentScan(
    basePackages = "io.gravitee.gamma.rest.core.observability.logs",
    includeFilters = {
        @ComponentScan.Filter(type = FilterType.ANNOTATION, value = UseCase.class),
        @ComponentScan.Filter(type = FilterType.ANNOTATION, value = DomainService.class),
    }
)
public class GammaLogsConfiguration {

    @Bean
    public ObservabilityLogsDataPort observabilityLogsDataPort(
        ConnectionLogsCrudService connectionLogsCrudService,
        UserContextLoader userContextLoader,
        PlanCrudService planCrudService,
        ApplicationCrudService applicationCrudService,
        InstanceQueryService instanceQueryService,
        ApiProductQueryService apiProductQueryService
    ) {
        return new ObservabilityLogsDataPortAdapter(
            connectionLogsCrudService,
            userContextLoader,
            planCrudService,
            applicationCrudService,
            instanceQueryService,
            apiProductQueryService
        );
    }
}

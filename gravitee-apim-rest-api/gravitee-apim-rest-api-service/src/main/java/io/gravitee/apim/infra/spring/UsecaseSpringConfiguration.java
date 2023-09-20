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
package io.gravitee.apim.infra.spring;

import io.gravitee.apim.core.application.crud_service.ApplicationCrudService;
import io.gravitee.apim.core.log.crud_service.ConnectionLogCrudService;
import io.gravitee.apim.core.log.crud_service.MessageLogCrudService;
import io.gravitee.apim.core.log.usecase.SearchConnectionLogUsecase;
import io.gravitee.apim.core.log.usecase.SearchMessageLogUsecase;
import io.gravitee.apim.core.plan.crud_service.PlanCrudService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UsecaseSpringConfiguration {

    @Bean
    public SearchConnectionLogUsecase searchConnectionLogUsecase(
        ConnectionLogCrudService connectionLogCrudService,
        PlanCrudService planCrudService,
        ApplicationCrudService applicationCrudService
    ) {
        return new SearchConnectionLogUsecase(connectionLogCrudService, planCrudService, applicationCrudService);
    }

    @Bean
    public SearchMessageLogUsecase searchMessageLogUsecase(MessageLogCrudService messageLogCrudService) {
        return new SearchMessageLogUsecase(messageLogCrudService);
    }
}

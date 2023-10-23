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

import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.domain_service.VerifyApiPathDomainService;
import io.gravitee.apim.core.api.query_service.ApiQueryService;
import io.gravitee.apim.core.api.usecase.VerifyApiPathsUsecase;
import io.gravitee.apim.core.application.crud_service.ApplicationCrudService;
import io.gravitee.apim.core.documentation.crud_service.PageCrudService;
import io.gravitee.apim.core.documentation.domain_service.ApiDocumentationDomainService;
import io.gravitee.apim.core.documentation.domain_service.CreateApiDocumentationDomainService;
import io.gravitee.apim.core.documentation.usecase.ApiCreateDocumentationPageUsecase;
import io.gravitee.apim.core.documentation.usecase.ApiGetDocumentationPageUsecase;
import io.gravitee.apim.core.documentation.usecase.ApiGetDocumentationPagesUsecase;
import io.gravitee.apim.core.environment.crud_service.EnvironmentCrudService;
import io.gravitee.apim.core.log.crud_service.ConnectionLogCrudService;
import io.gravitee.apim.core.log.crud_service.MessageLogCrudService;
import io.gravitee.apim.core.log.usecase.SearchConnectionLogUsecase;
import io.gravitee.apim.core.log.usecase.SearchMessageLogUsecase;
import io.gravitee.apim.core.plan.crud_service.PlanCrudService;
import io.gravitee.apim.core.subscription.crud_service.SubscriptionCrudService;
import io.gravitee.apim.core.subscription.domain_service.CloseSubscriptionDomainService;
import io.gravitee.apim.core.subscription.query_service.SubscriptionQueryService;
import io.gravitee.apim.core.subscription.usecase.CloseExpiredSubscriptionsUsecase;
import io.gravitee.apim.core.subscription.usecase.CloseSubscriptionUsecase;
import io.gravitee.apim.infra.sanitizer.HtmlSanitizerImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({ CoreServiceSpringConfiguration.class })
public class UsecaseSpringConfiguration {

    @Bean
    public CloseSubscriptionUsecase closeSubscriptionUsecase(
        SubscriptionCrudService subscriptionCrudService,
        CloseSubscriptionDomainService closeSubscriptionDomainService
    ) {
        return new CloseSubscriptionUsecase(subscriptionCrudService, closeSubscriptionDomainService);
    }

    @Bean
    public CloseExpiredSubscriptionsUsecase closeExpiredSubscriptionsUsecase(
        SubscriptionQueryService subscriptionQueryService,
        ApiQueryService apiQueryService,
        EnvironmentCrudService environmentCrudService,
        CloseSubscriptionDomainService closeSubscriptionDomainService
    ) {
        return new CloseExpiredSubscriptionsUsecase(
            subscriptionQueryService,
            apiQueryService,
            environmentCrudService,
            closeSubscriptionDomainService
        );
    }

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

    @Bean
    public VerifyApiPathsUsecase verifyApiPathDomainUsecase(VerifyApiPathDomainService verifyApiPathDomainService) {
        return new VerifyApiPathsUsecase(verifyApiPathDomainService);
    }

    @Bean
    public ApiGetDocumentationPagesUsecase apiGetDocumentationPagesUsecase(
        ApiDocumentationDomainService apiDocumentationService,
        PageCrudService pageCrudService,
        ApiCrudService apiCrudService
    ) {
        return new ApiGetDocumentationPagesUsecase(apiDocumentationService, pageCrudService, apiCrudService);
    }

    @Bean
    public ApiGetDocumentationPageUsecase apiGetDocumentationPageUsecase(PageCrudService pageCrudService, ApiCrudService apiCrudService) {
        return new ApiGetDocumentationPageUsecase(pageCrudService, apiCrudService);
    }

    @Bean
    public ApiCreateDocumentationPageUsecase apiCreateDocumentationPageUsecase(
        CreateApiDocumentationDomainService apiDocumentationDomainService,
        PageCrudService pageCrudService
    ) {
        return new ApiCreateDocumentationPageUsecase(apiDocumentationDomainService, pageCrudService, new HtmlSanitizerImpl());
    }
}

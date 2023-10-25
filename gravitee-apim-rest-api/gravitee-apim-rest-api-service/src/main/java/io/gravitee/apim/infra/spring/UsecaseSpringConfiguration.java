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
import io.gravitee.apim.core.documentation.domain_service.HomepageDomainService;
import io.gravitee.apim.core.documentation.domain_service.UpdateApiDocumentationDomainService;
import io.gravitee.apim.core.documentation.usecase.*;
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
        ApiCrudService apiCrudService,
        PageCrudService pageCrudService
    ) {
        return new ApiGetDocumentationPagesUsecase(apiDocumentationService, apiCrudService, pageCrudService);
    }

    @Bean
    public ApiGetDocumentationPageUsecase apiGetDocumentationPageUsecase(
        ApiDocumentationDomainService apiDocumentationDomainService,
        ApiCrudService apiCrudService,
        PageCrudService pageCrudService
    ) {
        return new ApiGetDocumentationPageUsecase(apiDocumentationDomainService, apiCrudService, pageCrudService);
    }

    @Bean
    public ApiCreateDocumentationPageUsecase apiCreateDocumentationPageUsecase(
        CreateApiDocumentationDomainService createApiDocumentationDomainService,
        ApiDocumentationDomainService apiDocumentationDomainService,
        HomepageDomainService homepageDomainService,
        PageCrudService pageCrudService
    ) {
        return new ApiCreateDocumentationPageUsecase(
            createApiDocumentationDomainService,
            apiDocumentationDomainService,
            homepageDomainService,
            pageCrudService
        );
    }

    @Bean
    public ApiUpdateDocumentationPageUsecase apiUpdateDocumentationPageUsecase(
        UpdateApiDocumentationDomainService updateApiDocumentationDomainService,
        ApiDocumentationDomainService apiDocumentationDomainService,
        HomepageDomainService homepageDomainService,
        ApiCrudService apiCrudService,
        PageCrudService pageCrudService
    ) {
        return new ApiUpdateDocumentationPageUsecase(
            updateApiDocumentationDomainService,
            apiDocumentationDomainService,
            homepageDomainService,
            apiCrudService,
            pageCrudService
        );
    }

    @Bean
    public ApiPublishDocumentationPageUsecase apiPublishDocumentationPageUsecase(
        ApiDocumentationDomainService apiDocumentationDomainService,
        UpdateApiDocumentationDomainService updateApiDocumentationDomainService,
        ApiCrudService apiCrudService,
        PageCrudService pageCrudService
    ) {
        return new ApiPublishDocumentationPageUsecase(
            apiDocumentationDomainService,
            updateApiDocumentationDomainService,
            apiCrudService,
            pageCrudService
        );
    }
}

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

import io.gravitee.apim.core.api.domain_service.ApiDefinitionParserDomainService;
import io.gravitee.apim.core.api.domain_service.ApiHostValidatorDomainService;
import io.gravitee.apim.core.api.domain_service.VerifyApiPathDomainService;
import io.gravitee.apim.core.api.query_service.ApiQueryService;
import io.gravitee.apim.core.api_key.crud_service.ApiKeyCrudService;
import io.gravitee.apim.core.api_key.domain_service.RevokeApiKeyDomainService;
import io.gravitee.apim.core.api_key.query_service.ApiKeyQueryService;
import io.gravitee.apim.core.application.crud_service.ApplicationCrudService;
import io.gravitee.apim.core.audit.crud_service.AuditCrudService;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.environment.crud_service.EnvironmentCrudService;
import io.gravitee.apim.core.notification.domain_service.TriggerNotificationDomainService;
import io.gravitee.apim.core.plan.crud_service.PlanCrudService;
import io.gravitee.apim.core.subscription.crud_service.SubscriptionCrudService;
import io.gravitee.apim.core.subscription.domain_service.CloseSubscriptionDomainService;
import io.gravitee.apim.core.subscription.domain_service.RejectSubscriptionDomainService;
import io.gravitee.apim.core.user.crud_service.UserCrudService;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CoreServiceSpringConfiguration {

    @Bean
    public AuditDomainService auditDomainService(
        AuditCrudService auditCrudService,
        UserCrudService userCrudService,
        JacksonJsonDiffProcessor jacksonJsonDiffProcessor
    ) {
        return new AuditDomainService(auditCrudService, userCrudService, jacksonJsonDiffProcessor);
    }

    @Bean
    public CloseSubscriptionDomainService closeSubscriptionDomainService(
        SubscriptionCrudService subscriptionCrudService,
        AuditDomainService auditDomainService,
        ApplicationCrudService applicationCrudService,
        TriggerNotificationDomainService triggerNotificationDomainService,
        RejectSubscriptionDomainService rejectSubscriptionDomainService,
        RevokeApiKeyDomainService revokeApiKeyDomainService
    ) {
        return new CloseSubscriptionDomainService(
            subscriptionCrudService,
            applicationCrudService,
            auditDomainService,
            triggerNotificationDomainService,
            rejectSubscriptionDomainService,
            revokeApiKeyDomainService
        );
    }

    @Bean
    public RejectSubscriptionDomainService rejectSubscriptionDomainService(
        SubscriptionCrudService subscriptionCrudService,
        PlanCrudService planCrudService,
        AuditDomainService auditDomainService,
        TriggerNotificationDomainService triggerNotificationDomainService,
        UserCrudService userCrudService
    ) {
        return new RejectSubscriptionDomainService(
            subscriptionCrudService,
            planCrudService,
            auditDomainService,
            triggerNotificationDomainService,
            userCrudService
        );
    }

    @Bean
    public RevokeApiKeyDomainService revokeApiKeyDomainService(
        ApiKeyCrudService apiKeyCrudService,
        ApiKeyQueryService apiKeyQueryService,
        AuditDomainService auditDomainService
    ) {
        return new RevokeApiKeyDomainService(apiKeyCrudService, apiKeyQueryService, auditDomainService);
    }

    @Bean
    public VerifyApiPathDomainService verifyApiPathDomainService(
        EnvironmentCrudService environmentCrudService,
        ApiQueryService apiSearchService,
        ApiDefinitionParserDomainService apiDefinitionParserDomainService,
        ApiHostValidatorDomainService apiHostValidatorDomainService
    ) {
        return new VerifyApiPathDomainService(
            environmentCrudService,
            apiSearchService,
            apiDefinitionParserDomainService,
            apiHostValidatorDomainService
        );
    }
}

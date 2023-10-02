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
package io.gravitee.rest.api.management.rest.spring;

import inmemory.ApiKeyCrudServiceInMemory;
import inmemory.ApiKeyQueryServiceInMemory;
import inmemory.ApiQueryServiceInMemory;
import inmemory.ApplicationCrudServiceInMemory;
import inmemory.AuditCrudServiceInMemory;
import inmemory.ConnectionLogCrudServiceInMemory;
import inmemory.EnvironmentCrudServiceInMemory;
import inmemory.MessageLogCrudServiceInMemory;
import inmemory.PlanCrudServiceInMemory;
import inmemory.SubscriptionCrudServiceInMemory;
import inmemory.TriggerNotificationDomainServiceInMemory;
import inmemory.UserCrudServiceInMemory;
import io.gravitee.apim.core.environment.crud_service.EnvironmentCrudService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InMemoryConfiguration {

    @Bean
    public ApplicationCrudServiceInMemory applicationRepository() {
        return new ApplicationCrudServiceInMemory();
    }

    @Bean
    public ConnectionLogCrudServiceInMemory connectionLogRepository() {
        return new ConnectionLogCrudServiceInMemory();
    }

    @Bean
    public MessageLogCrudServiceInMemory messageLogRepository() {
        return new MessageLogCrudServiceInMemory();
    }

    @Bean
    public PlanCrudServiceInMemory planRepository() {
        return new PlanCrudServiceInMemory();
    }

    @Bean
    public EnvironmentCrudService environmentCrudService() {
        return new EnvironmentCrudServiceInMemory();
    }

    @Bean
    ApiQueryServiceInMemory apiQueryServiceInMemory() {
        return new ApiQueryServiceInMemory();
    }

    @Bean
    AuditCrudServiceInMemory auditCrudServiceInMemory() {
        return new AuditCrudServiceInMemory();
    }

    @Bean
    UserCrudServiceInMemory userCrudServiceInMemory() {
        return new UserCrudServiceInMemory();
    }

    @Bean
    SubscriptionCrudServiceInMemory subscriptionCrudServiceInMemory() {
        return new SubscriptionCrudServiceInMemory();
    }

    @Bean
    ApiKeyCrudServiceInMemory apiKeyCrudServiceInMemory() {
        return new ApiKeyCrudServiceInMemory();
    }

    @Bean
    ApiKeyQueryServiceInMemory apiKeyQueryServiceInMemory(ApiKeyCrudServiceInMemory apiKeyCrudServiceInMemory) {
        return new ApiKeyQueryServiceInMemory(apiKeyCrudServiceInMemory);
    }

    @Bean
    TriggerNotificationDomainServiceInMemory triggerNotificationDomainServiceInMemory() {
        return new TriggerNotificationDomainServiceInMemory();
    }
}

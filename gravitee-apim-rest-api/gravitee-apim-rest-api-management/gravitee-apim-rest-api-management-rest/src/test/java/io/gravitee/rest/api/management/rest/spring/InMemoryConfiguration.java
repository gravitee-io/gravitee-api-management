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

import inmemory.*;
import io.gravitee.apim.core.environment.crud_service.EnvironmentCrudService;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InMemoryConfiguration {

    @Bean
    public ApplicationCrudServiceInMemory applicationRepository() {
        return new ApplicationCrudServiceInMemory();
    }

    @Bean
    public ConnectionLogsCrudServiceInMemory connectionLogRepository() {
        return new ConnectionLogsCrudServiceInMemory();
    }

    @Bean
    public MessageLogCrudServiceInMemory messageLogRepository() {
        return new MessageLogCrudServiceInMemory();
    }

    @Bean
    public PlanCrudServiceInMemory planCrudServiceInMemory() {
        return new PlanCrudServiceInMemory();
    }

    @Bean
    public PlanQueryServiceInMemory planQueryServiceInMemory() {
        return new PlanQueryServiceInMemory();
    }

    @Bean
    public EnvironmentCrudService environmentCrudService() {
        return new EnvironmentCrudServiceInMemory();
    }

    @Bean
    public ApiQueryServiceInMemory apiQueryServiceInMemory() {
        return new ApiQueryServiceInMemory();
    }

    @Bean
    public ApiCrudServiceInMemory apiCrudServiceInMemory() {
        return new ApiCrudServiceInMemory();
    }

    @Bean
    public AuditCrudServiceInMemory auditCrudServiceInMemory() {
        return new AuditCrudServiceInMemory();
    }

    @Bean
    public UserCrudServiceInMemory userCrudServiceInMemory() {
        return new UserCrudServiceInMemory();
    }

    @Bean
    public SubscriptionCrudServiceInMemory subscriptionCrudServiceInMemory() {
        return new SubscriptionCrudServiceInMemory();
    }

    @Bean
    public SubscriptionQueryServiceInMemory subscriptionQueryServiceInMemory(
        SubscriptionCrudServiceInMemory subscriptionCrudServiceInMemory
    ) {
        return new SubscriptionQueryServiceInMemory(subscriptionCrudServiceInMemory);
    }

    @Bean
    public ApiKeyCrudServiceInMemory apiKeyCrudServiceInMemory() {
        return Mockito.spy(new ApiKeyCrudServiceInMemory());
    }

    @Bean
    public ApiKeyQueryServiceInMemory apiKeyQueryServiceInMemory(ApiKeyCrudServiceInMemory apiKeyCrudServiceInMemory) {
        return new ApiKeyQueryServiceInMemory(apiKeyCrudServiceInMemory);
    }

    @Bean
    public TriggerNotificationDomainServiceInMemory triggerNotificationDomainServiceInMemory() {
        return new TriggerNotificationDomainServiceInMemory();
    }

    @Bean
    public PageQueryServiceInMemory pageQueryServiceInMemory() {
        return new PageQueryServiceInMemory();
    }

    @Bean
    public PageCrudServiceInMemory pageCrudServiceInMemory() {
        return new PageCrudServiceInMemory();
    }

    @Bean
    public PageRevisionCrudServiceInMemory pageRevisionCrudServiceInMemory() {
        return new PageRevisionCrudServiceInMemory();
    }

    @Bean
    public InstanceQueryServiceInMemory instanceQueryServiceInMemory() {
        return new InstanceQueryServiceInMemory();
    }

    @Bean
    public EventCrudInMemory debugEventDomainServiceInMemory() {
        return new EventCrudInMemory();
    }

    @Bean
    public ParametersQueryServiceInMemory parametersQueryServiceInMemory() {
        return new ParametersQueryServiceInMemory();
    }

    @Bean
    public EntrypointPluginQueryServiceInMemory entrypointPluginQueryServiceInMemory() {
        return new EntrypointPluginQueryServiceInMemory();
    }

    @Bean
    public EndpointPluginQueryServiceInMemory endpointPluginQueryServiceInMemory() {
        return new EndpointPluginQueryServiceInMemory();
    }

    @Bean
    public FlowCrudServiceInMemory flowCrudServiceInMemory() {
        return new FlowCrudServiceInMemory();
    }

    @Bean
    public LicenseCrudServiceInMemory licenseCrudServiceInMemory() {
        return new LicenseCrudServiceInMemory();
    }

    @Bean
    public IntegrationCrudServiceInMemory integrationCrudServiceInMemory() {
        return new IntegrationCrudServiceInMemory();
    }

    @Bean
    public IntegrationQueryServiceInMemory integrationQueryServiceInMemory() {
        return new IntegrationQueryServiceInMemory();
    }
}

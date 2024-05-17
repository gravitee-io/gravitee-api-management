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
package io.gravitee.rest.api.management.v2.rest.spring;

import inmemory.*;
<<<<<<< HEAD:gravitee-apim-rest-api/gravitee-apim-rest-api-management-v2/gravitee-apim-rest-api-management-v2-rest/src/test/java/io/gravitee/rest/api/management/v2/rest/spring/InMemoryConfiguration.java
import io.gravitee.apim.core.audit.query_service.AuditEventQueryService;
import io.gravitee.apim.core.event.crud_service.EventCrudService;
import io.gravitee.apim.core.event.query_service.EventQueryService;
import io.gravitee.apim.core.gateway.query_service.InstanceQueryService;
import io.gravitee.apim.core.installation.query_service.InstallationAccessQueryService;
import io.gravitee.apim.core.search.Indexer;
=======
import io.gravitee.apim.core.api.domain_service.ApiCRDExportDomainService;
>>>>>>> afb57e3dd2 (feat: export v4 API as a kubernetes resource):gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/test/java/inmemory/spring/InMemoryConfiguration.java
import io.gravitee.apim.infra.query_service.audit.AuditEventQueryServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class InMemoryConfiguration {

    @Bean
    public ApiQueryServiceInMemory apiQueryServiceInMemory() {
        return new ApiQueryServiceInMemory();
    }

    @Bean
    public ApiCrudServiceInMemory apiCrudServiceInMemory() {
        return new ApiCrudServiceInMemory();
    }

    @Bean
    public ApiMetadataQueryServiceInMemory apiMetadataQueryServiceInMemory() {
        return new ApiMetadataQueryServiceInMemory();
    }

    @Bean
    public ApplicationCrudServiceInMemory applicationRepository() {
        return new ApplicationCrudServiceInMemory();
    }

    @Bean
    public ApiKeyCrudServiceInMemory apiKeyCrudServiceInMemory() {
        return new ApiKeyCrudServiceInMemory();
    }

    @Bean
    public ApiKeyQueryServiceInMemory apiKeyQueryServiceInMemory(ApiKeyCrudServiceInMemory apiKeyCrudServiceInMemory) {
        return new ApiKeyQueryServiceInMemory(apiKeyCrudServiceInMemory);
    }

    @Bean
    public AuditCrudServiceInMemory auditCrudServiceInMemory() {
        return new AuditCrudServiceInMemory();
    }

    @Bean
    public ConnectionLogsCrudServiceInMemory connectionLogRepository() {
        return new ConnectionLogsCrudServiceInMemory();
    }

    @Bean
    public EnvironmentCrudServiceInMemory environmentCrudServiceInMemory() {
        return new EnvironmentCrudServiceInMemory();
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
    TriggerNotificationDomainServiceInMemory triggerNotificationDomainServiceInMemory() {
        return new TriggerNotificationDomainServiceInMemory();
    }

    @Bean
    public UserCrudServiceInMemory userCrudServiceInMemory() {
        return new UserCrudServiceInMemory();
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
    public AccessPointQueryServiceInMemory accessPointQueryServiceInMemory() {
        return new AccessPointQueryServiceInMemory();
    }

    @Bean
    public InstallationAccessQueryService installationAccessServiceInMemory() {
        return new InstallationAccessQueryServiceInMemory();
    }

    @Bean
    public ParametersDomainServiceInMemory parametersDomainServiceInMemory() {
        return new ParametersDomainServiceInMemory();
    }

    @Bean
    public InstanceQueryService instanceQueryService() {
        return new InstanceQueryServiceInMemory();
    }

    @Bean
    public EventCrudService eventCrudService() {
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
    public PolicyPluginQueryServiceInMemory policyPluginQueryServiceInMemory() {
        return new PolicyPluginQueryServiceInMemory();
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
    public AuditQueryServiceInMemory auditQueryServiceInMemory(AuditCrudServiceInMemory auditCrudServiceInMemory) {
        return new AuditQueryServiceInMemory(auditCrudServiceInMemory);
    }

    @Bean
    public AuditMetadataQueryServiceInMemory auditMetadataQueryServiceInMemory() {
        return new AuditMetadataQueryServiceInMemory();
    }

    @Bean
    public AuditEventQueryService auditEventQueryService() {
        return new AuditEventQueryServiceImpl();
    }

    @Bean
    public EventQueryServiceInMemory eventQueryService() {
        return new EventQueryServiceInMemory();
    }

    @Bean
    public GroupQueryServiceInMemory groupQueryService() {
        return new GroupQueryServiceInMemory();
    }

    @Bean
    public MembershipQueryServiceInMemory membershipQueryServiceInMemory() {
        return new MembershipQueryServiceInMemory();
    }

    @Bean
    public RoleQueryServiceInMemory roleQueryServiceInMemory() {
        return new RoleQueryServiceInMemory();
    }

    @Bean
    public IndexerInMemory indexer() {
        return new IndexerInMemory();
    }

    @Bean
<<<<<<< HEAD:gravitee-apim-rest-api/gravitee-apim-rest-api-management-v2/gravitee-apim-rest-api-management-v2-rest/src/test/java/io/gravitee/rest/api/management/v2/rest/spring/InMemoryConfiguration.java
    public MembershipCrudServiceInMemory membershipCrudServiceInMemory() {
        return new MembershipCrudServiceInMemory();
=======
    public NoopTemplateResolverDomainService noopTemplateResolverDomainService() {
        return new NoopTemplateResolverDomainService();
    }

    @Bean
    public NoopSwaggerOpenApiResolver noopSwaggerOpenApiResolver() {
        return new NoopSwaggerOpenApiResolver();
    }

    @Bean
    public IntegrationCrudServiceInMemory integrationCrudService() {
        return new IntegrationCrudServiceInMemory();
    }

    @Bean
    public IntegrationQueryServiceInMemory integrationQueryService(IntegrationCrudServiceInMemory integrationCrudServiceInMemory) {
        return new IntegrationQueryServiceInMemory(integrationCrudServiceInMemory);
    }

    @Bean
    public MetadataCrudServiceInMemory metadataCrudService() {
        return new MetadataCrudServiceInMemory();
    }

    @Bean
    public IntegrationAgentInMemory integrationAgent() {
        return new IntegrationAgentInMemory();
    }

    @Bean
    public ApiCategoryQueryServiceInMemory apiCategoryQueryService() {
        return new ApiCategoryQueryServiceInMemory();
    }

    @Bean
    public PrimaryOwnerDomainServiceInMemory primaryOwnerDomainService() {
        return new PrimaryOwnerDomainServiceInMemory();
    }

    @Bean
    public ApiCRDExportDomainServiceInMemory apiCRDExportDomainService() {
        return new ApiCRDExportDomainServiceInMemory();
    }

    @Bean
    public ThemeQueryServiceInMemory themeQueryService() {
        return new ThemeQueryServiceInMemory();
>>>>>>> afb57e3dd2 (feat: export v4 API as a kubernetes resource):gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/test/java/inmemory/spring/InMemoryConfiguration.java
    }
}

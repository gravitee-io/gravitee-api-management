/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.repository.config;

import static org.mockito.Mockito.mock;

import io.gravitee.repository.config.mock.*;
import org.springframework.context.annotation.Bean;

public class MockTestRepositoryConfiguration {

    @Bean
    public TestRepositoryInitializer testRepositoryInitializer() {
        return mock(TestRepositoryInitializer.class);
    }

    @Bean
    public AuditRepositoryMock auditRepository() {
        return new AuditRepositoryMock();
    }

    @Bean
    public ApiKeyRepositoryMock apiKeyRepository() {
        return new ApiKeyRepositoryMock();
    }

    @Bean
    public ApiRepositoryMock apiRepository() {
        return new ApiRepositoryMock();
    }

    @Bean
    public ApplicationRepositoryMock applicationRepository() {
        return new ApplicationRepositoryMock();
    }

    @Bean
    public EventRepositoryMock eventRepository() {
        return new EventRepositoryMock();
    }

    @Bean
    public UserRepositoryMock userRepository() {
        return new UserRepositoryMock();
    }

    @Bean
    public CategoryRepositoryMock categoryRepository() {
        return new CategoryRepositoryMock();
    }

    @Bean
    public TagRepositoryMock tagRepository() {
        return new TagRepositoryMock();
    }

    @Bean
    public GroupRepositoryMock groupRepository() {
        return new GroupRepositoryMock();
    }

    @Bean
    public PlanRepositoryMock planRepository() {
        return new PlanRepositoryMock();
    }

    @Bean
    public MembershipRepositoryMock membershipRepository() {
        return new MembershipRepositoryMock();
    }

    @Bean
    public PageRepositoryMock pageRepository() {
        return new PageRepositoryMock();
    }

    @Bean
    public PageRevisionRepositoryMock pageRevisionRepository() {
        return new PageRevisionRepositoryMock();
    }

    @Bean
    public SubscriptionRepositoryMock subscriptionRepository() {
        return new SubscriptionRepositoryMock();
    }

    @Bean
    public TenantRepositoryMock tenantRepository() {
        return new TenantRepositoryMock();
    }

    @Bean
    public MetadataRepositoryMock metadataRepository() {
        return new MetadataRepositoryMock();
    }

    @Bean
    public RoleRepositoryMock roleRepository() {
        return new RoleRepositoryMock();
    }

    @Bean
    public RatingRepositoryMock ratingRepository() {
        return new RatingRepositoryMock();
    }

    @Bean
    public RatingAnswerRepositoryMock ratingAnswerRepository() {
        return new RatingAnswerRepositoryMock();
    }

    @Bean
    public PortalNotificationRepositoryMock notificationRepository() {
        return new PortalNotificationRepositoryMock();
    }

    @Bean
    public PortalNotificationConfigRepositoryMock portalNotificationConfigRepository() {
        return new PortalNotificationConfigRepositoryMock();
    }

    @Bean
    public GenericNotificationConfigRepositoryMock genericNotificationConfigRepository() {
        return new GenericNotificationConfigRepositoryMock();
    }

    @Bean
    public ParameterRepositoryMock parameterRepository() {
        return new ParameterRepositoryMock();
    }

    @Bean
    public DictionaryRepositoryMock dictionaryRepository() {
        return new DictionaryRepositoryMock();
    }

    @Bean
    public ApiHeaderRepositoryMock apiHeaderRepository() {
        return new ApiHeaderRepositoryMock();
    }

    @Bean
    public IdentityProviderRepositoryMock identityProviderRepository() {
        return new IdentityProviderRepositoryMock();
    }

    @Bean
    public MediaRepositoryMock mediaRepository() {
        return new MediaRepositoryMock();
    }

    @Bean
    public AlertTriggerRepositoryMock alertRepository() {
        return new AlertTriggerRepositoryMock();
    }

    @Bean
    public EntrypointRepositoryMock entrypointRepositoryMock() {
        return new EntrypointRepositoryMock();
    }

    @Bean
    public InvitationRepositoryMock invitationRepositoryMock() {
        return new InvitationRepositoryMock();
    }

    @Bean
    public ClientRegistrationProviderRepositoryMock clientRegistrationProviderRepositoryMock() {
        return new ClientRegistrationProviderRepositoryMock();
    }

    @Bean
    public CommandRepositoryMock messageRepository() {
        return new CommandRepositoryMock();
    }

    @Bean
    public WorkflowRepositoryMock workflowRepository() {
        return new WorkflowRepositoryMock();
    }

    @Bean
    public QualityRuleRepositoryMock qualityRuleRepository() throws Exception {
        return new QualityRuleRepositoryMock();
    }

    @Bean
    public ApiQualityRuleRepositoryMock apiQualityRuleRepository() throws Exception {
        return new ApiQualityRuleRepositoryMock();
    }

    @Bean
    public DashboardRepositoryMock dashboardRepositoryMock() {
        return new DashboardRepositoryMock();
    }

    @Bean
    public AlertEventRepositoryMock alertEventRepositoryMock() {
        return new AlertEventRepositoryMock();
    }

    @Bean
    public EnvironmentRepositoryMock environmentRepository() throws Exception {
        return new EnvironmentRepositoryMock();
    }

    @Bean
    public OrganizationRepositoryMock organizationRepository() throws Exception {
        return new OrganizationRepositoryMock();
    }

    @Bean
    public ThemeRepositoryMock themeRepository() throws Exception {
        return new ThemeRepositoryMock();
    }

    @Bean
    public TokenRepositoryMock tokenRepositoryMock() {
        return new TokenRepositoryMock();
    }

    @Bean
    public CustomUserFieldsRepositoryMock customUserFieldsRepositoryMock() {
        return new CustomUserFieldsRepositoryMock();
    }

    @Bean
    public IdentityProviderActivationRepositoryMock identityProviderActivationRepository() throws Exception {
        return new IdentityProviderActivationRepositoryMock();
    }

    @Bean
    public NotificationTemplateRepositoryMock notificationTemplateRepositoryMock() {
        return new NotificationTemplateRepositoryMock();
    }

    @Bean
    public TicketRepositoryMock ticketRepositoryMock() {
        return new TicketRepositoryMock();
    }

    @Bean
    public InstallationRepositoryMock installationRepositoryMock() {
        return new InstallationRepositoryMock();
    }

    @Bean
    public NodeMonitoringRepositoryMock nodeMonitoringRepositoryMock() {
        return new NodeMonitoringRepositoryMock();
    }

    @Bean
    public FlowRepositoryMock flowRepository() {
        return new FlowRepositoryMock();
    }

    @Bean
    public PromotionRepositoryMock promotionRepository() {
        return new PromotionRepositoryMock();
    }

    @Bean
    public UpgraderRepositoryMock upgraderRepository() {
        return new UpgraderRepositoryMock();
    }
}

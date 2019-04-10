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

import io.gravitee.repository.config.mock.*;
import org.springframework.context.annotation.Bean;

import static org.mockito.Mockito.mock;

public class MockTestRepositoryConfiguration {

    @Bean
    public TestRepositoryInitializer testRepositoryInitializer() {
        return mock(TestRepositoryInitializer.class);
    }

    @Bean
    public AuditRepositoryMock auditRepository() throws Exception {
        return new AuditRepositoryMock();
    }

    @Bean
    public ApiKeyRepositoryMock apiKeyRepository() throws Exception {
        return new ApiKeyRepositoryMock();
    }

    @Bean
    public ApiRepositoryMock apiRepository() throws Exception {
        return new ApiRepositoryMock();
    }

    @Bean
    public ApplicationRepositoryMock applicationRepository() throws Exception {
        return new ApplicationRepositoryMock();
    }

    @Bean
    public EventRepositoryMock eventRepository() throws Exception {
        return new EventRepositoryMock();
    }

    @Bean
    public UserRepositoryMock userRepository() throws Exception {
        return new UserRepositoryMock();
    }

    @Bean
    public ViewRepositoryMock viewRepository() throws Exception {
        return new ViewRepositoryMock();
    }

    @Bean
    public TagRepositoryMock tagRepository() throws Exception {
        return new TagRepositoryMock();
    }


    @Bean
    public GroupRepositoryMock groupRepository() throws Exception {
        return new GroupRepositoryMock();
    }

    @Bean
    public PlanRepositoryMock planRepository() throws Exception {
        return new PlanRepositoryMock();
    }

    @Bean
    public MembershipRepositoryMock membershipRepository() throws Exception {
        return new MembershipRepositoryMock();
    }

    @Bean
    public PageRepositoryMock pageRepository() throws Exception {
        return new PageRepositoryMock();
    }

    @Bean
    public SubscriptionRepositoryMock subscriptionRepository() throws Exception {
        return new SubscriptionRepositoryMock();
    }

    @Bean
    public TenantRepositoryMock tenantRepository() throws Exception {
        return new TenantRepositoryMock();
    }

    @Bean
    public MetadataRepositoryMock metadataRepository() throws Exception {
        return new MetadataRepositoryMock();
    }

    @Bean
    public RoleRepositoryMock roleRepository() throws Exception {
        return new RoleRepositoryMock();
    }

    @Bean
    public RatingRepositoryMock ratingRepository() throws Exception {
        return new RatingRepositoryMock();
    }

    @Bean
    public RatingAnswerRepositoryMock ratingAnswerRepository() throws Exception {
        return new RatingAnswerRepositoryMock();
    }

    @Bean
    public PortalNotificationRepositoryMock notificationRepository() throws Exception {
        return new PortalNotificationRepositoryMock();
    }

    @Bean
    public PortalNotificationConfigRepositoryMock portalNotificationConfigRepository() throws Exception {
        return new PortalNotificationConfigRepositoryMock();
    }

    @Bean
    public GenericNotificationConfigRepositoryMock genericNotificationConfigRepository() throws Exception {
        return new GenericNotificationConfigRepositoryMock();
    }

    @Bean
    public ParameterRepositoryMock parameterRepository() throws Exception {
        return new ParameterRepositoryMock();
    }

    @Bean
    public DictionaryRepositoryMock dictionaryRepository() throws Exception {
        return new DictionaryRepositoryMock();
    }

    @Bean
    public ApiHeaderRepositoryMock apiHeaderRepository() throws Exception {
        return new ApiHeaderRepositoryMock();
    }

    @Bean
    public IdentityProviderRepositoryMock identityProviderRepository() throws Exception {
        return new IdentityProviderRepositoryMock();
    }

    @Bean
    public MediaRepositoryMock mediaRepository() throws Exception {
        return new MediaRepositoryMock();
    }

    @Bean
    public AlertRepositoryMock alertRepository() throws Exception {
        return new AlertRepositoryMock();
    }

    @Bean
    public EntrypointRepositoryMock entrypointRepositoryMock() throws Exception {
        return new EntrypointRepositoryMock();
    }

    @Bean
    public InvitationRepositoryMock invitationRepositoryMock() throws Exception {
        return new InvitationRepositoryMock();
    }

    @Bean
    public ClientRegistrationProviderRepositoryMock clientRegistrationProviderRepositoryMock() throws Exception {
        return new ClientRegistrationProviderRepositoryMock();
    }
}

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
package io.gravitee.repository.management;

import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Ignore
@RunWith(Suite.class)
@Suite.SuiteClasses(
    {
        AccessPointRepositoryTest.class,
        AlertEventRepositoryTest.class,
        AlertRepositoryTest.class,
        ApiHeaderRepositoryTest.class,
        ApiKeyRepositoryTest.class,
        ApiQualityRuleRepositoryTest.class,
        ApiRepositoryTest.class,
        ApplicationRepositoryTest.class,
        AuditRepositoryTest.class,
        CategoryRepositoryTest.class,
        ClientRegistrationProviderRepositoryTest.class,
        CommandRepositoryTest.class,
        CustomUserFieldsRepositoryTest.class,
        DashboardRepositoryTest.class,
        DictionaryRepositoryTest.class,
        EntrypointRepositoryTest.class,
        EnvironmentRepositoryTest.class,
        EventRepositoryTest.class,
        FlowRepositoryTest.class,
        FlowV4RepositoryTest.class,
        GenericNotificationConfigRepositoryTest.class,
        GroupRepositoryTest.class,
        IdentityProviderActivationRepositoryTest.class,
        IdentityProviderRepositoryTest.class,
        InstallationRepositoryTest.class,
        IntegrationRepositoryTest.class,
        InvitationRepositoryTest.class,
        MediaRepositoryTest.class,
        MembershipRepositoryTest.class,
        MetadataRepositoryTest.class,
        NodeMonitoringRepositoryTest.class,
        NotificationTemplateRepositoryTest.class,
        OrganizationRepositoryTest.class,
        LicenseRepositoryTest.class,
        PageRepository_searchTest.class,
        PageRepositoryTest.class,
        PageRevisionRepositoryTest.class,
        ParameterRepositoryTest.class,
        PlanRepositoryTest.class,
        PortalNotificationConfigRepositoryTest.class,
        PortalNotificationRepositoryTest.class,
        PromotionRepositoryTest.class,
        QualityRuleRepositoryTest.class,
        RatingRepositoryTest.class,
        RoleRepositoryTest.class,
        SubscriptionRepositoryTest.class,
        TagRepositoryTest.class,
        TenantRepositoryTest.class,
        ThemeRepositoryTest.class,
        TicketRepositoryTest.class,
        TokenRepositoryTest.class,
        UserRepositoryTest.class,
        WorkflowRepositoryTest.class,
    }
)
public class RepositoryTestSuite {}

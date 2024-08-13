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

import io.gravitee.node.api.Monitoring;
import io.gravitee.node.api.NodeMonitoringRepository;
import io.gravitee.node.api.upgrader.UpgradeRecord;
import io.gravitee.node.api.upgrader.UpgraderRepository;
import io.gravitee.repository.config.AbstractRepositoryTest;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.*;
import io.gravitee.repository.management.model.*;
import io.gravitee.repository.management.model.flow.Flow;
import io.gravitee.repository.media.api.MediaRepository;
import jakarta.inject.Inject;
import java.util.concurrent.TimeUnit;

/**
 * @author Azize Elamrani (azize dot elamrani at gmail dot com)
 */
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
public abstract class AbstractManagementRepositoryTest extends AbstractRepositoryTest {

    @Inject
    protected ApplicationRepository applicationRepository;

    @Inject
    protected ApiRepository apiRepository;

    @Inject
    protected UserRepository userRepository;

    @Inject
    protected ApiKeyRepository apiKeyRepository;

    @Inject
    protected EventRepository eventRepository;

    @Inject
    protected EventLatestRepository eventLatestRepository;

    @Inject
    protected CategoryRepository categoryRepository;

    @Inject
    protected GroupRepository groupRepository;

    @Inject
    protected MembershipRepository membershipRepository;

    @Inject
    protected PlanRepository planRepository;

    @Inject
    protected TagRepository tagRepository;

    @Inject
    protected PageRepository pageRepository;

    @Inject
    protected PageRevisionRepository pageRevisionRepository;

    @Inject
    protected SubscriptionRepository subscriptionRepository;

    @Inject
    protected TenantRepository tenantRepository;

    @Inject
    protected MetadataRepository metadataRepository;

    @Inject
    protected RoleRepository roleRepository;

    @Inject
    protected AuditRepository auditRepository;

    @Inject
    protected RatingRepository ratingRepository;

    @Inject
    protected RatingAnswerRepository ratingAnswerRepository;

    @Inject
    protected PortalNotificationRepository portalNotificationRepository;

    @Inject
    protected PortalNotificationConfigRepository portalNotificationConfigRepository;

    @Inject
    protected GenericNotificationConfigRepository genericNotificationConfigRepository;

    @Inject
    protected ParameterRepository parameterRepository;

    @Inject
    protected DictionaryRepository dictionaryRepository;

    @Inject
    protected ApiHeaderRepository apiHeaderRepository;

    @Inject
    protected CommandRepository commandRepository;

    @Inject
    protected MediaRepository mediaRepository;

    @Inject
    protected IdentityProviderRepository identityProviderRepository;

    @Inject
    protected AlertTriggerRepository alertRepository;

    @Inject
    protected EntrypointRepository entrypointRepository;

    @Inject
    protected InvitationRepository invitationRepository;

    @Inject
    protected ClientRegistrationProviderRepository clientRegistrationProviderRepository;

    @Inject
    protected WorkflowRepository workflowRepository;

    @Inject
    protected QualityRuleRepository qualityRuleRepository;

    @Inject
    protected ApiQualityRuleRepository apiQualityRuleRepository;

    @Inject
    protected DashboardRepository dashboardRepository;

    @Inject
    protected AlertEventRepository alertEventRepository;

    @Inject
    protected EnvironmentRepository environmentRepository;

    @Inject
    protected OrganizationRepository organizationRepository;

    @Inject
    protected LicenseRepository licenseRepository;

    @Inject
    protected ThemeRepository themeRepository;

    @Inject
    protected TokenRepository tokenRepository;

    @Inject
    protected CustomUserFieldsRepository customUserFieldsRepository;

    @Inject
    protected IdentityProviderActivationRepository identityProviderActivationRepository;

    @Inject
    protected NotificationTemplateRepository notificationTemplateRepository;

    @Inject
    protected TicketRepository ticketRepository;

    @Inject
    protected InstallationRepository installationRepository;

    @Inject
    protected NodeMonitoringRepository nodeMonitoringRepository;

    @Inject
    protected FlowRepository flowRepository;

    @Inject
    protected PromotionRepository promotionRepository;

    @Inject
    protected UpgraderRepository upgraderRepository;

    @Inject
    protected AccessPointRepository accessPointRepository;

    @Inject
    protected IntegrationRepository integrationRepository;

    @Inject
    protected IntegrationJobRepository integrationJobRepository;

    @Inject
    protected ApiCategoryOrderRepository apiCategoryOrderRepository;

    @Inject
    protected SharedPolicyGroupRepository sharedPolicyGroupRepository;

    @Inject
    protected SharedPolicyGroupHistoryRepository sharedPolicyGroupHistoryRepository;

    protected void createModel(Object object) throws TechnicalException {
        if (object instanceof Application application) {
            applicationRepository.create(application);
        } else if (object instanceof Api api) {
            apiRepository.create(api);
        } else if (object instanceof User user) {
            userRepository.create(user);
        } else if (object instanceof Event event) {
            eventRepository.create(event);
        } else if (object instanceof ApiKey apiKey) {
            apiKeyRepository.create(apiKey);
        } else if (object instanceof Category category) {
            categoryRepository.create(category);
        } else if (object instanceof Group group) {
            groupRepository.create(group);
        } else if (object instanceof Membership membership) {
            membershipRepository.create(membership);
        } else if (object instanceof Plan plan) {
            planRepository.create(plan);
        } else if (object instanceof Tag tag) {
            tagRepository.create(tag);
        } else if (object instanceof Page page) {
            pageRepository.create(page);
        } else if (object instanceof Subscription subscription) {
            subscriptionRepository.create(subscription);
        } else if (object instanceof Tenant tenant) {
            tenantRepository.create(tenant);
        } else if (object instanceof Metadata metadata) {
            metadataRepository.create(metadata);
        } else if (object instanceof Role role) {
            roleRepository.create(role);
        } else if (object instanceof Audit audit) {
            auditRepository.create(audit);
        } else if (object instanceof Rating rating) {
            ratingRepository.create(rating);
        } else if (object instanceof RatingAnswer ratingAnswer) {
            ratingAnswerRepository.create(ratingAnswer);
        } else if (object instanceof PortalNotification portalNotification) {
            portalNotificationRepository.create(portalNotification);
        } else if (object instanceof PortalNotificationConfig portalNotificationConfig) {
            portalNotificationConfigRepository.create(portalNotificationConfig);
        } else if (object instanceof GenericNotificationConfig genericNotificationConfig) {
            genericNotificationConfigRepository.create(genericNotificationConfig);
        } else if (object instanceof Parameter parameter) {
            parameterRepository.create(parameter);
        } else if (object instanceof Dictionary dictionary) {
            dictionaryRepository.create(dictionary);
        } else if (object instanceof ApiHeader apiHeader) {
            apiHeaderRepository.create(apiHeader);
        } else if (object instanceof Command command) {
            commandRepository.create(command);
        } else if (object instanceof IdentityProvider identityProvider) {
            identityProviderRepository.create(identityProvider);
        } else if (object instanceof AlertTrigger alertTrigger) {
            alertRepository.create(alertTrigger);
        } else if (object instanceof Entrypoint entrypoint) {
            entrypointRepository.create(entrypoint);
        } else if (object instanceof Invitation invitation) {
            invitationRepository.create(invitation);
        } else if (object instanceof ClientRegistrationProvider clientRegistrationProvider) {
            clientRegistrationProviderRepository.create(clientRegistrationProvider);
        } else if (object instanceof Workflow workflow) {
            workflowRepository.create(workflow);
        } else if (object instanceof QualityRule qualityRule) {
            qualityRuleRepository.create(qualityRule);
        } else if (object instanceof ApiQualityRule apiQualityRule) {
            apiQualityRuleRepository.create(apiQualityRule);
        } else if (object instanceof Dashboard apiQualityRule) {
            dashboardRepository.create(apiQualityRule);
        } else if (object instanceof AlertEvent alertEvent) {
            alertEventRepository.create(alertEvent);
        } else if (object instanceof Environment environment) {
            environmentRepository.create(environment);
        } else if (object instanceof Organization organization) {
            organizationRepository.create(organization);
        } else if (object instanceof License license) {
            licenseRepository.create(license);
        } else if (object instanceof Theme theme) {
            themeRepository.create(theme);
        } else if (object instanceof IdentityProviderActivation identityProviderActivation) {
            identityProviderActivationRepository.create(identityProviderActivation);
        } else if (object instanceof Token token) {
            tokenRepository.create(token);
        } else if (object instanceof PageRevision pageRevision) {
            pageRevisionRepository.create(pageRevision);
        } else if (object instanceof CustomUserField customUserField) {
            customUserFieldsRepository.create(customUserField);
        } else if (object instanceof NotificationTemplate notificationTemplate) {
            notificationTemplateRepository.create(notificationTemplate);
        } else if (object instanceof Ticket ticket) {
            ticketRepository.create(ticket);
        } else if (object instanceof Installation installation) {
            installationRepository.create(installation);
        } else if (object instanceof Monitoring monitoring) {
            try {
                nodeMonitoringRepository.create(monitoring).test().await(15, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        } else if (object instanceof Flow flow) {
            flowRepository.create(flow);
        } else if (object instanceof Promotion promotion) {
            promotionRepository.create(promotion);
        } else if (object instanceof UpgradeRecord upgradeRecord) {
            upgraderRepository.create(upgradeRecord);
        } else if (object instanceof AccessPoint accessPoint) {
            accessPointRepository.create(accessPoint);
        } else if (object instanceof Integration integration) {
            integrationRepository.create(integration);
        } else if (object instanceof IntegrationJob job) {
            integrationJobRepository.create(job);
        } else if (object instanceof ApiCategoryOrder apiCategoryOrder) {
            apiCategoryOrderRepository.create(apiCategoryOrder);
        } else if (object instanceof SharedPolicyGroup sharedPolicyGroup) {
            sharedPolicyGroupRepository.create(sharedPolicyGroup);
        }
    }

    protected String getModelPackage() {
        return "io.gravitee.repository.management.model.";
    }
}

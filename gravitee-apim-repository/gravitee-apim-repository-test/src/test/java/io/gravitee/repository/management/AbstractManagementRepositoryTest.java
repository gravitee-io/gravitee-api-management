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
package io.gravitee.repository.management;

import io.gravitee.node.api.Monitoring;
import io.gravitee.node.api.NodeMonitoringRepository;
import io.gravitee.repository.config.AbstractRepositoryTest;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.*;
import io.gravitee.repository.management.model.*;
import io.gravitee.repository.management.model.flow.Flow;
import io.gravitee.repository.media.api.MediaRepository;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;

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

    protected void createModel(Object object) throws TechnicalException {
        if (object instanceof Application) {
            applicationRepository.create((Application) object);
        } else if (object instanceof Api) {
            apiRepository.create((Api) object);
        } else if (object instanceof User) {
            userRepository.create((User) object);
        } else if (object instanceof Event) {
            eventRepository.create((Event) object);
        } else if (object instanceof ApiKey) {
            final ApiKey apiKey = (ApiKey) object;
            apiKeyRepository.create(apiKey);
        } else if (object instanceof Category) {
            categoryRepository.create((Category) object);
        } else if (object instanceof Group) {
            groupRepository.create((Group) object);
        } else if (object instanceof Membership) {
            membershipRepository.create((Membership) object);
        } else if (object instanceof Plan) {
            planRepository.create((Plan) object);
        } else if (object instanceof Tag) {
            tagRepository.create((Tag) object);
        } else if (object instanceof Page) {
            pageRepository.create((Page) object);
        } else if (object instanceof Subscription) {
            subscriptionRepository.create((Subscription) object);
        } else if (object instanceof Tenant) {
            tenantRepository.create((Tenant) object);
        } else if (object instanceof Metadata) {
            metadataRepository.create((Metadata) object);
        } else if (object instanceof Role) {
            roleRepository.create((Role) object);
        } else if (object instanceof Audit) {
            auditRepository.create((Audit) object);
        } else if (object instanceof Rating) {
            ratingRepository.create((Rating) object);
        } else if (object instanceof RatingAnswer) {
            ratingAnswerRepository.create((RatingAnswer) object);
        } else if (object instanceof PortalNotification) {
            portalNotificationRepository.create((PortalNotification) object);
        } else if (object instanceof PortalNotificationConfig) {
            portalNotificationConfigRepository.create((PortalNotificationConfig) object);
        } else if (object instanceof GenericNotificationConfig) {
            genericNotificationConfigRepository.create((GenericNotificationConfig) object);
        } else if (object instanceof Parameter) {
            parameterRepository.create((Parameter) object);
        } else if (object instanceof Dictionary) {
            dictionaryRepository.create((Dictionary) object);
        } else if (object instanceof ApiHeader) {
            apiHeaderRepository.create((ApiHeader) object);
        } else if (object instanceof Command) {
            commandRepository.create((Command) object);
        } else if (object instanceof IdentityProvider) {
            identityProviderRepository.create((IdentityProvider) object);
        } else if (object instanceof AlertTrigger) {
            alertRepository.create((AlertTrigger) object);
        } else if (object instanceof Entrypoint) {
            entrypointRepository.create((Entrypoint) object);
        } else if (object instanceof Invitation) {
            invitationRepository.create((Invitation) object);
        } else if (object instanceof ClientRegistrationProvider) {
            clientRegistrationProviderRepository.create((ClientRegistrationProvider) object);
        } else if (object instanceof Workflow) {
            workflowRepository.create((Workflow) object);
        } else if (object instanceof QualityRule) {
            qualityRuleRepository.create((QualityRule) object);
        } else if (object instanceof ApiQualityRule) {
            apiQualityRuleRepository.create((ApiQualityRule) object);
        } else if (object instanceof Dashboard) {
            dashboardRepository.create((Dashboard) object);
        } else if (object instanceof AlertEvent) {
            alertEventRepository.create((AlertEvent) object);
        } else if (object instanceof Environment) {
            environmentRepository.create((Environment) object);
        } else if (object instanceof Organization) {
            organizationRepository.create((Organization) object);
        } else if (object instanceof Theme) {
            themeRepository.create((Theme) object);
        } else if (object instanceof IdentityProviderActivation) {
            identityProviderActivationRepository.create((IdentityProviderActivation) object);
        } else if (object instanceof Token) {
            tokenRepository.create((Token) object);
        } else if (object instanceof PageRevision) {
            pageRevisionRepository.create((PageRevision) object);
        } else if (object instanceof CustomUserField) {
            customUserFieldsRepository.create((CustomUserField) object);
        } else if (object instanceof NotificationTemplate) {
            notificationTemplateRepository.create((NotificationTemplate) object);
        } else if (object instanceof Ticket) {
            ticketRepository.create((Ticket) object);
        } else if (object instanceof Installation) {
            installationRepository.create((Installation) object);
        } else if (object instanceof Monitoring) {
            nodeMonitoringRepository.create((Monitoring) object).test().awaitTerminalEvent(15, TimeUnit.SECONDS);
        } else if (object instanceof Flow) {
            flowRepository.create((Flow) object);
        } else if (object instanceof Promotion) {
            promotionRepository.create((Promotion) object);
        }
    }

    protected String getModelPackage() {
        return "io.gravitee.repository.management.model.";
    }
}

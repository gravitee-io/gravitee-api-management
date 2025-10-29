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
    protected ScoringReportRepository scoringReportRepository;

    @Inject
    protected ScoringRulesetRepository scoringRulesetRepository;

    @Inject
    protected ScoringFunctionRepository scoringFunctionRepository;

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
    protected AsyncJobRepository asyncJobRepository;

    @Inject
    protected ApiCategoryOrderRepository apiCategoryOrderRepository;

    @Inject
    protected SharedPolicyGroupRepository sharedPolicyGroupRepository;

    @Inject
    protected SharedPolicyGroupHistoryRepository sharedPolicyGroupHistoryRepository;

    @Inject
    protected PortalMenuLinkRepository portalMenuLinkRepository;

    @Inject
    protected ClusterRepository clusterRepository;

    @Inject
    protected PortalPageRepository portalPageRepository;

    @Inject
    protected PortalPageContextRepository portalPageContextRepository;

    @Inject
    protected PortalNavigationItemRepository portalNavigationItemRepository;

    @Inject
    protected PortalPageContentRepository portalPageContentRepository;

    protected void createModel(Object object) throws TechnicalException {
        switch (object) {
            case Application application -> applicationRepository.create(application);
            case Api api -> apiRepository.create(api);
            case User user -> userRepository.create(user);
            case Event event -> eventRepository.create(event);
            case ApiKey apiKey -> apiKeyRepository.create(apiKey);
            case Category category -> categoryRepository.create(category);
            case Group group -> groupRepository.create(group);
            case Membership membership -> membershipRepository.create(membership);
            case Plan plan -> planRepository.create(plan);
            case Tag tag -> tagRepository.create(tag);
            case Page page -> pageRepository.create(page);
            case Subscription subscription -> subscriptionRepository.create(subscription);
            case Tenant tenant -> tenantRepository.create(tenant);
            case Metadata metadata -> metadataRepository.create(metadata);
            case Role role -> roleRepository.create(role);
            case Audit audit -> auditRepository.create(audit);
            case Rating rating -> ratingRepository.create(rating);
            case RatingAnswer ratingAnswer -> ratingAnswerRepository.create(ratingAnswer);
            case PortalNotification portalNotification -> portalNotificationRepository.create(portalNotification);
            case PortalNotificationConfig portalNotificationConfig -> portalNotificationConfigRepository.create(portalNotificationConfig);
            case GenericNotificationConfig genericNotificationConfig -> genericNotificationConfigRepository.create(
                genericNotificationConfig
            );
            case Parameter parameter -> parameterRepository.create(parameter);
            case Dictionary dictionary -> dictionaryRepository.create(dictionary);
            case ApiHeader apiHeader -> apiHeaderRepository.create(apiHeader);
            case Command command -> commandRepository.create(command);
            case IdentityProvider identityProvider -> identityProviderRepository.create(identityProvider);
            case AlertTrigger alertTrigger -> alertRepository.create(alertTrigger);
            case Entrypoint entrypoint -> entrypointRepository.create(entrypoint);
            case Invitation invitation -> invitationRepository.create(invitation);
            case ClientRegistrationProvider clientRegistrationProvider -> clientRegistrationProviderRepository.create(
                clientRegistrationProvider
            );
            case Workflow workflow -> workflowRepository.create(workflow);
            case QualityRule qualityRule -> qualityRuleRepository.create(qualityRule);
            case ApiQualityRule apiQualityRule -> apiQualityRuleRepository.create(apiQualityRule);
            case ScoringReport scoringReport -> scoringReportRepository.create(scoringReport);
            case ScoringRuleset scoringRuleset -> scoringRulesetRepository.create(scoringRuleset);
            case ScoringFunction scoringFunction -> scoringFunctionRepository.create(scoringFunction);
            case Dashboard apiQualityRule -> dashboardRepository.create(apiQualityRule);
            case AlertEvent alertEvent -> alertEventRepository.create(alertEvent);
            case Environment environment -> environmentRepository.create(environment);
            case Organization organization -> organizationRepository.create(organization);
            case License license -> licenseRepository.create(license);
            case Theme theme -> themeRepository.create(theme);
            case IdentityProviderActivation identityProviderActivation -> identityProviderActivationRepository.create(
                identityProviderActivation
            );
            case Token token -> tokenRepository.create(token);
            case PageRevision pageRevision -> pageRevisionRepository.create(pageRevision);
            case CustomUserField customUserField -> customUserFieldsRepository.create(customUserField);
            case NotificationTemplate notificationTemplate -> notificationTemplateRepository.create(notificationTemplate);
            case Ticket ticket -> ticketRepository.create(ticket);
            case Installation installation -> installationRepository.create(installation);
            case Monitoring monitoring -> {
                try {
                    nodeMonitoringRepository.create(monitoring).test().await(15, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            case Flow flow -> flowRepository.create(flow);
            case Promotion promotion -> promotionRepository.create(promotion);
            case UpgradeRecord upgradeRecord -> upgraderRepository.create(upgradeRecord);
            case AccessPoint accessPoint -> accessPointRepository.create(accessPoint);
            case Integration integration -> integrationRepository.create(integration);
            case AsyncJob job -> asyncJobRepository.create(job);
            case ApiCategoryOrder apiCategoryOrder -> apiCategoryOrderRepository.create(apiCategoryOrder);
            case SharedPolicyGroup sharedPolicyGroup -> sharedPolicyGroupRepository.create(sharedPolicyGroup);
            case PortalMenuLink portalMenuLink -> portalMenuLinkRepository.create(portalMenuLink);
            case Cluster cluster -> clusterRepository.create(cluster);
            case PortalPage portalPage -> portalPageRepository.create(portalPage);
            case PortalPageContext portalPageContext -> portalPageContextRepository.create(portalPageContext);
            case io.gravitee.repository.management.model.PortalNavigationItem portalNavigationItem -> portalNavigationItemRepository.create(
                portalNavigationItem
            );
            case io.gravitee.repository.management.model.PortalPageContent portalPageContent -> portalPageContentRepository.create(
                portalPageContent
            );
            case null, default -> {}
        }
    }

    protected String getModelPackage() {
        return "io.gravitee.repository.management.model.";
    }
}

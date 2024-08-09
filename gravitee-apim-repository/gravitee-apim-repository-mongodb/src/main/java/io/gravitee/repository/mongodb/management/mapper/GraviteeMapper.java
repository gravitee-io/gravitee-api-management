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
package io.gravitee.repository.mongodb.management.mapper;

import io.gravitee.node.api.Monitoring;
import io.gravitee.node.api.upgrader.UpgradeRecord;
import io.gravitee.repository.management.model.*;
import io.gravitee.repository.management.model.flow.Flow;
import io.gravitee.repository.mongodb.management.internal.model.*;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.mapstruct.*;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@Mapper
public interface GraviteeMapper {
    // AlertEvent mapping
    AlertEvent map(AlertEventMongo toMap);

    AlertEventMongo map(AlertEvent toMap);

    List<AlertEvent> mapAlertEvents(Collection<AlertEventMongo> toMap);

    // AlertTrigger mapping
    AlertTrigger map(AlertTriggerMongo toMap);

    AlertTriggerMongo map(AlertTrigger toMap);

    // ApiHeader mapping
    ApiHeader map(ApiHeaderMongo toMap);

    ApiHeaderMongo map(ApiHeader toMap);

    // ApiKey mapping
    ApiKey map(ApiKeyMongo toMap);

    ApiKeyMongo map(ApiKey toMap);

    List<ApiKey> mapApiKeys(Collection<ApiKeyMongo> toMap);

    // ApiQualityRule mapping
    @Mapping(target = "api", source = "id.api")
    @Mapping(target = "qualityRule", source = "id.qualityRule")
    ApiQualityRule map(ApiQualityRuleMongo toMap);

    @InheritInverseConfiguration
    ApiQualityRuleMongo map(ApiQualityRule toMap);

    // Api mapping
    Api map(ApiMongo toMap);

    ApiMongo map(Api toMap);

    List<Api> mapApis(Collection<ApiMongo> toMap);

    // Application mapping
    Application map(ApplicationMongo toMap);

    ApplicationMongo map(Application toMap);

    // Audit mapping
    Audit map(AuditMongo toMap);

    AuditMongo map(Audit toMap);

    List<Audit> mapAudits(Collection<AuditMongo> toMap);

    // ApiCategoryOrder mapping
    @Mapping(target = "apiId", source = "id.apiId")
    @Mapping(target = "categoryId", source = "id.categoryId")
    ApiCategoryOrder map(ApiCategoryOrderMongo toMap);

    @InheritInverseConfiguration
    ApiCategoryOrderMongo map(ApiCategoryOrder toMap);

    Set<ApiCategoryOrder> map(Collection<ApiCategoryOrderMongo> toMap);

    // Category mapping
    Category map(CategoryMongo toMap);

    CategoryMongo map(Category toMap);

    // ClientRegistrationProvider mapping
    ClientRegistrationProvider map(ClientRegistrationProviderMongo toMap);

    ClientRegistrationProviderMongo map(ClientRegistrationProvider toMap);

    Set<ClientRegistrationProvider> mapClientRegistrationProviders(Collection<ClientRegistrationProviderMongo> toMap);

    // Command mapping
    Command map(CommandMongo toMap);

    CommandMongo map(Command toMap);

    List<Command> mapCommands(Collection<CommandMongo> toMap);

    // CustomUserField mapping
    @Mapping(target = "key", source = "id.key")
    @Mapping(target = "referenceId", source = "id.referenceId")
    @Mapping(target = "referenceType", source = "id.referenceType")
    CustomUserField map(CustomUserFieldMongo toMap);

    @InheritInverseConfiguration
    CustomUserFieldMongo map(CustomUserField toMap);

    // CustomDomain mapping
    AccessPoint map(AccessPointMongo toMap);

    AccessPointMongo map(AccessPoint toMap);

    // Dashboard mapping
    Dashboard map(DashboardMongo toMap);

    DashboardMongo map(Dashboard toMap);

    Set<Dashboard> mapDashboards(Collection<DashboardMongo> toMap);

    // Dictionary mapping
    Dictionary map(DictionaryMongo toMap);

    DictionaryMongo map(Dictionary toMap);

    Set<Dictionary> mapDictionaries(Collection<DictionaryMongo> toMap);

    // Entrypoint mapping
    Entrypoint map(EntrypointMongo toMap);

    EntrypointMongo map(Entrypoint toMap);

    // Environment mapping
    Environment map(EnvironmentMongo toMap);

    EnvironmentMongo map(Environment toMap);

    // Event mapping
    Event map(EventMongo toMap);

    EventMongo map(Event toMap);

    List<Event> mapEvents(Collection<EventMongo> toMap);

    List<Event> mapEventLatests(Collection<EventLatestMongo> toMap);

    EventLatestMongo mapEvent(Event toMap);

    // Flow mapping
    Flow map(FlowMongo toMap);

    FlowMongo map(Flow toMap);

    // Group mapping
    Group map(GroupMongo toMap);

    GroupMongo map(Group toMap);

    // IdentityProvider mapping
    IdentityProvider map(IdentityProviderMongo toMap);

    IdentityProviderMongo map(IdentityProvider toMap);

    // Installation mapping
    Installation map(InstallationMongo toMap);

    InstallationMongo map(Installation toMap);

    // Integration mapping
    Integration map(IntegrationMongo integrationMongo);

    IntegrationMongo map(Integration toMap);

    // Invitation mapping
    Invitation map(InvitationMongo toMap);

    InvitationMongo map(Invitation toMap);

    // IntegrationJob mapping
    IntegrationJob map(IntegrationJobMongo source);

    IntegrationJobMongo map(IntegrationJob source);

    // License mapping
    @Mapping(target = "referenceId", source = "id.referenceId")
    @Mapping(target = "referenceType", source = "id.referenceType")
    License map(LicenseMongo toMap);

    @InheritInverseConfiguration
    LicenseMongo map(License toMap);

    List<License> mapLicenses(Collection<LicenseMongo> toMap);

    // NodeMonitoring mapping
    Monitoring map(MonitoringMongo toMap);

    MonitoringMongo map(Monitoring toMap);

    // NotificationTemplate mapping
    NotificationTemplate map(NotificationTemplateMongo toMap);

    NotificationTemplateMongo map(NotificationTemplate toMap);

    Set<NotificationTemplate> mapNotificationTemplates(Collection<NotificationTemplateMongo> toMap);

    // Organization mapping
    Organization map(OrganizationMongo toMap);

    OrganizationMongo map(Organization toMap);

    // Page mapping
    @Mapping(target = "visibility", source = "visibility", defaultValue = "PUBLIC")
    Page map(PageMongo toMap);

    @Mapping(target = "visibility", source = "visibility", defaultValue = "PUBLIC")
    PageMongo map(Page toMap);

    @InheritConfiguration
    List<Page> mapPages(Collection<PageMongo> toMap);

    // PageRevision mapping
    @Mapping(target = "pageId", source = "id.pageId")
    @Mapping(target = "revision", source = "id.revision")
    PageRevision map(PageRevisionMongo toMap);

    @InheritInverseConfiguration
    PageRevisionMongo map(PageRevision toMap);

    // Plan mapping
    Plan map(PlanMongo toMap);

    PlanMongo map(Plan toMap);

    // PortalNotification mapping
    PortalNotification map(PortalNotificationMongo toMap);

    PortalNotificationMongo map(PortalNotification toMap);

    // Promotion mapping
    Promotion map(PromotionMongo toMap);

    PromotionMongo map(Promotion toMap);

    List<Promotion> mapPromotions(Collection<PromotionMongo> toMap);

    // QualityRule mapping
    QualityRule map(QualityRuleMongo toMap);

    QualityRuleMongo map(QualityRule toMap);

    // Subscription mapping
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "generalConditionsContentPageId", source = "generalConditionsContentRevision.pageId")
    @Mapping(target = "generalConditionsContentRevision", source = "generalConditionsContentRevision.revision")
    Subscription map(SubscriptionMongo toMap);

    @InheritInverseConfiguration
    SubscriptionMongo map(Subscription toMap);

    @InheritConfiguration
    List<Subscription> mapSubscriptions(Collection<SubscriptionMongo> toMap);

    // Tag mapping
    Tag map(TagMongo toMap);

    TagMongo map(Tag toMap);

    // Tenant mapping
    Tenant map(TenantMongo toMap);

    TenantMongo map(Tenant toMap);

    // Theme mapping
    Theme map(ThemeMongo toMap);

    ThemeMongo map(Theme toMap);

    // Ticket mapping
    Ticket map(TicketMongo toMap);

    TicketMongo map(Ticket toMap);

    List<Ticket> mapTickets(Collection<TicketMongo> toMap);

    // Token mapping
    Token map(TokenMongo toMap);

    TokenMongo map(Token toMap);

    // UpgradeRecord mapping
    UpgradeRecord map(UpgradeRecordMongo toMap);

    UpgradeRecordMongo map(UpgradeRecord toMap);

    // User mapping
    User map(UserMongo toMap);

    UserMongo map(User toMap);

    List<User> mapUserList(Collection<UserMongo> toMap);

    Set<User> mapUsers(Collection<UserMongo> toMap);

    // Workflow mapping
    Workflow map(WorkflowMongo toMap);

    WorkflowMongo map(Workflow toMap);

    List<Integration> mapIntegrationsList(Collection<IntegrationMongo> toMap);

    // SharedPolicyGroup mapping
    SharedPolicyGroupMongo map(SharedPolicyGroup item);
    SharedPolicyGroup map(SharedPolicyGroupMongo item);

    // SharedPolicyGroupHistory mapping
    SharedPolicyGroupHistoryMongo mapHistory(SharedPolicyGroup item);
    SharedPolicyGroup mapHistory(SharedPolicyGroupHistoryMongo item);
}
